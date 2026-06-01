package com.airline.checkin.ui

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Flight
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.FlightTakeoff
import androidx.compose.material.icons.filled.ConfirmationNumber
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.ui.unit.dp
import androidx.compose.material3.Text
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
import com.airline.checkin.ui.checkin.BookingFoundScreen
import com.airline.checkin.ui.checkin.BookingLookupScreen
import com.airline.checkin.ui.checkin.CheckInScreen
import com.airline.checkin.ui.checkin.MyBookingsScreen
import com.airline.checkin.ui.home.HomeScreen
import com.airline.checkin.ui.profile.ProfileScreen
import com.airline.checkin.ui.onboarding.OnboardingScreen

object Routes {
    const val HOME_LOOKUP    = "home_lookup/{reference}/{lastName}"
    const val ONBOARDING     = "onboarding"
    const val WELCOME        = "welcome"
    const val COMPLETE_PROFILE = "complete_profile"
    const val HOME           = "home"
    const val CHECK_IN_LOOKUP = "check_in_lookup"
    const val PROFILE        = "profile"
    const val MY_BOOKINGS    = "my_bookings"
    const val BOOKING_FOUND  = "booking_found/{bookingId}"
    const val CHECK_IN       = "check_in/{bookingId}"
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
        BottomNavItem(route = Routes.CHECK_IN_LOOKUP, label = "Check-In", icon = Icons.Default.FlightTakeoff),
        BottomNavItem(route = Routes.MY_BOOKINGS, label = "Passes", icon = Icons.Default.ConfirmationNumber),
        BottomNavItem(route = Routes.PROFILE, label = "Profile", icon = Icons.Default.Person)
    )
    val bottomBarRoutes = setOf(
        Routes.HOME, Routes.CHECK_IN_LOOKUP, Routes.MY_BOOKINGS, Routes.PROFILE,
        Routes.BOOKING_FOUND, Routes.CHECK_IN, Routes.BOARDING_PASS
    )
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
                                if (item.route == Routes.CHECK_IN_LOOKUP) {
                                    navController.navigate(item.route) {
                                        popUpTo(Routes.HOME) { saveState = false }
                                        launchSingleTop = true
                                        restoreState = false
                                    }
                                } else {
                                    navController.navigate(item.route) {
                                        popUpTo(Routes.HOME) { saveState = true }
                                        launchSingleTop = true
                                        restoreState = true
                                    }
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
                val userName by authViewModel.displayName.collectAsState()
                val recentBooking by authViewModel.recentBooking.collectAsState()
                HomeScreen(
                    userName = userName,
                    recentBooking = recentBooking,
                    onViewBookings = { navController.navigate(Routes.MY_BOOKINGS) },
                    onProfileClick = { navController.navigate(Routes.PROFILE) },
                    onFindBooking = { ref, last ->
                        navController.navigate("home_lookup/${ref}/${last}")
                    }
                )
            }

            composable(Routes.PROFILE) {
                val authViewModel: AuthViewModel = hiltViewModel()
                val userName by authViewModel.displayName.collectAsState()
                ProfileScreen(
                    userName = userName,
                    onEditProfile = { navController.navigate(Routes.COMPLETE_PROFILE) },
                    onSignOut = {
                        authViewModel.signOut()
                        navController.navigate(Routes.WELCOME) {
                            popUpTo(Routes.HOME) { inclusive = true }
                        }
                    },
                    onBack = { navController.popBackStack() }
                )
            }

            composable(Routes.MY_BOOKINGS) {
                MyBookingsScreen(
                    onNavigateToCheckIn = { bookingId -> navController.navigate("check_in/$bookingId") },
                    onNavigateToBoardingPass = { bookingId -> navController.navigate("boarding_pass/$bookingId") }
                )
            }

            composable(Routes.CHECK_IN_LOOKUP) {
                BookingLookupScreen(
                    onBookingFound = { bookingId -> navController.navigate("booking_found/$bookingId") }
                )
            }

            // Home quick-lookup: resolve reference+lastName then go to booking_found
            composable(
                route = Routes.HOME_LOOKUP,
                arguments = listOf(
                    androidx.navigation.navArgument("reference") { type = androidx.navigation.NavType.StringType },
                    androidx.navigation.navArgument("lastName") { type = androidx.navigation.NavType.StringType }
                )
            ) { backStack ->
                val reference = backStack.arguments?.getString("reference") ?: ""
                val lastName  = backStack.arguments?.getString("lastName") ?: ""
                val vm: com.airline.checkin.ui.checkin.BookingLookupViewModel = hiltViewModel()
                val launched = remember { mutableStateOf(false) }
                LaunchedEffect(Unit) {
                    if (!launched.value) {
                        launched.value = true
                        vm.updateReference(reference)
                        vm.updateLastName(lastName)
                        vm.lookup()
                    }
                }
                val state by vm.uiState.collectAsState()
                val navigated = remember { mutableStateOf(false) }
                LaunchedEffect(state.result, state.error) {
                    if (state.result != null && !navigated.value) {
                        navigated.value = true
                        navController.navigate("booking_found/${state.result!!.id}") {
                            popUpTo(Routes.HOME) { inclusive = false }
                        }
                    }
                }
                Box(modifier = androidx.compose.ui.Modifier.fillMaxSize(), contentAlignment = androidx.compose.ui.Alignment.Center) {
                    when {
                        state.isLoading -> androidx.compose.material3.CircularProgressIndicator(color = com.airline.checkin.ui.AppColors.Primary)
                        state.error != null -> {
                            Column(horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally) {
                                com.airline.checkin.ui.auth.ErrorBanner(state.error ?: "Not found")
                                Spacer(Modifier.height(16.dp))
                                androidx.compose.material3.Button(onClick = { navController.popBackStack() },
                                    colors = androidx.compose.material3.ButtonDefaults.buttonColors(containerColor = com.airline.checkin.ui.AppColors.Primary)) {
                                    Text("Back")
                                }
                            }
                        }
                        else -> androidx.compose.material3.CircularProgressIndicator(color = com.airline.checkin.ui.AppColors.Primary)
                    }
                }
            }

            composable(Routes.BOOKING_FOUND) { backStack ->
                val bookingId = backStack.arguments?.getString("bookingId") ?: ""
                BookingFoundScreen(
                    bookingId = bookingId,
                    onStartCheckIn = { id ->
                        navController.navigate("check_in/$id") {
                            // Keep home in back stack so back button works
                            popUpTo(Routes.HOME) { inclusive = false }
                        }
                    },
                    onViewBoardingPass = { id -> navController.navigate("boarding_pass/$id") },
                    onBack = {
                        // Go back to wherever we came from (home or check-in tab)
                        if (!navController.popBackStack()) navController.navigate(Routes.HOME)
                    }
                )
            }


            composable(Routes.CHECK_IN) { backStack ->
                val bookingId = backStack.arguments?.getString("bookingId") ?: ""
                CheckInScreen(
                    bookingId  = bookingId,
                    onGoToSeat = { /* Deprecated */ },
                    onDone     = {
                        navController.navigate("boarding_pass/$bookingId") {
                            popUpTo(Routes.HOME) { inclusive = false }
                        }
                    }
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