package com.lumacam.app.navigation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.lumacam.app.ui.camera.CameraScreen

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
            PlaceholderScreen(
                title = "Settings",
                subtitle = "AI tiers & DataStore preferences land here (PRD §7).",
                onOpenSettings = null
            )
        }
    }
}

@Composable
private fun PlaceholderScreen(
    title: String,
    subtitle: String,
    onOpenSettings: (() -> Unit)?
) {
    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(text = title, style = androidx.compose.material3.MaterialTheme.typography.headlineMedium)
        Text(
            text = subtitle,
            style = androidx.compose.material3.MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(top = 8.dp)
        )
        if (onOpenSettings != null) {
            Button(
                onClick = onOpenSettings,
                modifier = Modifier.padding(top = 16.dp)
            ) {
                Text("Open Settings")
            }
        }
    }
}
