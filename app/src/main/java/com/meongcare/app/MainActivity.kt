package com.meongcare.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
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

                HomeScreen(uiState = uiState)
            }
        }
    }
}
