package com.meongcare.app.ui.hospital

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.meongcare.app.R
import com.meongcare.app.domain.hospital.Congestion
import com.meongcare.app.domain.hospital.HospitalOrigin
import com.meongcare.app.domain.hospital.RankBasis
import com.meongcare.app.domain.hospital.RankedHospital
import com.meongcare.app.domain.hospital.StatusFreshness

/** 목록 위에 보이는 작은 안내 (출품 유의사항: 시연용임을 숨기지 않는다). 화면 글자는 strings.xml `finder_notice_*`. */
enum class FinderNotice {
    /** 서버(병원 웹)에서 받은 대기 시간 */
    SERVER,

    /** 처음부터 내장 시연 병원 (Fake 모드, 서버 설정이 없는 시연) */
    BUILT_IN,

    /** 서버에 닿지 못해 내장 시연 병원으로 대신함 */
    FALLBACK,

    /**
     * 근처 검색 병원 (카카오, 대기 정보 없음). 제휴 목록 전체의 출처가 근처 검색일 일은 없지만,
     * [HospitalOrigin.NEARBY_SEARCH]를 받아도 맞는 안내가 나오게 둔다 (계획 015 단계 1).
     */
    NEARBY,

    /** 이동 시간은 직선 거리 어림값 */
    ESTIMATED_TRAVEL,

    /** 현재 위치가 시연용 Fake 위치 (011 K4) */
    SAMPLE_LOCATION,
}

/**
 * 목록 위 안내를 고른다.
 * - 출처 한 줄은 늘 보인다.
 * - 위치를 알면(이동 시간이 있으면) "직선 거리 어림"을 알린다.
 * - 위치 출처가 Fake면 "시연용 위치 기준"을 알린다. 위치를 몰라 이동 시간을 쓰지 않았으면 알리지 않는다.
 */
fun finderNoticesOf(origin: HospitalOrigin, basis: RankBasis, isLocationSample: Boolean): List<FinderNotice> = buildList {
    add(
        when (origin) {
            HospitalOrigin.SERVER -> FinderNotice.SERVER
            HospitalOrigin.BUILT_IN -> FinderNotice.BUILT_IN
            HospitalOrigin.BUILT_IN_FALLBACK -> FinderNotice.FALLBACK
            HospitalOrigin.NEARBY_SEARCH -> FinderNotice.NEARBY
        },
    )
    if (basis == RankBasis.TRAVEL_PLUS_WAIT) {
        add(FinderNotice.ESTIMATED_TRAVEL)
        if (isLocationSample) add(FinderNotice.SAMPLE_LOCATION)
    }
}

/** 근처 동물병원 묶음에 무엇을 보일지 (계획 015 "설계 결정 8"). */
enum class NearbySection {
    /** 묶음 없음 (기능 꺼짐) */
    HIDDEN,

    /** "현재 위치를 몰라 근처 동물병원은 찾지 않았어요" */
    NEEDS_LOCATION,

    /** 진행 표시 + "근처 동물병원을 찾는 중…" */
    SEARCHING,

    /** 병원 줄 */
    FOUND,

    /** "5km 안에서 찾은 동물병원이 없어요" */
    EMPTY,

    /** "근처 동물병원을 찾지 못했어요" + [다시 찾기] */
    FAILED,

    /** 실패 뒤 다시 찾는 중 (진행 표시) */
    RETRYING,
}

fun nearbySectionOf(state: NearbyState): NearbySection = when (state) {
    NearbyState.Off -> NearbySection.HIDDEN
    NearbyState.NeedsLocation -> NearbySection.NEEDS_LOCATION
    NearbyState.Searching -> NearbySection.SEARCHING
    is NearbyState.Found -> NearbySection.FOUND
    NearbyState.Empty -> NearbySection.EMPTY
    is NearbyState.Failed -> if (state.isRetrying) NearbySection.RETRYING else NearbySection.FAILED
}

/** 근처 묶음 위 작은 안내. 화면 글자는 strings.xml `finder_nearby_notice_*`. */
enum class NearbyNotice {
    /** 시연용 가짜 근처 병원 (Fake 모드) */
    SAMPLE,

    /** 현재 위치(약 100m 단위)를 카카오에 보낸다 (사용자 결정 U2) */
    SENDS_LOCATION,

    /** 장소 정보: 카카오 (U4, K6) */
    SOURCE_KAKAO,
}

/**
 * 근처 묶음 위 안내를 고른다.
 * - 묶음이 없거나 위치를 몰라 찾지 않았으면 안내도 없다 (위치를 보내지 않았다).
 * - 가짜 근처 병원이면 "시연용"만 알린다 (카카오에 아무것도 보내지 않는다).
 * - 실제 검색이면 위치를 보낸다는 것과 출처를 알린다.
 *
 * @param isSampleSource 근처 병원 출처가 Fake인지
 */
fun nearbyNoticesOf(state: NearbyState, isSampleSource: Boolean): List<NearbyNotice> = when {
    state == NearbyState.Off || state == NearbyState.NeedsLocation -> emptyList()
    isSampleSource -> listOf(NearbyNotice.SAMPLE)
    else -> listOf(NearbyNotice.SENDS_LOCATION, NearbyNotice.SOURCE_KAKAO)
}

@Composable
fun nearbyNoticeText(notice: NearbyNotice): String = stringResource(
    when (notice) {
        NearbyNotice.SAMPLE -> R.string.finder_nearby_notice_sample
        NearbyNotice.SENDS_LOCATION -> R.string.finder_nearby_notice_location
        NearbyNotice.SOURCE_KAKAO -> R.string.finder_nearby_notice_source
    },
)

