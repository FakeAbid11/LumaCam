package com.lumacam.app.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.lumacam.app.ui.camera.CameraScreen
import com.lumacam.app.ui.settings.SettingsScreen

object Routes {
    const val CAMERA = "camera"
    const val SETTINGS = "settings"
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
            SettingsScreen(onBack = { navController.popBackStack() })
        }
    }
}
