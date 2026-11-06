package com.meongcare.app.domain.trip

import com.meongcare.app.domain.guidance.GuidanceSource
import com.meongcare.app.domain.guidance.GuidanceState
import com.meongcare.app.domain.hospital.Hospital
import com.meongcare.app.domain.hospital.HospitalCandidate
import com.meongcare.app.domain.hospital.RankedHospital
import java.time.Instant
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds
import kotlin.time.toKotlinDuration
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.transformWhile
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull

/** 정차 화면 안의 병원 찾기 단계 (계획 012 "설계 결정 8"). */
sealed interface FinderStep {
    /** 병원 찾기 화면이 아니다 */
    data object None : FinderStep

    /** 병원 목록 */
    data object Hospitals : FinderStep

    /** 고른 병원 확인 */
    data class Confirm(val choice: RankedHospital) : FinderStep
}

/** 카드를 병원에 보낸 상태. 카드 화면의 전송 줄이 이것을 보인다. */
sealed interface CardDelivery {
    /** [카드 없이 길안내만]으로 출발했다 */
    data object NotSent : CardDelivery

    data object Sending : CardDelivery

    /**
     * 보냈다. [toServer]가 false면 시연용(내장 병원)이라 실제로는 보내지 않았다.
     * [acknowledgedAt]은 병원이 [카드 확인]을 누른 시각(서버 시각). 아직이거나 확인을 읽지 않는 병원이면 null (013 U1)
     */
    data class Sent(val toServer: Boolean, val acknowledgedAt: Instant? = null) : CardDelivery

    /** 보내지 못했다. [HospitalTrip.resend]로 다시 보낸다 */
    data object Failed : CardDelivery
}

/**
 * 고른 병원으로 가는 중인 상태.
 * @param startMinutes 진행 막대의 시작: 이번 안내에서 본 가장 큰 남은 분 (013). 아직 모르면 null.
 *   목록의 이동 어림은 길안내 남은 시간이 아니라서 쓰지 않는다.
 */
data class TripState(val candidate: HospitalCandidate, val delivery: CardDelivery, val startMinutes: Int? = null)

/**
 * 병원을 고르고 출발한 뒤의 일을 맡는다 (계획 012 "설계 결정 8"의 4·7).
 *
 * - [depart]: (1) 고른 병원을 기억하고 (2) 그 병원으로 길안내를 시작하고 (3) 카드와 도착 예정을 뒤에서 보낸다.
 *   **전송이 실패해도 길안내는 계속된다.** 두 번 불러도 한 번만 출발한다.
 * - 안내 중: 남은 분이 바뀌었고 마지막으로 보낸 지 [etaInterval]이 지났을 때만 도착 예정을 보낸다 (1분에 한 번 이하).
 *   다만 처음 보낸 값(목록의 이동 어림)을 내비가 준 첫 남은 시간으로 바꿀 때는 기다리지 않는다.
 *   갱신이 실패하면 로그만 남기고 다음 변화 때 다시 보낸다.
 * - 도착하면 "도착", 안내 중에 취소되면(앱 [안내 취소], 내비 "안내 종료") "취소"를 보낸다. 취소되면 고른 병원을 비운다.
 * - [clear] (도착 화면 [닫기]): 고른 병원을 비우고 아무것도 보내지 않는다.
 *
 * `AppContainer`가 하나 만들어 프로세스 내내 쓴다 (`MedicalCardSession`과 같은 수명).
 * 앱 프로세스가 죽으면 진행 중인 전송 정보는 사라진다 (복원하지 않는다).
 *
 * @param senderFor 병원 출처에 맞는 전송 구현 (서버 병원 → 서버, 내장 병원 → Fake)
 * @param scope 전송과 갱신이 도는 범위. 화면이 사라져도 계속돼야 해서 앱 범위를 준다.
 * @param log 한 일마다 한 줄 (예: "카드 전송: 성공"). **반려견 이름, 증상, 좌표, 병원 이름은 넣지 않는다.**
 */
