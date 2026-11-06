package com.meongcare.app.data.server

import com.meongcare.app.domain.card.Symptom
import com.meongcare.app.domain.hospital.Hospital
import com.meongcare.app.domain.trip.CaseCard
import com.meongcare.app.domain.trip.CaseEta
import com.meongcare.app.domain.trip.CaseRef
import com.meongcare.app.domain.trip.CaseSender
import com.meongcare.app.domain.trip.CaseStatus
import com.meongcare.app.domain.trip.SendResult
import java.io.IOException
import java.time.Instant
import org.json.JSONException
import org.json.JSONObject

/**
 * 진료 카드를 서버로 보낸다 (Firestore REST). 형식: docs/data/emergency-case.md, 규칙: firebase/firestore.rules.
 *
 * - 보내기: `POST .../hospitals/{id}/cases` (문서 ID는 서버가 만든다)
 * - 남은 시간·도착·취소: `POST .../documents:commit` + `updateMask`로 바꾼 필드만. 있는 문서에만 쓴다.
 *   (`HttpURLConnection`은 PATCH를 보내지 못해서 commit을 쓴다.)
 *
 * 실패는 결과 값이다 (취소 말고는 던지지 않는다). 요청·응답은 로그에 남기지 않는다 (카드에 반려견 정보가 있다).
 * @param symptomLabel 차 화면의 증상 글자 (strings.xml)
 */
class FirestoreCaseSender(
    private val config: ServerConfig,
    private val auth: AnonymousAuth,
    private val transport: HttpTransport,
    private val symptomLabel: (Symptom) -> String,
) : CaseSender {

    override suspend fun send(hospital: Hospital, card: CaseCard, eta: CaseEta): SendResult {
        val hospitalId = hospital.id ?: return SendResult.Failed("병원 ID 없음")
        val session = auth.session() ?: return SendResult.Failed("익명 로그인 실패")
        val body = JSONObject().put("fields", FirestoreJson.newCaseFields(card, eta, session.uid, symptomLabel)).toString()
        val response = request("POST", "${config.documentsUrl}/hospitals/$hospitalId/cases", session, body)
            ?: return SendResult.Failed("네트워크")
        if (!response.isSuccessful) return SendResult.Failed("HTTP ${response.status}")
        val caseId = try {
            JSONObject(response.body).optString("name").substringAfterLast('/')
        } catch (_: JSONException) {
            ""
        }
        return if (caseId.isEmpty()) SendResult.Failed("형식 오류") else SendResult.Sent(CaseRef(hospitalId, caseId))
    }

    override suspend fun updateEta(ref: CaseRef, eta: CaseEta): Boolean =
        commit(ref, FirestoreJson.etaFields(eta), FirestoreJson.ETA_FIELDS)

    override suspend fun finish(ref: CaseRef, status: CaseStatus, at: Instant): Boolean =
        commit(ref, FirestoreJson.finishFields(status, at), FirestoreJson.FINISH_FIELDS)

    private suspend fun commit(ref: CaseRef, fields: JSONObject, fieldPaths: List<String>): Boolean {
        val session = auth.session() ?: return false
        val name = "${config.documentsName}/hospitals/${ref.hospitalId}/cases/${ref.caseId}"
        val response = request("POST", "${config.documentsUrl}:commit", session, FirestoreJson.commitBody(name, fields, fieldPaths))
        return response?.isSuccessful == true
    }

    private suspend fun request(method: String, url: String, session: AuthSession, body: String): HttpResponse? = try {
        transport.send(
            HttpRequest(
                method = method,
                url = url,
                headers = mapOf("Authorization" to "Bearer ${session.idToken}", "Content-Type" to "application/json"),
                body = body,
            ),
        )
    } catch (_: IOException) {
        null
    }
}
