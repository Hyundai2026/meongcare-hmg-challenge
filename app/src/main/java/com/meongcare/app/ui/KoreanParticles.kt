package com.meongcare.app.ui

/** 목적격 조사: 받침이 있으면 "을", 없으면(한글이 아니어도) "를". 예: "보리를", "병원을" */
fun withObjectParticle(word: String): String {
    val last = word.lastOrNull() ?: return word
    val hasFinal = last in '가'..'힣' && (last - '가') % 28 != 0
    return word + if (hasFinal) "을" else "를"
}
