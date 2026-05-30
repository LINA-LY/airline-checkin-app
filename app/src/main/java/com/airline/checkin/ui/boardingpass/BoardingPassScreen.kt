package com.airline.checkin.ui.boardingpass

import android.Manifest
import android.app.DownloadManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Flight
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import kotlinx.coroutines.launch

private val PurplePrimary = Color(0xFF8B5CF6)
private val PurpleLight = Color(0xFFEDE9FE)
private val GrayLabel = Color(0xFF9CA3AF)
private val GrayDivider = Color(0xFFE5E7EB)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BoardingPassScreen(
    bookingId: String,
    onBackClick: () -> Unit = {},
    onBackToHome: () -> Unit = {},
    viewModel: BoardingPassViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    var showSuccessDialog by remember { mutableStateOf(false) }
    var showQrFullScreen by remember { mutableStateOf(false) }
    var savedPdfUri by remember { mutableStateOf<String?>(null) }

    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val storageGranted = permissions[Manifest.permission.WRITE_EXTERNAL_STORAGE] ?: true
        if (storageGranted || Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            coroutineScope.launch {
                val uriString = PdfGenerator.generateAndSavePdf(context, uiState.boardingPass!!, uiState.qrCodeBitmap)
                if (uriString != null) {
                    savedPdfUri = uriString
                    showSuccessDialog = true
                    showDownloadNotification(context, uriString)
                }
            }
        } else {
            Toast.makeText(context, "Storage permission is required.", Toast.LENGTH_LONG).show()
        }
    }

    val downloadTicket: () -> Unit = {
        val permissionsToRequest = mutableListOf<String>()
        if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.P &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            permissionsToRequest.add(Manifest.permission.WRITE_EXTERNAL_STORAGE)
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            permissionsToRequest.add(Manifest.permission.POST_NOTIFICATIONS)
        }

        if (permissionsToRequest.isNotEmpty()) {
            permissionLauncher.launch(permissionsToRequest.toTypedArray())
        } else {
            coroutineScope.launch {
                val uriString = PdfGenerator.generateAndSavePdf(context, uiState.boardingPass!!, uiState.qrCodeBitmap)
                if (uriString != null) {
                    savedPdfUri = uriString
                    showSuccessDialog = true
                    showDownloadNotification(context, uriString)
                }
            }
        }
    }

    LaunchedEffect(bookingId) { viewModel.loadBoardingPass(bookingId) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Boarding Pass", fontWeight = FontWeight.SemiBold) },
                navigationIcon = {
                    IconButton(onClick = onBackClick) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back") }
                },
                actions = {
                    IconButton(onClick = { }) { Icon(Icons.Default.MoreVert, "More options") }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White)
            )
        },
        containerColor = Color(0xFFF9FAFB)
    ) { paddingValues ->
        Box(modifier = Modifier.fillMaxSize().padding(paddingValues)) {
            when {
                uiState.isLoading -> CircularProgressIndicator(modifier = Modifier.align(Alignment.Center), color = PurplePrimary)
                uiState.error != null -> {
                    Column(modifier = Modifier.align(Alignment.Center).padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(uiState.error ?: "Error", color = MaterialTheme.colorScheme.error)
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(onClick = { viewModel.loadBoardingPass(bookingId) }, colors = ButtonDefaults.buttonColors(containerColor = PurplePrimary)) { Text("Retry") }
                    }
                }
                uiState.boardingPass != null -> {
                    BoardingPassContent(
                        boardingPass = uiState.boardingPass!!,
                        qrCodeBitmap = uiState.qrCodeBitmap,
                        onDownloadClick = downloadTicket,
                        onViewQrFullScreen = { showQrFullScreen = true }
                    )
                }
            }
        }
    }

    if (showSuccessDialog) {
        SuccessDialog(
            onSeeTicket = {
                showSuccessDialog = false
                openPdf(context, savedPdfUri)
            },
            onBackToHome = { showSuccessDialog = false; onBackToHome() }
        )
    }

    if (showQrFullScreen && uiState.qrCodeBitmap != null) {
        Dialog(
            onDismissRequest = { showQrFullScreen = false },
            properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false)
        ) {
            Surface(modifier = Modifier.fillMaxSize(), color = Color.White) {
                Column(
                    modifier = Modifier.fillMaxSize().padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text("Scan at Gate", fontWeight = FontWeight.Bold, fontSize = 32.sp, color = Color.Black)
                    Spacer(modifier = Modifier.height(48.dp))
                    Image(
                        bitmap = uiState.qrCodeBitmap!!.asImageBitmap(),
                        contentDescription = "Full Screen QR",
                        modifier = Modifier.fillMaxWidth().aspectRatio(1f)
                    )
                    Spacer(modifier = Modifier.height(64.dp))
                    Button(
                        onClick = { showQrFullScreen = false },
                        modifier = Modifier.fillMaxWidth().height(56.dp),
                        shape = RoundedCornerShape(28.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = PurplePrimary)
                    ) { Text("Close Full Screen", fontSize = 18.sp, fontWeight = FontWeight.SemiBold) }
                }
            }
        }
    }
}

