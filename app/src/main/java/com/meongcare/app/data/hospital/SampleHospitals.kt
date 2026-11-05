package com.meongcare.app.data.hospital

import com.meongcare.app.domain.guidance.Destination
import com.meongcare.app.domain.hospital.Hospital

/**
 * 시연용 샘플 병원. 병원 검색(카카오 로컬 + 서버)이 붙기 전까지 쓴다.
 *
 * - 이름은 가상이다. 실제 병원 이름은 영상에 나오면 제3자 권리 문제가 생길 수 있어 쓰지 않는다
 *   (docs/contest/requirements.md "출품 유의사항").
 * - 좌표는 Pleos NaviHelper 문서의 예시 좌표다 (docs/pleos/sdk-notes.md NaviHelper 절).
 *   Fake에서는 쓰이지 않고, Real 길안내를 시험할 때 쓴다.
 * - 주소는 화면에 보여주지 않는다.
 * - 전화번호 `02-0000-0000`도 시연용이다 (사용자 결정 2026-11-05). 국번이 0으로 시작해 일반 가입 번호로
 *   쓰이지 않는 형식이다. 전화 앱에 띄우기만 하고 걸지 않는다. 차에서 전화 앱이 열리는지는 문의 B4로 미확인.
 */
object SampleHospitals {
    val demo = Hospital(
        destination = Destination(
            name = "시연용 동물병원",
            latitude = 37.48544722,
            longitude = 127.03636666,
        ),
        phoneNumber = "02-0000-0000",
        isSample = true,
    )
}
