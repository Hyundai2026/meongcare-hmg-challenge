package com.meongcare.app.ui.card

import com.meongcare.app.domain.card.Symptom

/** 진료 카드 흐름의 버튼 동작 묶음. `MainActivity`가 [MedicalCardViewModel]의 함수로 채운다. */
data class MedicalCardActions(
    val onStartCard: () -> Unit = {},
    val onToggleSymptom: (Symptom) -> Unit = {},
    val onConfirmSymptoms: () -> Unit = {},
    val onCancelPicking: () -> Unit = {},
    val onEditSymptoms: () -> Unit = {},
    val onCloseCard: () -> Unit = {},
    val onCallHospital: () -> Unit = {},
    val onDismissPhoneNumber: () -> Unit = {},
    val onGoHome: () -> Unit = {},
    val onOpenCard: () -> Unit = {},
    val onStartNewCard: () -> Unit = {},
)
