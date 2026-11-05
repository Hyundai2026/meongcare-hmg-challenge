package com.meongcare.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.meongcare.app.di.AppRestarter
import com.meongcare.app.di.DataMode
import com.meongcare.app.di.SourceKind
import com.meongcare.app.di.SourcePlan
import com.meongcare.app.di.next
import com.meongcare.app.domain.driving.DrivingState
import com.meongcare.app.ui.debug.DebugPanel
import com.meongcare.app.ui.guidance.GuidanceViewModel
import com.meongcare.app.ui.home.HomeScreen
import com.meongcare.app.ui.home.HomeViewModel
import com.meongcare.app.ui.theme.MeongCareTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val container = (application as MeongCareApplication).container

        setContent {
            MeongCareTheme {
                val homeViewModel: HomeViewModel =
                    viewModel(factory = HomeViewModel.factory(container.drivingStateSource))
                val uiState by homeViewModel.uiState.collectAsStateWithLifecycle()
                val guidanceViewModel: GuidanceViewModel = viewModel(
                    factory = GuidanceViewModel.factory(container.guidanceSource, container.sampleHospital),
                )
                val guidanceUiState by guidanceViewModel.uiState.collectAsStateWithLifecycle()

                HomeScreen(
                    uiState = uiState,
                    guidanceUiState = guidanceUiState,
                    onStartGuidance = guidanceViewModel::onStartGuidance,
                    onCancelGuidance = guidanceViewModel::onCancelGuidance,
                    onDismissArrival = guidanceViewModel::onDismissArrival,
                    isGuidanceSimulated = container.sourcePlan.isGuidanceSimulated,
                    bottomBar = {
                        if (BuildConfig.DEBUG) {
                            val fake = container.fakeDrivingStateSource
                            DebugPanel(
                                modeLabel = debugModeLabel(container.dataMode, container.sourcePlan),
                                nextModeLabel = "${container.dataMode.next().debugName()}${
                                    if (container.dataMode.next() == DataMode.DEMO) "으로" else "로"
                                } 전환",
                                showsFakeDrivingControls = container.sourcePlan.driving == SourceKind.FAKE,
                                onSetParked = { fake.set(DrivingState.PARKED) },
                                onSetDriving = { fake.set(DrivingState.DRIVING) },
                                onSetUnknown = { fake.set(DrivingState.UNKNOWN) },
                                onToggleDataMode = {
                                    AppRestarter.restartWith(this@MainActivity, container.dataMode.next())
                                },
                            )
                        }
                    },
                )
            }
        }
    }
}

/** debug 패널에 보이는 모드 이름. */
private fun DataMode.debugName(): String = when (this) {
    DataMode.FAKE -> "Fake"
    DataMode.DEMO -> "시연"
    DataMode.REAL -> "Real"
}

/** 예: "DEBUG · 시연 (주행 Real)". 스크린샷만 보고도 어느 구성인지 알 수 있게 시연 모드는 주행 상태 출처를 붙인다. */
private fun debugModeLabel(mode: DataMode, plan: SourcePlan): String {
    val drivingSource = if (plan.driving == SourceKind.REAL) "Real" else "Fake"
    return if (mode == DataMode.DEMO) "DEBUG · 시연 (주행 $drivingSource)" else "DEBUG · ${mode.debugName()}"
}
