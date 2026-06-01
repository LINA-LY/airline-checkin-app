package com.airline.checkin.ui.home

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Flight
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.airline.checkin.domain.model.Booking
import com.airline.checkin.ui.AppColors
import com.airline.checkin.ui.AppDimens
import com.airline.checkin.ui.auth.AppTextField
import com.airline.checkin.ui.auth.AuthViewModel

@Composable
fun HomeScreen(
    userName: String?,
    recentBooking: Booking? = null,
    onViewBookings: () -> Unit,
    onProfileClick: () -> Unit,
    authViewModel: AuthViewModel = hiltViewModel()
) {
    val userBookings by authViewModel.userBookings.collectAsState()

    var bookingRef by remember { mutableStateOf("") }
    var lastName   by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(AppColors.Gray50)
            .verticalScroll(rememberScrollState())
    ) {
        // ── Top bar ──────────────────────────────────────────────
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(AppColors.White)
                .padding(start = 20.dp, end = 20.dp, top = 52.dp, bottom = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "Hello, ${userName ?: "Traveler"}",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = AppColors.Gray900
                )
                Text(
                    text = "Find your booking below",
                    fontSize = 13.sp,
                    color = AppColors.Gray500
                )
            }
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(AppColors.PrimaryFaint)
                    .border(1.dp, AppColors.PrimaryLight, CircleShape)
                    .clickable { onProfileClick() },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Outlined.Person,
                    contentDescription = "Profile",
                    tint = AppColors.Primary,
                    modifier = Modifier.size(20.dp)
                )
            }
        }

        // ── Booking Lookup Card ───────────────────────────────────
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(AppColors.White)
                .padding(horizontal = 20.dp, vertical = 16.dp)
        ) {
            Text(
                text = "Find my booking",
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
                color = AppColors.Gray900
            )
            Spacer(Modifier.height(16.dp))

            AppTextField(
                value = bookingRef,
                onValueChange = { bookingRef = it },
                label = "Booking reference",
                icon = Icons.Outlined.Flight
            )
            Spacer(Modifier.height(12.dp))
            AppTextField(
                value = lastName,
                onValueChange = { lastName = it },
                label = "Last name"
            )

            Spacer(Modifier.height(16.dp))

            Button(
                onClick = { onViewBookings() },
                modifier = Modifier.fillMaxWidth().height(AppDimens.buttonHeight),
                shape = RoundedCornerShape(AppDimens.radiusFull),
                colors = ButtonDefaults.buttonColors(containerColor = AppColors.Primary)
            ) {
                Icon(Icons.Outlined.Search, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text("Find my booking", fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
            }
        }

        HorizontalDivider(color = AppColors.Gray100)

        // ── My Flights Section ────────────────────────────────────
        if (userBookings.isNotEmpty()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 20.dp, end = 20.dp, top = 20.dp, bottom = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("My flights", fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = AppColors.Gray900)
                Text(
                    "See all",
                    fontSize = 13.sp,
                    color = AppColors.Primary,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.clickable { onViewBookings() }
                )
            }

            userBookings.take(3).forEach { booking ->
                HomeFlightCard(booking = booking, onClick = { onViewBookings() })
                Spacer(Modifier.height(4.dp))
            }
        } else {
            Spacer(Modifier.height(32.dp))
            Box(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Outlined.Flight, contentDescription = null, tint = AppColors.Gray300, modifier = Modifier.size(40.dp))
                    Spacer(Modifier.height(10.dp))
                    Text(
                        text = "No flights found.\nEnter your booking reference above.",
                        color = AppColors.Gray500,
                        textAlign = TextAlign.Center,
                        fontSize = 14.sp
                    )
                }
            }
        }

        Spacer(Modifier.height(40.dp))
    }
}

@Composable
private fun HomeFlightCard(booking: Booking, onClick: () -> Unit) {
    val (depTime, arrTime, dateDisplay) = remember(booking.id) {
        try {
            val norm = booking.departureTime.let { if (!it.endsWith("Z") && !it.contains("+")) "${it}Z" else it }
            val depInstant = java.time.Instant.parse(norm)
            val ld = java.time.LocalDateTime.ofInstant(depInstant, java.time.ZoneId.systemDefault())
            Triple(
                ld.format(java.time.format.DateTimeFormatter.ofPattern("HH:mm")),
                ld.plusHours(2).format(java.time.format.DateTimeFormatter.ofPattern("HH:mm")),
                ld.format(java.time.format.DateTimeFormatter.ofPattern("EEE, MMM d"))
            )
        } catch (e: Exception) { Triple("--:--", "--:--", "—") }
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 6.dp)
            .clip(RoundedCornerShape(AppDimens.radiusLarge))
            .background(AppColors.White)
            .border(1.dp, AppColors.Gray100, RoundedCornerShape(AppDimens.radiusLarge))
            .clickable { onClick() }
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Route row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(horizontalAlignment = Alignment.Start) {
                    Text(booking.departure.take(3).uppercase().ifEmpty { "???" }, fontSize = 22.sp, fontWeight = FontWeight.Bold, color = AppColors.Gray900)
                    Text(depTime, fontSize = 12.sp, color = AppColors.Gray500)
                }

                Box(
                    modifier = Modifier.weight(1f).height(48.dp).padding(horizontal = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Canvas(modifier = Modifier.fillMaxWidth().height(28.dp)) {
                        drawArc(
                            color = AppColors.Primary,
                            startAngle = 180f,
                            sweepAngle = 180f,
                            useCenter = false,
                            topLeft = Offset(size.width * 0.05f, 0f),
                            size = androidx.compose.ui.geometry.Size(size.width * 0.9f, size.height * 2.2f),
                            style = Stroke(width = 2.dp.toPx(), pathEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 8f), 0f))
                        )
                    }
                    Icon(Icons.Outlined.Flight, contentDescription = null, modifier = Modifier.size(18.dp).rotate(90f), tint = AppColors.Primary)
                }

                Column(horizontalAlignment = Alignment.End) {
                    Text(booking.destination.take(3).uppercase().ifEmpty { "???" }, fontSize = 22.sp, fontWeight = FontWeight.Bold, color = AppColors.Gray900)
                    Text(arrTime, fontSize = 12.sp, color = AppColors.Gray500)
                }
            }

            Spacer(Modifier.height(12.dp))
            HorizontalDivider(color = AppColors.Gray100)
            Spacer(Modifier.height(10.dp))

            Row(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Date", fontSize = 11.sp, color = AppColors.Gray500)
                    Text(dateDisplay, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = AppColors.Gray700)
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text("Class", fontSize = 11.sp, color = AppColors.Gray500)
                    Text(booking.cabinClass.ifBlank { "Economy" }, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = AppColors.Gray700)
                }
                Column(modifier = Modifier.weight(1f), horizontalAlignment = Alignment.End) {
                    if (booking.checkInStatus) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(AppDimens.radiusFull))
                                .background(AppColors.SuccessLight)
                                .padding(horizontal = 8.dp, vertical = 3.dp)
                        ) {
                            Text("Checked in", fontSize = 11.sp, color = AppColors.Success, fontWeight = FontWeight.SemiBold)
                        }
                    } else {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(AppDimens.radiusFull))
                                .background(AppColors.PrimaryFaint)
                                .padding(horizontal = 8.dp, vertical = 3.dp)
                        ) {
                            Text("Check in", fontSize = 11.sp, color = AppColors.Primary, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            }
        }
    }
}