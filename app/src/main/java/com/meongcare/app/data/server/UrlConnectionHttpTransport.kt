package com.meongcare.app.data.server

import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.asExecutor
import kotlinx.coroutines.suspendCancellableCoroutine

/**
 * [HttpTransport]의 진짜 구현: 자바 기본 `HttpURLConnection` (런타임 의존성 없음, 계획 012 "설계 결정 3").
 * `java.net`은 이 파일에서만 쓴다.
 *
 * - 연결·읽기 시간 초과는 각각 [timeoutMillis]. 실패는 모두 `java.io.IOException`으로 던진다 (그 밖의 예외도 감싼다).
 * - **부른 코루틴이 취소되면 바로 돌아오고 연결을 닫는다.** 막힌 소켓 읽기는 코루틴 취소로 끊기지 않아서,
 *   I/O는 별도 스레드에서 돌리고 취소되면 `disconnect()`로 소켓을 닫는다. 그래서 시연 모드의 5초 대체와
 *   Real 모드의 5초 시간 초과가 제때 끝난다.
 * - **로그를 남기지 않는다** (요청에 토큰과 반려견 정보가 있다).
 * - 평문 HTTP는 debug 빌드의 에뮬레이터 주소에만 허용된다 (app/src/debug의 network_security_config).
 */
class UrlConnectionHttpTransport(private val timeoutMillis: Int = 5_000) : HttpTransport {

    override suspend fun send(request: HttpRequest): HttpResponse {
        val connection = try {
            URL(request.url).openConnection() as HttpURLConnection
        } catch (e: Exception) {
            throw e as? IOException ?: IOException(e)
        }
        return suspendCancellableCoroutine { continuation ->
            continuation.invokeOnCancellation { connection.disconnect() }
            Dispatchers.IO.asExecutor().execute {
                val result = runCatching { exchange(connection, request) }
                connection.disconnect()
                // 이미 취소된 continuation에 resume하면 무시된다
                result.fold(
                    onSuccess = { continuation.resume(it) },
                    onFailure = { continuation.resumeWithException(it as? IOException ?: IOException(it)) },
                )
            }
        }
    }

    private fun exchange(connection: HttpURLConnection, request: HttpRequest): HttpResponse {
        connection.requestMethod = request.method
        connection.connectTimeout = timeoutMillis
        connection.readTimeout = timeoutMillis
        connection.useCaches = false
        request.headers.forEach { (name, value) -> connection.setRequestProperty(name, value) }
        request.body?.let { body ->
            connection.doOutput = true
            connection.outputStream.use { it.write(body.toByteArray(Charsets.UTF_8)) }
        }
        val status = connection.responseCode
        val stream = if (status in 200..299) connection.inputStream else connection.errorStream
        val text = stream?.use { it.readBytes().toString(Charsets.UTF_8) }.orEmpty()
        return HttpResponse(status, text)
    }
}
