package com.meongcare.app.data.server

import java.io.IOException
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.json.JSONException
import org.json.JSONObject

/** 익명 계정: Firestore 요청에 붙이는 토큰과 uid (환자 건의 `createdBy`). */
data class AuthSession(val idToken: String, val uid: String)

/**
 * Firebase 익명 로그인 (Identity Toolkit REST `accounts:signUp`). 한 번 가입하면 프로세스 동안 같은 계정을 쓴다.
 * 한 번에 하나만 가입한다 ([Mutex]). 실패하면 null이고, 다음에 부르면 다시 시도한다.
 *
 * 토큰은 약 1시간 유효로 안다. 갱신은 아직 없다 (계획 012 있으면 좋음 C): 그 전에는 오래 안내하면 갱신이 실패할 수 있다.
 * 토큰과 uid는 로그에 남기지 않는다.
 */
class AnonymousAuth(private val config: ServerConfig, private val transport: HttpTransport) {
    private val mutex = Mutex()
    private var session: AuthSession? = null

    suspend fun session(): AuthSession? = mutex.withLock {
        session ?: signUp()?.also { session = it }
    }

    private suspend fun signUp(): AuthSession? {
        val response = try {
            transport.send(
                HttpRequest(
                    method = "POST",
                    url = config.signUpUrl,
                    headers = mapOf("Content-Type" to "application/json"),
                    body = JSONObject().put("returnSecureToken", true).toString(),
                ),
            )
        } catch (_: IOException) {
            return null
        }
        if (!response.isSuccessful) return null
        return try {
            val json = JSONObject(response.body)
            val token = json.optString("idToken").takeIf { it.isNotEmpty() } ?: return null
            val uid = json.optString("localId").takeIf { it.isNotEmpty() } ?: return null
            AuthSession(token, uid)
        } catch (_: JSONException) {
            null
        }
    }
}
