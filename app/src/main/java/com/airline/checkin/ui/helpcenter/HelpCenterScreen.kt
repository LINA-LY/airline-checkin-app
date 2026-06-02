package com.airline.checkin.ui.helpcenter

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.Send
import androidx.compose.material.icons.outlined.Support
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.airline.checkin.ui.AppColors
import com.airline.checkin.ui.AppDimens
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import javax.inject.Inject

// ─── Model ────────────────────────────────────────────────────────────────────

data class ChatMessage(
    val id: String,
    val text: String,
    val isUser: Boolean,
    val time: String = LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm"))
)

data class HelpUiState(
    val messages: List<ChatMessage> = listOf(
        ChatMessage(
            id = "welcome",
            text = "Hello! I'm your virtual assistant. How can I help you today?\n\nYou can ask me about:\n• Flight check-in\n• Boarding passes\n• Baggage policies\n• Seat selection",
            isUser = false
        )
    ),
    val inputText: String = "",
    val isTyping: Boolean = false
)

// ─── ViewModel ────────────────────────────────────────────────────────────────

@HiltViewModel
class HelpCenterViewModel @Inject constructor() : ViewModel() {

    private val _state = MutableStateFlow(HelpUiState())
    val state = _state.asStateFlow()

    fun onInputChange(text: String) {
        _state.value = _state.value.copy(inputText = text)
    }

    fun send() {
        val text = _state.value.inputText.trim()
        if (text.isBlank()) return

        val userMsg = ChatMessage(id = System.currentTimeMillis().toString(), text = text, isUser = true)
        _state.value = _state.value.copy(
            messages = _state.value.messages + userMsg,
            inputText = "",
            isTyping = true
        )

        viewModelScope.launch {
            delay(1200)
            val reply = generateReply(text)
            val botMsg = ChatMessage(id = System.currentTimeMillis().toString(), text = reply, isUser = false)
            _state.value = _state.value.copy(
                messages = _state.value.messages + botMsg,
                isTyping = false
            )
        }
    }

    private fun generateReply(input: String): String {
        val lower = input.lowercase()
        return when {
            lower.contains("check") && lower.contains("in") ->
                "Online check-in opens 24 hours before your departure. You can check in from the Check-In tab using your booking reference and last name."
            lower.contains("boarding") || lower.contains("pass") ->
                "Your boarding pass is available in the Passes tab once you've completed check-in. You can download it as a PDF or show the QR code at the gate."
            lower.contains("baggage") || lower.contains("luggage") || lower.contains("bag") ->
                "Standard allowance is 1 carry-on (7 kg) and 1 checked bag (23 kg) for economy. Business class passengers get 2 checked bags (32 kg each)."
            lower.contains("seat") ->
                "Seat selection is available during the check-in process. You can choose any available seat on the seat map. Exit rows and extra-legroom seats may incur an additional charge."
            lower.contains("cancel") ->
                "Cancellations can be made up to 24 hours before departure for a full refund. After that, a cancellation fee applies. Please contact our support team for assistance."
            lower.contains("delay") || lower.contains("flight status") ->
                "For real-time flight status updates, please check the airline's official website or contact the airport directly. We'll send you push notifications if your flight is affected."
            lower.contains("passport") || lower.contains("document") ->
                "Ensure your passport is valid for at least 6 months beyond your travel date. You can save your travel documents in the Profile > Travel Documents section."
            lower.contains("hello") || lower.contains("hi") || lower.contains("help") ->
                "I'm here to help! Feel free to ask about check-in, boarding passes, baggage, seat selection, or any other travel questions."
            lower.contains("thank") ->
                "You're welcome! Is there anything else I can help you with?"
            lower.contains("arabic") || lower.contains("عربي") || lower.contains("language") ->
                "You can change the app language in Profile > Preferences. We currently support English and Arabic."
            else ->
                "I understand you're asking about \"${input.take(60)}${if (input.length > 60) "…" else ""}\". For complex inquiries, please contact our support team at support@airline.com or call +1-800-AIR-HELP."
        }
    }
}

// ─── Quick Replies ─────────────────────────────────────────────────────────────

private val quickReplies = listOf(
    "How do I check in?",
    "Where is my boarding pass?",
    "Baggage allowance",
    "Change my seat"
)

// ─── Screen ───────────────────────────────────────────────────────────────────

