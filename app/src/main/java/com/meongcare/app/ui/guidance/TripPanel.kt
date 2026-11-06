package com.meongcare.app.ui.guidance

import com.meongcare.app.domain.hospital.HospitalStatus
import com.meongcare.app.domain.trip.CardDelivery
import com.meongcare.app.domain.trip.TripState
import java.time.Instant
import java.time.LocalTime
import java.time.ZoneId
import java.time.temporal.ChronoUnit

/**
 * 안내 영역(정차 화면)과 주행 화면이 보여 줄 것 (013). [tripPanelOf]가 만든다.
 *
 * @param remainingMinutes null = 경로를 찾는 중
 * @param arrivesAt 도착 예정 = 지금 + 남은 분, 분 아래는 버린다 (병원 웹 `clockText`와 같다). 남은 분을 모르거나 도착했으면 null
 * @param progress 0.0(출발) ~ 1.0. 시작값이나 남은 분을 모르면 null (막대를 숨긴다)
 * @param statusAtDeparture 출발할 때 받은 병원 상태 (대기 분·혼잡도). 없으면 null
 */
data class TripPanel(
    val destinationName: String,
    val remainingMinutes: Int?,
    val arrivesAt: LocalTime?,
    val progress: Float?,
    val statusAtDeparture: HospitalStatus?,
    val delivery: DeliveryLine,
    val arrived: Boolean,
)

/** 진료 카드 전송·병원 확인 줄 */
sealed interface DeliveryLine {
    /** 출발 기록이 없다 (줄 없음) */
    data object None : DeliveryLine

    data object Sending : DeliveryLine

    /** 서버로 보냈고 병원 확인을 기다린다 */
    data object SentWaiting : DeliveryLine

    /** 병원이 카드를 확인했다. [at]은 기기 시간대의 시:분 */
    data class Acknowledged(val at: LocalTime) : DeliveryLine

    /** 시연용 내장 병원: 실제로 보내지 않았다 (확인 줄이 없다) */
    data object Sample : DeliveryLine

    data object Failed : DeliveryLine

    /** [카드 없이 길안내만] */
    data object NotSent : DeliveryLine
}

/**
 * 길안내 상태와 고른 병원(전송 상태)을 합쳐 안내 영역을 만든다. 안내 중·도착이 아니면 null.
 * 이름은 길안내 상태의 목적지 이름을 쓴다.
 */
fun tripPanelOf(guidance: GuidanceUiState, trip: TripState?, now: Instant, zone: ZoneId): TripPanel? {
    val delivery = trip?.delivery.toLine(zone)
    val status = trip?.candidate?.status
    return when (guidance) {
        is GuidanceUiState.Idle -> null
        is GuidanceUiState.Guiding -> {
            val minutes = guidance.remainingMinutes
            TripPanel(
                destinationName = guidance.destinationName,
                remainingMinutes = minutes,
                arrivesAt = minutes?.let { clockOf(now.plusSeconds(it * 60L), zone) },
                progress = progressOf(start = trip?.startMinutes, remaining = minutes),
                statusAtDeparture = status,
                delivery = delivery,
                arrived = false,
            )
        }
        is GuidanceUiState.Arrived -> TripPanel(
            destinationName = guidance.destinationName,
            remainingMinutes = null,
            arrivesAt = null,
            progress = null,
            statusAtDeparture = status,
            delivery = delivery,
            arrived = true,
        )
    }
}

/** 시작 대비 지난 비율 (0~1). 시작이 없거나 0 이하, 남은 분을 모르면 null */
internal fun progressOf(start: Int?, remaining: Int?): Float? {
    if (start == null || start <= 0 || remaining == null) return null
    return ((start - remaining).toFloat() / start).coerceIn(0f, 1f)
}

private fun CardDelivery?.toLine(zone: ZoneId): DeliveryLine = when (this) {
    null -> DeliveryLine.None
    CardDelivery.Sending -> DeliveryLine.Sending
    CardDelivery.Failed -> DeliveryLine.Failed
    CardDelivery.NotSent -> DeliveryLine.NotSent
    is CardDelivery.Sent -> when {
        !toServer -> DeliveryLine.Sample
        acknowledgedAt != null -> DeliveryLine.Acknowledged(clockOf(acknowledgedAt, zone))
        else -> DeliveryLine.SentWaiting
    }
}

private fun clockOf(instant: Instant, zone: ZoneId): LocalTime =
    instant.atZone(zone).toLocalTime().truncatedTo(ChronoUnit.MINUTES)
