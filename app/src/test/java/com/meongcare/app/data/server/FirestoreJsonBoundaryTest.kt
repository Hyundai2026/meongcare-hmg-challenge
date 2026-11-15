package com.meongcare.app.data.server

import com.meongcare.app.data.pet.SamplePetProfiles
import com.meongcare.app.domain.card.Symptom
import com.meongcare.app.domain.hospital.Congestion
import com.meongcare.app.domain.pet.MedicalInfo
import com.meongcare.app.domain.pet.Sex
import com.meongcare.app.domain.trip.CaseEta
import com.meongcare.app.domain.trip.CaseStatus
import com.meongcare.app.domain.trip.caseCardOf
import java.time.Instant
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Firestore REST 값 변환 경계.
 * 기준: docs/data/hospital-status.md "읽는 쪽 규칙", docs/data/emergency-case.md "필드"·"card.pet"·"상태 흐름",
 * docs/data/pet-profile.md(형식), firebase/firestore.rules(`validNewCase`, 차가 바꿀 수 있는 필드), 계획 012 "설계 결정 3"(REST 값 형식).
 */
class FirestoreJsonBoundaryTest {

    private val at = "2026-11-06T05:00:00Z"

    private val statusFields = linkedMapOf(
        "waitMinutes" to """{"integerValue": "10"}""",
        "congestion" to """{"stringValue": "normal"}""",
        "acceptingEmergency" to """{"booleanValue": true}""",
        "updatedAt" to """{"timestampValue": "$at"}""",
    )

    private fun statusValue(fields: Map<String, String> = statusFields) =
        """{"mapValue": {"fields": {${fields.entries.joinToString(",") { "\"${it.key}\": ${it.value}" }}}}}"""

    private val goodFields = linkedMapOf(
        "schemaVersion" to """{"integerValue": "1"}""",
        "name" to """{"stringValue": "시연용 바른 동물병원"}""",
        "phone" to """{"stringValue": "02-0000-0001"}""",
        "latitude" to """{"doubleValue": 37.367}""",
        "longitude" to """{"doubleValue": 127.1083}""",
        "address" to """{"nullValue": null}""",
        "isSample" to """{"booleanValue": true}""",
        "status" to statusValue(),
    )

    private fun doc(fields: Map<String, String?>, id: String = "demo-fast"): String {
        val body = fields.entries.filter { it.value != null }.joinToString(",") { "\"${it.key}\": ${it.value}" }
        return """{"name": "projects/p/databases/(default)/documents/hospitals/$id", "fields": {$body}}"""
    }

    private fun fieldsWith(vararg changes: Pair<String, String?>): Map<String, String?> = LinkedHashMap<String, String?>(goodFields).apply { changes.forEach { put(it.first, it.second) } }

    private fun read(fields: Map<String, String?>) = FirestoreJson.hospitalsOf("""{"documents": [${doc(fields)}]}""")

    // ---- 병원 읽기: 좌표 ----

    @Test
    fun `위도와 경도의 경계 값은 받는다`() {
        listOf(90.0 to 180.0, -90.0 to -180.0, 0.0 to 0.0).forEach { (lat, lon) ->
            val hospital = read(fieldsWith("latitude" to """{"doubleValue": $lat}""", "longitude" to """{"doubleValue": $lon}""")).single().hospital
            assertEquals(lat, hospital.destination.latitude, 0.0)
            assertEquals(lon, hospital.destination.longitude, 0.0)
        }
    }

    @Test
    fun `위도나 경도가 범위를 조금이라도 넘으면 그 병원을 건너뛴다`() {
        listOf(
            "latitude" to """{"doubleValue": 90.000001}""",
            "latitude" to """{"doubleValue": -90.000001}""",
            "longitude" to """{"doubleValue": 180.000001}""",
            "longitude" to """{"doubleValue": -180.000001}""",
            "latitude" to """{"integerValue": "91"}""",
            "longitude" to """{"integerValue": "-181"}""",
        ).forEach { change -> assertTrue(change.toString(), read(fieldsWith(change)).isEmpty()) }
    }

