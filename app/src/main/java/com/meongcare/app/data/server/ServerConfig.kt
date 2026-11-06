package com.meongcare.app.data.server

/**
 * 서버(Firebase: Firestore + 익명 Auth) 주소 (계획 012 "설계 결정 3"). 순수 Kotlin이라 JVM으로 시험한다.
 *
 * @param documentsUrl `.../v1/projects/<id>/databases/(default)/documents` (끝에 `/` 없음)
 * @param signUpUrl 익명 가입 주소 (`accounts:signUp?key=...`)
 * @param refreshUrl 익명 토큰 갱신 주소 (securetoken `v1/token?key=...`, 013). 갱신 토큰은 URL이 아니라 몸통에 넣는다
 */
data class ServerConfig(
    val projectId: String,
    val documentsUrl: String,
    val signUpUrl: String,
    val refreshUrl: String,
    val isEmulator: Boolean,
) {
    /** Firestore commit 요청에 쓰는 문서 이름 앞부분: `projects/<id>/databases/(default)/documents` */
    val documentsName: String get() = "projects/$projectId/databases/(default)/documents"
}

/**
 * BuildConfig 값(secrets.properties)으로 서버 주소를 정한다. 서버를 쓰지 않으면 null이다.
 *
 * - 프로젝트 ID가 비면 null (설정 없음 = CI·새 팀원 PC. 앱은 내장 시연 병원으로 돈다).
 * - debug이고 에뮬레이터 주소가 있으면 PC의 Firebase 에뮬레이터 (`http://<host>:8080`, `:9099`).
 *   API 키가 비면 `demo-key` (에뮬레이터는 키를 검사하지 않는다, 2026-11-06 확인).
 * - **release는 에뮬레이터 주소를 무시한다** (평문 HTTP 금지, ux-guidelines "HTTPS·TLS 1.2+"). API 키가 비면 null.
 */
fun serverConfigOf(projectId: String, apiKey: String, emulatorHost: String, isDebugBuild: Boolean): ServerConfig? {
    val project = projectId.trim()
    if (project.isEmpty()) return null
    val host = emulatorHost.trim()
    val key = apiKey.trim()
    if (isDebugBuild && host.isNotEmpty()) {
        val urlHost = if (host.contains(':') && !host.startsWith("[")) "[$host]" else host
        val emulatorKey = key.ifEmpty { "demo-key" }
        return ServerConfig(
            projectId = project,
            documentsUrl = "http://$urlHost:$FIRESTORE_EMULATOR_PORT/v1/projects/$project/databases/(default)/documents",
            signUpUrl = "http://$urlHost:$AUTH_EMULATOR_PORT/identitytoolkit.googleapis.com/v1/accounts:signUp?key=$emulatorKey",
            // Auth 에뮬레이터도 갱신을 받는다 (2026-11-06 확인: form 몸통, 응답 id_token·refresh_token·expires_in·user_id)
            refreshUrl = "http://$urlHost:$AUTH_EMULATOR_PORT/securetoken.googleapis.com/v1/token?key=$emulatorKey",
            isEmulator = true,
        )
    }
    if (key.isEmpty()) return null
    return ServerConfig(
        projectId = project,
        documentsUrl = "https://firestore.googleapis.com/v1/projects/$project/databases/(default)/documents",
        signUpUrl = "https://identitytoolkit.googleapis.com/v1/accounts:signUp?key=$key",
        refreshUrl = "https://securetoken.googleapis.com/v1/token?key=$key",
        isEmulator = false,
    )
}

private const val FIRESTORE_EMULATOR_PORT = 8080
private const val AUTH_EMULATOR_PORT = 9099
