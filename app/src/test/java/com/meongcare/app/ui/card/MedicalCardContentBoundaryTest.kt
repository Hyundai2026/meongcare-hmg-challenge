package com.meongcare.app.ui.card

import com.meongcare.app.data.pet.ExamplePetProfiles
import com.meongcare.app.domain.card.Symptom
import com.meongcare.app.domain.pet.AgeLabel
import com.meongcare.app.domain.pet.MedicalInfo
import com.meongcare.app.domain.pet.Medication
import com.meongcare.app.domain.pet.PetProfile
import com.meongcare.app.domain.pet.PetProfileLoad
import com.meongcare.app.domain.pet.Sex
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * 계획 009 설계 결정 6 (카드 내용), docs/data/pet-profile.md (빈 배열 = 등록하지 않음, 읽는 쪽은 짐작하지 않는다,
 * 길이 제한은 쓰는 쪽 책임)의 경계.
 */
class MedicalCardContentBoundaryTest {
    private val today = LocalDate.of(2026, 11, 5)
    private val seoul = ZoneId.of("Asia/Seoul")
    private val bori = ExamplePetProfiles.bori

    private fun loaded(profile: PetProfile) = PetProfileLoad.Loaded(profile)

    private fun card(profile: PetProfileLoad?, symptoms: Set<Symptom> = emptySet(), on: LocalDate = today) =
        medicalCardOf(profile, symptoms, on, seoul)

    private fun registered(profile: PetProfile, on: LocalDate = today) =
        card(loaded(profile), on = on).pet as PetHeader.Registered

    @Test
    fun `12개 증상을 거꾸로 눌러도 카드에는 프리셋 순서로 모두 적힌다`() {
        val pressedBackwards = LinkedHashSet(Symptom.entries.reversed())
        assertEquals(Symptom.entries.toList(), card(loaded(bori), pressedBackwards).symptoms)
    }

    @Test
    fun `의료정보가 모두 빈 목록이어도 등록된 프로필이면 의료정보 줄이 있고 각 줄이 빈 목록이다`() {
        val empty = bori.copy(medical = MedicalInfo(emptyList(), emptyList(), emptyList()))
        val result = card(loaded(empty))
        assertEquals(
            MedicalRows(
                conditions = emptyList(),
                allergies = emptyList(),
                medications = emptyList(),
                updatedOn = LocalDate.of(2026, 11, 5),
            ),
            result.medical,
        )
        assertEquals("보리", (result.pet as PetHeader.Registered).name)
    }

    @Test
    fun `모든 항목이 공백뿐이면 빈 목록과 같다`() {
        val blank = bori.copy(
            medical = MedicalInfo(
                conditions = listOf("", " ", "\t", "\n", " 　 "),
                allergies = listOf(" \t "),
                medications = listOf(Medication("  ", ""), Medication("", "1일 1회"), Medication("\n", " ")),
            ),
        )
        val medical = card(loaded(blank)).medical!!
        assertEquals(emptyList<String>(), medical.conditions)
        assertEquals(emptyList<String>(), medical.allergies)
        assertEquals(emptyList<MedicationText>(), medical.medications)
    }

    @Test
    fun `항목 가운데 글자와 공백은 바꾸지 않고 앞뒤 공백만 지운다`() {
        val profile = bori.copy(
            medical = MedicalInfo(
                conditions = listOf(" 심장 질환  ", "슬개골\t탈구"),
                allergies = listOf("  닭고기  알레르기 "),
                medications = emptyList(),
            ),
        )
        val medical = card(loaded(profile)).medical!!
        assertEquals(listOf("심장 질환", "슬개골\t탈구"), medical.conditions)
        assertEquals(listOf("닭고기  알레르기"), medical.allergies)
    }

    @Test
    fun `같은 항목이 두 번 있어도 합치지 않고 받은 순서 그대로 둔다`() {
        val profile = bori.copy(
            medical = MedicalInfo(
                conditions = listOf("심장질환", "피부염", "심장질환"),
                allergies = listOf("소고기", "닭고기"),
                medications = listOf(Medication("진통제", ""), Medication("심장약", "1일 1회"), Medication("진통제", "")),
            ),
        )
        val medical = card(loaded(profile)).medical!!
        assertEquals(listOf("심장질환", "피부염", "심장질환"), medical.conditions)
        assertEquals(listOf("소고기", "닭고기"), medical.allergies)
        assertEquals(
            listOf(MedicationText("진통제", null), MedicationText("심장약", "1일 1회"), MedicationText("진통제", null)),
            medical.medications,
        )
    }

    @Test
    fun `약 이름과 복용법의 앞뒤 공백을 지운다`() {
        val profile = bori.copy(medical = bori.medical.copy(medications = listOf(Medication(" 심장약 ", " 1일 1회 "))))
        assertEquals(listOf(MedicationText("심장약", "1일 1회")), card(loaded(profile)).medical!!.medications)
    }

    @Test
    fun `이름이 공백인 약만 건너뛰고 앞뒤의 다른 약은 남는다`() {
        val profile = bori.copy(
            medical = bori.medical.copy(
                medications = listOf(Medication("  ", "1일 1회"), Medication("심장약", "1일 1회"), Medication("\t", ""), Medication("진통제", "")),
            ),
        )
        assertEquals(
            listOf(MedicationText("심장약", "1일 1회"), MedicationText("진통제", null)),
            card(loaded(profile)).medical!!.medications,
        )
    }

