package com.meongcare.app.domain.inquiry

import java.time.Duration
import java.time.Instant

/**
 * 동물병원 문의 (계획 020, 디자인 프로토타입 `search`→`hosp` 상태 규칙). **답변은 앱 안 시연 응답이다** ([InquiryScript]):
 * 병원 웹과 연결하지 않는다 (사용자 결정 2026-11-13). 시간은 밖에서 넣는다 (테스트·화면이 같은 규칙).
 *
 * - [start]: 이동 시간이 짧은 [FIRST_ASK]곳에 문의한다. [expand]: 나머지 전부에 문의한다.
 * - 한 곳의 상태: 문의 전 / 답변 대기(문의 후 [DELAY_AFTER] 이하) / 응답 지연(초과) / 진료 불가 / 진료 가능.
 * - 진료 가능이면 남은 대기 `wl = max(0, w - 응답 뒤 지난 분)`, **진료 시작까지 = max(이동, wl)**, 도착 후 대기 = max(0, wl - 이동).
 */
class VetInquiry(
    private val hospitals: List<InquiryHospital>,
    private val script: InquiryScript,
) {
    private val askedAt = linkedMapOf<String, Instant>()
    private var startedAt: Instant? = null
    private var promptDismissed = false

    /** 새 문의: 가까운 [FIRST_ASK]곳 */
    fun start(now: Instant) {
        askedAt.clear()
        promptDismissed = false
        startedAt = now
        hospitals.sortedBy { it.travelMinutes }.take(FIRST_ASK).forEach { askedAt[it.key] = now }
    }

    /** [다른 병원 N곳에도 문의]: 아직 문의하지 않은 병원 전부. 그 병원들의 응답 시계는 지금부터. 더한 곳 수를 돌려준다 */
    fun expand(now: Instant): Int {
        if (startedAt == null) return 0
        val rest = hospitals.filter { it.key !in askedAt }
        rest.forEach { askedAt[it.key] = now }
        return rest.size
    }

    /** 응답 없음 팝업의 [계속 기다리기] */
    fun dismissNoResponse() {
        promptDismissed = true
    }

    fun snapshot(now: Instant): InquirySnapshot {
        val rows = hospitals.map { rowOf(it, now) }
        val okCount = rows.count { it.status == InquiryStatus.AVAILABLE }
        val asked = rows.filter { it.status != InquiryStatus.NOT_ASKED }
        val summary = when {
            okCount > 0 -> InquirySummary.Accepting(okCount)
            asked.isNotEmpty() && asked.all { it.status == InquiryStatus.UNAVAILABLE } -> InquirySummary.NoneAvailable
            else -> InquirySummary.Waiting
        }
        val started = startedAt
        val sinceStart = started?.let { Duration.between(it, now) } ?: Duration.ZERO
        val noResponseList = rows.filter { it.status != InquiryStatus.UNAVAILABLE }.sortedBy { it.travelMinutes }
        val searchRows = asked.sortedWith(displayOrder)
        return InquirySnapshot(
            searchRows = searchRows,
            compareRows = rows.filter { it.status != InquiryStatus.NOT_ASKED && it.status != InquiryStatus.UNAVAILABLE }.sortedWith(displayOrder),
            noResponseRows = noResponseList,
            okCount = okCount,
            summary = summary,
            restCount = if (started == null) 0 else hospitals.count { it.key !in askedAt },
            showNoResponsePrompt = started != null && okCount == 0 && !promptDismissed &&
                sinceStart.seconds > DELAY_AFTER.seconds && noResponseList.isNotEmpty(),
            progress = if (started == null) 0f else (sinceStart.toMillis().toFloat() / PROGRESS_SPAN.toMillis()).coerceIn(0f, 1f),
            best = searchRows.firstOrNull { it.status == InquiryStatus.AVAILABLE && it.startMinutes != null },
        )
    }

    private fun rowOf(h: InquiryHospital, now: Instant): InquiryRow {
        val at = askedAt[h.key] ?: return InquiryRow(h.key, h.travelMinutes, InquiryStatus.NOT_ASKED)
        val reply = script.replies[h.key]
        val since = Duration.between(at, now)
        if (reply != null && reply.answer !is ScriptedAnswer.Silent && since.seconds >= reply.afterSeconds) {
            val answeredAt = at.plusSeconds(reply.afterSeconds.toLong())
            return when (val answer = reply.answer) {
                ScriptedAnswer.Unavailable -> InquiryRow(h.key, h.travelMinutes, InquiryStatus.UNAVAILABLE, answeredAt = answeredAt)
                is ScriptedAnswer.Available -> {
                    val elapsed = Duration.between(answeredAt, now).toMinutes().toInt().coerceAtLeast(0)
                    val left = answer.waitMinutes?.let { (it - elapsed).coerceAtLeast(0) }
                    InquiryRow(
                        key = h.key,
                        travelMinutes = h.travelMinutes,
                        status = InquiryStatus.AVAILABLE,
                        waitMinutes = answer.waitMinutes,
                        answeredAt = answeredAt,
                        elapsedMinutes = elapsed,
                        startMinutes = left?.let { maxOf(h.travelMinutes, it) },
                        addMinutes = left?.let { (it - h.travelMinutes).coerceAtLeast(0) },
                        needsRecheck = elapsed >= RECHECK_AFTER_MINUTES,
                    )
                }
                ScriptedAnswer.Silent -> error("위에서 걸렀다")
            }
        }
        val status = if (since.seconds > DELAY_AFTER.seconds) InquiryStatus.DELAYED else InquiryStatus.WAITING
        return InquiryRow(h.key, h.travelMinutes, status)
    }

    companion object {
        const val FIRST_ASK = 3
        val DELAY_AFTER: Duration = Duration.ofSeconds(20)
        val PROGRESS_SPAN: Duration = Duration.ofSeconds(60)
        const val RECHECK_AFTER_MINUTES = 10

        /** 보이는 순서: 진료 가능+시간 있음 → 진료 가능 → 대기 → 그 밖, 같으면 진료 시작까지 → 이동 */
        private val displayOrder: Comparator<InquiryRow> = compareBy<InquiryRow>(
            {
                when {
                    it.status == InquiryStatus.AVAILABLE && it.startMinutes != null -> 0
                    it.status == InquiryStatus.AVAILABLE -> 1
                    it.status == InquiryStatus.WAITING -> 2
                    else -> 3
                }
            },
            { it.startMinutes ?: Int.MAX_VALUE },
            { it.travelMinutes },
        )
    }
}

