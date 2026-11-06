package com.meongcare.app.data.server

import com.meongcare.app.domain.card.Symptom
import com.meongcare.app.domain.guidance.Destination
import com.meongcare.app.domain.hospital.Congestion
import com.meongcare.app.domain.hospital.Hospital
import com.meongcare.app.domain.hospital.HospitalCandidate
import com.meongcare.app.domain.hospital.HospitalOrigin
import com.meongcare.app.domain.hospital.HospitalStatus
import com.meongcare.app.domain.pet.PetProfile
import com.meongcare.app.domain.pet.Sex
import com.meongcare.app.domain.trip.CaseCard
import com.meongcare.app.domain.trip.CaseEta
import com.meongcare.app.domain.trip.CaseStatus
import java.time.Instant
import java.time.format.DateTimeParseException
import org.json.JSONArray
import org.json.JSONObject

/**
 * Firestore REST 값 형식 ↔ 도메인 (계획 012 "설계 결정 3·5"). 기준: docs/data/hospital-status.md, emergency-case.md.
 *
 * REST 값은 `{"stringValue": "..."}`, `{"integerValue": "12"}`(문자열), `{"doubleValue": 1.5}`, `{"booleanValue": true}`,
 * `{"nullValue": null}`, `{"timestampValue": "...Z"}`, `{"mapValue": {"fields": {...}}}`, `{"arrayValue": {"values": [...]}}`다.
 * 빈 배열은 `values` 없이 `{"arrayValue": {}}`로 온다. 정수처럼 보이는 좌표(127)는 `integerValue`로 올 수 있다.
 *
 * 읽는 쪽은 짐작하지 않는다: 병원 필수 값이 틀리면 그 병원을 건너뛰고, status가 하나라도 틀리면 대기 정보 없음(null)이다.
 * Android 내장 org.json을 쓴다 (테스트에서는 org.json:json).
 */
object FirestoreJson {

    private const val SCHEMA_VERSION = 1L
    private const val WAIT_MINUTES_MAX = 600L

    // ---- 읽기: 병원 목록 ----

    /** `GET .../documents/hospitals` 응답 → 후보 목록. 형식이 틀린 병원은 건너뛴다. 문서가 없으면 빈 목록. */
    fun hospitalsOf(responseBody: String): List<HospitalCandidate> {
        val root = JSONObject(responseBody)
        val documents = root.optJSONArray("documents") ?: return emptyList()
        return (0 until documents.length()).mapNotNull { index ->
            documents.optJSONObject(index)?.let { hospitalOf(it) }
        }
    }

    /** 문서 하나 (`{"name": ".../hospitals/<id>", "fields": {...}}`) → 후보. 틀리면 null. */
    fun hospitalOf(document: JSONObject): HospitalCandidate? {
        val id = (document.opt("name") as? String)?.substringAfterLast('/')?.takeIf { it.isNotEmpty() } ?: return null
        val fields = document.optJSONObject("fields") ?: return null
        if (fields.longOf("schemaVersion") != SCHEMA_VERSION) return null
        val name = fields.stringOf("name")?.takeIf { it.isNotBlank() } ?: return null
        val latitude = fields.numberOf("latitude")?.takeIf { it.isFinite() && it in -90.0..90.0 } ?: return null
        val longitude = fields.numberOf("longitude")?.takeIf { it.isFinite() && it in -180.0..180.0 } ?: return null
        val phone = fields.stringOf("phone")?.takeIf { it.isNotBlank() }
        // 불리언이 아니면 시연용으로 본다 (시연용이라고 알리는 쪽이 안전하다)
        val isSample = fields.booleanOf("isSample") ?: true
        return HospitalCandidate(
            hospital = Hospital(
                destination = Destination(name = name, latitude = latitude, longitude = longitude),
                phoneNumber = phone,
                isSample = isSample,
                id = id,
            ),
            status = fields.mapOf("status")?.let { statusOf(it) },
            origin = HospitalOrigin.SERVER,
        )
    }

    /** status 맵의 fields → 상태. 하나라도 틀리면 null (대기 정보 없음). */
    fun statusOf(fields: JSONObject): HospitalStatus? {
        val wait = fields.longOf("waitMinutes")?.takeIf { it in 0..WAIT_MINUTES_MAX } ?: return null
        val congestion = when (fields.stringOf("congestion")) {
            "low" -> Congestion.LOW
            "normal" -> Congestion.NORMAL
            "high" -> Congestion.HIGH
            else -> return null
        }
        val accepting = fields.booleanOf("acceptingEmergency") ?: return null
        val updatedAt = fields.timestampOf("updatedAt") ?: return null
        return HospitalStatus(wait.toInt(), congestion, accepting, updatedAt)
    }

