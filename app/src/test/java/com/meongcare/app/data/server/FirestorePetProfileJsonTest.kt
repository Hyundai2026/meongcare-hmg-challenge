package com.meongcare.app.data.server

import com.meongcare.app.data.pet.ExamplePetProfiles
import com.meongcare.app.testutil.DocsData
import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/** docs/data/pet-profile.md "읽는 쪽 규칙"을 Firestore REST 형식으로 (계획 014 단계 8-1) */
class FirestorePetProfileJsonTest {

    /** JSON 값 → Firestore REST 값 (보호자 웹 JS SDK가 쓰는 꼴: 정수는 integerValue, 빈 배열은 values 없음) */
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

    private fun document(json: JSONObject): String =
        JSONObject().put("name", "projects/p/databases/(default)/documents/petProfiles/owner-uid-1").put("fields", restFields(json)).toString()

    private fun example(): JSONObject = JSONObject(DocsData.read("pet-profile.example.json"))

    private fun withField(key: String, value: Any?): String = document(example().put(key, value ?: JSONObject.NULL))

    @Test
    fun `예시 프로필을 읽으면 샘플 보리와 같다`() {
        assertEquals(ExamplePetProfiles.bori.copy(photoDataUrl = null), FirestoreJson.petProfileOf(document(example())))
    }

    @Test
    fun `owner-link 예시의 서버 프로필 문서도 같다`() {
        val serverDoc = JSONObject(DocsData.read("owner-link.example.json")).getJSONObject("petProfiles/owner-uid-1")
        assertEquals(ExamplePetProfiles.bori.copy(photoDataUrl = null), FirestoreJson.petProfileOf(document(serverDoc)))
    }

    @Test
    fun `형식 버전이 1이 아니면 읽지 않는다`() {
        assertNull(FirestoreJson.petProfileOf(withField("schemaVersion", 2)))
        assertNull(FirestoreJson.petProfileOf(document(example().apply { remove("schemaVersion") })))
    }

    @Test
    fun `정수 doubleValue 형식 버전도 읽는다`() {
        val fields = restFields(example()).put("schemaVersion", JSONObject().put("doubleValue", 1.0))
        val body = JSONObject().put("fields", fields).toString()
        assertEquals("보리", FirestoreJson.petProfileOf(body)?.name)
    }

    @Test
    fun `필수 필드가 없거나 타입이 틀리면 손상이다`() {
        assertNull(FirestoreJson.petProfileOf(withField("name", "")))
        assertNull(FirestoreJson.petProfileOf(withField("name", 3)))
        assertNull(FirestoreJson.petProfileOf(document(example().apply { remove("breed") })))
        assertNull(FirestoreJson.petProfileOf(withField("sex", "unknown")))
        assertNull(FirestoreJson.petProfileOf(withField("birthDateIsEstimated", "false")))
        assertNull(FirestoreJson.petProfileOf(withField("medical", JSONObject().put("conditions", JSONArray()))))
        assertNull(FirestoreJson.petProfileOf("not json"))
        assertNull(FirestoreJson.petProfileOf("""{"name": "x"}"""))
    }

    @Test
    fun `없는 날짜의 생년월일은 손상이다`() {
        assertNull(FirestoreJson.petProfileOf(withField("birthDate", "2023-02-30")))
        assertNull(FirestoreJson.petProfileOf(withField("birthDate", "2023-2-3")))
    }

    @Test
    fun `updatedAt이 UTC 형식이 아니면 손상이다`() {
        assertNull(FirestoreJson.petProfileOf(withField("updatedAt", "2026-11-05T09:00:00+09:00")))
        assertNull(FirestoreJson.petProfileOf(withField("updatedAt", "2026-11-05")))
        assertNull(FirestoreJson.petProfileOf(withField("updatedAt", "2026-11-05T25:00:00Z")))
    }

    @Test
    fun `빈 배열은 values 없이 와도 읽는다`() {
        val medical = JSONObject().put("conditions", JSONArray()).put("allergies", JSONArray()).put("medications", JSONArray())
        val profile = FirestoreJson.petProfileOf(withField("medical", medical))!!
        assertEquals(emptyList<String>(), profile.medical.conditions)
        assertEquals(emptyList<Any>(), profile.medical.medications)
    }

    @Test
    fun `모르는 필드는 무시한다`() {
        assertEquals("보리", FirestoreJson.petProfileOf(withField("weightKg", 30))?.name)
    }

    @Test
    fun `null인 품종·성별·생년월일을 읽는다`() {
        val json = example().put("breed", JSONObject.NULL).put("sex", JSONObject.NULL).put("birthDate", JSONObject.NULL)
        val profile = FirestoreJson.petProfileOf(document(json))!!
        assertNull(profile.breed)
        assertNull(profile.sex)
        assertNull(profile.birthDate)
    }
}
