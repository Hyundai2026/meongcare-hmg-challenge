package com.meongcare.app.ui.card

import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.meongcare.app.R
import com.meongcare.app.domain.card.Symptom
import com.meongcare.app.domain.pet.AgeLabel
import com.meongcare.app.domain.pet.Sex
import java.time.LocalDate

/* 카드 값 → 화면 글자. 글자는 strings.xml에 있다. */

@StringRes
fun symptomLabelRes(symptom: Symptom): Int = when (symptom) {
    Symptom.BREATHING_DIFFICULTY -> R.string.symptom_breathing_difficulty
    Symptom.SEIZURE -> R.string.symptom_seizure
    Symptom.COLLAPSE -> R.string.symptom_collapse
    Symptom.BLEEDING_OR_WOUND -> R.string.symptom_bleeding_or_wound
    Symptom.SWALLOWED_OBJECT -> R.string.symptom_swallowed_object
    Symptom.VOMITING -> R.string.symptom_vomiting
    Symptom.DIARRHEA -> R.string.symptom_diarrhea
    Symptom.COUGH -> R.string.symptom_cough
    Symptom.LETHARGY -> R.string.symptom_lethargy
    Symptom.NO_APPETITE -> R.string.symptom_no_appetite
    Symptom.LIMPING -> R.string.symptom_limping
    Symptom.BLOATED_BELLY -> R.string.symptom_bloated_belly
}

@Composable
fun ageText(age: AgeLabel, estimated: Boolean): String {
    val text = when (age) {
        is AgeLabel.YearsAndMonths -> stringResource(R.string.card_age_years_months, age.years, age.months)
        is AgeLabel.Years -> stringResource(R.string.card_age_years, age.years)
        is AgeLabel.Months -> stringResource(R.string.card_age_months, age.months)
        AgeLabel.UnderOneMonth -> stringResource(R.string.card_age_under_one_month)
    }
    return if (estimated) stringResource(R.string.card_age_estimated, text) else text
}

/** "12세 3개월 · 골든리트리버". 없는 값은 빼고, 둘 다 없으면 null (칩을 그리지 않는다) */
@Composable
fun petChipText(pet: PetHeader.Registered): String? {
    val parts = listOfNotNull(pet.age?.let { ageText(it, pet.ageIsEstimated) }, pet.breed)
    return if (parts.isEmpty()) null else parts.joinToString(stringResource(R.string.card_chip_separator))
}

@Composable
fun sexSymbol(sex: Sex): String = stringResource(if (sex == Sex.MALE) R.string.card_sex_male else R.string.card_sex_female)

@Composable
fun sexDescription(sex: Sex): String =
    stringResource(if (sex == Sex.MALE) R.string.card_sex_male_description else R.string.card_sex_female_description)

@Composable
fun joinedText(values: List<String>): String = values.joinToString(stringResource(R.string.card_list_separator))

@Composable
fun medicationText(medication: MedicationText): String =
    medication.dosage?.let { stringResource(R.string.card_medication_with_dosage, medication.name, it) } ?: medication.name

@Composable
fun updatedOnText(date: LocalDate): String =
    stringResource(R.string.card_updated_on, date.year, date.monthValue, date.dayOfMonth)

/** 반려견 정보가 없을 때 카드 머리의 안내 한 줄 */
@Composable
fun petMissingText(pet: PetHeader): String? = when (pet) {
    PetHeader.Loading -> stringResource(R.string.card_pet_loading)
    PetHeader.NotRegistered -> stringResource(R.string.card_pet_not_registered)
    PetHeader.Unavailable -> stringResource(R.string.card_pet_unavailable)
    is PetHeader.Registered -> null
}
