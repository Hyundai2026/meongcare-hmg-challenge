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
)

fun hospitalRowValuesOf(row: RankedHospital): HospitalRowValues = HospitalRowValues(
    totalMinutes = row.total?.inWholeMinutes?.toInt(),
    travelMinutes = row.travel?.inWholeMinutes?.toInt(),
    waitMinutes = row.wait?.inWholeMinutes?.toInt(),
    congestion = row.status?.congestion?.takeIf { row.freshness == StatusFreshness.FRESH },
    reportedMinutesAgo = row.statusAge?.inWholeMinutes,
    freshness = row.freshness,
    isSample = row.hospital.isSample,
)

@Composable
fun finderNoticeText(notice: FinderNotice): String = stringResource(
    when (notice) {
        FinderNotice.SERVER -> R.string.finder_notice_server
        FinderNotice.BUILT_IN -> R.string.finder_notice_built_in
        FinderNotice.FALLBACK -> R.string.finder_notice_fallback
        FinderNotice.NEARBY -> R.string.finder_notice_nearby
        FinderNotice.ESTIMATED_TRAVEL -> R.string.finder_notice_estimated
        FinderNotice.SAMPLE_LOCATION -> R.string.finder_notice_sample_location
    },
)

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
    val wait = values.waitMinutes?.let { stringResource(R.string.finder_wait, it) }
    return when {
        wait == null -> travel
        values.travelMinutes != null -> travel + stringResource(R.string.finder_plus) + wait
        else -> travel + SEPARATOR + wait
    }
}

/** 대기 정보 상태: "보통 · 2분 전 알림" / "대기 정보 90분 전" / "대기 정보 없음" */
@Composable
fun statusText(values: HospitalRowValues): String = when (values.freshness) {
    StatusFreshness.FRESH -> listOfNotNull(
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
fun hospitalRowDetail(values: HospitalRowValues): String = travelAndWaitText(values) + SEPARATOR + statusText(values)

private const val SEPARATOR = " · "
