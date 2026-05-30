package com.airline.checkin.ui

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Flight
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
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
import com.airline.checkin.ui.booking.CabinClass
import com.airline.checkin.ui.booking.FlightDetailScreen
import com.airline.checkin.ui.booking.FlightResultsScreen
import com.airline.checkin.ui.booking.FlightSearchScreen
import com.airline.checkin.ui.booking.PaymentScreen
import com.airline.checkin.ui.booking.PaymentSuccessScreen
import com.airline.checkin.ui.checkin.MyBookingsScreen
import com.airline.checkin.ui.checkin.CheckInScreen
import com.airline.checkin.ui.home.HomeScreen
import com.airline.checkin.ui.profile.ProfileScreen
import com.airline.checkin.ui.profile.SavedTravelerFormScreen
import com.airline.checkin.ui.onboarding.OnboardingScreen
import com.airline.checkin.ui.seat.SeatMapScreen

object Routes {
    const val ONBOARDING     = "onboarding"
    const val WELCOME        = "welcome"
    const val COMPLETE_PROFILE = "complete_profile"
    const val HOME           = "home"
    const val BOOK_FLIGHT    = "book_flight"
    const val PROFILE        = "profile"
    const val NEW_SAVED_TRAVELER = "profile/traveler/new"
    const val FLIGHT_RESULTS = "flight_results/{origin}/{destination}/{startDate}/{endDate}/{passengers}"
    const val FLIGHT_DETAIL  = "flight_detail/{flightId}/{passengers}"
    const val PASSENGER_INFO = "passenger_info/{flightId}/{passengers}/{cabin}/{price}"
    const val PAYMENT        = "payment/{flightId}/{passengers}/{cabin}/{price}"
    const val PAYMENT_SUCCESS = "payment_success/{bookingId}/{reference}/{currency}/{amount}"
    const val MY_BOOKINGS    = "my_bookings"
    const val CHECK_IN       = "check_in/{bookingId}"
    const val SEAT_MAP       = "seat_map/{flightId}/{passengerIndex}/{cabinClass}"
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
        BottomNavItem(route = Routes.MY_BOOKINGS, label = "Bookings", icon = Icons.Default.Search),
        BottomNavItem(route = Routes.PROFILE, label = "Profile", icon = Icons.Default.Person)
    )
    val bottomBarRoutes = setOf(Routes.HOME, Routes.MY_BOOKINGS, Routes.PROFILE)
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
                val userName by authViewModel.displayName.collectAsState()
                HomeScreen(
                    userName = userName,
                    onViewBookings = { navController.navigate(Routes.MY_BOOKINGS) },
                    onBookFlight = { navController.navigate(Routes.BOOK_FLIGHT) },
                    onProfileClick = { navController.navigate(Routes.PROFILE) }
                )
            }

            composable(Routes.PROFILE) {
                val authViewModel: AuthViewModel = hiltViewModel()
                val userName by authViewModel.displayName.collectAsState()
                ProfileScreen(
                    userName = userName,
                    onEditProfile = { navController.navigate(Routes.COMPLETE_PROFILE) },
                    onAddNewTraveler = { navController.navigate(Routes.NEW_SAVED_TRAVELER) },
                    onSignOut = {
                        authViewModel.signOut()
                        navController.navigate(Routes.WELCOME) {
                            popUpTo(Routes.HOME) { inclusive = true }
                        }
                    },
                    onBack = { navController.popBackStack() }
                )
            }

            composable(Routes.NEW_SAVED_TRAVELER) {
                SavedTravelerFormScreen(
                    onBack = { navController.popBackStack() }
                )
            }

            composable(Routes.MY_BOOKINGS) {
                MyBookingsScreen(
                    onNavigateToCheckIn = { bookingId -> navController.navigate("check_in/$bookingId") },
                    onNavigateToBoardingPass = { bookingId -> navController.navigate("boarding_pass/$bookingId") }
                )
            }

            composable(Routes.BOOK_FLIGHT) {
                FlightSearchScreen(
                    onSearch = { origin, destination, startDate, endDate, passengers ->
                        navController.navigate("flight_results/$origin/$destination/$startDate/$endDate/$passengers")
                    }
                )
            }

            composable(Routes.FLIGHT_RESULTS) { backStack ->
                val origin = backStack.arguments?.getString("origin") ?: ""
                val destination = backStack.arguments?.getString("destination") ?: ""
                val startDate = backStack.arguments?.getString("startDate") ?: ""
                val endDate = backStack.arguments?.getString("endDate") ?: ""
                val passengers = backStack.arguments?.getString("passengers")?.toIntOrNull() ?: 1
                FlightResultsScreen(
                    origin = origin,
                    destination = destination,
                    startDate = startDate,
                    endDate = endDate,
                    passengers = passengers,
                    onBack = { navController.popBackStack() },
                    onSelectFlight = { flight ->
                        navController.navigate("flight_detail/${flight.id}/$passengers")
                    }
                )
            }

            composable(Routes.FLIGHT_DETAIL) { backStack ->
                val flightId = backStack.arguments?.getString("flightId") ?: ""
                val passengers = backStack.arguments?.getString("passengers")?.toIntOrNull() ?: 1
                FlightDetailScreen(
                    flightId = flightId,
                    passengers = passengers,
                    onBack = { navController.popBackStack() },
                    onSelectTicket = { cabinClass, pricePerPerson ->
                        navController.navigate(
                            "passenger_info/$flightId/$passengers/${cabinClass.name}/$pricePerPerson"
                        )
                    }
                )
            }

            composable(Routes.PAYMENT) { backStack ->
                val flightId = backStack.arguments?.getString("flightId") ?: ""
                val passengers = backStack.arguments?.getString("passengers")?.toIntOrNull() ?: 1
                val cabinRaw = backStack.arguments?.getString("cabin") ?: CabinClass.ECONOMY.name
                val cabinClass = runCatching { CabinClass.valueOf(cabinRaw) }
                    .getOrDefault(CabinClass.ECONOMY)
                val pricePerPerson = backStack.arguments?.getString("price")?.toIntOrNull() ?: 0

                val passengerEntry = remember(backStack) {
                    navController.getBackStackEntry(Routes.PASSENGER_INFO)
                }
                val flowVm: com.airline.checkin.ui.booking.BookingFlowViewModel = hiltViewModel(passengerEntry)

                PaymentScreen(
                    flightId = flightId,
                    passengers = passengers,
                    cabinClass = cabinClass,
                    pricePerPerson = pricePerPerson,
                    onBack = { navController.popBackStack() },
                    onPaymentSuccess = { confirmation, amount, currency ->
                        navController.navigate(
                            "payment_success/${confirmation.id}/${confirmation.reference}/$currency/$amount"
                        )
                    },
                    flowViewModel = flowVm
                )
            }

            composable(Routes.PASSENGER_INFO) { backStack ->
                val flightId = backStack.arguments?.getString("flightId") ?: ""
                val passengers = backStack.arguments?.getString("passengers")?.toIntOrNull() ?: 1
                val cabin = backStack.arguments?.getString("cabin") ?: CabinClass.ECONOMY.name
                val pricePerPerson = backStack.arguments?.getString("price")?.toIntOrNull() ?: 0
                val flowVm: com.airline.checkin.ui.booking.BookingFlowViewModel = hiltViewModel()
                
                val savedStateHandle = backStack.savedStateHandle
                val pickedIdx = savedStateHandle.get<Int>("pickedSeat_idx")
                val pickedId = savedStateHandle.get<String>("pickedSeat_id")
                val pickedNum = savedStateHandle.get<String>("pickedSeat_num")
                
                androidx.compose.runtime.LaunchedEffect(pickedIdx, pickedId, pickedNum) {
                    if (pickedIdx != null && pickedId != null && pickedNum != null) {
                        flowVm.setSeatForPassenger(pickedIdx, pickedId, pickedNum)
                        savedStateHandle.remove<Int>("pickedSeat_idx")
                        savedStateHandle.remove<String>("pickedSeat_id")
                        savedStateHandle.remove<String>("pickedSeat_num")
                    }
                }

                com.airline.checkin.ui.booking.PassengerListScreen(
                    flightId = flightId,
                    passengers = passengers,
                    cabin = runCatching { CabinClass.valueOf(cabin) }.getOrDefault(CabinClass.ECONOMY),
                    pricePerPerson = pricePerPerson,
                    onBack = { navController.popBackStack() },
                    onPickSeat = { idx, passCabin -> navController.navigate("seat_map/$flightId/$idx/$passCabin") },
                    onDone = {
                        navController.navigate("payment/$flightId/${flowVm.draft.value.passengersCount}/$cabin/$pricePerPerson")
                    },
                    viewModel = flowVm
                )
            }

            composable(Routes.PAYMENT_SUCCESS) { backStack ->
                val bookingId = backStack.arguments?.getString("bookingId") ?: ""
                val reference = backStack.arguments?.getString("reference") ?: ""
                val currency = backStack.arguments?.getString("currency") ?: "USD"
                val amount = backStack.arguments?.getString("amount")?.toIntOrNull() ?: 0
                PaymentSuccessScreen(
                    reference = reference,
                    amount = amount,
                    currency = currency,
                    onSeeTicket = { navController.navigate("boarding_pass/$bookingId") },
                    onBackHome = {
                        navController.navigate(Routes.HOME) {
                            popUpTo(Routes.HOME) { inclusive = true }
                        }
                    }
                )
            }


            composable(Routes.CHECK_IN) { backStack ->
                val bookingId = backStack.arguments?.getString("bookingId") ?: ""
                CheckInScreen(
                    bookingId  = bookingId,
                    onGoToSeat = { flightId -> navController.navigate("seat_map/$flightId/0/ECONOMY") },
                    onDone     = { navController.navigate("boarding_pass/$bookingId") }
                )
            }

            composable(Routes.SEAT_MAP) { backStack ->
                val flightId = backStack.arguments?.getString("flightId") ?: ""
                val passengerIndex = backStack.arguments?.getString("passengerIndex")?.toIntOrNull() ?: 0
                val cabinClass = backStack.arguments?.getString("cabinClass") ?: "ECONOMY"

                val passengerEntry = remember(backStack) {
                    runCatching { navController.getBackStackEntry(Routes.PASSENGER_INFO) }.getOrNull()
                }
                val flowVm: com.airline.checkin.ui.booking.BookingFlowViewModel? =
                    if (passengerEntry != null) hiltViewModel(passengerEntry) else null

                com.airline.checkin.ui.seat.SeatMapScreen(
                    flightId = flightId,
                    passengerIndex = passengerIndex,
                    cabinClass = cabinClass,
                    onSeatPicked = { seatId, seatNumber ->
                        if (flowVm != null) {
                            flowVm.setSeatForPassenger(passengerIndex, seatId, seatNumber)
                        } else {
                            navController.previousBackStackEntry?.savedStateHandle?.set("pickedSeat_idx", passengerIndex)
                            navController.previousBackStackEntry?.savedStateHandle?.set("pickedSeat_id", seatId)
                            navController.previousBackStackEntry?.savedStateHandle?.set("pickedSeat_num", seatNumber)
                        }
                        navController.popBackStack()
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
