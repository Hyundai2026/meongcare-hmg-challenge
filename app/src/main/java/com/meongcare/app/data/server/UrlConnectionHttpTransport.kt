package com.meongcare.app.data.server

import java.net.HttpURLConnection
import java.net.URL
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * [HttpTransport]의 진짜 구현: 자바 기본 `HttpURLConnection` (런타임 의존성 없음, 계획 012 "설계 결정 3").
 * `java.net`은 이 파일에서만 쓴다.
 *
 * - 연결·읽기 시간 초과는 각각 [timeoutMillis]. 네트워크 실패는 `java.io.IOException`으로 던진다.
 * - 막힌 소켓 읽기는 코루틴 취소로 바로 끊기지 않는다. 그래서 시연 모드의 5초 대체(FallbackHospitalBoardSource)가
 *   이 시간 초과만큼 늦어질 수 있다 (PC 에뮬레이터가 꺼져 있으면 연결이 바로 거부돼 빠르다).
 * - **로그를 남기지 않는다** (요청에 토큰과 반려견 정보가 있다).
 * - 평문 HTTP는 debug 빌드의 에뮬레이터 주소에만 허용된다 (app/src/debug의 network_security_config).
 */
class UrlConnectionHttpTransport(private val timeoutMillis: Int = 5_000) : HttpTransport {

    override suspend fun send(request: HttpRequest): HttpResponse = withContext(Dispatchers.IO) {
        val connection = URL(request.url).openConnection() as HttpURLConnection
        try {
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
            HttpResponse(status, text)
        } finally {
            connection.disconnect()
        }
    }
}
