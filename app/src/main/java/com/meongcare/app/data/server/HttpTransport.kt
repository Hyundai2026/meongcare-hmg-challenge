package com.meongcare.app.data.server

/** HTTP 요청 하나. 몸통은 JSON 문자열이다. */
data class HttpRequest(
    val method: String,
    val url: String,
    val headers: Map<String, String> = emptyMap(),
    val body: String? = null,
)

/** HTTP 응답. [status]가 200번대가 아니어도 예외가 아니라 이 값으로 온다. */
data class HttpResponse(val status: Int, val body: String) {
    val isSuccessful: Boolean get() = status in 200..299
}

/**
 * HTTP를 보내는 경계 (계획 012 "설계 결정 3"). 진짜 구현은 [UrlConnectionHttpTransport] 하나다.
 * 테스트는 가짜를 넣어 JVM에서 돈다.
 *
 * 네트워크 실패(연결 안 됨, 시간 초과)는 `java.io.IOException`을 던진다. 부르는 쪽이 결과 값으로 바꾼다.
 * 구현은 로그를 남기지 않는다 (요청·응답에 개인정보와 토큰이 있다).
 */
fun interface HttpTransport {
    suspend fun send(request: HttpRequest): HttpResponse
}
