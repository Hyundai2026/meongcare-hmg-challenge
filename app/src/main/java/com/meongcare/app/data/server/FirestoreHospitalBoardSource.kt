package com.meongcare.app.data.server

import com.meongcare.app.domain.hospital.HospitalBoard
import com.meongcare.app.domain.hospital.HospitalBoardSource
import com.meongcare.app.domain.hospital.HospitalOrigin
import java.io.IOException
import org.json.JSONException

/**
 * 서버의 제휴 병원 목록 (Firestore REST `GET .../documents/hospitals`). 형식: docs/data/hospital-status.md.
 * 익명 로그인 토큰을 붙인다 (보안 규칙: 로그인한 계정만 읽는다). 하위 컬렉션(staff, cases)은 목록에 들어오지 않는다.
 *
 * 실패는 [HospitalBoard.Unavailable]이다 (취소 말고는 던지지 않는다).
 * @param log 결과 종류·개수·걸린 시간만 (예: "병원 목록: 서버 5곳 (812ms)"). 병원 이름·좌표는 넣지 않는다.
 */
class FirestoreHospitalBoardSource(
    private val config: ServerConfig,
    private val auth: AnonymousAuth,
    private val transport: HttpTransport,
    private val clockMillis: () -> Long = System::currentTimeMillis,
    private val log: (String) -> Unit = {},
) : HospitalBoardSource {

    override suspend fun load(): HospitalBoard {
        val started = clockMillis()
        val result = loadOnce()
        val took = clockMillis() - started
        log(
            when (result) {
                is HospitalBoard.Loaded -> "병원 목록: 서버 ${result.candidates.size}곳 (${took}ms)"
                is HospitalBoard.Unavailable -> "병원 목록: 서버 실패 (${result.reason}, ${took}ms)"
            },
        )
        return result
    }

    private suspend fun loadOnce(): HospitalBoard {
        val session = auth.session() ?: return HospitalBoard.Unavailable("익명 로그인 실패")
        val response = try {
            transport.send(
                HttpRequest(
                    method = "GET",
                    url = "${config.documentsUrl}/hospitals?pageSize=$PAGE_SIZE",
                    headers = mapOf("Authorization" to "Bearer ${session.idToken}"),
                ),
            )
        } catch (_: IOException) {
            return HospitalBoard.Unavailable("네트워크")
        }
        if (!response.isSuccessful) return HospitalBoard.Unavailable("HTTP ${response.status}")
        return try {
            HospitalBoard.Loaded(FirestoreJson.hospitalsOf(response.body), HospitalOrigin.SERVER)
        } catch (_: JSONException) {
            HospitalBoard.Unavailable("형식 오류")
        }
    }

    private companion object {
        /** 제휴 시연 병원은 5곳이다. 순위 화면은 10곳까지 보인다 */
        const val PAGE_SIZE = 50
    }
}
