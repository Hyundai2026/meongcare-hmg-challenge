package com.meongcare.app.domain.hospital

import java.time.Instant

/** 예시 대기 시간의 범위 (분, 5분 단위) */
private const val MIN_EXAMPLE_WAIT = 5
private const val MAX_EXAMPLE_WAIT = 60

/**
 * 근처 검색(카카오) 병원에 붙이는 **시연용 예시 대기 상태** (계획 015 "설계 변경 V2", 사용자 결정 2026-11-08).
 *
 * 실제 대기 정보가 아니다. 화면은 줄마다 "(예시)"로 알린다 (출품 유의사항: 실존 병원에 사실이 아닌 정보가 실제처럼 보이지 않게).
 * - 병원 이름·좌표로 정하므로 같은 병원은 늘 같은 값이다 (새로 고칠 때마다 순위가 바뀌지 않는다).
 * - 5~60분, 5분 단위. 혼잡도는 대기로 정한다. 응급 접수 중, 알린 시각은 [now] (늘 쓸 만한 정보로 본다).
 */
fun exampleStatusOf(hospital: Hospital, now: Instant): HospitalStatus {
    val key = "${hospital.name}|${hospital.destination.latitude}|${hospital.destination.longitude}"
    val steps = (MAX_EXAMPLE_WAIT - MIN_EXAMPLE_WAIT) / 5 + 1
    val wait = MIN_EXAMPLE_WAIT + Math.floorMod(stableHash(key), steps) * 5
    val congestion = when {
        wait < 15 -> Congestion.LOW
        wait < 35 -> Congestion.NORMAL
        else -> Congestion.HIGH
    }
    return HospitalStatus(waitMinutes = wait, congestion = congestion, acceptingEmergency = true, updatedAt = now)
}

/** 플랫폼과 상관없이 같은 값: FNV-1a 뒤에 murmur3 마무리 섞기 (FNV만으로는 작은 수로 나눈 나머지가 고르지 않다) */
private fun stableHash(text: String): Int {
    var hash = 0x811C9DC5.toInt()
    for (byte in text.toByteArray(Charsets.UTF_8)) {
        hash = hash xor (byte.toInt() and 0xFF)
        hash *= 0x01000193
    }
    hash = hash xor (hash ushr 16)
    hash *= 0x85EBCA6B.toInt()
    hash = hash xor (hash ushr 13)
    hash *= 0xC2B2AE35.toInt()
    return hash xor (hash ushr 16)
}
