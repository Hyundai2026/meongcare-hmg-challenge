package com.meongcare.app.data.server

import com.meongcare.app.data.pet.ExamplePetProfiles
import com.meongcare.app.data.pet.SamplePetProfiles
import com.meongcare.app.domain.card.Symptom
import com.meongcare.app.domain.hospital.Congestion
import com.meongcare.app.domain.hospital.HospitalOrigin
import com.meongcare.app.domain.trip.CaseEta
import com.meongcare.app.domain.trip.CaseStatus
import com.meongcare.app.domain.trip.caseCardOf
import com.meongcare.app.testutil.DocsData
import java.time.Instant
import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class FirestoreJsonTest {

    private val at = "2026-11-06T05:00:00Z"

    private fun hospitalDoc(id: String = "demo-fast", fields: String) =
        """{"name": "projects/p/databases/(default)/documents/hospitals/$id", "fields": {$fields}}"""

    private val goodFields = """
        "schemaVersion": {"integerValue": "1"},
        "name": {"stringValue": "시연용 바른 동물병원"},
        "phone": {"stringValue": "02-0000-0001"},
        "latitude": {"doubleValue": 37.367},
        "longitude": {"doubleValue": 127.1083},
        "address": {"nullValue": null},
        "isSample": {"booleanValue": true},
        "status": {"mapValue": {"fields": {
            "waitMinutes": {"integerValue": "10"},
            "congestion": {"stringValue": "normal"},
            "acceptingEmergency": {"booleanValue": true},
            "updatedAt": {"timestampValue": "$at"}
        }}}
    """

    private fun list(vararg docs: String) = """{"documents": [${docs.joinToString(",")}]}"""

    @Test
    fun `병원 문서를 읽는다`() {
        val candidate = FirestoreJson.hospitalsOf(list(hospitalDoc(fields = goodFields))).single()
        assertEquals("demo-fast", candidate.hospital.id)
        assertEquals("시연용 바른 동물병원", candidate.hospital.name)
        assertEquals("02-0000-0001", candidate.hospital.phoneNumber)
        assertEquals(37.367, candidate.hospital.destination.latitude, 0.0)
        assertEquals(HospitalOrigin.SERVER, candidate.origin)
        assertTrue(candidate.sendsCardToServer)
        val status = candidate.status!!
        assertEquals(10, status.waitMinutes)
        assertEquals(Congestion.NORMAL, status.congestion)
        assertEquals(Instant.parse(at), status.updatedAt)
    }

    @Test
    fun `좌표가 정수 값으로 와도 읽는다`() {
        val fields = goodFields.replace("\"longitude\": {\"doubleValue\": 127.1083}", "\"longitude\": {\"integerValue\": \"127\"}")
        assertEquals(127.0, FirestoreJson.hospitalsOf(list(hospitalDoc(fields = fields))).single().hospital.destination.longitude, 0.0)
    }

    @Test
    fun `문서가 없으면 빈 목록이다`() {
        assertTrue(FirestoreJson.hospitalsOf("{}").isEmpty())
    }

    @Test
    fun `상태 필드가 틀리면 대기 정보 없음이다`() {
        listOf(
            "\"waitMinutes\": {\"integerValue\": \"10\"}" to "\"waitMinutes\": {\"integerValue\": \"601\"}",
            "\"waitMinutes\": {\"integerValue\": \"10\"}" to "\"waitMinutes\": {\"doubleValue\": 10.5}",
            "\"congestion\": {\"stringValue\": \"normal\"}" to "\"congestion\": {\"stringValue\": \"busy\"}",
            "\"acceptingEmergency\": {\"booleanValue\": true}" to "\"acceptingEmergency\": {\"stringValue\": \"true\"}",
            "\"updatedAt\": {\"timestampValue\": \"$at\"}" to "\"updatedAt\": {\"stringValue\": \"$at\"}",
        ).forEach { (from, to) ->
            val candidate = FirestoreJson.hospitalsOf(list(hospitalDoc(fields = goodFields.replace(from, to)))).single()
            assertNull(to, candidate.status)
        }
        val noStatus = goodFields.substringBefore(",\n        \"status\"")
        assertNull(FirestoreJson.hospitalsOf(list(hospitalDoc(fields = noStatus))).single().status)
    }

    @Test
    fun `모르는 형식 버전이나 이름 없는 병원은 건너뛴다`() {
        val v2 = hospitalDoc("v2", goodFields.replace("\"schemaVersion\": {\"integerValue\": \"1\"}", "\"schemaVersion\": {\"integerValue\": \"2\"}"))
        val noName = hospitalDoc("blank", goodFields.replace("\"name\": {\"stringValue\": \"시연용 바른 동물병원\"}", "\"name\": {\"stringValue\": \" \"}"))
        val badLat = hospitalDoc("lat", goodFields.replace("\"latitude\": {\"doubleValue\": 37.367}", "\"latitude\": {\"doubleValue\": 91.0}"))
        val ok = hospitalDoc("ok", goodFields)
        assertEquals(listOf("ok"), FirestoreJson.hospitalsOf(list(v2, noName, badLat, ok)).map { it.hospital.id })
    }

    @Test
    fun `isSample이 불리언이 아니면 시연용으로 본다`() {
        val fields = goodFields.replace("\"isSample\": {\"booleanValue\": true}", "\"isSample\": {\"stringValue\": \"no\"}")
        assertTrue(FirestoreJson.hospitalsOf(list(hospitalDoc(fields = fields))).single().hospital.isSample)
    }

    @Test
    fun `환자 건을 형식 문서대로 쓴다`() {
        val card = caseCardOf(ExamplePetProfiles.bori, setOf(Symptom.COUGH, Symptom.BREATHING_DIFFICULTY))
        val eta = CaseEta(remainingMinutes = 12, measuredAt = Instant.parse(at))
        val labels = mapOf(Symptom.BREATHING_DIFFICULTY to "호흡 곤란", Symptom.COUGH to "기침")
        val fields = FirestoreJson.newCaseFields(card, eta, createdBy = "anon-uid-example") { labels.getValue(it) }

        val written = plain(JSONObject().put("mapValue", JSONObject().put("fields", fields)))
        val example = plainJson(JSONObject(DocsData.read("emergency-case.example.json")))
        // 타임스탬프는 Instant 문자열로 비교한다 (예시의 .000Z 같은 표기 차이는 같은 시각이다)
        assertEquals(normalize(example), normalize(written))
    }

    @Test
    fun `사진과 위치는 쓰지 않는다`() {
        val card = caseCardOf(SamplePetProfiles.demoPet.copy(photoDataUrl = "data:image/jpeg;base64,AAAA"), setOf(Symptom.COUGH))
        val text = FirestoreJson.newCaseFields(card, CaseEta(5, Instant.parse(at)), "u") { it.name }.toString()
        assertFalse(text.contains("photo"))
        assertFalse(text.contains("latitude") || text.contains("longitude"))
    }

    @Test
    fun `남은 시간을 모르면 null로 쓴다`() {
        val fields = FirestoreJson.etaFields(CaseEta(null, Instant.parse(at)))
        assertTrue(fields.getJSONObject("remainingMinutes").has("nullValue"))
        assertTrue(fields.getJSONObject("etaAt").has("nullValue"))
        assertEquals(at, fields.getJSONObject("etaUpdatedAt").getString("timestampValue"))
    }

    @Test
    fun `커밋 요청은 바꾼 필드만 updateMask로 보내고 있는 문서에만 쓴다`() {
        val body = JSONObject(
            FirestoreJson.commitBody("projects/p/databases/(default)/documents/hospitals/h/cases/c", FirestoreJson.finishFields(CaseStatus.ARRIVED, Instant.parse(at)), FirestoreJson.FINISH_FIELDS),
        )
        val write = body.getJSONArray("writes").getJSONObject(0)
        assertEquals(listOf("status", "etaUpdatedAt"), write.getJSONObject("updateMask").getJSONArray("fieldPaths").let { a -> (0 until a.length()).map { a.getString(it) } })
        assertEquals("arrived", write.getJSONObject("update").getJSONObject("fields").getJSONObject("status").getString("stringValue"))
        assertTrue(write.getJSONObject("currentDocument").getBoolean("exists"))
    }

    // ---- Firestore 값 → 일반 값 (비교용) ----

    private fun plain(value: JSONObject): Any? = when {
        value.has("stringValue") -> value.getString("stringValue")
        value.has("integerValue") -> value.getString("integerValue").toLong()
        value.has("doubleValue") -> value.getDouble("doubleValue")
        value.has("booleanValue") -> value.getBoolean("booleanValue")
        value.has("nullValue") -> null
        value.has("timestampValue") -> "ts:" + Instant.parse(value.getString("timestampValue"))
        value.has("mapValue") -> value.getJSONObject("mapValue").optJSONObject("fields").let { f ->
            if (f == null) emptyMap() else f.keys().asSequence().associateWith { plain(f.getJSONObject(it)) }
        }
        value.has("arrayValue") -> value.getJSONObject("arrayValue").optJSONArray("values").let { a ->
            if (a == null) emptyList() else (0 until a.length()).map { plain(a.getJSONObject(it)) }
        }
        else -> error("모르는 값 $value")
    }

    private fun plainJson(value: Any?): Any? = when (value) {
        JSONObject.NULL, null -> null
        is JSONObject -> value.keys().asSequence().associateWith { plainJson(value.get(it)) }
        is JSONArray -> (0 until value.length()).map { plainJson(value.get(it)) }
        is Int -> value.toLong()
        else -> value
    }

    /** 예시 JSON의 타임스탬프 필드(문자열)를 쓴 쪽과 같은 표기로 맞춘다 */
    @Suppress("UNCHECKED_CAST")
    private fun normalize(value: Any?, key: String = ""): Any? = when (value) {
        is Map<*, *> -> (value as Map<String, Any?>).mapValues { (k, v) -> normalize(v, k) }
        is List<*> -> value.map { normalize(it) }
        is String -> if (key in TIMESTAMP_KEYS && !value.startsWith("ts:")) "ts:" + Instant.parse(value) else value
        else -> value
    }

    private companion object {
        // pet.updatedAt은 문자열이지만 pet-profile.md상 소수 자리가 있어도 같은 시각이다
        val TIMESTAMP_KEYS = setOf("createdAt", "etaAt", "etaUpdatedAt", "updatedAt")
    }
}
