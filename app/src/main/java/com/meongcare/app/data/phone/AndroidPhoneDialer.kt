package com.meongcare.app.data.phone

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import com.meongcare.app.domain.phone.PhoneDialer

/**
 * Android 전화 앱에 번호를 띄운다 (`ACTION_DIAL`). 걸지 않으므로 권한(`CALL_PHONE`)이 필요 없다.
 *
 * - 미리 조회하지 않고(`resolveActivity`) 바로 띄운 뒤 예외로 대비한다. 그래서 Android 11+ 패키지 가시성
 *   제한(`<queries>`)과 관계가 없다.
 * - ViewModel이 Activity를 들고 있지 않게 application context를 쓴다. 그래서 `FLAG_ACTIVITY_NEW_TASK`가 필요하다.
 * - 번호를 로그에 남기지 않는다.
 * - 차(실차)에서 서드파티 앱이 전화 앱을 열 수 있는지는 미확인이다 (docs/architecture.md 문의 B4).
 */
class AndroidPhoneDialer(context: Context) : PhoneDialer {
    private val appContext = context.applicationContext

    override fun openDialer(number: String): Boolean {
        val intent = Intent(Intent.ACTION_DIAL, Uri.fromParts("tel", number, null))
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        return try {
            appContext.startActivity(intent)
            true
        } catch (e: ActivityNotFoundException) {
            false
        } catch (e: SecurityException) {
            false
        }
    }
}
