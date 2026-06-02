// app/src/main/java/com/airline/checkin/ui/profile/ProfileScreens.kt
package com.airline.checkin.ui.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.Logout
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.airline.checkin.ui.AppColors
import com.airline.checkin.ui.AppDimens
import com.airline.checkin.ui.ThemeViewModel
import com.airline.checkin.ui.auth.AppTextField
import com.airline.checkin.ui.auth.ErrorBanner

@Composable
fun ProfileScreen(
    userName: String?,
    onEditProfile: () -> Unit,
    onSignOut: () -> Unit,
    onBack: () -> Unit,
    onHelpCenter: () -> Unit,
    onAbout: () -> Unit,
    onSavedDocs: () -> Unit,
    viewModel: ProfileViewModel = hiltViewModel(),
    themeViewModel: ThemeViewModel = hiltViewModel()
) {
    var showChangePassword by remember { mutableStateOf(false) }
    val isDarkMode by themeViewModel.isDarkMode.collectAsState()
    val userProfile by viewModel.userProfile.collectAsState()

    // Refresh profile details on screen launch / edit profile navigation back
    LaunchedEffect(Unit) {
        viewModel.loadUserProfile()
    }

    val displayName = userProfile?.fullName?.takeIf { it.isNotBlank() } ?: userName ?: "Traveler"
    val initial = displayName.firstOrNull()?.uppercase() ?: "?"

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(AppColors.Gray50)
            .verticalScroll(rememberScrollState())
    ) {
        // ── Top Bar ──────────────────────────────────────────────
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(AppColors.White)
                .padding(start = 4.dp, end = 20.dp, top = 48.dp, bottom = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "Back", tint = AppColors.Gray900)
            }
            Text("Profile", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = AppColors.Gray900)
        }

        // ── Avatar + Name ─────────────────────────────────────────
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(AppColors.White)
                .padding(top = 20.dp, bottom = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier.size(80.dp).clip(CircleShape).background(AppColors.Primary),
                contentAlignment = Alignment.Center
            ) {
                Text(initial, color = Color.White, fontSize = 30.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
            }
            Spacer(Modifier.height(12.dp))
            Text(displayName, fontSize = 20.sp, fontWeight = FontWeight.Bold, color = AppColors.Gray900)
            Spacer(Modifier.height(12.dp))
            OutlinedButton(
                onClick = onEditProfile,
                shape = RoundedCornerShape(AppDimens.radiusFull),
                border = ButtonDefaults.outlinedButtonBorder.copy(brush = androidx.compose.ui.graphics.SolidColor(AppColors.Primary)),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = AppColors.Primary)
            ) {
                Icon(Icons.Outlined.Edit, contentDescription = null, modifier = Modifier.size(15.dp))
                Spacer(Modifier.width(6.dp))
                Text("Edit profile", fontSize = 14.sp)
            }
        }

        Spacer(Modifier.height(12.dp))

        // ── App Settings ─────────────────────────────────────────
        SectionLabel("App Settings")
        SettingsCard {
            SettingsRowSwitch(
                icon = Icons.Outlined.DarkMode, iconBg = AppColors.Gray100, iconTint = AppColors.Gray900,
                title = "Dark Mode", subtitle = "Toggle dark/light theme",
                checked = isDarkMode, onCheckedChange = { themeViewModel.toggle() }
            )
        }

        Spacer(Modifier.height(12.dp))

        // ── Personal Details ──────────────────────────────────────
        userProfile?.let { profile ->
            val nameParts = profile.fullName.trim().split(" ")
            val firstName = nameParts.firstOrNull().orEmpty()
            val lastName = if (nameParts.size > 1) nameParts.drop(1).joinToString(" ") else ""

            SectionLabel("Personal Details")
            SettingsCard {
                DetailRow(label = "First Name", value = firstName.ifBlank { "—" })
                RowDivider()
                DetailRow(label = "Last Name", value = lastName.ifBlank { "—" })
                RowDivider()
                DetailRow(label = "Phone Number", value = profile.phone.ifBlank { "—" })
                RowDivider()
                DetailRow(label = "Email Address", value = profile.email.ifBlank { "—" })
            }
            Spacer(Modifier.height(12.dp))
        }

        // ── Security ──────────────────────────────────────
        SectionLabel("Security")
        SettingsCard {
            SettingsRow(icon = Icons.Outlined.Lock, iconBg = Color(0xFFFEE2E2), iconTint = AppColors.Error, title = "Change password", subtitle = "Update your password", onClick = { showChangePassword = true })
        }

        Spacer(Modifier.height(12.dp))

        // ── Travel Documents ─────────────────────────────────────
        SectionLabel("Travel documents")
        SettingsCard {
            SettingsRow(icon = Icons.Outlined.Badge, iconBg = Color(0xFFDBFAE2), iconTint = AppColors.Success, title = "Saved documents", subtitle = "Passports, IDs", onClick = onSavedDocs)
        }

        Spacer(Modifier.height(12.dp))

        // ── Support ───────────────────────────────────────────────
        SectionLabel("Support")
        SettingsCard {
            SettingsRow(icon = Icons.Outlined.Help, iconBg = AppColors.Gray100, iconTint = AppColors.Gray700, title = "Help center", subtitle = "FAQs and virtual assistant", onClick = onHelpCenter)
            RowDivider()
            SettingsRow(icon = Icons.Outlined.Info, iconBg = AppColors.Gray100, iconTint = AppColors.Gray700, title = "About", subtitle = "App version and info", onClick = onAbout)
        }

        Spacer(Modifier.height(12.dp))

        // ── Sign Out ──────────────────────────────────────────────
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .clip(RoundedCornerShape(AppDimens.radiusLarge))
                .background(AppColors.White)
                .clickable { onSignOut() }
                .padding(horizontal = 16.dp, vertical = 16.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Box(
                    modifier = Modifier.size(36.dp).clip(RoundedCornerShape(AppDimens.radiusMedium)).background(AppColors.ErrorLight),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.AutoMirrored.Outlined.Logout, contentDescription = null, tint = AppColors.Error, modifier = Modifier.size(18.dp))
                }
                Text("Sign out", fontSize = 15.sp, fontWeight = FontWeight.Medium, color = AppColors.Error)
            }
        }

        Spacer(Modifier.height(40.dp))
    }

    if (showChangePassword) {
        ChangePasswordSheet(onDismiss = { showChangePassword = false }, viewModel = viewModel)
    }
}