    // ---- 읽기: 환자 건의 병원 확인 (013 U1) ----

    /** 확인 시각을 읽은 결과. [Malformed]는 짐작하지 않고 실패로 본다 */
    sealed interface Ack {
        data object Pending : Ack
        data class At(val at: Instant) : Ack
        data object Malformed : Ack
    }

    /**
     * 단일 문서 GET 응답(`mask.fieldPaths=acknowledgedAt`) → 확인 시각.
     * `acknowledgedAt`이 없거나 `nullValue`면 아직, `timestampValue`면 그 시각, 그 밖의 형식·JSON이 아니면 [Ack.Malformed].
     */
    fun ackOf(responseBody: String): Ack {
        val root = try {
            JSONObject(responseBody)
        } catch (_: org.json.JSONException) {
            return Ack.Malformed
        }
        // mask로 읽은 필드가 모두 없으면 fields가 빠진다 (아직). fields가 객체가 아니면 짐작하지 않는다
        val fields = root.optJSONObject("fields") ?: return if (root.has("fields")) Ack.Malformed else Ack.Pending
        val value = fields.optJSONObject("acknowledgedAt") ?: return if (fields.has("acknowledgedAt")) Ack.Malformed else Ack.Pending
        return when {
            value.has("nullValue") -> Ack.Pending
            value.has("timestampValue") -> fields.timestampOf("acknowledgedAt")?.let { Ack.At(it) } ?: Ack.Malformed
            else -> Ack.Malformed
        }
    }

    // ---- 쓰기: 환자 건 ----

    /**
     * 새 환자 건의 fields (docs/data/emergency-case.md). **사진과 차 위치는 쓰지 않는다.**
     * @param symptomLabel 차 화면의 증상 글자 (strings.xml). 도메인은 Android 리소스를 모르므로 받는다.
     */
    fun newCaseFields(card: CaseCard, eta: CaseEta, createdBy: String, symptomLabel: (Symptom) -> String): JSONObject =
        JSONObject()
            .put("schemaVersion", integer(SCHEMA_VERSION))
            .put("createdBy", string(createdBy))
            .put("createdAt", timestamp(eta.measuredAt))
            .put("status", string(statusName(CaseStatus.EN_ROUTE)))
            .put("remainingMinutes", eta.remainingMinutes?.let { integer(it.toLong()) } ?: nullValue())
            .put("etaAt", eta.arrivesAt?.let { timestamp(it) } ?: nullValue())
            .put("etaUpdatedAt", timestamp(eta.measuredAt))
            .put(
                "card",
                map(
                    JSONObject()
                        .put("pet", card.pet?.let { map(petFields(it)) } ?: nullValue())
                        .put(
                            "symptoms",
                            array(card.symptoms.map { map(JSONObject().put("code", string(it.name)).put("label", string(symptomLabel(it)))) }),
                        ),
                ),
            )
            .put("acknowledgedAt", nullValue())

    /** 남은 시간 갱신에 바꾸는 필드 */
    val ETA_FIELDS = listOf("remainingMinutes", "etaAt", "etaUpdatedAt")

    fun etaFields(eta: CaseEta): JSONObject = JSONObject()
        .put("remainingMinutes", eta.remainingMinutes?.let { integer(it.toLong()) } ?: nullValue())
        .put("etaAt", eta.arrivesAt?.let { timestamp(it) } ?: nullValue())
        .put("etaUpdatedAt", timestamp(eta.measuredAt))

    /** 도착·취소에 바꾸는 필드 */
    val FINISH_FIELDS = listOf("status", "etaUpdatedAt")

    fun finishFields(status: CaseStatus, at: Instant): JSONObject = JSONObject()
        .put("status", string(statusName(status)))
        .put("etaUpdatedAt", timestamp(at))

