package com.meongcare.app.data.server

import com.meongcare.app.data.pet.SamplePetProfiles
import com.meongcare.app.domain.pet.Medication
import com.meongcare.app.testutil.DocsData
import java.time.Instant
import java.time.LocalDate
import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * docs/data/pet-profile.md "읽는 쪽 규칙"의 경계를 Firestore REST 형식으로.
 * owner-link.md "차가 읽을 때": 그 규칙을 그대로 따르고, 차는 사진을 받지 않는다.
 */
class FirestorePetProfileJsonBoundaryTest {

    /** JSON 값 → Firestore REST 값 (정수는 integerValue, 빈 배열은 values 없음) */
    private fun restValue(value: Any?): JSONObject = when (value) {
        null, JSONObject.NULL -> JSONObject().put("nullValue", JSONObject.NULL)
        is Boolean -> JSONObject().put("booleanValue", value)
        is Int, is Long -> JSONObject().put("integerValue", value.toString())
        is Number -> JSONObject().put("doubleValue", value.toDouble())
        is String -> JSONObject().put("stringValue", value)
        is JSONArray -> JSONObject().put(
            "arrayValue",
            if (value.length() == 0) JSONObject() else JSONObject().put("values", JSONArray((0 until value.length()).map { restValue(value.get(it)) })),
        )
        is JSONObject -> JSONObject().put("mapValue", JSONObject().put("fields", restFields(value)))
        else -> error("모르는 값 $value")
    }

    private fun restFields(json: JSONObject): JSONObject =
        JSONObject().also { out -> json.keys().forEach { key -> out.put(key, restValue(json.get(key))) } }

    private fun example(): JSONObject = JSONObject(DocsData.read("pet-profile.example.json"))

    private fun bodyOf(fields: JSONObject): String =
        JSONObject().put("name", "projects/p/databases/(default)/documents/petProfiles/owner-uid-1").put("fields", fields).toString()

    private fun document(json: JSONObject): String = bodyOf(restFields(json))

    /** 예시의 필드 하나를 REST 값 그대로 바꾼다 (타입을 일부러 틀리게 넣을 때) */
    private fun withRest(key: String, rest: JSONObject): String = bodyOf(restFields(example()).put(key, rest))

    private fun withMedicalRest(key: String, rest: JSONObject): String {
        val fields = restFields(example())
        fields.getJSONObject("medical").getJSONObject("mapValue").getJSONObject("fields").put(key, rest)
        return bodyOf(fields)
    }

    private fun read(body: String) = FirestoreJson.petProfileOf(body)

    @Test
    fun `서버 문서에 사진 글자가 있어도 차는 사진을 쓰지 않는다`() {
        val withPhoto = example().put("photoDataUrl", "data:image/jpeg;base64,/9j/4AAQ")
        assertNull(read(document(withPhoto))!!.photoDataUrl)
    }

    @Test
    fun `형식 버전 경계 - 2, 0, 소수, 문자열, 불리언은 읽지 않는다`() {
        assertNull(read(withRest("schemaVersion", JSONObject().put("integerValue", "2"))))
        assertNull(read(withRest("schemaVersion", JSONObject().put("integerValue", "0"))))
        assertNull(read(withRest("schemaVersion", JSONObject().put("doubleValue", 1.5))))
        assertNull(read(withRest("schemaVersion", JSONObject().put("stringValue", "1"))))
        assertNull(read(withRest("schemaVersion", JSONObject().put("booleanValue", true))))
        assertNull(read(withRest("schemaVersion", JSONObject().put("nullValue", JSONObject.NULL))))
    }

    @Test
    fun `updatedAt이 서버 타임스탬프 값이면 손상이다`() {
        // owner-link.md: 프로필 문서의 updatedAt은 ISO 8601 UTC 문자열이다 (서버 타임스탬프가 아니다)
        assertNull(read(withRest("updatedAt", JSONObject().put("timestampValue", "2026-11-06T09:00:00Z"))))
    }

    @Test
    fun `updatedAt은 소수 자리가 있어도 없어도 읽는다`() {
        assertEquals(Instant.parse("2026-11-06T09:00:00Z"), read(document(example().put("updatedAt", "2026-11-06T09:00:00Z")))!!.updatedAt)
        assertEquals(
            Instant.parse("2026-11-06T09:00:00.123456789Z"),
            read(document(example().put("updatedAt", "2026-11-06T09:00:00.123456789Z")))!!.updatedAt,
        )
    }

    @Test
    fun `updatedAt이 없는 날짜·시각이면 손상이다`() {
        // 보호자 웹(petProfileJson.ts isIsoUtcInstant)은 시 < 24, 분 < 60, 초 < 60만 받는다. 같은 문서를 따르는 차도 같아야 한다
        val accepted = listOf(
            "2026-02-30T09:00:00Z",
            "2026-11-06T24:00:00Z",
            "2026-11-06T23:59:60Z",
            "2026-11-06T09:60:00Z",
            "2026-11-06t09:00:00z",
            "2026-11-06T09:00Z",
            "",
        ).filter { text -> read(document(example().put("updatedAt", text))) != null }
        assertEquals("손상으로 봐야 하는데 읽은 값", emptyList<String>(), accepted)
    }

