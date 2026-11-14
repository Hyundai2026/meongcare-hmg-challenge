package com.meongcare.app.ui.trip

import com.meongcare.app.domain.inquiry.InquiryRow
import com.meongcare.app.domain.inquiry.InquiryStatus
import com.meongcare.app.domain.trip.CardDelivery
import com.meongcare.app.domain.trip.TripState
import com.meongcare.app.ui.guidance.TripPanel

/**
 * 길안내 영역([TripPanel]) + 고른 병원([TripState]) + 문의 답변([InquiryRow]) → "병원으로 이동 중" 화면 값.
 * 규칙은 디자인 시안 그대로다 (카드 없이 출발 → 카드 없음, 진료 가능 답 → 받음, 진료 불가 → 불가, 그 밖 → 미확인).
 *
 * @param reply 이 병원의 문의 답. 문의하지 않은 병원(홈 [병원 찾기])이면 null
 * @param waitText 병원 대기 줄 (진료 가능 답일 때만 화면이 글자로 만든다). null = 미확인
 */
fun tripViewOf(
    panel: TripPanel,
    trip: TripState?,
    reply: InquiryRow?,
    petName: String?,
    waitText: String?,
    relaxOn: Boolean,
    relaxText: String,
    arrivesAt: java.time.LocalTime? = null,
): TripView = TripView(
    hospitalName = panel.destinationName,
    // 숫자가 줄어들지 않게 출발할 때의 이동 시간과 도착 예정 시각을 쓴다 (사용자 요청 2026-11-14)
    remainingMinutes = trip?.candidate?.hospital?.destination?.expectedTravel?.inWholeMinutes?.toInt() ?: panel.remainingMinutes,
    arrivesAt = arrivesAt ?: panel.arrivesAt,
    reply = when {
        trip?.delivery == CardDelivery.NotSent -> TripReply.NO_CARD
        reply?.status == InquiryStatus.AVAILABLE -> TripReply.ACCEPTED
        reply?.status == InquiryStatus.UNAVAILABLE -> TripReply.REFUSED
        else -> TripReply.UNKNOWN
    },
    petName = petName,
    waitText = waitText.takeIf { reply?.status == InquiryStatus.AVAILABLE },
    relaxOn = relaxOn,
    relaxText = relaxText,
    canCall = trip?.candidate?.hospital?.phoneNumber != null,
)
