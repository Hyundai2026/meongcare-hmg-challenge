package com.meongcare.app.data.server

import com.meongcare.app.domain.guidance.Destination
import com.meongcare.app.domain.hospital.Hospital
import com.meongcare.app.domain.hospital.NearbyVetSource
import com.meongcare.app.domain.hospital.NearbyVets
import com.meongcare.app.domain.location.CurrentLocation
import java.io.IOException
import kotlin.coroutines.cancellation.CancellationException
import org.json.JSONException
import org.json.JSONObject

/**
 * 근처 동물병원을 Firebase Function `nearbyVets`(카카오 로컬)로 찾는다 (계획 015 "설계 결정 6").
 *
 * - Firebase SDK 없이 callable 규약대로 HTTP POST를 보낸다: 몸통 `{"data": {...}}`, 성공 몸통 `{"result": {...}}`.
 * - 익명 계정은 병원·반려견과 같은 것을 쓴다 (014 `serverClient`).
 * - 좌표는 그대로 보낸다. 반올림(약 100m)은 서버가 한다 (U2).
 * - 결과는 저장하지 않는다 (카카오 운영정책). 로그에는 개수·걸린 시간·실패 이유만 (좌표·이름·토큰 없음).
 * - 형식: docs/data/nearby-vets.md. 읽는 쪽은 짐작하지 않는다: 틀린 항목은 빼고, 틀린 형식은 실패로 본다.
 */
class FunctionNearbyVetSource(
    private val url: String,
    private val auth: AnonymousAuth,
    private val transport: HttpTransport,
    private val log: (String) -> Unit = {},
    private val clock: () -> Long = System::currentTimeMillis,
) : NearbyVetSource {

    override suspend fun search(center: CurrentLocation): NearbyVets {
        val started = clock()
        val result = try {
            searchOnce(center)
        } catch (e: CancellationException) {
            throw e
        } catch (_: Exception) {
            NearbyVets.Unavailable("오류")
        }
        val took = clock() - started
        log(
            when (result) {
                is NearbyVets.Found -> "근처 병원: ${result.hospitals.size}곳 (${took}ms)"
                is NearbyVets.Unavailable -> "근처 병원: 실패 (${result.reason}, ${took}ms)"
            },
        )
        return result
    }

    private suspend fun searchOnce(center: CurrentLocation): NearbyVets {
        val session = auth.session() ?: return NearbyVets.Unavailable("인증 실패")
        val body = JSONObject()
            .put("data", JSONObject().put("latitude", center.latitude).put("longitude", center.longitude))
            .toString()
        val response = try {
            transport.send(
                HttpRequest(
                    method = "POST",
                    url = url,
                    headers = mapOf(
                        "Authorization" to "Bearer ${session.idToken}",
                        "Content-Type" to "application/json; charset=utf-8",
                    ),
                    body = body,
                ),
            )
        } catch (_: IOException) {
            return NearbyVets.Unavailable("네트워크")
        }
        if (!response.isSuccessful) return NearbyVets.Unavailable("HTTP ${response.status}")
        return try {
            parse(response.body)
        } catch (_: JSONException) {
            NearbyVets.Unavailable("형식 오류")
        }
    }

    private fun parse(responseBody: String): NearbyVets {
        val root = JSONObject(responseBody)
        if (root.has("error")) return NearbyVets.Unavailable("함수 오류")
        val result = root.optJSONObject("result") ?: return NearbyVets.Unavailable("형식 오류")
        // 짐작하지 않는다: 정수 1만 받는다 (문자열 "1"·소수 1.5는 형식 오류. optInt는 둘 다 1로 바꾼다)
        val version = result.opt("schemaVersion")
        val isVersionOne = (version is Int && version == SCHEMA_VERSION) || (version is Long && version == SCHEMA_VERSION.toLong())
        if (!isVersionOne) return NearbyVets.Unavailable("형식 오류")
        val vets = result.optJSONArray("vets") ?: return NearbyVets.Unavailable("형식 오류")
        val hospitals = (0 until vets.length()).mapNotNull { index -> vets.optJSONObject(index)?.let(::hospitalOf) }
        return NearbyVets.Found(hospitals)
    }

    /** 항목 하나. 틀리면 null (그 항목만 뺀다). */
    private fun hospitalOf(item: JSONObject): Hospital? {
        val name = (item.opt("name") as? String)?.trim().orEmpty()
        if (name.isEmpty() || name.length > MAX_NAME_LENGTH) return null
        val latitude = (item.opt("latitude") as? Number)?.toDouble() ?: return null
        val longitude = (item.opt("longitude") as? Number)?.toDouble() ?: return null
        if (!latitude.isFinite() || latitude !in -90.0..90.0) return null
        if (!longitude.isFinite() || longitude !in -180.0..180.0) return null
        val phone = (item.opt("phone") as? String)?.trim()?.ifEmpty { null }
        val address = (item.opt("address") as? String)?.trim()?.ifEmpty { null }
        return Hospital(
            destination = Destination(name = name, latitude = latitude, longitude = longitude, address = address),
            phoneNumber = phone,
            isSample = false,
            id = null,
        )
    }

    private companion object {
        const val SCHEMA_VERSION = 1

        /** 이보다 긴 이름은 형식 오류로 본다 (화면이 깨지지 않게) */
        const val MAX_NAME_LENGTH = 40
    }
}