    @Test
    fun `좌표가 NaN이나 무한대면 그 병원을 건너뛴다`() {
        // Firestore REST는 NaN·무한대를 문자열로 준다
        listOf("\"NaN\"", "\"Infinity\"", "\"-Infinity\"").forEach { value ->
            assertTrue("latitude $value", read(fieldsWith("latitude" to """{"doubleValue": $value}""")).isEmpty())
            assertTrue("longitude $value", read(fieldsWith("longitude" to """{"doubleValue": $value}""")).isEmpty())
        }
    }

    @Test
    fun `좌표가 숫자가 아니거나 없으면 그 병원을 건너뛴다`() {
        listOf(
            "latitude" to """{"stringValue": "37.367"}""",
            "latitude" to """{"nullValue": null}""",
            "longitude" to """{"booleanValue": true}""",
            "latitude" to null,
            "longitude" to null,
        ).forEach { change -> assertTrue(change.toString(), read(fieldsWith(change)).isEmpty()) }
    }

    // ---- 병원 읽기: 그 밖의 필드 ----

    @Test
    fun `형식 버전이 없거나 숫자가 아니면 그 병원을 건너뛴다`() {
        assertTrue(read(fieldsWith("schemaVersion" to null)).isEmpty())
        assertTrue(read(fieldsWith("schemaVersion" to """{"stringValue": "1"}""")).isEmpty())
        assertTrue(read(fieldsWith("schemaVersion" to """{"integerValue": "0"}""")).isEmpty())
    }

    @Test
    fun `이름이 없거나 문자열이 아니거나 빈 문자열이면 그 병원을 건너뛴다`() {
        assertTrue(read(fieldsWith("name" to null)).isEmpty())
        assertTrue(read(fieldsWith("name" to """{"integerValue": "1"}""")).isEmpty())
        assertTrue(read(fieldsWith("name" to """{"stringValue": ""}""")).isEmpty())
    }

    @Test
    fun `전화번호가 비었거나 문자열이 아니면 전화번호 없음이고 병원은 남는다`() {
        listOf(
            """{"stringValue": ""}""",
            """{"integerValue": "20000000"}""",
            """{"nullValue": null}""",
            null,
        ).forEach { value ->
            val candidate = read(fieldsWith("phone" to value)).single()
            assertNull(value.toString(), candidate.hospital.phoneNumber)
        }
    }

    @Test
    fun `isSample이 없으면 시연용이고 false면 실제 병원이다`() {
        assertTrue(read(fieldsWith("isSample" to null)).single().hospital.isSample)
        assertFalse(read(fieldsWith("isSample" to """{"booleanValue": false}""")).single().hospital.isSample)
    }

    @Test
    fun `모르는 필드는 문서와 status 모두에서 무시한다`() {
        val status = statusValue(LinkedHashMap(statusFields).apply { put("note", """{"stringValue": "x"}""") })
        val candidate = read(fieldsWith("openingHours" to """{"stringValue": "24h"}""", "status" to status)).single()
        assertEquals(10, candidate.status?.waitMinutes)
    }

    @Test
    fun `목록에서 객체가 아닌 항목과 문서 이름이 없는 항목은 건너뛴다`() {
        val good = doc(goodFields)
        val noName = """{"fields": {${goodFields.entries.joinToString(",") { "\"${it.key}\": ${it.value}" }}}}"""
        val list = FirestoreJson.hospitalsOf("""{"documents": [1, "x", null, $noName, $good]}""")
        assertEquals(listOf("demo-fast"), list.map { it.hospital.id })
    }

    // ---- status ----

    @Test
    fun `status가 null 값이거나 빈 맵이면 대기 정보 없음이고 병원은 남는다`() {
        listOf("""{"nullValue": null}""", """{"mapValue": {}}""", """{"mapValue": {"fields": {}}}""").forEach { value ->
            val candidate = read(fieldsWith("status" to value)).single()
            assertNull(value, candidate.status)
        }
    }