class HospitalTrip(
    private val guidance: GuidanceSource,
    private val senderFor: (HospitalCandidate) -> CaseSender,
    private val scope: CoroutineScope,
    private val now: () -> Instant = Instant::now,
    private val log: (String) -> Unit = {},
    private val etaInterval: Duration = 60.seconds,
) {
    private val _finder = MutableStateFlow<FinderStep>(FinderStep.None)
    val finder: StateFlow<FinderStep> = _finder.asStateFlow()

    private val _trip = MutableStateFlow<TripState?>(null)

    /** 고른 병원과 카드 전송 상태. 출발 전이면 null */
    val trip: StateFlow<TripState?> = _trip.asStateFlow()

    /** 고른 병원. 카드의 [병원에 전화]가 이 번호로 건다. 출발 전이면 null */
    val selectedHospital: StateFlow<Hospital?> =
        _trip.map { it?.candidate?.hospital }.stateIn(scope, SharingStarted.Eagerly, null)

    private var job: Job? = null
    private var retry = Channel<Unit>(Channel.CONFLATED)

    /** 카드의 [가장 빨리 진료받을 곳 찾기]. 이미 출발했으면 아무것도 하지 않는다 */
    fun openFinder() {
        if (_trip.value == null) _finder.value = FinderStep.Hospitals
    }

    /** 목록에서 병원 한 곳을 골랐다 → 확인 단계 */
    fun choose(choice: RankedHospital) {
        if (_finder.value != FinderStep.None) _finder.value = FinderStep.Confirm(choice)
    }

    /** 확인 화면 [뒤로] → 목록, 목록 [뒤로] → 카드 */
    fun back() {
        _finder.update { if (it is FinderStep.Confirm) FinderStep.Hospitals else FinderStep.None }
    }

    /** 병원 찾기를 닫는다 (카드로 돌아간다) */
    fun closeFinder() {
        _finder.value = FinderStep.None
    }

    /**
     * [출발] (`card` 있음) 또는 [카드 없이 길안내만] (`card` = null).
     * 이미 출발했으면 아무것도 하지 않는다.
     */
    fun depart(choice: RankedHospital, card: CaseCard?) {
        val delivery = if (card == null) CardDelivery.NotSent else CardDelivery.Sending
        val state = TripState(choice.candidate, delivery)
        if (!_trip.compareAndSet(null, state)) return
        _finder.value = FinderStep.None
        retry = Channel(Channel.CONFLATED)
        job = scope.launch { run(state, choice, card) }
    }

    /** 전송 줄의 [다시 보내기]. 실패했을 때만 다시 보낸다 */
    fun resend() {
        if (_trip.value?.delivery == CardDelivery.Failed) retry.trySend(Unit)
    }

    /** 도착 화면 [닫기]: 고른 병원을 비운다. 아무것도 보내지 않는다 */
    fun clear() {
        job?.cancel()
        job = null
        _trip.value = null
        _finder.value = FinderStep.None
    }

    private suspend fun run(state: TripState, choice: RankedHospital, card: CaseCard?) = coroutineScope {
        val phase = MutableStateFlow<Phase>(Phase.Starting)

        // 이동 어림을 목적지에 실어 보낸다. Fake 길안내는 처음 남은 시간으로 쓰고, 차량 내비는 보지 않는다.
        launch { guidance.start(choice.hospital.destination.copy(expectedTravel = choice.travel)) }
        val delivery = card?.let {
            launch { deliver(state.candidate, it, choice.travel?.toCaseMinutes(), phase) }
        }

        val terminal = trackPhase(phase, state.candidate)
        if (terminal == Phase.Cancelled) {
            // 보낸 건(또는 보내는 중인 건)이 있으면 "취소"를 보낼 시간을 준다. 실패해서 기다리는 중이면 바로 멈춘다.
            if (delivery != null) {
                if (_trip.value?.delivery == CardDelivery.Failed) {
                    delivery.cancel()
                } else {
                    withTimeoutOrNull(CANCEL_REPORT_TIMEOUT) { delivery.join() } ?: delivery.cancel()
                }
            }
            _trip.update { if (it?.candidate == state.candidate) null else it }
        }
    }

    /**
     * 길안내 상태를 따라 [phase]를 바꾸고, 도착·취소가 되면 그 값을 돌려준다.
     * 남은 분을 알게 되면 진행 막대의 시작값([TripState.startMinutes])을 올린다 (013).
     */
    private suspend fun trackPhase(phase: MutableStateFlow<Phase>, candidate: HospitalCandidate): Phase {
        var sawGuiding = false
        guidance.state.first { state ->
            when (state) {
                is GuidanceState.Guiding -> {
                    sawGuiding = true
                    val minutes = state.remainingTime?.toCaseMinutes()
                    phase.value = Phase.EnRoute(minutes)
                    minutes?.let { raiseStart(candidate, it) }
                }
                is GuidanceState.Arrived -> phase.value = Phase.Arrived
                // 출발 전의 "안내 없음"은 무시한다. 안내를 본 뒤의 "안내 없음"만 취소다.
                GuidanceState.Idle -> if (sawGuiding) phase.value = Phase.Cancelled
            }
            phase.value.isTerminal
        }
        return phase.value
    }

    private suspend fun deliver(
        candidate: HospitalCandidate,
        card: CaseCard,
        estimateMinutes: Int?,
        phase: StateFlow<Phase>,
    ) {
        val sender = senderFor(candidate)
        var ref: CaseRef? = null
        var sent = CaseEta(estimateMinutes, now())
        var sentIsEstimate = true
        while (ref == null) {
            setDelivery(candidate, CardDelivery.Sending)
            val measured = (phase.value as? Phase.EnRoute)?.minutes
            sent = CaseEta(measured ?: estimateMinutes, now())
            sentIsEstimate = measured == null
            when (val result = sender.send(candidate.hospital, card, sent)) {
                is SendResult.Sent -> {
                    ref = result.ref
                    setDelivery(candidate, CardDelivery.Sent(toServer = candidate.sendsCardToServer))
                    log(if (candidate.sendsCardToServer) "카드 전송: 성공" else "카드 전송: 시연용 (서버로 보내지 않음)")
                }
                is SendResult.Failed -> {
                    setDelivery(candidate, CardDelivery.Failed)
                    log("카드 전송: 실패 (${result.reason})")
                    // 보내는 중에 이미 취소됐으면 병원에 만든 건이 없다. 다시 보내기를 기다리지 않고 끝낸다
                    // (그래야 고른 병원이 바로 비워지고, 취소한 뒤에 새 건을 만들지 않는다).
                    if (phase.value == Phase.Cancelled) return
                    retry.receive()
                }
            }
        }
        follow(sender, ref, phase, first = sent, firstIsEstimate = sentIsEstimate)
    }

    /**
     * 보낸 건의 도착 예정을 갱신하고, 도착·취소를 알린다.
     * @param firstIsEstimate 처음 보낸 값이 목록의 이동 어림이었는지. 그렇다면 내비가 준 첫 값은 기다리지 않고 보낸다.
     */
    private suspend fun follow(sender: CaseSender, ref: CaseRef, phase: StateFlow<Phase>, first: CaseEta, firstIsEstimate: Boolean) {
        var lastMinutes = first.remainingMinutes
        var lastAt = first.measuredAt
        var lastWasEstimate = firstIsEstimate
        phase
            .transformWhile { emit(it); !it.isTerminal }
            .collectLatest { current ->
                when (current) {
                    Phase.Starting -> Unit
                    is Phase.EnRoute -> {
                        val minutes = current.minutes ?: return@collectLatest
                        if (minutes == lastMinutes && !lastWasEstimate) return@collectLatest
                        if (!lastWasEstimate) {
                            val wait = etaInterval - java.time.Duration.between(lastAt, now()).toKotlinDuration()
                            if (wait.isPositive()) delay(wait)
                        }
                        val at = now()
                        if (sender.updateEta(ref, CaseEta(minutes, at))) {
                            lastMinutes = minutes
                            lastAt = at
                            lastWasEstimate = false
                            log("도착 예정 갱신: 성공")
                        } else {
                            log("도착 예정 갱신: 실패")
                        }
                    }
                    Phase.Arrived -> log("도착 알림: " + if (sender.finish(ref, CaseStatus.ARRIVED, now())) "성공" else "실패")
                    Phase.Cancelled -> log("취소 알림: " + if (sender.finish(ref, CaseStatus.CANCELLED, now())) "성공" else "실패")
                }
            }
    }

    private fun setDelivery(candidate: HospitalCandidate, delivery: CardDelivery) {
        _trip.update { if (it?.candidate == candidate) it.copy(delivery = delivery) else it }
    }

    /** 진행 막대의 시작값: 처음 알게 된 남은 분, 그보다 커지면(재탐색 등) 큰 값 */
    private fun raiseStart(candidate: HospitalCandidate, minutes: Int) {
        _trip.update { current ->
            if (current?.candidate != candidate) return@update current
            val start = current.startMinutes
            if (start == null || minutes > start) current.copy(startMinutes = minutes) else current
        }
    }

    private sealed interface Phase {
        val isTerminal: Boolean get() = this == Arrived || this == Cancelled

        data object Starting : Phase
        data class EnRoute(val minutes: Int?) : Phase
        data object Arrived : Phase
        data object Cancelled : Phase
    }

    private companion object {
        val CANCEL_REPORT_TIMEOUT = 10.seconds
    }
}

/**
 * 병원에 보내는 남은 분. 화면(`GuidanceViewModel`의 `toDisplayMinutes`)과 같은 규칙이다:
 * 분 단위로 올림하고(90초 → 2분), 1분보다 작으면 1분.
 */
fun Duration.toCaseMinutes(): Int {
    val minutes = (inWholeMilliseconds + MILLIS_PER_MINUTE - 1).floorDiv(MILLIS_PER_MINUTE)
    return minutes.coerceIn(1L, Int.MAX_VALUE.toLong()).toInt()
}

private const val MILLIS_PER_MINUTE = 60_000L
