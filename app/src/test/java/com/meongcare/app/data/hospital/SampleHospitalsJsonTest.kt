package com.meongcare.app.data.hospital

import com.meongcare.app.domain.hospital.Congestion
import com.meongcare.app.testutil.DocsData
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Test

/** 내장 시연 병원이 docs/data/hospitals.sample.json과 같은지 (형식의 기준은 그 파일이다). */
class SampleHospitalsJsonTest {

    @Test
    fun `내장 시연 병원이 hospitals_sample_json과 같다`() {
        val json = JSONObject(DocsData.read("hospitals.sample.json")).getJSONArray("hospitals")
        assertEquals(json.length(), SampleHospitals.partners.size)
        for (i in 0 until json.length()) {
            val entry = json.getJSONObject(i)
            val doc = entry.getJSONObject("document")
            val status = doc.getJSONObject("status")
            val partner = SampleHospitals.partners[i]
            val hospital = partner.hospital
            val id = entry.getString("id")
            assertEquals(id, hospital.id)
            assertEquals(id, 1, doc.getInt("schemaVersion"))
            assertEquals(id, doc.getString("name"), hospital.name)
            assertEquals(id, doc.getString("phone"), hospital.phoneNumber)
            assertEquals(id, doc.getDouble("latitude"), hospital.destination.latitude, 0.0)
            assertEquals(id, doc.getDouble("longitude"), hospital.destination.longitude, 0.0)
            assertEquals(id, doc.getBoolean("isSample"), hospital.isSample)
            assertEquals(id, status.getInt("waitMinutes"), partner.waitMinutes)
            assertEquals(id, congestionOf(status.getString("congestion")), partner.congestion)
            assertEquals(id, status.getBoolean("acceptingEmergency"), partner.acceptingEmergency)
            assertEquals(id, entry.getLong("seedUpdatedMinutesAgo"), partner.updatedMinutesAgo)
        }
    }

    @Test
    fun `010의 고정 시연 병원은 제휴 목록의 demo-main과 같은 곳이다`() {
        val main = SampleHospitals.partners.single { it.hospital.id == "demo-main" }.hospital
        assertEquals(SampleHospitals.demo.destination, main.destination)
        assertEquals(SampleHospitals.demo.phoneNumber, main.phoneNumber)
    }

    private fun congestionOf(value: String) = when (value) {
        "low" -> Congestion.LOW
        "normal" -> Congestion.NORMAL
        "high" -> Congestion.HIGH
        else -> error("모르는 혼잡도 $value")
    }
}