@Composable
fun HelpCenterScreen(
    onBack: () -> Unit,
    viewModel: HelpCenterViewModel = androidx.hilt.navigation.compose.hiltViewModel()
) {
    val state by viewModel.state.collectAsState()
    val listState = rememberLazyListState()

    // Auto-scroll to bottom on new messages
    LaunchedEffect(state.messages.size) {
        if (state.messages.isNotEmpty()) {
            listState.animateScrollToItem(state.messages.size - 1)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(AppColors.Gray50)
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
                Icon(
                    Icons.AutoMirrored.Outlined.ArrowBack,
                    contentDescription = "Back",
                    tint = AppColors.Gray900
                )
            }

            // Agent avatar
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(AppColors.Primary),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Outlined.Support,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(20.dp)
                )
            }

            Spacer(Modifier.width(10.dp))

            Column {
                Text(
                    "Help Center",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = AppColors.Gray900
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(6.dp)
                            .clip(CircleShape)
                            .background(AppColors.Success)
                    )
                    Spacer(Modifier.width(4.dp))
                    Text("Virtual assistant · Always online", fontSize = 11.sp, color = AppColors.Gray500)
                }
            }
        }

        HorizontalDivider(color = AppColors.Gray100)

        // ── Messages ─────────────────────────────────────────────
        LazyColumn(
            state = listState,
            modifier = Modifier.weight(1f),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(state.messages, key = { it.id }) { message ->
                AnimatedVisibility(
                    visible = true,
                    enter = fadeIn() + slideInVertically(initialOffsetY = { it / 2 })
                ) {
                    MessageBubble(message = message)
                }
            }

            if (state.isTyping) {
                item {
                    TypingIndicator()
                }
            }
        }

        // ── Quick Replies (shown only when idle) ─────────────────
        if (!state.isTyping && state.messages.size < 3) {
            QuickRepliesRow(
                suggestions = quickReplies,
                onSelect = { viewModel.onInputChange(it); viewModel.send() }
            )
        }

        HorizontalDivider(color = AppColors.Gray100)

        // ── Input Bar ─────────────────────────────────────────────
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(AppColors.White)
                .padding(horizontal = 16.dp, vertical = 10.dp)
                .navigationBarsPadding()
                .imePadding(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = state.inputText,
                onValueChange = viewModel::onInputChange,
                modifier = Modifier.weight(1f),
                placeholder = {
                    Text("Type a message…", color = AppColors.Gray500, fontSize = 14.sp)
                },
                shape = RoundedCornerShape(AppDimens.radiusFull),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = AppColors.Primary,
                    unfocusedBorderColor = AppColors.Gray300,
                    cursorColor = AppColors.Primary,
                    focusedContainerColor = AppColors.Gray50,
                    unfocusedContainerColor = AppColors.Gray50,
                ),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                keyboardActions = KeyboardActions(onSend = { viewModel.send() }),
                maxLines = 4,
                textStyle = LocalTextStyle.current.copy(fontSize = 14.sp, color = AppColors.Gray900)
            )

            Spacer(Modifier.width(8.dp))

            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(if (state.inputText.isBlank()) AppColors.Gray100 else AppColors.Primary),
                contentAlignment = Alignment.Center
            ) {
                IconButton(
                    onClick = viewModel::send,
                    enabled = state.inputText.isNotBlank()
                ) {
                    Icon(
                        Icons.AutoMirrored.Outlined.Send,
                        contentDescription = "Send",
                        tint = if (state.inputText.isBlank()) AppColors.Gray500 else Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}

// ─── Message Bubble ───────────────────────────────────────────────────────────

@Composable
private fun MessageBubble(message: ChatMessage) {
    val isRtl = LocalLayoutDirection.current == LayoutDirection.Rtl

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (message.isUser) Arrangement.End else Arrangement.Start
    ) {
        if (!message.isUser) {
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .clip(CircleShape)
                    .background(AppColors.Primary),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Outlined.Support,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(14.dp)
                )
            }
            Spacer(Modifier.width(8.dp))
        }

        Column(
            horizontalAlignment = if (message.isUser) Alignment.End else Alignment.Start,
            modifier = Modifier.widthIn(max = 280.dp)
        ) {
            Box(
                modifier = Modifier
                    .clip(
                        RoundedCornerShape(
                            topStart = 16.dp,
                            topEnd = 16.dp,
                            bottomEnd = if (message.isUser) 4.dp else 16.dp,
                            bottomStart = if (message.isUser) 16.dp else 4.dp
                        )
                    )
                    .background(if (message.isUser) AppColors.Primary else AppColors.White)
                    .padding(horizontal = 14.dp, vertical = 10.dp)
            ) {
                Text(
                    text = message.text,
                    fontSize = 14.sp,
                    color = if (message.isUser) Color.White else AppColors.Gray900,
                    lineHeight = 20.sp
                )
            }

            Spacer(Modifier.height(2.dp))
            Text(
                text = message.time,
                fontSize = 10.sp,
                color = AppColors.Gray500,
                modifier = Modifier.padding(horizontal = 4.dp)
            )
        }

        if (message.isUser) Spacer(Modifier.width(8.dp))
    }
}

// ─── Typing Indicator ─────────────────────────────────────────────────────────

@Composable
private fun TypingIndicator() {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Start
    ) {
        Box(
            modifier = Modifier
                .clip(
                    RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp, bottomEnd = 16.dp, bottomStart = 4.dp)
                )
                .background(AppColors.White)
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
                repeat(3) { index ->
                    var visible by remember { mutableStateOf(false) }
                    LaunchedEffect(Unit) {
                        delay(index * 200L)
                        while (true) {
                            visible = true
                            delay(600)
                            visible = false
                            delay(600)
                        }
                    }
                    Box(
                        modifier = Modifier
                            .size(6.dp)
                            .clip(CircleShape)
                            .background(if (visible) AppColors.Primary else AppColors.Gray300)
                    )
                }
            }
        }
    }
}

// ─── Quick Replies Row ────────────────────────────────────────────────────────

@Composable
private fun QuickRepliesRow(
    suggestions: List<String>,
    onSelect: (String) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(AppColors.White)
            .padding(horizontal = 12.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        suggestions.take(2).forEach { suggestion ->
            SuggestionChip(
                onClick = { onSelect(suggestion) },
                label = {
                    Text(
                        suggestion,
                        fontSize = 12.sp,
                        color = AppColors.Primary,
                        maxLines = 1
                    )
                },
                colors = SuggestionChipDefaults.suggestionChipColors(
                    containerColor = AppColors.PrimaryFaint
                ),
                border = SuggestionChipDefaults.suggestionChipBorder(
                    enabled = true,
                    borderColor = AppColors.PrimaryLight
                ),
                modifier = Modifier.weight(1f)
            )
        }
    }
}