    /** `documents:commit` 요청 몸통: 문서 하나의 [fieldPaths]만 바꾼다 (있는 문서일 때만). */
    fun commitBody(documentName: String, fields: JSONObject, fieldPaths: List<String>): String = JSONObject()
        .put(
            "writes",
            JSONArray().put(
                JSONObject()
                    .put("update", JSONObject().put("name", documentName).put("fields", fields))
                    .put("updateMask", JSONObject().put("fieldPaths", JSONArray(fieldPaths)))
                    .put("currentDocument", JSONObject().put("exists", true)),
            ),
        )
        .toString()

    fun statusName(status: CaseStatus): String = when (status) {
        CaseStatus.EN_ROUTE -> "en_route"
        CaseStatus.ARRIVED -> "arrived"
        CaseStatus.CANCELLED -> "cancelled"
    }

    /** pet-profile v1 필드 중 카드에 넣는 것 (photoDataUrl, schemaVersion 제외). 형식은 pet-profile.md와 같다. */
    private fun petFields(pet: PetProfile): JSONObject = JSONObject()
        .put("name", string(pet.name))
        .put("breed", pet.breed?.let { string(it) } ?: nullValue())
        .put("sex", pet.sex?.let { string(if (it == Sex.MALE) "male" else "female") } ?: nullValue())
        .put("birthDate", pet.birthDate?.let { string(it.toString()) } ?: nullValue())
        .put("birthDateIsEstimated", boolean(pet.birthDateIsEstimated))
        .put(
            "medical",
            map(
                JSONObject()
                    .put("conditions", array(pet.medical.conditions.map { string(it) }))
                    .put("allergies", array(pet.medical.allergies.map { string(it) }))
                    .put(
                        "medications",
                        array(pet.medical.medications.map { map(JSONObject().put("name", string(it.name)).put("dosage", string(it.dosage))) }),
                    ),
            ),
        )
        // pet-profile.md: updatedAt은 ISO 8601 UTC 문자열이다 (Firestore 타임스탬프가 아니다)
        .put("updatedAt", string(pet.updatedAt.toString()))

    // ---- 값 만들기 ----

    private fun string(value: String) = JSONObject().put("stringValue", value)
    private fun integer(value: Long) = JSONObject().put("integerValue", value.toString())
    private fun boolean(value: Boolean) = JSONObject().put("booleanValue", value)
    private fun timestamp(value: Instant) = JSONObject().put("timestampValue", value.toString())
    private fun nullValue() = JSONObject().put("nullValue", JSONObject.NULL)
    private fun map(fields: JSONObject) = JSONObject().put("mapValue", JSONObject().put("fields", fields))
    private fun array(values: List<JSONObject>) =
        JSONObject().put("arrayValue", if (values.isEmpty()) JSONObject() else JSONObject().put("values", JSONArray(values)))

    // ---- 값 읽기 (틀리면 null) ----

    private fun JSONObject.value(key: String): JSONObject? = optJSONObject(key)

    private fun JSONObject.stringOf(key: String): String? =
        value(key)?.takeIf { it.has("stringValue") }?.opt("stringValue") as? String

    private fun JSONObject.booleanOf(key: String): Boolean? =
        value(key)?.takeIf { it.has("booleanValue") }?.let { it.opt("booleanValue") as? Boolean }

    private fun JSONObject.longOf(key: String): Long? {
        val v = value(key) ?: return null
        return when {
            v.has("integerValue") -> (v.opt("integerValue") as? String)?.toLongOrNull()
            // 정수 값의 doubleValue(예: 10.0)는 정수로 받는다. 소수(10.5)는 틀린 값이다.
            v.has("doubleValue") -> v.optDouble("doubleValue").takeIf { it.isFinite() && it == Math.floor(it) }?.toLong()
            else -> null
        }
    }

    private fun JSONObject.numberOf(key: String): Double? {
        val v = value(key) ?: return null
        return when {
            v.has("doubleValue") -> v.optDouble("doubleValue").takeIf { !it.isNaN() }
            v.has("integerValue") -> (v.opt("integerValue") as? String)?.toDoubleOrNull()
            else -> null
        }
    }

    private fun JSONObject.timestampOf(key: String): Instant? {
        val text = value(key)?.takeIf { it.has("timestampValue") }?.opt("timestampValue") as? String ?: return null
        return try {
            Instant.parse(text)
        } catch (_: DateTimeParseException) {
            null
        }
    }

    private fun JSONObject.mapOf(key: String): JSONObject? = value(key)?.optJSONObject("mapValue")?.optJSONObject("fields")
}