    @Test
    fun `status 필드가 하나라도 없으면 대기 정보 없음이다`() {
        statusFields.keys.forEach { missing ->
            val fields = LinkedHashMap(statusFields).apply { remove(missing) }
            assertNull(missing, read(fieldsWith("status" to statusValue(fields))).single().status)
        }
    }

    @Test
    fun `대기 분 0과 600은 받고 -1은 대기 정보 없음이다`() {
        fun waitOf(value: String) = read(fieldsWith("status" to statusValue(LinkedHashMap(statusFields).apply { put("waitMinutes", value) }))).single().status?.waitMinutes
        assertEquals(0, waitOf("""{"integerValue": "0"}"""))
        assertEquals(600, waitOf("""{"integerValue": "600"}"""))
        assertNull(waitOf("""{"integerValue": "-1"}"""))
        assertNull(waitOf("""{"integerValue": "ten"}"""))
        assertNull(waitOf("""{"stringValue": "10"}"""))
    }

    @Test
    fun `혼잡도는 소문자 세 값만 받는다`() {
        fun congestionOf(value: String) = read(fieldsWith("status" to statusValue(LinkedHashMap(statusFields).apply { put("congestion", """{"stringValue": "$value"}""") }))).single().status?.congestion
        assertEquals(Congestion.LOW, congestionOf("low"))
        assertEquals(Congestion.NORMAL, congestionOf("normal"))
        assertEquals(Congestion.HIGH, congestionOf("high"))
        assertNull(congestionOf("Normal"))
        assertNull(congestionOf(""))
    }

    @Test
    fun `소수 초가 있는 갱신 시각도 읽고 읽을 수 없는 시각이면 대기 정보 없음이다`() {
        fun updatedOf(value: String) = read(fieldsWith("status" to statusValue(LinkedHashMap(statusFields).apply { put("updatedAt", value) }))).single().status?.updatedAt
        assertEquals(Instant.parse("2026-11-06T05:00:00.123456Z"), updatedOf("""{"timestampValue": "2026-11-06T05:00:00.123456Z"}"""))
        assertNull(updatedOf("""{"timestampValue": "yesterday"}"""))
        assertNull(updatedOf("""{"nullValue": null}"""))
    }

    // ---- 환자 건 쓰기 ----

    private val eta = CaseEta(remainingMinutes = 12, measuredAt = Instant.parse(at))

    private fun JSONObject.v(key: String): JSONObject = getJSONObject(key)

    @Test
    fun `새 환자 건의 키는 보안 규칙이 받는 키뿐이고 필수 키가 다 있다`() {
        val fields = FirestoreJson.newCaseFields(caseCardOf(SamplePetProfiles.demoPet, setOf(Symptom.COUGH)), eta, "uid-1") { it.name }
        val allowed = setOf("schemaVersion", "createdBy", "createdAt", "status", "remainingMinutes", "etaAt", "etaUpdatedAt", "card", "acknowledgedAt")
        val required = setOf("schemaVersion", "createdBy", "createdAt", "status", "card")
        val keys = fields.keySet()
        assertTrue("규칙에 없는 키: ${keys - allowed}", allowed.containsAll(keys))
        assertTrue("빠진 필수 키: ${required - keys}", keys.containsAll(required))
        assertEquals(setOf("pet", "symptoms"), fields.v("card").v("mapValue").v("fields").keySet())
    }

    @Test
    fun `새 환자 건 값의 타입이 보안 규칙과 맞다`() {
        val fields = FirestoreJson.newCaseFields(caseCardOf(SamplePetProfiles.demoPet, setOf(Symptom.COUGH)), eta, "uid-1") { it.name }
        assertEquals("1", fields.v("schemaVersion").getString("integerValue"))
        assertEquals("uid-1", fields.v("createdBy").getString("stringValue"))
        assertTrue(fields.v("createdAt").has("timestampValue"))
        assertEquals("en_route", fields.v("status").getString("stringValue"))
        assertEquals("12", fields.v("remainingMinutes").getString("integerValue"))
        assertTrue(fields.v("etaAt").has("timestampValue"))
        assertTrue(fields.v("etaUpdatedAt").has("timestampValue"))
        assertTrue(fields.v("acknowledgedAt").has("nullValue"))
    }