/** 문의할 수 있는 병원 하나. [key]는 시나리오와 화면이 같이 쓰는 이름(병원 이름) */
data class InquiryHospital(val key: String, val travelMinutes: Int)

enum class InquiryStatus { NOT_ASKED, WAITING, DELAYED, UNAVAILABLE, AVAILABLE }

/**
 * 한 병원의 지금 상태.
 * @param startMinutes 진료 시작까지 (진료 가능 + 대기 분을 알 때만)
 * @param addMinutes 도착 후 대기 (0이면 "거의 없음")
 * @param needsRecheck 응답 후 10분 이상 지남
 */
data class InquiryRow(
    val key: String,
    val travelMinutes: Int,
    val status: InquiryStatus,
    val waitMinutes: Int? = null,
    val answeredAt: Instant? = null,
    val elapsedMinutes: Int? = null,
    val startMinutes: Int? = null,
    val addMinutes: Int? = null,
    val needsRecheck: Boolean = false,
)

sealed interface InquirySummary {
    data class Accepting(val count: Int) : InquirySummary
    data object NoneAvailable : InquirySummary
    data object Waiting : InquirySummary
}

/**
 * @param searchRows 응답 확인 화면 줄 (문의한 병원)
 * @param compareRows 비교 화면 줄 (진료 불가·문의 전 제외. 진료 가능이 아니면 고를 수 없다)
 * @param noResponseRows 응답 없음 팝업 목록 (진료 불가 제외, 이동 순)
 * @param restCount 아직 문의하지 않은 병원 수 ([다른 병원 N곳에도 문의])
 * @param progress 문의부터 60초 진행 (0~1)
 * @param best 진료 시작이 가장 빠른 진료 가능 병원
 */
data class InquirySnapshot(
    val searchRows: List<InquiryRow>,
    val compareRows: List<InquiryRow>,
    val noResponseRows: List<InquiryRow>,
    val okCount: Int,
    val summary: InquirySummary,
    val restCount: Int,
    val showNoResponsePrompt: Boolean,
    val progress: Float,
    val best: InquiryRow?,
)

/** 시연 응답: 문의 뒤 [afterSeconds]초에 [answer] */
data class ScriptedReply(val afterSeconds: Int, val answer: ScriptedAnswer)

sealed interface ScriptedAnswer {
    /** 진료 가능. [waitMinutes]는 진료 가능까지 분 (모르면 null) */
    data class Available(val waitMinutes: Int?) : ScriptedAnswer
    data object Unavailable : ScriptedAnswer
    data object Silent : ScriptedAnswer
}

/** 병원 key → 시연 응답. 없으면 답하지 않는다 */
data class InquiryScript(val replies: Map<String, ScriptedReply>)

/** 시연 응답 시나리오 (계획 020 D2). 화면·테스트가 같이 쓴다 */
object InquiryScripts {
    /** 보통: 온유 4초 가능(5분), 별숲 7초 가능(30분), 나래 무응답 / 추가: 달빛 5초 불가, 가람 9초 가능(미확인) */
    val DEFAULT = InquiryScript(
        mapOf(
            "온유동물병원" to ScriptedReply(4, ScriptedAnswer.Available(5)),
            "별숲동물의료센터" to ScriptedReply(7, ScriptedAnswer.Available(30)),
            "나래동물의료센터" to ScriptedReply(0, ScriptedAnswer.Silent),
            "달빛24시동물의료센터" to ScriptedReply(5, ScriptedAnswer.Unavailable),
            "가람24시동물병원" to ScriptedReply(9, ScriptedAnswer.Available(null)),
        ),
    )

    /** 응답 없음 (debug에서 고름): 처음 3곳·달빛 무응답, 다른 병원 문의 뒤 가람 8초 가능(10분) */
    val NO_RESPONSE = InquiryScript(
        mapOf("가람24시동물병원" to ScriptedReply(8, ScriptedAnswer.Available(10))),
    )
}