    @Test
    fun `윤년 2월 29일 생년월일은 읽고 평년 2월 29일은 손상이다`() {
        assertEquals(LocalDate.of(2024, 2, 29), read(document(example().put("birthDate", "2024-02-29")))!!.birthDate)
        assertNull(read(document(example().put("birthDate", "2023-02-29"))))
    }

    @Test
    fun `생년월일이 시각이 붙은 문자열이거나 타임스탬프 값이면 손상이다`() {
        assertNull(read(document(example().put("birthDate", "2014-08-01T00:00:00Z"))))
        assertNull(read(withRest("birthDate", JSONObject().put("timestampValue", "2014-08-01T00:00:00Z"))))
    }

    @Test
    fun `성별은 소문자 male·female만 읽는다`() {
        assertNull(read(document(example().put("sex", "MALE"))))
        assertNull(read(document(example().put("sex", "Female"))))
        assertNull(read(document(example().put("sex", ""))))
    }

    @Test
    fun `추정 표시가 null이면 손상이다`() {
        assertNull(read(withRest("birthDateIsEstimated", JSONObject().put("nullValue", JSONObject.NULL))))
    }

    @Test
    fun `의료정보가 null이거나 목록이 배열이 아니면 손상이다`() {
        assertNull(read(withRest("medical", JSONObject().put("nullValue", JSONObject.NULL))))
        assertNull(read(withMedicalRest("conditions", JSONObject().put("nullValue", JSONObject.NULL))))
        assertNull(read(withMedicalRest("allergies", JSONObject().put("stringValue", "닭고기"))))
    }

    @Test
    fun `목록 항목이 문자열이 아니면 손상이다`() {
        val badItem = JSONObject().put("arrayValue", JSONObject().put("values", JSONArray().put(JSONObject().put("integerValue", "3"))))
        assertNull(read(withMedicalRest("conditions", badItem)))
        val nullItem = JSONObject().put("arrayValue", JSONObject().put("values", JSONArray().put(JSONObject().put("nullValue", JSONObject.NULL))))
        assertNull(read(withMedicalRest("allergies", nullItem)))
    }

    @Test
    fun `복용 약은 이름과 복용법이 모두 문자열이어야 하고 빈 복용법은 읽는다`() {
        fun meds(vararg items: JSONObject) = example().apply {
            getJSONObject("medical").put("medications", JSONArray(items.toList()))
        }
        assertNull(read(document(meds(JSONObject().put("name", "관절약")))))
        assertNull(read(document(meds(JSONObject().put("name", "관절약").put("dosage", JSONObject.NULL)))))
        assertNull(read(document(meds(JSONObject().put("dosage", "1일 1회")))))
        assertEquals(
            listOf(Medication("관절약", "")),
            read(document(meds(JSONObject().put("name", "관절약").put("dosage", ""))))!!.medical.medications,
        )
        // 항목이 맵이 아니면 손상
        val notMap = JSONObject().put("arrayValue", JSONObject().put("values", JSONArray().put(JSONObject().put("stringValue", "관절약"))))
        assertNull(read(withMedicalRest("medications", notMap)))
    }

    @Test
    fun `의료정보 안의 모르는 필드도 무시한다`() {
        val json = example().apply {
            getJSONObject("medical").put("vaccinations", JSONArray().put("광견병"))
            getJSONObject("medical").getJSONArray("medications").getJSONObject(0).put("note", "식후")
        }
        val profile = read(document(json))!!
        assertEquals(SamplePetProfiles.demoPet.medical, profile.medical)
    }

    @Test
    fun `길이 제한을 넘긴 값도 타입이 맞으면 읽는다`() {
        val longName = "가".repeat(40)
        val many = JSONArray((1..30).map { "알레르기 $it" })
        val json = example().put("name", longName).apply { getJSONObject("medical").put("allergies", many) }
        val profile = read(document(json))!!
        assertEquals(longName, profile.name)
        assertEquals(30, profile.medical.allergies.size)
    }

    @Test
    fun `공백뿐인 이름과 빈 품종은 빈 문자열이 아니면 그대로 읽는다`() {
        // 읽는 쪽은 빈 이름만 손상으로 본다. 앞뒤 공백 정리는 쓰는 쪽 일이다
        assertEquals(" ", read(document(example().put("name", " ")))!!.name)
        assertEquals("", read(document(example().put("breed", "")))!!.breed)
    }

    @Test
    fun `몸통이 객체가 아니거나 비었으면 손상이다`() {
        assertNull(read(""))
        assertNull(read("[]"))
        assertNull(read("null"))
        assertNull(read("""{"fields": null}"""))
        assertNull(read("""{"fields": []}"""))
    }

    @Test
    fun `필드 하나씩 빠지면 모두 손상이다`() {
        val required = listOf("schemaVersion", "name", "breed", "sex", "birthDate", "birthDateIsEstimated", "medical", "updatedAt")
        for (key in required) {
            assertNull(key, read(document(example().apply { remove(key) })))
        }
        for (key in listOf("conditions", "allergies", "medications")) {
            assertNull(key, read(document(example().apply { getJSONObject("medical").remove(key) })))
        }
        assertNotNull(read(document(example())))
    }
}
