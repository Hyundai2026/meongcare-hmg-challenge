package com.meongcare.app.data.server

import com.meongcare.app.domain.card.Symptom
import com.meongcare.app.domain.hospital.Hospital
import com.meongcare.app.domain.trip.AckRead
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
 * - 병원 확인 읽기 (013 U1): `GET .../hospitals/{id}/cases/{caseId}?mask.fieldPaths=acknowledgedAt`. 보안 규칙상 자기 건만 읽힌다.
 *   지운 건(병원 [완료])은 403이 온다 (규칙이 없는 문서의 `createdBy`를 볼 수 없다, 에뮬레이터 2026-11-06 확인).
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
            (JSONObject(response.body).opt("name") as? String)?.substringAfterLast('/').orEmpty()
        } catch (_: JSONException) {
            ""
        }
        return if (caseId.isEmpty()) SendResult.Failed("형식 오류") else SendResult.Sent(CaseRef(hospitalId, caseId))
    }

    override suspend fun updateEta(ref: CaseRef, eta: CaseEta): Boolean =
        commit(ref, FirestoreJson.etaFields(eta), FirestoreJson.ETA_FIELDS)

    override suspend fun finish(ref: CaseRef, status: CaseStatus, at: Instant): Boolean =
        commit(ref, FirestoreJson.finishFields(status, at), FirestoreJson.FINISH_FIELDS)

    override suspend fun readAck(ref: CaseRef): AckRead {
        val session = auth.session() ?: return AckRead.Failed("익명 로그인 실패")
        val url = "${config.documentsUrl}/hospitals/${ref.hospitalId}/cases/${ref.caseId}?mask.fieldPaths=acknowledgedAt"
        val response = request("GET", url, session, body = null) ?: return AckRead.Failed("네트워크")
        return when {
            response.status == 403 || response.status == 404 -> AckRead.Gone
            !response.isSuccessful -> AckRead.Failed("HTTP ${response.status}")
            else -> when (val ack = FirestoreJson.ackOf(response.body)) {
                FirestoreJson.Ack.Pending -> AckRead.Pending
                is FirestoreJson.Ack.At -> AckRead.Acknowledged(ack.at)
                FirestoreJson.Ack.Malformed -> AckRead.Failed("형식 오류")
            }
        }
    }

    private suspend fun commit(ref: CaseRef, fields: JSONObject, fieldPaths: List<String>): Boolean {
        val session = auth.session() ?: return false
        val name = "${config.documentsName}/hospitals/${ref.hospitalId}/cases/${ref.caseId}"
        val response = request("POST", "${config.documentsUrl}:commit", session, FirestoreJson.commitBody(name, fields, fieldPaths))
        return response?.isSuccessful == true
    }

    private suspend fun request(method: String, url: String, session: AuthSession, body: String?): HttpResponse? = try {
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
}