    @Test
    fun `도착 예정 시각은 잰 시각에 남은 분을 더한 값이고 보낸 시각은 잰 시각이다`() {
        val fields = FirestoreJson.newCaseFields(caseCardOf(null, emptySet()), CaseEta(7, Instant.parse(at)), "u") { it.name }
        assertEquals(Instant.parse("2026-11-06T05:07:00Z"), Instant.parse(fields.v("etaAt").getString("timestampValue")))
        assertEquals(Instant.parse(at), Instant.parse(fields.v("createdAt").getString("timestampValue")))
        assertEquals(Instant.parse(at), Instant.parse(fields.v("etaUpdatedAt").getString("timestampValue")))
    }

    @Test
    fun `반려견 정보를 못 받았으면 pet은 null 값이다`() {
        val fields = FirestoreJson.newCaseFields(caseCardOf(null, setOf(Symptom.COUGH)), eta, "u") { it.name }
        assertTrue(fields.v("card").v("mapValue").v("fields").v("pet").has("nullValue"))
    }

    @Test
    fun `증상이 없으면 values 없는 빈 배열이다`() {
        val fields = FirestoreJson.newCaseFields(caseCardOf(SamplePetProfiles.demoPet, emptySet()), eta, "u") { it.name }
        val symptoms = fields.v("card").v("mapValue").v("fields").v("symptoms").v("arrayValue")
        assertFalse(symptoms.has("values"))
    }

    @Test
    fun `증상 12개는 프리셋 순서대로 code와 label을 모두 쓴다`() {
        val all = Symptom.entries.toSet()
        val fields = FirestoreJson.newCaseFields(caseCardOf(null, all.reversed().toSet()), eta, "u") { "글자-${it.ordinal}" }
        val values = fields.v("card").v("mapValue").v("fields").v("symptoms").v("arrayValue").getJSONArray("values")
        assertEquals(12, values.length())
        Symptom.entries.forEachIndexed { i, symptom ->
            val item = values.getJSONObject(i).v("mapValue").v("fields")
            assertEquals(symptom.name, item.v("code").getString("stringValue"))
            assertEquals("글자-${symptom.ordinal}", item.v("label").getString("stringValue"))
        }
    }

    @Test
    fun `반려견 정보의 키는 pet-profile에서 사진과 형식 버전을 뺀 것과 같다`() {
        val pet = SamplePetProfiles.demoPet.copy(photoDataUrl = "data:image/jpeg;base64,AAAA")
        val fields = FirestoreJson.newCaseFields(caseCardOf(pet, setOf(Symptom.COUGH)), eta, "u") { it.name }
        val petFields = fields.v("card").v("mapValue").v("fields").v("pet").v("mapValue").v("fields")
        assertEquals(setOf("name", "breed", "sex", "birthDate", "birthDateIsEstimated", "medical", "updatedAt"), petFields.keySet())
        assertEquals(setOf("conditions", "allergies", "medications"), petFields.v("medical").v("mapValue").v("fields").keySet())
        assertFalse(fields.toString().contains("base64"))
    }

    @Test
    fun `반려견의 비어 있는 값은 null 값으로, 빈 목록은 values 없는 빈 배열로 쓴다`() {
        val pet = SamplePetProfiles.demoPet.copy(
            breed = null,
            sex = null,
            birthDate = null,
            medical = MedicalInfo(emptyList(), emptyList(), emptyList()),
        )
        val petFields = FirestoreJson.newCaseFields(caseCardOf(pet, emptySet()), eta, "u") { it.name }
            .v("card").v("mapValue").v("fields").v("pet").v("mapValue").v("fields")
        listOf("breed", "sex", "birthDate").forEach { assertTrue(it, petFields.v(it).has("nullValue")) }
        assertFalse(petFields.v("birthDateIsEstimated").getBoolean("booleanValue"))
        val medical = petFields.v("medical").v("mapValue").v("fields")
        listOf("conditions", "allergies", "medications").forEach { assertFalse(it, medical.v(it).v("arrayValue").has("values")) }
    }

