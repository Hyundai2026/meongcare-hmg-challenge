package com.meongcare.app.data.server

import com.meongcare.app.data.pet.LinkedProfileRead
import com.meongcare.app.data.pet.LinkedProfileReader
import java.io.IOException

/**
 * 연결된 보호자의 반려견 프로필을 읽는다 (Firestore REST, 014). 형식: docs/data/owner-link.md "차가 읽을 때".
 *
 * `GET .../petProfiles/{보호자 uid}` 한 건만 읽는다 (보안 규칙상 허가 문서가 있을 때만 읽힌다).
 *
 * | 응답 | 결과 |
 * |---|---|
 * | 200, 형식 맞음 | 프로필 |
 * | 200, 형식 틀림·버전이 1이 아님 | 읽지 못함 |
 * | 403 | 연결 끊김 (허가가 없어졌다) |
 * | 404 | 등록 안 됨 (보호자가 프로필을 지웠다) |
 * | 그 밖·네트워크 | 읽지 못함 |
 *
 * 로그에는 결과 종류·걸린 시간·HTTP 상태만 남긴다 (반려견 이름·uid·토큰 없음).
 * @param ownerUid 연결된 보호자 uid ([FirestoreOwnerLink]). 연결 전이면 null
 */
class FirestorePetProfileReader(
    private val config: ServerConfig,
    private val auth: AnonymousAuth,
    private val transport: HttpTransport,
    private val ownerUid: () -> String?,
    private val elapsedMillis: () -> Long = System::currentTimeMillis,
    private val log: (String) -> Unit = {},
) : LinkedProfileReader {

    override suspend fun read(): LinkedProfileRead {
        val owner = ownerUid() ?: return LinkedProfileRead.Unavailable.also { log("반려견 정보: 연결 없음") }
        val session = auth.session() ?: return LinkedProfileRead.Unavailable.also { log("반려견 정보: 익명 로그인 실패") }
        val started = elapsedMillis()
        val response = try {
            transport.send(
                HttpRequest(
                    method = "GET",
                    url = "${config.documentsUrl}/petProfiles/$owner",
                    headers = mapOf("Authorization" to "Bearer ${session.idToken}"),
                ),
            )
        } catch (_: IOException) {
            log("반려견 정보: 네트워크 실패")
            return LinkedProfileRead.Unavailable
        }
        val took = elapsedMillis() - started
        val result = when {
            response.status == 403 -> LinkedProfileRead.Revoked
            response.status == 404 -> LinkedProfileRead.NotRegistered
            !response.isSuccessful -> LinkedProfileRead.Unavailable
            else -> FirestoreJson.petProfileOf(response.body)?.let { LinkedProfileRead.Loaded(it) } ?: LinkedProfileRead.Unavailable
        }
        log("반려견 정보: ${labelOf(result)} (HTTP ${response.status}, ${took}ms)")
        return result
    }

    private fun labelOf(result: LinkedProfileRead): String = when (result) {
        is LinkedProfileRead.Loaded -> "서버"
        LinkedProfileRead.NotRegistered -> "등록 안 됨"
        LinkedProfileRead.Revoked -> "연결 끊김"
        LinkedProfileRead.Unavailable -> "읽지 못함"
    }
}
