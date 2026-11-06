package com.meongcare.app.domain.link

import java.time.Duration
import java.time.Instant
import kotlin.random.Random

/**
 * 보호자 웹 연결 코드 (014). 규칙: docs/data/owner-link.md "연결 코드".
 *
 * - 대문자·숫자 31종 6자. 헷갈리는 `0`·`1`·`I`·`L`·`O`는 쓰지 않는다.
 * - 차가 만들고 보호자가 폰 웹에 입력한다 (차에서는 글자를 입력하지 않는다, AGENTS.md 규칙 3).
 * - 만료는 서버가 서버 시각으로 판단한다. 차 화면의 남은 시간([remainingOf])은 차 시계로 어림한다.
 */
@JvmInline
value class LinkCode private constructor(val value: String) {

    /** 화면에 보이는 꼴: 세 글자씩 띄어 "K7P 3QX" */
    val display: String get() = value.substring(0, HALF) + " " + value.substring(HALF)

    override fun toString(): String = "LinkCode(******)"

    companion object {
        const val ALPHABET = "23456789ABCDEFGHJKMNPQRSTUVWXYZ"
        const val LENGTH = 6
        private const val HALF = LENGTH / 2

        /** 코드가 유효한 시간. 서버 규칙(firestore.rules)과 같다 */
        val VALIDITY: Duration = Duration.ofMinutes(10)

        /** 형식이 맞으면 코드, 아니면 null. 정규화(소문자·공백)는 하지 않는다 (차는 자기가 만든 코드만 다룬다) */
        fun of(text: String): LinkCode? =
            text.takeIf { it.length == LENGTH && it.all { c -> c in ALPHABET } }?.let(::LinkCode)

        /** [random]으로 새 코드를 만든다. 진짜는 `SecureRandom`을 넣는다 */
        fun generate(random: Random): LinkCode =
            LinkCode(String(CharArray(LENGTH) { ALPHABET[random.nextInt(ALPHABET.length)] }))
    }
}

/**
 * 코드를 만든 시각부터 [LinkCode.VALIDITY]까지 남은 시간. 0 아래로 내려가지 않고 10분을 넘지 않는다
 * (시동 뒤 차 시계가 맞춰지며 뒤로 가도 "12분 안에"처럼 보이지 않게).
 */
fun remainingOf(createdAt: Instant, now: Instant): Duration {
    val left = Duration.between(now, createdAt.plus(LinkCode.VALIDITY))
    return when {
        left.isNegative -> Duration.ZERO
        left > LinkCode.VALIDITY -> LinkCode.VALIDITY
        else -> left
    }
}

/** 화면의 "N분 안에": 남은 시간을 분 단위로 올림한다. 만료면 0 */
fun remainingMinutesOf(createdAt: Instant, now: Instant): Int {
    val left = remainingOf(createdAt, now)
    val minutes = left.toMinutes()
    return (if (left.minusMinutes(minutes).isZero) minutes else minutes + 1).toInt()
}

/** 만료됐는지 (정확히 10분이 된 순간부터 만료) */
fun isExpired(createdAt: Instant, now: Instant): Boolean = remainingOf(createdAt, now).isZero