// --- Helper Functions to Open PDF directly ---
private fun openPdf(context: Context, uriString: String?) {
    if (uriString != null && uriString.startsWith("content://")) {
        try {
            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(Uri.parse(uriString), "application/pdf")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            context.startActivity(Intent(DownloadManager.ACTION_VIEW_DOWNLOADS).apply { addFlags(Intent.FLAG_ACTIVITY_NEW_TASK) })
        }
    } else {
        try {
            context.startActivity(Intent(DownloadManager.ACTION_VIEW_DOWNLOADS).apply { addFlags(Intent.FLAG_ACTIVITY_NEW_TASK) })
        } catch (e: Exception) {
            Toast.makeText(context, "Saved to Downloads", Toast.LENGTH_SHORT).show()
        }
    }
}

private fun showDownloadNotification(context: Context, uriString: String?) {
    val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    val channelId = "boarding_pass_downloads"

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        val channel = NotificationChannel(channelId, "Downloads", NotificationManager.IMPORTANCE_DEFAULT)
        notificationManager.createNotificationChannel(channel)
    }

    val intent = if (uriString != null && uriString.startsWith("content://")) {
        Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(Uri.parse(uriString), "application/pdf")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
    } else {
        Intent(DownloadManager.ACTION_VIEW_DOWNLOADS)
    }

    val pendingIntent = PendingIntent.getActivity(
        context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    )

    val builder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        android.app.Notification.Builder(context, channelId)
    } else {
        @Suppress("DEPRECATION")
        android.app.Notification.Builder(context)
    }

    builder.setSmallIcon(android.R.drawable.stat_sys_download_done)
        .setContentTitle("Boarding Pass Saved")
        .setContentText("Tap to view your ticket")
        .setContentIntent(pendingIntent)
        .setAutoCancel(true)

    try {
        notificationManager.notify(System.currentTimeMillis().toInt(), builder.build())
    } catch (e: SecurityException) {
        // Ignored
    }
}

