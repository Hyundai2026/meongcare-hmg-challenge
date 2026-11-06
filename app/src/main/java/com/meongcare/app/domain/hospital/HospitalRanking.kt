package com.meongcare.app.domain.hospital

import com.meongcare.app.domain.location.CurrentLocation
import java.time.Instant
import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes
import kotlin.time.toKotlinDuration

/** 순위 규칙의 값 (계획 012 "설계 결정 2"). */
data class RankingRules(
    /** 이보다 오래된 대기 정보는 "지남". 정확히 이 값은 아직 유효하다 */
    val staleAfter: Duration = 30.minutes,
    /** 알린 시각이 지금보다 이보다 더 뒤면 시계가 어긋난 것으로 보고 "지남"으로 본다 */
    val futureTolerance: Duration = 5.minutes,
    /** 각 묶음에 보일 최대 개수 */
    val maxPerGroup: Int = 10,
)

/** 무엇을 기준으로 순위를 매겼는지. */
enum class RankBasis {
    /** 이동 어림 + 대기 */
    TRAVEL_PLUS_WAIT,

    /** 현재 위치를 몰라 대기 시간만으로 (이동 시간은 비운다) */
    WAIT_ONLY,
}

/** 대기 정보가 쓸 만한지. */
enum class StatusFreshness {
    /** 30분 안에 알린 정보 */
    FRESH,

    /** 30분보다 오래됐거나 시계가 어긋남 */
    STALE,

    /** 알린 정보가 없음 */
    NONE,
}

/** 순위 화면의 한 줄. */
data class RankedHospital(
    val candidate: HospitalCandidate,
    /** 이동 어림. 현재 위치를 모르면 null */
    val travel: Duration?,
    val freshness: StatusFreshness,
    /** 알린 지 얼마나 됐는지 (미래 시각이면 0). 정보가 없으면 null */
    val statusAge: Duration?,
) {
    val hospital: Hospital get() = candidate.hospital
    val status: HospitalStatus? get() = candidate.status

    /** 대기 시간. 쓸 만한(FRESH) 정보일 때만 있다 */
    val wait: Duration? get() = candidate.status?.takeIf { freshness == StatusFreshness.FRESH }?.waitMinutes?.minutes

    /** 이동 + 대기. 둘 다 알 때만 있다 */
    val total: Duration? get() = travel?.let { t -> wait?.let { t + it } }
}

data class HospitalRanking(
    /** 대기 정보가 쓸 만하고 응급 접수 중인 곳. 빠른 순. 고를 수 있다 */
    val ranked: List<RankedHospital>,
    /** 대기 정보가 없거나 지난 곳. [ranked] 뒤에 보인다. 고를 수 있다 */
    val unranked: List<RankedHospital>,
    /** 쓸 만한 정보로 "응급 접수 안 함"이라고 알린 곳. 이름만 보이고 고를 수 없다 */
    val notAccepting: List<RankedHospital>,
    val basis: RankBasis,
) {
    /** 1위 ("가장 빠름") */
    val fastest: RankedHospital? get() = ranked.firstOrNull()
}

/**
 * 제휴 병원들을 "가장 빨리 진료받을 수 있는" 순으로 나눈다. 순수 함수다 (같은 입력이면 항상 같은 순서).
 *
 * | 경우 | 규칙 |
 * |---|---|
 * | 순위 값 | 이동 어림 + 대기 분. 혼잡도는 표시만 한다 (대기 분에 이미 들어 있다) |
 * | 동점 | 합계 → 이동 시간 → 이름 → id |
 * | 응급 접수 안 함 | 정보가 쓸 만하면 [HospitalRanking.notAccepting] |
 * | 정보 지남 | `지금 - 알린 시각 > 30분`, 또는 알린 시각이 5분 넘게 미래 → [HospitalRanking.unranked]. 그 "응급 접수 안 함"도 믿지 않는다 |
 * | 정보 없음 | [HospitalRanking.unranked] |
 * | 위치 모름 | [RankBasis.WAIT_ONLY]: 대기 → 이름 → id. 정보 없는 곳은 이름 순 |
 *
 * @param from 현재 위치. null이면 위치를 모른다 (내비 준비 전 등). 다른 위치로 대신 계산하지 않는다.
 */
fun rankHospitals(
    candidates: List<HospitalCandidate>,
    from: CurrentLocation?,
    now: Instant,
    rules: RankingRules = RankingRules(),
): HospitalRanking {
    val rows = candidates.map { candidate ->
        val (freshness, age) = freshnessOf(candidate.status, now, rules)
        RankedHospital(
            candidate = candidate,
            travel = from?.let { estimateTravel(it, candidate.hospital.destination) },
            freshness = freshness,
            statusAge = age,
        )
    }
    val fresh = rows.filter { it.freshness == StatusFreshness.FRESH }
    val accepting = fresh.filter { it.status!!.acceptingEmergency }
    val notAccepting = fresh.filterNot { it.status!!.acceptingEmergency }
    val unknown = rows.filter { it.freshness != StatusFreshness.FRESH }

    val basis = if (from == null) RankBasis.WAIT_ONLY else RankBasis.TRAVEL_PLUS_WAIT
    val byName = compareBy<RankedHospital> { it.hospital.name }.thenBy { it.hospital.id.orEmpty() }
    val rankedOrder = when (basis) {
        RankBasis.TRAVEL_PLUS_WAIT -> compareBy<RankedHospital> { it.total }.thenBy { it.travel }.then(byName)
        RankBasis.WAIT_ONLY -> compareBy<RankedHospital> { it.wait }.then(byName)
    }
    val unrankedOrder = when (basis) {
        RankBasis.TRAVEL_PLUS_WAIT -> compareBy<RankedHospital> { it.travel }.then(byName)
        RankBasis.WAIT_ONLY -> byName
    }
    return HospitalRanking(
        ranked = accepting.sortedWith(rankedOrder).take(rules.maxPerGroup),
        unranked = unknown.sortedWith(unrankedOrder).take(rules.maxPerGroup),
        notAccepting = notAccepting.sortedWith(byName).take(rules.maxPerGroup),
        basis = basis,
    )
}

private fun freshnessOf(status: HospitalStatus?, now: Instant, rules: RankingRules): Pair<StatusFreshness, Duration?> {
    if (status == null) return StatusFreshness.NONE to null
    val age = java.time.Duration.between(status.updatedAt, now).toKotlinDuration()
    val freshness = when {
        age > rules.staleAfter -> StatusFreshness.STALE
        -age > rules.futureTolerance -> StatusFreshness.STALE
        else -> StatusFreshness.FRESH
    }
    return freshness to if (age.isNegative()) Duration.ZERO else age
}