/** 목록 한 줄에 쓰는 값. 글자로 바꾸는 것은 [hospitalRowDetail]이 한다. */
data class HospitalRowValues(
    /** 큰 글씨 "약 N분". 이동과 대기를 둘 다 알 때만 */
    val totalMinutes: Int?,
    /** 이동 어림 분. 위치를 모르면 null */
    val travelMinutes: Int?,
    /** 대기 분. 쓸 만한(30분 안) 정보일 때만 */
    val waitMinutes: Int?,
    /** 혼잡도. 쓸 만한 정보일 때만 */
    val congestion: Congestion?,
    /** 알린 지 몇 분 됐는지 (내림). 정보가 없으면 null */
    val reportedMinutesAgo: Long?,
    val freshness: StatusFreshness,
    val isSample: Boolean,
    /** 대기 시간이 시연용 예시인지 (근처 검색 병원, 계획 015 설계 변경 V2). "(예시)"를 붙인다 */
    val isWaitExample: Boolean = false,
)

fun hospitalRowValuesOf(row: RankedHospital): HospitalRowValues = HospitalRowValues(
    totalMinutes = row.total?.inWholeMinutes?.toInt(),
    travelMinutes = row.travel?.inWholeMinutes?.toInt(),
    waitMinutes = row.wait?.inWholeMinutes?.toInt(),
    congestion = row.status?.congestion?.takeIf { row.freshness == StatusFreshness.FRESH },
    isWaitExample = row.candidate.origin == HospitalOrigin.NEARBY_SEARCH && row.status != null,
    reportedMinutesAgo = row.statusAge?.inWholeMinutes,
    freshness = row.freshness,
    isSample = row.hospital.isSample,
)

/** @param sampleLocationName 시연용 고정 위치 이름 ("서초동", "수정구 시흥동"). [FinderNotice.SAMPLE_LOCATION]에 쓴다 (016) */
@Composable
fun finderNoticeText(notice: FinderNotice, sampleLocationName: String? = null): String = when (notice) {
    FinderNotice.SERVER -> stringResource(R.string.finder_notice_server)
    FinderNotice.BUILT_IN -> stringResource(R.string.finder_notice_built_in)
    FinderNotice.FALLBACK -> stringResource(R.string.finder_notice_fallback)
    FinderNotice.NEARBY -> stringResource(R.string.finder_notice_nearby)
    FinderNotice.ESTIMATED_TRAVEL -> stringResource(R.string.finder_notice_estimated)
    FinderNotice.SAMPLE_LOCATION -> stringResource(
        R.string.finder_notice_sample_location,
        sampleLocationName ?: stringResource(R.string.finder_sample_location_unnamed),
    )
}

@Composable
fun congestionText(congestion: Congestion): String = stringResource(
    when (congestion) {
        Congestion.LOW -> R.string.finder_congestion_low
        Congestion.NORMAL -> R.string.finder_congestion_normal
        Congestion.HIGH -> R.string.finder_congestion_high
    },
)

/** "이동 약 19분 + 대기 10분", 위치를 모르면 "이동 시간 모름 · 대기 10분" 식의 앞부분 */
@Composable
fun travelAndWaitText(values: HospitalRowValues): String {
    val travel = values.travelMinutes?.let { stringResource(R.string.finder_travel, it) }
        ?: stringResource(R.string.finder_travel_unknown)
    val wait = values.waitMinutes?.let {
        stringResource(if (values.isWaitExample) R.string.finder_wait_example else R.string.finder_wait, it)
    }
    return when {
        wait == null -> travel
        values.travelMinutes != null -> travel + stringResource(R.string.finder_plus) + wait
        else -> travel + SEPARATOR + wait
    }
}

/** 대기 정보 상태: "보통 · 2분 전 알림" / "대기 정보 90분 전" / "대기 정보 없음" */
@Composable
fun statusText(values: HospitalRowValues): String = when (values.freshness) {
    // 예시 대기는 알린 시각이 없다 (병원이 알린 것이 아니다)
    StatusFreshness.FRESH -> if (values.isWaitExample) stringResource(R.string.finder_wait_example_status) else listOfNotNull(
        values.congestion?.let { congestionText(it) },
        values.reportedMinutesAgo?.let {
            if (it < 1) stringResource(R.string.finder_reported_just_now)
            else stringResource(R.string.finder_reported_minutes_ago, it.toInt())
        },
    ).joinToString(SEPARATOR)
    // 알린 시각이 지금보다 뒤(시계가 어긋남)면 "0분 전"이 아니라 시각이 맞지 않는다고 알린다
    StatusFreshness.STALE -> values.reportedMinutesAgo?.takeIf { it >= 1 }
        ?.let { stringResource(R.string.finder_status_stale, it.toInt()) }
        ?: stringResource(R.string.finder_status_clock_mismatch)
    StatusFreshness.NONE -> stringResource(R.string.finder_status_none)
}

/** 목록 줄의 작은 글씨 한 줄: "이동 약 19분 + 대기 10분 · 보통 · 2분 전 알림" */
@Composable
fun hospitalRowDetail(values: HospitalRowValues): String =
    // 예시 대기는 "대기 N분(예시)"로 이미 알렸으므로 상태 글자를 덧붙이지 않는다
    if (values.isWaitExample) travelAndWaitText(values) else travelAndWaitText(values) + SEPARATOR + statusText(values)

private const val SEPARATOR = " · "