// ─── Sub-Screens for Profile ─────────────────────────────────────────

@Composable
fun AboutScreen(onBack: () -> Unit) {
    Column(modifier = Modifier.fillMaxSize().background(AppColors.Gray50)) {
        Row(
            modifier = Modifier.fillMaxWidth().background(AppColors.White).padding(start = 4.dp, end = 20.dp, top = 48.dp, bottom = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "Back", tint = AppColors.Gray900) }
            Text("About", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = AppColors.Gray900)
        }
        HorizontalDivider(color = AppColors.Gray100)

        Column(
            modifier = Modifier.fillMaxWidth().padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(modifier = Modifier.size(80.dp).clip(RoundedCornerShape(20.dp)).background(AppColors.Primary), contentAlignment = Alignment.Center) {
                Icon(Icons.Outlined.FlightTakeoff, contentDescription = null, tint = Color.White, modifier = Modifier.size(40.dp))
            }
            Spacer(Modifier.height(16.dp))
            Text("Airline Check-in", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = AppColors.Gray900)
            Text("Version 1.0.0", fontSize = 14.sp, color = AppColors.Gray500)
           
            Spacer(Modifier.height(32.dp))
            Text("Designed for seamless travel.", fontSize = 16.sp, color = AppColors.Gray700, textAlign = TextAlign.Center)
            Spacer(Modifier.height(8.dp))
            Text("Built using Jetpack Compose, Hilt, and Firebase.", fontSize = 14.sp, color = AppColors.Gray500, textAlign = TextAlign.Center)
        }
    }
}

@Composable
fun SavedDocumentsScreen(onBack: () -> Unit, viewModel: ProfileViewModel = hiltViewModel()) {
    val doc by viewModel.savedPassport.collectAsState()
   
    Column(modifier = Modifier.fillMaxSize().background(AppColors.Gray50)) {
        Row(
            modifier = Modifier.fillMaxWidth().background(AppColors.White).padding(start = 4.dp, end = 20.dp, top = 48.dp, bottom = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "Back", tint = AppColors.Gray900) }
            Text("Saved Documents", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = AppColors.Gray900)
        }
        HorizontalDivider(color = AppColors.Gray100)

        if (doc == null) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Outlined.Badge, contentDescription = null, tint = AppColors.Gray300, modifier = Modifier.size(64.dp))
                    Spacer(Modifier.height(16.dp))
                    Text("No saved documents", fontSize = 18.sp, fontWeight = FontWeight.SemiBold, color = AppColors.Gray900)
                    Text("Scan your passport during check-in\nto save it here automatically.", fontSize = 14.sp, color = AppColors.Gray500, textAlign = TextAlign.Center)
                }
            }
        } else {
            Column(modifier = Modifier.padding(20.dp)) {
                Box(
                    modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(AppDimens.radiusLarge)).background(AppColors.White).border(1.dp, AppColors.Gray100, RoundedCornerShape(AppDimens.radiusLarge)).padding(20.dp)
                ) {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Outlined.Badge, contentDescription = null, tint = AppColors.Primary)
                            Spacer(Modifier.width(10.dp))
                            Text("Passport", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = AppColors.Gray900)
                        }
                        Spacer(Modifier.height(16.dp))
                        Text("Name: ${doc!!.firstName} ${doc!!.lastName}", fontSize = 14.sp, color = AppColors.Gray700)
                        Spacer(Modifier.height(4.dp))
                        Text("Passport No: ${doc!!.number}", fontSize = 14.sp, color = AppColors.Gray700)
                        Spacer(Modifier.height(4.dp))
                        Text("Date of Birth: ${doc!!.dob}", fontSize = 14.sp, color = AppColors.Gray700)
                    }
                }
               
                Spacer(Modifier.height(24.dp))
               
                OutlinedButton(
                    onClick = { viewModel.deleteSavedPassport() },
                    modifier = Modifier.fillMaxWidth().height(AppDimens.buttonHeight),
                    shape = RoundedCornerShape(AppDimens.radiusFull),
                    border = ButtonDefaults.outlinedButtonBorder.copy(brush = androidx.compose.ui.graphics.SolidColor(AppColors.Error)),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = AppColors.Error)
                ) {
                    Icon(Icons.Outlined.Delete, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Delete Document")
                }
            }
        }
    }
}

