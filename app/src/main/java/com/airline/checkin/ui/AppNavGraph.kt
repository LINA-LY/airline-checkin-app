package com.airline.checkin.ui

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Flight
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.hilt.navigation.compose.hiltViewModel
import com.airline.checkin.ui.auth.AuthViewModel
import com.airline.checkin.ui.auth.CompleteProfileScreen
import com.airline.checkin.ui.auth.WelcomeScreen
import com.airline.checkin.ui.boardingpass.BoardingPassScreen
import com.airline.checkin.ui.checkin.BookingLookupScreen
import com.airline.checkin.ui.checkin.CheckInScreen
import com.airline.checkin.ui.home.BoardingPassHubScreen
import com.airline.checkin.ui.home.HomeScreen
import com.airline.checkin.ui.onboarding.OnboardingScreen
import com.airline.checkin.ui.seat.SeatMapScreen

object Routes {
    const val ONBOARDING     = "onboarding"
    const val WELCOME        = "welcome"
    const val COMPLETE_PROFILE = "complete_profile"
    const val HOME           = "home"
    const val BOOKING_LOOKUP = "booking_lookup"
    const val PASSES         = "passes"
    const val CHECK_IN       = "check_in/{bookingId}"
    const val SEAT_MAP       = "seat_map/{flightId}"
    const val BOARDING_PASS  = "boarding_pass/{bookingId}"
}

@Composable
fun AppNavGraph(
    startDestination: String,
    onOnboardingComplete: () -> Unit,
    navController: NavHostController = rememberNavController()
) {
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route

    val bottomNavItems = listOf(
        BottomNavItem(route = Routes.HOME, label = "Home", icon = Icons.Default.Home),
        BottomNavItem(route = Routes.BOOKING_LOOKUP, label = "Bookings", icon = Icons.Default.Search),
        BottomNavItem(route = Routes.PASSES, label = "Passes", icon = Icons.Default.Flight)
    )
    val bottomBarRoutes = setOf(Routes.HOME, Routes.BOOKING_LOOKUP, Routes.PASSES)
    val showBottomBar = currentRoute in bottomBarRoutes

    Scaffold(
        bottomBar = {
            if (showBottomBar) {
                NavigationBar {
                    bottomNavItems.forEach { item ->
                        val selected = currentRoute == item.route
                        NavigationBarItem(
                            selected = selected,
                            onClick = {
                                navController.navigate(item.route) {
                                    popUpTo(Routes.HOME) { saveState = true }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                            icon = { Icon(item.icon, contentDescription = item.label) },
                            label = { Text(item.label) }
                        )
                    }
                }
            }
        }
    ) { paddingValues ->
        NavHost(
            navController = navController,
            startDestination = startDestination,
            modifier = Modifier.padding(paddingValues)
        ) {
            composable(Routes.ONBOARDING) {
                OnboardingScreen(
                    onFinish = {
                        onOnboardingComplete()
                        navController.navigate(Routes.WELCOME) {
                            popUpTo(Routes.ONBOARDING) { inclusive = true }
                        }
                    }
                )
            }

            composable(Routes.WELCOME) {
                WelcomeScreen(
                    onAuthSuccess = {
                        navController.navigate(Routes.HOME) {
                            popUpTo(Routes.WELCOME) { inclusive = true }
                        }
                    },
                    onProfileRequired = {
                        navController.navigate(Routes.COMPLETE_PROFILE)
                    }
                )
            }

            composable(Routes.COMPLETE_PROFILE) {
                CompleteProfileScreen(
                    onProfileSaved = {
                        navController.navigate(Routes.HOME) {
                            popUpTo(Routes.WELCOME) { inclusive = true }
                        }
                    }
                )
            }

            composable(Routes.HOME) {
                val authViewModel: AuthViewModel = hiltViewModel()
                HomeScreen(
                    onFindBooking = { navController.navigate(Routes.BOOKING_LOOKUP) },
                    onViewPasses = { navController.navigate(Routes.PASSES) },
                    onSignOut = {
                        authViewModel.signOut()
                        navController.navigate(Routes.WELCOME) {
                            popUpTo(Routes.HOME) { inclusive = true }
                        }
                    }
                )
            }

            composable(Routes.BOOKING_LOOKUP) {
                BookingLookupScreen(
                    onBookingFound = { bookingId ->
                        navController.navigate("check_in/$bookingId")
                    }
                )
            }

            composable(Routes.PASSES) {
                BoardingPassHubScreen(
                    onFindBooking = { navController.navigate(Routes.BOOKING_LOOKUP) }
                )
            }

            composable(Routes.CHECK_IN) { backStack ->
                val bookingId = backStack.arguments?.getString("bookingId") ?: ""
                CheckInScreen(
                    bookingId  = bookingId,
                    onGoToSeat = { flightId -> navController.navigate("seat_map/$flightId") },
                    onDone     = { navController.navigate("boarding_pass/$bookingId") }
                )
            }

            composable(Routes.SEAT_MAP) { backStack ->
                val flightId = backStack.arguments?.getString("flightId") ?: ""
                SeatMapScreen(
                    flightId   = flightId,
                    onSeatPicked = { navController.popBackStack() }
                )
            }

            composable(Routes.BOARDING_PASS) { backStack ->
                val bookingId = backStack.arguments?.getString("bookingId") ?: ""
                BoardingPassScreen(
                    bookingId = bookingId,
                    onBackClick = { navController.popBackStack() },
                    onBackToHome = {
                        navController.navigate(Routes.HOME) {
                            popUpTo(Routes.HOME) { inclusive = false }
                            launchSingleTop = true
                        }
                    }
                )
            }
        }
    }
}

private data class BottomNavItem(
    val route: String,
    val label: String,
    val icon: ImageVector
)
