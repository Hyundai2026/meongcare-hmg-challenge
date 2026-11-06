package com.meongcare.app.testutil

import java.io.File

/**
 * 저장소 `docs/data/`의 파일을 읽는다. 형식의 기준은 코드가 아니라 그 문서와 예시 JSON이라서,
 * 차 앱 값이 어긋나면 테스트가 깨지게 한다 (웹 테스트와 같은 방식).
 *
 * 위치는 Gradle이 시스템 속성 `meongcare.docsData`로 넘긴다 (app/build.gradle.kts).
 */
object DocsData {
    fun read(name: String): String {
        val dir = System.getProperty("meongcare.docsData")
            ?: error("meongcare.docsData 시스템 속성이 없다. Gradle로 테스트를 돌린다")
        return File(dir, name).readText(Charsets.UTF_8)
    }
}