@Composable
private fun BoardingPassContent(
    boardingPass: com.airline.checkin.domain.model.BoardingPass,
    qrCodeBitmap: Bitmap?,
    onDownloadClick: () -> Unit,
    onViewQrFullScreen: () -> Unit
) {
    // We removed verticalScroll() entirely and use weight(1f) to stretch perfectly
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // Ticket Card takes all remaining vertical space above the buttons
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 20.dp, vertical = 16.dp)
            ) {
                // Info Section: Takes majority of the card space, evenly spacing the rows
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.SpaceEvenly
                ) {
                    FlightRouteSection(
                        origin = boardingPass.origin.ifBlank { "—" }, originCity = "—",
                        destination = boardingPass.destination.ifBlank { "—" }, destinationCity = "—",
                        duration = "N/A"
                    )

                    Column {
                        InfoLabel(label = "Traveler Name")
                        Text(
                            text = boardingPass.passengerName.ifBlank { boardingPass.passengerId }.ifEmpty { "—" },
                            fontSize = 18.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color.Black
                        )
                    }

                    Row(modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.weight(1f)) {
                            InfoLabel(label = "Date")
                            val displayDate = try {
                                if (boardingPass.boardingTime.isNotBlank()) {
                                    val instant = java.time.Instant.parse(boardingPass.boardingTime.let { if (!it.endsWith("Z") && !it.contains("+")) "${it}Z" else it })
                                    val localDate = java.time.LocalDateTime.ofInstant(instant, java.time.ZoneId.systemDefault())
                                    localDate.format(java.time.format.DateTimeFormatter.ofPattern("EEE, MMM d"))
                                } else "—"
                            } catch (e: Exception) {
                                "—"
                            }
                            Text(displayDate, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = Color.Black)
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            InfoLabel(label = "Class")
                            Text(boardingPass.cabinClass.ifBlank { "Economy" }, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = Color.Black)
                        }
                    }

                    Row(modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.weight(1f)) {
                            InfoLabel(label = "Departure")
                            val displayTime = try {
                                if (boardingPass.boardingTime.isNotBlank()) {
                                    val instant = java.time.Instant.parse(boardingPass.boardingTime.let { if (!it.endsWith("Z") && !it.contains("+")) "${it}Z" else it })
                                    val localDate = java.time.LocalDateTime.ofInstant(instant, java.time.ZoneId.systemDefault())
                                    localDate.format(java.time.format.DateTimeFormatter.ofPattern("HH:mm"))
                                } else "—"
                            } catch (e: Exception) {
                                "—"
                            }
                            Text(displayTime, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = Color.Black)
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            InfoLabel(label = "Arrival")
                            Text("TBD", fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = Color.Black)
                        }
                    }

                    Row(modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.weight(1f)) {
                            InfoLabel(label = "Flight no")
                            Text(boardingPass.flightNumber.ifEmpty { "N/A" }, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = Color.Black)
                        }
                        Column(modifier = Modifier.weight(0.6f)) {
                            InfoLabel(label = "Gate")
                            Text(boardingPass.gate.ifEmpty { "N/A" }, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = Color.Black)
                        }
                        Column(modifier = Modifier.weight(0.6f)) {
                            InfoLabel(label = "Seat")
                            Text(boardingPass.seatNumber.ifEmpty { "N/A" }, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = Color.Black)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
                DashedDivider()
                Spacer(modifier = Modifier.height(16.dp))

                // QR Code Section: Scales dynamically but takes less space than the info section
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(0.55f),
                    contentAlignment = Alignment.Center
                ) {
                    if (qrCodeBitmap != null) {
                        Image(
                            bitmap = qrCodeBitmap.asImageBitmap(),
                            contentDescription = "QR Code",
                            modifier = Modifier
                                .fillMaxHeight() 
                                .aspectRatio(1f) 
                        )
                    } else {
                        CircularProgressIndicator(color = PurplePrimary)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Fixed size buttons at the bottom
        Button(
            onClick = onDownloadClick,
            modifier = Modifier.fillMaxWidth().height(50.dp),
            shape = RoundedCornerShape(25.dp),
            colors = ButtonDefaults.buttonColors(containerColor = PurplePrimary)
        ) { Text("Download", fontSize = 16.sp, fontWeight = FontWeight.SemiBold) }

        Spacer(modifier = Modifier.height(12.dp))

        OutlinedButton(
            onClick = onViewQrFullScreen,
            modifier = Modifier.fillMaxWidth().height(50.dp),
            shape = RoundedCornerShape(25.dp),
            border = ButtonDefaults.outlinedButtonBorder(enabled = true).copy(brush = SolidColor(PurplePrimary))
        ) { Text("View QR code full screen", fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = PurplePrimary) }
    }
}

