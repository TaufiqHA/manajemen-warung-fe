package com.example.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.ui.screens.LoginScreen
import com.example.ui.screens.SplashScreen
import com.example.ui.screens.DashboardScreen
import com.example.ui.screens.SalesScreen
import com.example.ui.screens.SettingsScreen
import com.example.data.UserRole

import com.example.ui.screens.MonthlyReportScreen

@Composable
fun AppNavigation() {
    val navController = rememberNavController()

    NavHost(navController = navController, startDestination = "splash") {
        composable("splash") {
            SplashScreen(
                onNavigateToLogin = {
                    navController.navigate("login") {
                        popUpTo("splash") { inclusive = true }
                    }
                }
            )
        }
        composable("login") {
            LoginScreen(
                onLoginSuccess = { role ->
                    navController.navigate("dashboard/${role.name}") {
                        popUpTo("login") { inclusive = true }
                    }
                }
            )
        }
        composable("dashboard/{role}") { backStackEntry ->
            val roleString = backStackEntry.arguments?.getString("role") ?: UserRole.ADMIN_TOKO.name
            val role = try { UserRole.valueOf(roleString) } catch (e: Exception) { UserRole.ADMIN_TOKO }
            DashboardScreen(
                role = role,
                onLogout = {
                    navController.navigate("login") {
                        popUpTo("login") { inclusive = true }
                    }
                },
                onNavigateToSales = {
                    navController.navigate("sales")
                },
                onNavigateToMonthlyReport = {
                    navController.navigate("monthly_report")
                },
                onNavigateToSettings = {
                    navController.navigate("settings")
                }
            )
        }
        composable("sales") {
            SalesScreen(
                onBack = {
                    navController.popBackStack()
                }
            )
        }
        composable("settings") {
            SettingsScreen(
                onBack = {
                    navController.popBackStack()
                }
            )
        }
        composable("monthly_report") {
            MonthlyReportScreen(
                onBack = {
                    navController.popBackStack()
                }
            )
        }
    }
}
