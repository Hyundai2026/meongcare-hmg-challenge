package com.meongcare.app.data.pet

/**
 * docs/data/pet-profile.example.json(보호자 웹 예시)과 같은 반려견. 차 앱 시연 샘플은 품종만 포메라니안으로 다르다
 * (사용자 결정 2026-11-14, 보리 사진). 예시 JSON과 비교하는 테스트는 이 값을 쓴다.
 */
object ExamplePetProfiles {
    val bori = SamplePetProfiles.demoPet.copy(breed = "골든리트리버")
}