@Composable
private fun FlightRouteSection(origin: String, originCity: String, destination: String, destinationCity: String, duration: String) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Top) {
        Column(horizontalAlignment = Alignment.Start) {
            Text(origin, fontSize = 28.sp, fontWeight = FontWeight.Bold, color = Color.Black)
            Text(originCity, fontSize = 12.sp, color = GrayLabel)
        }
        Box(modifier = Modifier.weight(1f).height(50.dp), contentAlignment = Alignment.Center) { FlightArc(duration) }
        Column(horizontalAlignment = Alignment.End) {
            Text(destination, fontSize = 28.sp, fontWeight = FontWeight.Bold, color = Color.Black)
            Text(destinationCity, fontSize = 12.sp, color = GrayLabel)
        }
    }
}

@Composable
private fun FlightArc(duration: String) {
    Box(modifier = Modifier.fillMaxWidth().height(50.dp), contentAlignment = Alignment.Center) {
        Canvas(modifier = Modifier.fillMaxWidth().height(30.dp)) {
            val strokeWidth = 2.dp.toPx()
            val dashEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 8f), 0f)
            drawArc(
                color = PurplePrimary, startAngle = 180f, sweepAngle = 180f, useCenter = false,
                topLeft = Offset(size.width * 0.1f, 0f),
                size = androidx.compose.ui.geometry.Size(size.width * 0.8f, size.height * 2f),
                style = Stroke(width = strokeWidth, pathEffect = dashEffect)
            )
        }
        Icon(Icons.Default.Flight, null, modifier = Modifier.size(20.dp).rotate(90f), tint = PurplePrimary)
        Text(duration, fontSize = 10.sp, fontWeight = FontWeight.Medium, color = PurplePrimary, modifier = Modifier.offset(y = 20.dp))
    }
}

@Composable
private fun InfoLabel(label: String) {
    Text(label, fontSize = 12.sp, color = GrayLabel, modifier = Modifier.padding(bottom = 2.dp))
}

@Composable
private fun DashedDivider() {
    Canvas(modifier = Modifier.fillMaxWidth().height(1.dp)) {
        drawLine(
            color = GrayDivider, start = Offset(0f, 0f), end = Offset(size.width, 0f), strokeWidth = 2f,
            pathEffect = PathEffect.dashPathEffect(floatArrayOf(15f, 15f), 0f)
        )
    }
}

@Composable
private fun SuccessDialog(onSeeTicket: () -> Unit, onBackToHome: () -> Unit) {
    Dialog(onDismissRequest = { }) {
        Card(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White)
        ) {
            Column(modifier = Modifier.fillMaxWidth().padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Box(modifier = Modifier.size(80.dp), contentAlignment = Alignment.Center) {
                    Box(modifier = Modifier.size(80.dp).background(PurpleLight, CircleShape))
                    Box(modifier = Modifier.size(60.dp).background(PurplePrimary, CircleShape), contentAlignment = Alignment.Center) {
                        Icon(Icons.Default.Check, null, modifier = Modifier.size(36.dp), tint = Color.White)
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
                Text("Success!", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Color.Black)
                Spacer(modifier = Modifier.height(8.dp))
                Text("Your boarding pass has been downloaded successfully.", fontSize = 14.sp, color = GrayLabel, textAlign = TextAlign.Center)
                Spacer(modifier = Modifier.height(24.dp))
                Button(
                    onClick = onSeeTicket, modifier = Modifier.fillMaxWidth().height(50.dp),
                    shape = RoundedCornerShape(25.dp), colors = ButtonDefaults.buttonColors(containerColor = PurplePrimary)
                ) { Text("See ticket", fontSize = 16.sp, fontWeight = FontWeight.SemiBold) }
                Spacer(modifier = Modifier.height(8.dp))
                TextButton(onClick = onBackToHome) { Text("Back to home", fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = GrayLabel) }
            }
        }
    }
}