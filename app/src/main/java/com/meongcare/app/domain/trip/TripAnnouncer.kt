package com.meongcare.app.domain.trip

import com.meongcare.app.domain.guidance.GuidanceState
import com.meongcare.app.domain.hospital.HospitalCandidate
import com.meongcare.app.domain.speech.SpeakResult
import com.meongcare.app.domain.speech.Speaker
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.coroutines.cancellation.CancellationException
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull

/**
 * 병원 가는 길의 사건을 소리로 알린다 (docs/plans 017 "설계 결정 2"의 R1~R12).
 *
 * - R1 출발: 고른 병원이 생기면 한 번. 카드를 보내는 중이면 결과를 [departureWait]까지 기다리고,
 *   그 안에 서버로 보냈으면 "보냈어요"를 붙인다.
 * - R2 늦게 보냄: 출발 안내에 "보냈어요"가 없었는데 그 뒤 서버로 보내졌으면 한 번.
 * - R3 못 보냄: 이 안내에서 처음 실패했을 때 한 번 (출발 대기 중에 실패했으면 출발 안내 뒤에).
 * - R4 병원 확인: 확인 시각이 처음 생겼을 때 한 번.
 * - R5 도착: 이 안내에서 안내 중을 본 뒤 도착이 되면 한 번. 그 뒤 생긴 카드 사건은 말하지 않는다.
 * - R6 고른 병원이 비면(취소, 도착 화면 [닫기]) 남은 말을 버리고, 말하는 중이면 멈춘다. 취소는 말하지 않는다.
 * - R7 생긴 순서대로 하나씩 말한다. R8 말하기가 실패하거나 멈춰도 로그만 남기고 다음 사건은 말한다.
 * - R9 앱 범위([scope])에서 돈다. 주행·정차, 앱이 화면에 있든 없든 말한다.
 * - R10 다음 안내는 사건을 처음부터 다시 센다. R11 남은 시간이 바뀌어도 말하지 않는다.
 * - R12 로그에는 사건 종류와 결과만 남긴다 (병원 이름·문장 없음).
 *
 * [HospitalTrip]의 공개 상태([trip])와 길안내 상태([guidance])만 읽는다. 둘 다 hot `StateFlow`라 구독이 늘어도 연결이 늘지 않는다.
 *
 * 알려진 한계: 같은 병원으로 취소 직후 곧바로 다시 출발해서 [trip]의 null을 놓치면 새 안내로 세지 못한다 (StateFlow는 값을 합친다).
 */
class TripAnnouncer(
    private val trip: StateFlow<TripState?>,
    private val guidance: Flow<GuidanceState>,
    private val speaker: Speaker,
    private val scope: CoroutineScope,
    private val log: (String) -> Unit = {},
    private val departureWait: Duration = 5.seconds,
) {
    private var job: Job? = null

    /** `AppContainer`가 한 번 부른다. 두 번 불러도 한 번만 시작한다. */
    fun start() {
        if (job != null) return
        job = scope.launch {
            trip.map { it?.candidate }
                .distinctUntilChanged()
                .collectLatest { candidate -> if (candidate != null) announce(candidate) }
        }
    }

    /** 안내 하나. 고른 병원이 바뀌거나 비면 `collectLatest`가 통째로 취소한다 (R6). */
    private suspend fun announce(candidate: HospitalCandidate) = coroutineScope {
        val name = candidate.hospital.name
        val queue = Channel<TripAnnouncement>(Channel.UNLIMITED)
        launch { for (announcement in queue) speakOne(announcement) }

        val deliveries = trip
            .mapNotNull { state -> state?.takeIf { it.candidate == candidate }?.delivery }
            .distinctUntilChanged()
        val departed = CompletableDeferred<Unit>()
        val arrived = AtomicBoolean(false)

        launch {
            var sawGuiding = false
            guidance.first { state ->
                when (state) {
                    is GuidanceState.Guiding -> {
                        sawGuiding = true
                        false
                    }
                    is GuidanceState.Arrived -> sawGuiding
                    GuidanceState.Idle -> false
                }
            }
            arrived.set(true)
            departed.await()
            queue.send(TripAnnouncement.Arrived(name))
        }

        val settled = withTimeoutOrNull(departureWait) { deliveries.first { it != CardDelivery.Sending } }
        val cardSent = (settled as? CardDelivery.Sent)?.toServer == true
        queue.send(TripAnnouncement.Departed(name, cardSent))
        departed.complete(Unit)

        var sentAnnounced = cardSent
        var failedAnnounced = false
        var ackAnnounced = false
        deliveries.collect { delivery ->
            if (arrived.get()) return@collect
            when (delivery) {
                is CardDelivery.Sent -> if (delivery.toServer) {
                    if (!sentAnnounced) {
                        sentAnnounced = true
                        queue.send(TripAnnouncement.CardSent(name))
                    }
                    if (delivery.acknowledgedAt != null && !ackAnnounced) {
                        ackAnnounced = true
                        queue.send(TripAnnouncement.Acknowledged(name))
                    }
                }
                CardDelivery.Failed -> if (!failedAnnounced) {
                    failedAnnounced = true
                    queue.send(TripAnnouncement.CardFailed)
                }
                CardDelivery.NotSent, CardDelivery.Sending -> Unit
            }
        }
    }

    private suspend fun speakOne(announcement: TripAnnouncement) {
        val result = try {
            speaker.speak(announcement.spokenText())
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            null
        }
        log("소리 안내: ${announcement.logName} → ${result?.logText ?: "실패(예외)"}")
    }

    private val SpeakResult.logText: String
        get() = when (this) {
            SpeakResult.Done -> "끝까지"
            SpeakResult.Stopped -> "멈춤"
            is SpeakResult.Failed -> "실패(${reason.name})"
        }
}
