package com.meongcare.app.data.server

/** Functions 에뮬레이터 포트 (`firebase/firebase.json`) */
private const val FUNCTIONS_EMULATOR_PORT = 5001

/** 함수 지역 (`firebase/functions/index.js`, Firestore 서울과 같게) */
private const val FUNCTIONS_REGION = "asia-northeast3"

/**
 * 근처 동물병원 함수 `nearbyVets`의 주소 (계획 015 "설계 결정 6").
 *
 * 함수는 배포하지 않으므로(D1) **debug 빌드 + 에뮬레이터 주소**가 있을 때만 있다. 그 밖은 null이다.
 * 형식은 `http://<host>:5001/<projectId>/asia-northeast3/nearbyVets` (docs/data/nearby-vets.md F5, 실제 호출로 확인).
 * IPv6 호스트는 `[...]`로 감싼다 ([serverConfigOf]와 같은 규칙).
 */
fun nearbyFunctionUrlOf(projectId: String, emulatorHost: String, isDebugBuild: Boolean): String? {
    val project = projectId.trim()
    val host = emulatorHost.trim()
    if (!isDebugBuild || project.isEmpty() || host.isEmpty()) return null
    val urlHost = if (host.contains(':') && !host.startsWith("[")) "[$host]" else host
    return "http://$urlHost:$FUNCTIONS_EMULATOR_PORT/$project/$FUNCTIONS_REGION/nearbyVets"
}
