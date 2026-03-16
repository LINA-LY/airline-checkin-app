package com.airline.checkin.ui

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.airline.checkin.ui.auth.LoginScreen
import com.airline.checkin.ui.auth.RegisterScreen
import com.airline.checkin.ui.boardingpass.BoardingPassScreen
import com.airline.checkin.ui.checkin.BookingLookupScreen
import com.airline.checkin.ui.checkin.CheckInScreen
import com.airline.checkin.ui.seat.SeatMapScreen

object Routes {
    const val LOGIN          = "login"
    const val REGISTER       = "register"
    const val BOOKING_LOOKUP = "booking_lookup"
    const val CHECK_IN       = "check_in/{bookingId}"
    const val SEAT_MAP       = "seat_map/{flightId}"
    const val BOARDING_PASS  = "boarding_pass/{bookingId}"
}

@Composable
fun AppNavGraph(navController: NavHostController = rememberNavController()) {
    NavHost(navController = navController, startDestination = Routes.LOGIN) {

        composable(Routes.LOGIN) {
            LoginScreen(
                onLoginSuccess  = { navController.navigate(Routes.BOOKING_LOOKUP) },
                onGoToRegister  = { navController.navigate(Routes.REGISTER) }
            )
        }

        composable(Routes.REGISTER) {
            RegisterScreen(
                onRegisterSuccess = { navController.navigate(Routes.BOOKING_LOOKUP) },
                onGoToLogin       = { navController.popBackStack() }
            )
        }

        composable(Routes.BOOKING_LOOKUP) {
            BookingLookupScreen(
                onBookingFound = { bookingId ->
                    navController.navigate("check_in/$bookingId")
                }
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
            BoardingPassScreen(bookingId = bookingId)
        }
    }
}
