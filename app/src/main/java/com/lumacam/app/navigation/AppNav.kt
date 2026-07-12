package com.lumacam.app.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.lumacam.app.ui.benchmark.DeviceBenchmarkScreen
import com.lumacam.app.ui.camera.CameraScreen
import com.lumacam.app.ui.settings.CloudAiSettingsScreen
import com.lumacam.app.ui.settings.LocalAiSettingsScreen
import com.lumacam.app.ui.settings.SettingsScreen

object Routes {
    const val CAMERA = "camera"
    const val SETTINGS = "settings"
    const val CLOUD_AI_SETTINGS = "cloud_ai_settings"
    const val LOCAL_AI_SETTINGS = "local_ai_settings"
    const val DEVICE_BENCHMARK = "device_benchmark"
}

@Composable
fun AppNav(
    modifier: Modifier = Modifier,
    navController: NavHostController = rememberNavController()
) {
    NavHost(
        navController = navController,
        startDestination = Routes.CAMERA,
        modifier = modifier
    ) {
        composable(Routes.CAMERA) {
            CameraScreen(navController = navController)
        }
        composable(Routes.SETTINGS) {
            SettingsScreen(
                onBack = { navController.popBackStack() },
                onOpenCloudAi = { navController.navigate(Routes.CLOUD_AI_SETTINGS) },
                onOpenLocalAi = { navController.navigate(Routes.LOCAL_AI_SETTINGS) },
                onOpenDeviceBenchmark = { navController.navigate(Routes.DEVICE_BENCHMARK) }
            )
        }
        composable(Routes.CLOUD_AI_SETTINGS) {
            CloudAiSettingsScreen(onBack = { navController.popBackStack() })
        }
        composable(Routes.LOCAL_AI_SETTINGS) {
            LocalAiSettingsScreen(onBack = { navController.popBackStack() })
        }
        composable(Routes.DEVICE_BENCHMARK) {
            DeviceBenchmarkScreen(onBack = { navController.popBackStack() })
        }
    }
}