    @Test
    fun `쓰는 쪽 제한을 넘은 긴 목록과 긴 항목도 자르지 않는다`() {
        // 쓰는 쪽 제한: 항목 50자, 20개. 읽는 쪽은 길이가 넘쳐도 읽는다 (pet-profile.md "읽는 쪽 규칙").
        val longItems = List(25) { i -> "가".repeat(79) + i }
        val longMedications = List(25) { i -> Medication("약".repeat(79) + i, "하루".repeat(40) + i) }
        val profile = bori.copy(medical = MedicalInfo(longItems, longItems, longMedications))
        val medical = card(loaded(profile)).medical!!
        assertEquals(longItems, medical.conditions)
        assertEquals(longItems, medical.allergies)
        assertEquals(longMedications.map { MedicationText(it.name, it.dosage) }, medical.medications)
    }

    @Test
    fun `이모지 50개짜리 항목 20개도 반으로 자르지 않고 모두 넣는다`() {
        val emojiItems = List(20) { "🐶".repeat(50) }
        val profile = bori.copy(medical = bori.medical.copy(allergies = emojiItems))
        val allergies = card(loaded(profile)).medical!!.allergies
        assertEquals(emojiItems, allergies)
        allergies.forEach { assertEquals(50, it.codePointCount(0, it.length)) }
    }

    @Test
    fun `프로필을 받지 못했거나 등록되지 않았거나 읽지 못해도 고른 증상은 프리셋 순서로 모두 보인다`() {
        val pressedBackwards = LinkedHashSet(Symptom.entries.reversed())
        listOf(null, PetProfileLoad.NotRegistered, PetProfileLoad.Unavailable).forEach { profile ->
            val result = card(profile, pressedBackwards)
            assertEquals("$profile", Symptom.entries.toList(), result.symptoms)
            assertNull("$profile", result.medical)
        }
    }

    @Test
    fun `프로필이 없고 고른 증상도 없으면 증상은 빈 목록이고 의료정보 줄이 없다`() {
        assertEquals(MedicalCardUiState(PetHeader.Loading, emptyList(), null), card(null))
        assertEquals(MedicalCardUiState(PetHeader.NotRegistered, emptyList(), null), card(PetProfileLoad.NotRegistered))
        assertEquals(MedicalCardUiState(PetHeader.Unavailable, emptyList(), null), card(PetProfileLoad.Unavailable))
    }

    @Test
    fun `나이는 넘겨받은 오늘로 센다`() {
        // 보리 생년월일 2014-08-01
        assertEquals(AgeLabel.Years(12), registered(bori, on = LocalDate.of(2026, 8, 1)).age)
        assertEquals(AgeLabel.YearsAndMonths(11, 11), registered(bori, on = LocalDate.of(2026, 7, 31)).age)
        assertEquals(AgeLabel.UnderOneMonth, registered(bori, on = LocalDate.of(2014, 8, 1)).age)
        assertNull(registered(bori, on = LocalDate.of(2014, 7, 31)).age)
    }

    @Test
    fun `생년월일이 미래여도 나이만 빠지고 나머지 머리 정보와 의료정보는 그대로다`() {
        val result = card(loaded(bori.copy(birthDate = LocalDate.of(2026, 11, 6))))
        assertEquals(
            PetHeader.Registered(
                name = "보리",
                initial = "보",
                sex = Sex.MALE,
                age = null,
                ageIsEstimated = false,
                breed = "골든리트리버",
            ),
            result.pet,
        )
        assertEquals(card(loaded(bori)).medical, result.medical)
    }

    @Test
    fun `성별이 없으면 성별 값이 없고 암컷이면 암컷이다`() {
        assertNull(registered(bori.copy(sex = null)).sex)
        assertEquals(Sex.FEMALE, registered(bori.copy(sex = Sex.FEMALE)).sex)
    }

    @Test
    fun `아바타 글자는 대소문자나 글자 조합을 바꾸지 않은 이름의 첫 코드 포인트다`() {
        assertEquals("m", initialOf("max"))
        assertEquals("𠀀", initialOf("𠀀보리"))
        // 웹 initialOf([...name][0])와 같다: 피부색 이모지는 첫 코드 포인트까지만
        assertEquals("👍", initialOf("👍🏽보리"))
        assertEquals("보", initialOf("보"))
        assertEquals("😀", registered(bori.copy(name = "😀초코")).initial)
    }

    @Test
    fun `정보 갱신일은 기기 시간대의 자정에 날짜가 바뀐다`() {
        fun updatedOn(at: String, zone: ZoneId) =
            medicalCardOf(loaded(bori.copy(updatedAt = Instant.parse(at))), emptySet(), today, zone).medical!!.updatedOn

        assertEquals(LocalDate.of(2026, 11, 4), updatedOn("2026-11-04T14:59:59.999Z", seoul))
        assertEquals(LocalDate.of(2026, 11, 5), updatedOn("2026-11-04T15:00:00Z", seoul))
        // 계획 "결정 / 배운 점": Pleos 에뮬레이터(America/Los_Angeles)에서는 보리의 갱신일이 11월 4일이다
        assertEquals(LocalDate.of(2026, 11, 4), updatedOn("2026-11-05T00:00:00Z", ZoneId.of("America/Los_Angeles")))
    }

    @Test
    fun `정보 갱신일은 오늘 날짜와 상관없다`() {
        val far = medicalCardOf(loaded(bori), emptySet(), LocalDate.of(2030, 1, 1), seoul)
        assertEquals(LocalDate.of(2026, 11, 5), far.medical!!.updatedOn)
    }
}
