package com.meongcare.app.domain.phone

/**
 * 전화 앱 열기. 번호를 띄우기만 하고 걸지 않는다 (권한이 필요 없다).
 * 차에서 서드파티 앱이 전화 앱을 열 수 있는지는 미확인이다 (docs/architecture.md 문의 B4).
 */
fun interface PhoneDialer {
    /** 전화 앱에 [number]를 띄운다. 띄울 앱이 없거나 막혀 있으면 false. 그때 화면은 번호를 크게 보여 준다. */
    fun openDialer(number: String): Boolean
}