// ─── Helpers ─────────────────────────────────────────────────────────

@Composable
private fun DetailRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, fontSize = 14.sp, color = AppColors.Gray500, fontWeight = FontWeight.Medium)
        Text(value, fontSize = 15.sp, color = AppColors.Gray900, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun SectionLabel(label: String) {
    Text(
        text = label.uppercase(), fontSize = 11.sp, fontWeight = FontWeight.SemiBold,
        color = AppColors.Gray500, letterSpacing = 0.8.sp,
        modifier = Modifier.padding(horizontal = 24.dp, vertical = 6.dp)
    )
}

@Composable
private fun SettingsCard(content: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp).clip(RoundedCornerShape(AppDimens.radiusLarge)).background(AppColors.White),
        content = content
    )
}

@Composable
private fun RowDivider() {
    HorizontalDivider(modifier = Modifier.padding(start = 64.dp), color = AppColors.Gray100)
}

@Composable
private fun SettingsRow(icon: ImageVector, iconBg: Color, iconTint: Color, title: String, subtitle: String, onClick: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().clickable { onClick() }.padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Box(modifier = Modifier.size(36.dp).clip(RoundedCornerShape(AppDimens.radiusMedium)).background(iconBg), contentAlignment = Alignment.Center) {
            Icon(icon, contentDescription = null, tint = iconTint, modifier = Modifier.size(18.dp))
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(title, fontSize = 15.sp, fontWeight = FontWeight.Medium, color = AppColors.Gray900)
            Text(subtitle, fontSize = 12.sp, color = AppColors.Gray500)
        }
        Icon(Icons.Outlined.ChevronRight, contentDescription = null, tint = AppColors.Gray300, modifier = Modifier.size(18.dp))
    }
}

@Composable
private fun SettingsRowSwitch(icon: ImageVector, iconBg: Color, iconTint: Color, title: String, subtitle: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().clickable { onCheckedChange(!checked) }.padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Box(modifier = Modifier.size(36.dp).clip(RoundedCornerShape(AppDimens.radiusMedium)).background(iconBg), contentAlignment = Alignment.Center) {
            Icon(icon, contentDescription = null, tint = iconTint, modifier = Modifier.size(18.dp))
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(title, fontSize = 15.sp, fontWeight = FontWeight.Medium, color = AppColors.Gray900)
            Text(subtitle, fontSize = 12.sp, color = AppColors.Gray500)
        }
        Switch(checked = checked, onCheckedChange = onCheckedChange, colors = SwitchDefaults.colors(checkedThumbColor = AppColors.Primary, checkedTrackColor = AppColors.PrimaryLight))
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ChangePasswordSheet(onDismiss: () -> Unit, viewModel: ProfileViewModel) {
    var current by remember { mutableStateOf("") }
    var newPass by remember { mutableStateOf("") }
    var confirm by remember { mutableStateOf("") }
    var submitted by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }

    ModalBottomSheet(onDismissRequest = onDismiss, containerColor = AppColors.White) {
        Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 16.dp)) {
            Text("Change password", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = AppColors.Gray900)
            Spacer(Modifier.height(20.dp))
            AppTextField(value = current, onValueChange = { current = it }, label = "Current password", visualTransformation = PasswordVisualTransformation(), error = if (submitted && current.isBlank()) "Required" else null)
            Spacer(Modifier.height(12.dp))
            AppTextField(value = newPass, onValueChange = { newPass = it }, label = "New password", visualTransformation = PasswordVisualTransformation(), error = if (submitted && newPass.length < 6) "Minimum 6 characters" else null)
            Spacer(Modifier.height(12.dp))
            AppTextField(value = confirm, onValueChange = { confirm = it }, label = "Confirm new password", visualTransformation = PasswordVisualTransformation(), error = if (submitted && newPass != confirm) "Passwords do not match" else null)
            error?.let { Spacer(Modifier.height(8.dp)); ErrorBanner(it) }
            Spacer(Modifier.height(24.dp))
            Button(
                onClick = {
                    submitted = true
                    if (current.isBlank() || newPass.length < 6 || newPass != confirm) return@Button
                    onDismiss()
                },
                modifier = Modifier.fillMaxWidth().height(AppDimens.buttonHeight), colors = ButtonDefaults.buttonColors(containerColor = AppColors.Primary)
            ) { Text("Update password", fontSize = 16.sp, fontWeight = FontWeight.SemiBold) }
            Spacer(Modifier.height(32.dp))
        }
    }
}