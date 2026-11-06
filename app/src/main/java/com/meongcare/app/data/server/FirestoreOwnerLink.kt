package com.meongcare.app.data.server

import com.meongcare.app.domain.link.LinkCheck
import com.meongcare.app.domain.link.LinkCode
import com.meongcare.app.domain.link.LinkStart
import com.meongcare.app.domain.link.OwnerLinkService
import com.meongcare.app.domain.link.OwnerLinkState
import java.io.IOException
import java.time.Instant
import kotlin.random.Random
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject

/**
 * 보호자 웹과 차의 연결 (Firestore REST, 014). 형식: docs/data/owner-link.md, 규칙: firebase/firestore.rules (R14~R24).
 *
 * - 코드 만들기: `documents:commit` + `currentDocument.exists=false` + `createdAt` = 서버 시각(REQUEST_TIME).
 *   같은 코드가 있으면 409라서 새 코드로 다시 만든다 (세 번까지).
 * - 확인: 자기 코드 문서를 GET. `ownerUid`가 생겼으면 연결됨 → 코드 문서를 지운다. 문서가 없으면(403·404) 코드 없음.
 * - 끊기: `petProfiles/{보호자}/readers/{나}`를 지운다. 지우지 못해도 차는 보호자를 잊는다.
 * - 보호자 uid는 이 클래스만 안다 ([ownerUid]). 앱을 다시 켜면 잊는다 (11/15 범위, 계획 014 U5 S1).
 *
 * 로그(`OwnerLink`)에는 결과 종류·HTTP 상태·걸린 시간만 남긴다 (코드·uid·토큰 없음).
 */
