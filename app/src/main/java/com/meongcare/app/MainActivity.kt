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
import com.meongcare.app.domain.driving.DrivingState
import com.meongcare.app.ui.debug.DebugPanel
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

                HomeScreen(
                    uiState = uiState,
                    bottomBar = {
                        if (BuildConfig.DEBUG) {
                            val fake = container.fakeDrivingStateSource
                            DebugPanel(
                                isFakeMode = container.dataMode == DataMode.FAKE,
                                onSetParked = { fake.set(DrivingState.PARKED) },
                                onSetDriving = { fake.set(DrivingState.DRIVING) },
                                onSetUnknown = { fake.set(DrivingState.UNKNOWN) },
                                onToggleDataMode = {
                                    val next = if (container.dataMode == DataMode.FAKE) DataMode.REAL else DataMode.FAKE
                                    AppRestarter.restartWith(this@MainActivity, next)
                                },
                            )
                        }
                    },
                )
            }
        }
    }
}
