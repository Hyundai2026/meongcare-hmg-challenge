package com.meongcare.app.ui.link

import com.meongcare.app.domain.link.LinkCode
import com.meongcare.app.domain.link.remainingOf
import java.time.Instant

/**
 * 연결 코드 화면의 "남은 시간 02:59"와 진행 막대 (계획 018, 디자인 프로토타입 1번 화면).
 *
 * @param text "mm:ss". 남은 초를 올림하지 않고 내림한다 (프로토타입과 같다). 만료면 "00:00"
 * @param fraction 남은 비율 0.0~1.0 (막대 채움). 10분 = 1.0
 */
data class LinkCountdown(val text: String, val fraction: Float)

/** [createdAt]에 만든 코드의 [now] 시점 남은 시간. 순수 함수다. 범위 처리는 [remainingOf]를 따른다 (0~10분). */
fun linkCountdownOf(createdAt: Instant, now: Instant): LinkCountdown {
    val left = remainingOf(createdAt, now)
    val seconds = left.seconds
    val text = "%02d:%02d".format(seconds / 60, seconds % 60)
    val fraction = (left.toMillis().toDouble() / LinkCode.VALIDITY.toMillis()).toFloat().coerceIn(0f, 1f)
    return LinkCountdown(text, fraction)
}