class FirestoreOwnerLink(
    private val config: ServerConfig,
    private val auth: AnonymousAuth,
    private val transport: HttpTransport,
    private val random: Random,
    private val now: () -> Instant = Instant::now,
    private val log: (String) -> Unit = {},
) : OwnerLinkService {

    private val mutableState = MutableStateFlow<OwnerLinkState>(OwnerLinkState.Unlinked)
    override val state: StateFlow<OwnerLinkState> = mutableState.asStateFlow()

    @Volatile
    private var owner: String? = null

    /** 이 차가 받는 보호자 uid. 연결 전이면 null ([FirestorePetProfileReader]가 쓴다) */
    val ownerUid: String? get() = owner.takeIf { mutableState.value is OwnerLinkState.Linked }

    override suspend fun start(): LinkStart {
        val session = auth.session() ?: return LinkStart.Failed("익명 로그인 실패").also { log("연결 코드: 실패 (익명 로그인)") }
        repeat(MAX_CODE_ATTEMPTS) {
            val code = LinkCode.generate(random)
            val response = send("POST", "${config.documentsUrl}:commit", session, createCodeBody(code, session.uid))
                ?: return LinkStart.Failed("네트워크").also { log("연결 코드: 실패 (네트워크)") }
            when {
                response.isSuccessful -> {
                    log("연결 코드: 만듦")
                    return LinkStart.Created(code, now())
                }
                // 같은 코드가 이미 있다 (F2: 409). 새 코드로 다시
                response.status == 409 -> log("연결 코드: 겹침, 다시 만듦")
                else -> return LinkStart.Failed("HTTP ${response.status}").also { log("연결 코드: 실패 (HTTP ${response.status})") }
            }
        }
        log("연결 코드: 실패 (겹침 ${MAX_CODE_ATTEMPTS}번)")
        return LinkStart.Failed("코드 겹침")
    }

    override suspend fun check(code: LinkCode): LinkCheck {
        val session = auth.session() ?: return LinkCheck.Failed("익명 로그인 실패")
        val response = send("GET", "${config.documentsUrl}/linkCodes/${code.value}", session, body = null)
            ?: return LinkCheck.Failed("네트워크")
        if (response.status == 403 || response.status == 404) return LinkCheck.Gone.also { log("연결: 코드 없음 (HTTP ${response.status})") }
        if (!response.isSuccessful) return LinkCheck.Failed("HTTP ${response.status}")
        val claimedBy = when (val read = claimedOwnerOf(response.body)) {
            Claim.Malformed -> return LinkCheck.Failed("형식 오류")
            Claim.Waiting -> return LinkCheck.Waiting
            is Claim.By -> read.ownerUid
        }
        // 한 차는 보호자 한 명 (계획 014 K5): 다른 보호자와 연결돼 있었으면 그 허가를 먼저 지운다
        val previous = owner
        if (previous != null && previous != claimedBy) deleteReader(previous, session)
        owner = claimedBy
        mutableState.value = OwnerLinkState.Linked(now())
        log("연결: 확인됨")
        // 쓴 코드는 지운다 (실패해도 보호자가 이미 claim해서 아무도 다시 쓰지 못한다)
        send("DELETE", "${config.documentsUrl}/linkCodes/${code.value}", session, body = null)
        return LinkCheck.Linked
    }

    override suspend fun cancel(code: LinkCode) {
        val session = auth.session() ?: return
        val response = send("DELETE", "${config.documentsUrl}/linkCodes/${code.value}", session, body = null)
        log("연결 코드: 지움 (${response?.status?.let { "HTTP $it" } ?: "네트워크 실패"})")
    }

    override suspend fun unlink(): Boolean {
        val previous = owner
        owner = null
        mutableState.value = OwnerLinkState.Unlinked
        if (previous == null) return true
        val session = auth.session() ?: return false.also { log("연결 끊기: 허가를 지우지 못함 (익명 로그인)") }
        return deleteReader(previous, session).also { log("연결 끊기: ${if (it) "허가 지움" else "허가를 지우지 못함"}") }
    }

    override fun markRevoked() {
        if (mutableState.value is OwnerLinkState.Linked) {
            owner = null
            mutableState.value = OwnerLinkState.Revoked
            log("연결: 보호자 쪽에서 끊김")
        }
    }

    override fun acknowledgeRevoked() {
        if (mutableState.value == OwnerLinkState.Revoked) mutableState.value = OwnerLinkState.Unlinked
    }

    private suspend fun deleteReader(ownerUid: String, session: AuthSession): Boolean {
        val response = send("DELETE", "${config.documentsUrl}/petProfiles/$ownerUid/readers/${session.uid}", session, body = null)
        return response?.isSuccessful == true
    }

    private fun createCodeBody(code: LinkCode, carUid: String): String {
        val fields = JSONObject()
            .put("schemaVersion", JSONObject().put("integerValue", "1"))
            .put("carUid", JSONObject().put("stringValue", carUid))
            .put("ownerUid", JSONObject().put("nullValue", JSONObject.NULL))
            .put("claimedAt", JSONObject().put("nullValue", JSONObject.NULL))
        val write = JSONObject()
            .put("update", JSONObject().put("name", "${config.documentsName}/linkCodes/${code.value}").put("fields", fields))
            .put("updateTransforms", JSONArray().put(JSONObject().put("fieldPath", "createdAt").put("setToServerValue", "REQUEST_TIME")))
            .put("currentDocument", JSONObject().put("exists", false))
        return JSONObject().put("writes", JSONArray().put(write)).toString()
    }

    private sealed interface Claim {
        data object Waiting : Claim
        data class By(val ownerUid: String) : Claim
        data object Malformed : Claim
    }

    /** 코드 문서의 `ownerUid`: nullValue면 아직, 문자열이면 그 보호자 */
    private fun claimedOwnerOf(body: String): Claim {
        val fields = try {
            JSONObject(body).optJSONObject("fields")
        } catch (_: JSONException) {
            null
        } ?: return Claim.Malformed
        val value = fields.optJSONObject("ownerUid") ?: return Claim.Malformed
        return when {
            value.has("nullValue") -> Claim.Waiting
            value.has("stringValue") -> (value.opt("stringValue") as? String)?.takeIf { it.isNotEmpty() }?.let { Claim.By(it) } ?: Claim.Malformed
            else -> Claim.Malformed
        }
    }

    private suspend fun send(method: String, url: String, session: AuthSession, body: String?): HttpResponse? = try {
        transport.send(
            HttpRequest(
                method = method,
                url = url,
                headers = if (body == null) {
                    mapOf("Authorization" to "Bearer ${session.idToken}")
                } else {
                    mapOf("Authorization" to "Bearer ${session.idToken}", "Content-Type" to "application/json")
                },
                body = body,
            ),
        )
    } catch (_: IOException) {
        null
    }

    private companion object {
        const val MAX_CODE_ATTEMPTS = 3
    }
}
