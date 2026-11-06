package com.meongcare.app.data.server

import java.io.IOException
import java.time.Instant
import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes
import kotlin.time.toJavaDuration
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.json.JSONException
import org.json.JSONObject

/** 익명 계정: Firestore 요청에 붙이는 토큰과 uid (환자 건의 `createdBy`). */
data class AuthSession(val idToken: String, val uid: String)

/**
 * Firebase 익명 로그인 (Identity Toolkit REST `accounts:signUp`). 한 번 가입하면 프로세스 동안 같은 계정을 쓴다.
 * 한 번에 하나만 가입·갱신한다 ([Mutex]). 가입에 실패하면 null이고, 다음에 부르면 다시 시도한다.
 *
 * **토큰 미리 갱신 (013, 012 있으면 좋음 C)**: 토큰은 약 1시간 유효하다 (`expiresIn` 3600). 만료 [refreshBefore] 전부터는
 * 갱신 토큰으로 새 토큰을 받는다 (securetoken `v1/token`, form 몸통). **uid는 그대로**여야 한다: 보안 규칙상
 * 차는 자기 uid로 만든 환자 건만 고치고 읽을 수 있다.
 *
 * | 상태 | 결과 |
 * |---|---|
 * | 만료 시각을 모름 (`expiresIn` 없음·틀림) | 지금 토큰 (갱신하지 않는다) |
 * | 만료 [refreshBefore] 전보다 이르다 | 지금 토큰 |
 * | 그 뒤 (정확히 그 시각 포함) | 갱신. 성공하면 새 토큰 |
 * | 갱신 실패 (네트워크·5xx·형식 오류) | 아직 만료 전이면 지금 토큰, 만료됐으면 null (자격은 두고 다음에 다시 갱신) |
 * | 갱신 거부 (HTTP 400: 에뮬레이터 재시작 등), 또는 갱신 토큰 없이 만료 | 자격을 버리고 새로 가입 (새 uid) |
 *
 * 토큰·갱신 토큰·uid는 로그에 남기지 않는다. 갱신 토큰은 URL이 아니라 요청 몸통에만 넣는다.
 * @param log 갱신 결과 한 줄 (`HospitalTrip` 태그)
 */
class AnonymousAuth(
    private val config: ServerConfig,
    private val transport: HttpTransport,
    private val now: () -> Instant = Instant::now,
    private val refreshBefore: Duration = 5.minutes,
    private val log: (String) -> Unit = {},
) {
    private val mutex = Mutex()
    private var credentials: Credentials? = null

    private class Credentials(val session: AuthSession, val refreshToken: String?, val expiresAt: Instant?)

    private sealed interface Refresh {
        data class Done(val credentials: Credentials) : Refresh
        data object Rejected : Refresh
        data class Failed(val reason: String) : Refresh
    }

    suspend fun session(): AuthSession? = mutex.withLock {
        val current = credentials ?: return@withLock signUp()?.also { credentials = it }?.session
        val expiresAt = current.expiresAt ?: return@withLock current.session
        val at = now()
        if (at.isBefore(expiresAt.minus(refreshBefore.toJavaDuration()))) return@withLock current.session
        val expired = !at.isBefore(expiresAt)
        if (current.refreshToken == null) {
            if (!expired) return@withLock current.session
            log("익명 토큰: 다시 가입 (갱신 토큰 없음)")
            credentials = null
            return@withLock signUp()?.also { credentials = it }?.session
        }
        when (val result = refresh(current, current.refreshToken)) {
            is Refresh.Done -> {
                credentials = result.credentials
                log("익명 토큰 갱신: 성공")
                result.credentials.session
            }
            Refresh.Rejected -> {
                log("익명 토큰: 다시 가입 (갱신 거부)")
                credentials = null
                signUp()?.also { credentials = it }?.session
            }
            is Refresh.Failed -> {
                log("익명 토큰 갱신: 실패 (${result.reason})")
                if (expired) null else current.session
            }
        }
    }

    private suspend fun signUp(): Credentials? {
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
            val token = (json.opt("idToken") as? String)?.takeIf { it.isNotEmpty() } ?: return null
            val uid = (json.opt("localId") as? String)?.takeIf { it.isNotEmpty() } ?: return null
            Credentials(
                session = AuthSession(token, uid),
                refreshToken = (json.opt("refreshToken") as? String)?.takeIf { it.isNotEmpty() },
                expiresAt = expiresAtOf(json.opt("expiresIn")),
            )
        } catch (_: JSONException) {
            null
        }
    }

    private suspend fun refresh(current: Credentials, refreshToken: String): Refresh {
        val response = try {
            transport.send(
                HttpRequest(
                    method = "POST",
                    url = config.refreshUrl,
                    headers = mapOf("Content-Type" to "application/x-www-form-urlencoded"),
                    body = "grant_type=refresh_token&refresh_token=${formEncode(refreshToken)}",
                ),
            )
        } catch (_: IOException) {
            return Refresh.Failed("네트워크")
        }
        if (response.status == 400) return Refresh.Rejected
        if (!response.isSuccessful) return Refresh.Failed("HTTP ${response.status}")
        return try {
            val json = JSONObject(response.body)
            val token = (json.opt("id_token") as? String)?.takeIf { it.isNotEmpty() } ?: return Refresh.Failed("형식 오류")
            val uid = current.session.uid
            val userId = (json.opt("user_id") as? String)?.takeIf { it.isNotEmpty() }
            // 갱신은 같은 계정이어야 한다. 다른 uid면 자기 환자 건을 못 고치므로 받아들이지 않는다
            if (userId != null && userId != uid) return Refresh.Failed("형식 오류")
            Refresh.Done(
                Credentials(
                    session = AuthSession(token, uid),
                    refreshToken = (json.opt("refresh_token") as? String)?.takeIf { it.isNotEmpty() } ?: refreshToken,
                    expiresAt = expiresAtOf(json.opt("expires_in")),
                ),
            )
        } catch (_: JSONException) {
            Refresh.Failed("형식 오류")
        }
    }

    /** `expiresIn`(가입)·`expires_in`(갱신): 문자열 또는 숫자, 1~86400초만 인정한다. 틀리면 모름(null) */
    private fun expiresAtOf(value: Any?): Instant? {
        val seconds = when (value) {
            is String -> value.toLongOrNull()
            is Int -> value.toLong()
            is Long -> value
            else -> null
        } ?: return null
        if (seconds !in 1..MAX_EXPIRES_SECONDS) return null
        return now().plusSeconds(seconds)
    }

    private companion object {
        const val MAX_EXPIRES_SECONDS = 86_400L

        /** form 몸통 값 인코딩 (RFC 3986 비예약 문자만 그대로). `java.net`은 전송 파일 하나에서만 쓴다 (AGENTS.md) */
        fun formEncode(value: String): String = buildString {
            for (byte in value.toByteArray(Charsets.UTF_8)) {
                val c = byte.toInt() and 0xFF
                val ch = c.toChar()
                if (ch in 'A'..'Z' || ch in 'a'..'z' || ch in '0'..'9' || ch == '-' || ch == '_' || ch == '.' || ch == '~') {
                    append(ch)
                } else {
                    append('%').append("0123456789ABCDEF"[c shr 4]).append("0123456789ABCDEF"[c and 0x0F])
                }
            }
        }
    }
}