    @Test
    fun `성별은 male과 female로 쓴다`() {
        fun sexOf(sex: Sex) = FirestoreJson.newCaseFields(caseCardOf(SamplePetProfiles.demoPet.copy(sex = sex), emptySet()), eta, "u") { it.name }
            .v("card").v("mapValue").v("fields").v("pet").v("mapValue").v("fields").v("sex").getString("stringValue")
        assertEquals("male", sexOf(Sex.MALE))
        assertEquals("female", sexOf(Sex.FEMALE))
    }

    @Test
    fun `반려견 갱신 시각과 생년월일은 pet-profile 문자열 형식이다`() {
        val pattern = Regex("""\d{4}-\d{2}-\d{2}T\d{2}:\d{2}:\d{2}(\.\d+)?Z""")
        listOf("2026-11-05T00:00:00Z", "2026-11-05T09:30:00.120Z", "2026-11-05T09:30:15.000000001Z").forEach { text ->
            val pet = SamplePetProfiles.demoPet.copy(updatedAt = Instant.parse(text))
            val petFields = FirestoreJson.newCaseFields(caseCardOf(pet, emptySet()), eta, "u") { it.name }
                .v("card").v("mapValue").v("fields").v("pet").v("mapValue").v("fields")
            val written = petFields.v("updatedAt").getString("stringValue")
            assertTrue(written, pattern.matches(written))
            assertEquals(Instant.parse(text), Instant.parse(written))
            assertEquals("2014-03-12", petFields.v("birthDate").getString("stringValue"))
        }
    }

    // ---- 갱신·도착·취소 ----

    @Test
    fun `갱신과 도착·취소 필드는 updateMask와 정확히 같고 보안 규칙이 차에게 허락한 필드 안이다`() {
        val carMayChange = setOf("status", "remainingMinutes", "etaAt", "etaUpdatedAt")
        val eta = FirestoreJson.etaFields(CaseEta(9, Instant.parse(at)))
        assertEquals(FirestoreJson.ETA_FIELDS.toSet(), eta.keySet())
        assertTrue(carMayChange.containsAll(FirestoreJson.ETA_FIELDS))
        CaseStatus.entries.forEach { status ->
            val finish = FirestoreJson.finishFields(status, Instant.parse(at))
            assertEquals(FirestoreJson.FINISH_FIELDS.toSet(), finish.keySet())
        }
        assertTrue(carMayChange.containsAll(FirestoreJson.FINISH_FIELDS))
    }

    @Test
    fun `커밋 요청의 updateMask는 보낸 필드와 정확히 같다`() {
        // updateMask에만 있고 fields에 없는 필드는 Firestore가 지운다
        listOf(
            FirestoreJson.etaFields(CaseEta(null, Instant.parse(at))) to FirestoreJson.ETA_FIELDS,
            FirestoreJson.finishFields(CaseStatus.CANCELLED, Instant.parse(at)) to FirestoreJson.FINISH_FIELDS,
        ).forEach { (fields, paths) ->
            val write = JSONObject(FirestoreJson.commitBody("projects/p/databases/(default)/documents/hospitals/h/cases/c", fields, paths))
                .getJSONArray("writes").getJSONObject(0)
            val mask = write.v("updateMask").getJSONArray("fieldPaths").let { a -> (0 until a.length()).map { a.getString(it) }.toSet() }
            assertEquals(write.v("update").v("fields").keySet(), mask)
        }
    }

    @Test
    fun `상태 이름은 형식 문서의 세 값이다`() {
        assertEquals("en_route", FirestoreJson.statusName(CaseStatus.EN_ROUTE))
        assertEquals("arrived", FirestoreJson.statusName(CaseStatus.ARRIVED))
        assertEquals("cancelled", FirestoreJson.statusName(CaseStatus.CANCELLED))
        val cancelled = FirestoreJson.finishFields(CaseStatus.CANCELLED, Instant.parse(at))
        assertEquals("cancelled", cancelled.v("status").getString("stringValue"))
        assertNotNull(cancelled.v("etaUpdatedAt").getString("timestampValue"))
    }
}
