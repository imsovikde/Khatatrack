package com.example.ui.components

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.os.Build
import android.Manifest
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SheetState
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Close
import coil.compose.AsyncImage
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.Transaction
import com.example.ui.theme.BodyStyle
import com.example.ui.theme.CaptionStyle
import com.example.ui.theme.DisplayStyle
import com.example.ui.theme.KhataTheme
import com.example.ui.theme.LabelStyle
import com.example.ui.theme.TitleStyle
import com.example.util.CurrencyFormatter
import com.example.util.DateTimeUtils
import java.util.Calendar

import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.runtime.rememberCoroutineScope
import com.example.data.model.Contact
import com.example.util.IntelligentParser
import com.example.util.SpeechRecognizerManager
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddTransactionBottomSheet(
    sheetState: SheetState,
    initialType: String, // Transaction.TYPE_YOU_GAVE or Transaction.TYPE_YOU_GOT
    contactName: String,
    contactsList: List<Contact> = emptyList(),
    editingTransaction: Transaction? = null,
    onDismiss: () -> Unit,
    onSave: (
        amount: Double,
        type: String,
        paymentMode: String,
        note: String?,
        dueDate: Long?,
        referenceNumber: String?,
        editingTxId: Long?,
        attachmentPhotoUri: String?,
        contactIdOverride: Long?
    ) -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = KhataTheme.colors
    val context = LocalContext.current
    val activeSymbol = CurrencyFormatter.getActiveCurrencySymbol()

    var entryMode by remember { mutableStateOf("FOR_CONTACT") }
    var selectedContactId by remember { mutableStateOf(contactsList.find { it.name == contactName }?.id) }

    var hasNotificationPermission by remember {
        mutableStateOf(
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
            } else {
                true
            }
        )
    }

    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasNotificationPermission = granted
        if (granted) {
            // Permission granted, handled in switch if needed. We don't automatically toggle here to avoid race condition with state.
        }
    }

    var attachmentPhotoUri by remember { mutableStateOf<String?>(editingTransaction?.attachmentPhoto) }
    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            attachmentPhotoUri = uri.toString()
        }
    }
    
    var amountText by remember { mutableStateOf(editingTransaction?.amount?.let { if (it % 1.0 == 0.0) it.toLong().toString() else it.toString() } ?: "") }
    var currentType by remember { mutableStateOf(editingTransaction?.type ?: initialType) }
    var selectedPaymentMode by remember { mutableStateOf(editingTransaction?.paymentMode ?: "Cash") }
    var noteText by remember { mutableStateOf(editingTransaction?.note ?: "") }
    var referenceNumberText by remember { mutableStateOf(editingTransaction?.referenceNumber ?: "") }

    val calendar = remember { Calendar.getInstance() }
    var selectedDateMillis by remember { mutableStateOf(editingTransaction?.transactionDate ?: calendar.timeInMillis) }
    var selectedTimeString by remember { mutableStateOf(editingTransaction?.transactionTime ?: DateTimeUtils.getCurrentTimeString()) }

    var isReminderEnabled by remember { mutableStateOf(editingTransaction?.collectionDueDate != null) }
    var reminderOption by remember { mutableStateOf("Tomorrow") }
    var calculatedDueDate by remember { mutableStateOf<Long?>(editingTransaction?.collectionDueDate) }

    val focusRequester = remember { FocusRequester() }
    val coroutineScope = rememberCoroutineScope()

    val speechManager = remember { SpeechRecognizerManager(context) }
    var isListeningVoice by remember { mutableStateOf(false) }
    var voiceStatusText by remember { mutableStateOf<String?>(null) }
    var voiceMode by remember { mutableStateOf("FREEFORM") }
    var activeGuidedField by remember { mutableStateOf<String?>(null) }

    fun processVoiceOrTextInput(text: String) {
        coroutineScope.launch {
            val result = IntelligentParser.parseInputText(context, text, contactsList)
            if (result.amount != null) {
                amountText = if (result.amount % 1.0 == 0.0) result.amount.toLong().toString() else result.amount.toString()
            }
            if (result.intent.isNotBlank()) {
                currentType = result.intent
            }
            if (result.paymentMode.isNotBlank()) {
                selectedPaymentMode = result.paymentMode
            }
            if (!result.note.isNullOrBlank()) {
                noteText = result.note
            }
            if (!result.referenceNumber.isNullOrBlank()) {
                referenceNumberText = result.referenceNumber
            }
            if (result.collectionDueDate != null) {
                isReminderEnabled = true
                calculatedDueDate = result.collectionDueDate
            }
            voiceStatusText = "Parsed via ${result.parsedSource}"
        }
    }

    val paymentModes = listOf("Cash", "UPI", "Bank Transfer", "Cheque", "Card", "Other")

    val isGot = currentType == Transaction.TYPE_YOU_GOT
    val semanticColor = if (isGot) colors.credit else colors.debit

    val isRefFieldVisible = selectedPaymentMode == "UPI" || selectedPaymentMode == "Bank Transfer" || selectedPaymentMode == "Cheque"
    val refFieldLabel = when (selectedPaymentMode) {
        "UPI", "Bank Transfer" -> "Transaction Reference / UTR Number"
        "Cheque" -> "Cheque Number"
        else -> "Reference Number"
    }

    // Auto focus amount input on launch if new
    LaunchedEffect(Unit) {
        if (editingTransaction == null) {
            try {
                focusRequester.requestFocus()
            } catch (e: Exception) {
                // ignore
            }
        }
    }

    // Calculate due date based on option
    LaunchedEffect(isReminderEnabled, reminderOption) {
        if (!isReminderEnabled) {
            calculatedDueDate = null
        } else if (editingTransaction?.collectionDueDate == null) {
            val cal = Calendar.getInstance()
            when (reminderOption) {
                "Tomorrow" -> cal.add(Calendar.DAY_OF_YEAR, 1)
                "Next Week" -> cal.add(Calendar.DAY_OF_YEAR, 7)
                "Next Month" -> cal.add(Calendar.MONTH, 1)
            }
            calculatedDueDate = cal.timeInMillis
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        shape = KhataTheme.shapes.sheetTop,
        containerColor = colors.bgSurface,
        scrimColor = colors.overlay,
        dragHandle = {
            Box(
                modifier = Modifier
                    .padding(vertical = 10.dp)
                    .size(width = 36.dp, height = 4.dp)
                    .clip(CircleShape)
                    .background(colors.textDisabled)
            )
        }
    ) {
        Column(
            modifier = modifier
                .fillMaxWidth()
                .padding(horizontal = KhataTheme.spacing.md)
                .navigationBarsPadding()
        ) {
            // Entry Mode Toggle (only if adding)
            if (editingTransaction == null) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
                    horizontalArrangement = Arrangement.Center
                ) {
                    Row(
                        modifier = Modifier
                            .clip(KhataTheme.shapes.sm)
                            .background(colors.divider)
                            .padding(2.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .clip(KhataTheme.shapes.sm)
                                .background(if (entryMode == "FOR_CONTACT") colors.bgSurface else colors.divider)
                                .clickable { entryMode = "FOR_CONTACT" }
                                .padding(horizontal = 16.dp, vertical = 8.dp)
                        ) {
                            Text(
                                text = "For Contact",
                                style = LabelStyle,
                                color = if (entryMode == "FOR_CONTACT") colors.textPrimary else colors.textSecondary
                            )
                        }
                        Box(
                            modifier = Modifier
                                .clip(KhataTheme.shapes.sm)
                                .background(if (entryMode == "QUICK_ENTRY") colors.bgSurface else colors.divider)
                                .clickable { entryMode = "QUICK_ENTRY" }
                                .padding(horizontal = 16.dp, vertical = 8.dp)
                        ) {
                            Text(
                                text = "Quick Entry",
                                style = LabelStyle,
                                color = if (entryMode == "QUICK_ENTRY") colors.textPrimary else colors.textSecondary
                            )
                        }
                    }
                }
                
                if (entryMode == "FOR_CONTACT") {
                    var expanded by remember { mutableStateOf(false) }
                    Box(modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)) {
                        Text(
                            text = "Contact: " + (contactsList.find { it.id == selectedContactId }?.name ?: "Select Contact"),
                            style = TitleStyle,
                            color = colors.textPrimary,
                            modifier = Modifier.clickable { expanded = true }.padding(vertical = 8.dp)
                        )
                        DropdownMenu(
                            expanded = expanded,
                            onDismissRequest = { expanded = false }
                        ) {
                            contactsList.forEach { contact ->
                                DropdownMenuItem(
                                    text = { Text(contact.name) },
                                    onClick = {
                                        selectedContactId = contact.id
                                        expanded = false
                                    }
                                )
                            }
                        }
                    }
                }
            }

            // Header Row: Title & Direction Switcher
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (editingTransaction != null) "Edit Transaction" else if (entryMode == "FOR_CONTACT") "Entry for Contact" else "Quick Entry",
                    style = TitleStyle,
                    color = colors.textPrimary
                )
                Row(
                    modifier = Modifier
                        .clip(KhataTheme.shapes.sm)
                        .background(colors.divider)
                        .padding(2.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .clip(KhataTheme.shapes.sm)
                            .background(if (!isGot) colors.debit else colors.divider)
                            .clickable { currentType = Transaction.TYPE_YOU_GAVE }
                            .padding(horizontal = 10.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = "GAVE",
                            style = LabelStyle.copy(fontSize = 12.sp),
                            color = if (!isGot) colors.bgSurface else colors.textSecondary
                        )
                    }
                    Box(
                        modifier = Modifier
                            .clip(KhataTheme.shapes.sm)
                            .background(if (isGot) colors.credit else colors.divider)
                            .clickable { currentType = Transaction.TYPE_YOU_GOT }
                            .padding(horizontal = 10.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = "GOT",
                            style = LabelStyle.copy(fontSize = 12.sp),
                            color = if (isGot) colors.bgSurface else colors.textSecondary
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(KhataTheme.spacing.md))

            // DOMINANT AMOUNT INPUT WITH INTELLIGENT MIC ENTRY
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp),
                contentAlignment = Alignment.Center
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = "$activeSymbol ",
                        style = DisplayStyle.copy(
                            fontSize = 44.sp,
                            color = colors.textSecondary,
                            fontWeight = FontWeight.Medium
                        )
                    )
                    BasicTextField(
                        value = amountText,
                        onValueChange = { input ->
                            if (input.isEmpty() || input.matches(Regex("^\\d*\\.?\\d{0,2}$"))) {
                                amountText = input
                            }
                        },
                        textStyle = DisplayStyle.copy(
                            fontSize = 44.sp,
                            color = semanticColor,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Start
                        ),
                        cursorBrush = SolidColor(semanticColor),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier
                            .width(180.dp)
                            .focusRequester(focusRequester)
                            .testTag("amount_input")
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        IconButton(
                            onClick = {
                                if (isListeningVoice) {
                                    isListeningVoice = false
                                    activeGuidedField = null
                                    speechManager.stopListening()
                                } else {
                                    isListeningVoice = true
                                    if (voiceMode == "GUIDED") {
                                        activeGuidedField = "AMOUNT"
                                        voiceStatusText = "Listening for amount..."
                                    } else {
                                        voiceStatusText = "Listening..."
                                    }
                                    speechManager.startListening(
                                        onResult = { text ->
                                            if (voiceMode == "GUIDED") {
                                                when (activeGuidedField) {
                                                    "AMOUNT" -> {
                                                        val amt = text.replace(Regex("[^0-9.]"), "")
                                                        if (amt.isNotBlank()) amountText = amt
                                                        activeGuidedField = "MODE"
                                                        voiceStatusText = "Listening for payment mode..."
                                                        // Keep listening (simulate chained prompts)
                                                        speechManager.startListening(
                                                            onResult = { modeText ->
                                                                if (modeText.contains("upi", true)) selectedPaymentMode = "UPI"
                                                                else if (modeText.contains("cash", true)) selectedPaymentMode = "Cash"
                                                                else if (modeText.contains("card", true)) selectedPaymentMode = "Card"
                                                                
                                                                isListeningVoice = false
                                                                activeGuidedField = null
                                                                voiceStatusText = null
                                                            },
                                                            onError = { err ->
                                                                isListeningVoice = false
                                                                activeGuidedField = null
                                                                voiceStatusText = err
                                                            }
                                                        )
                                                    }
                                                    else -> {
                                                        isListeningVoice = false
                                                        activeGuidedField = null
                                                        voiceStatusText = null
                                                    }
                                                }
                                            } else {
                                                isListeningVoice = false
                                                processVoiceOrTextInput(text)
                                            }
                                        },
                                        onError = { err ->
                                            isListeningVoice = false
                                            activeGuidedField = null
                                            voiceStatusText = err
                                        }
                                    )
                                }
                            },
                            modifier = Modifier.testTag("mic_entry_button")
                        ) {
                            Icon(
                                imageVector = if (isListeningVoice) Icons.Default.MicOff else Icons.Default.Mic,
                                contentDescription = "Voice Entry",
                                tint = if (isListeningVoice) colors.textPrimary else colors.textSecondary
                            )
                        }
                        Text(
                            text = if (voiceMode == "FREEFORM") "Freeform" else "Guided",
                            style = CaptionStyle.copy(fontSize = 10.sp),
                            color = colors.textSecondary,
                            modifier = Modifier.clickable { 
                                voiceMode = if (voiceMode == "FREEFORM") "GUIDED" else "FREEFORM" 
                            }.padding(horizontal = 4.dp, vertical = 2.dp)
                        )
                    }
                }
            }

            // Minimal Listening Waveform / Voice Status Indicator in text-secondary gray only
            if (isListeningVoice || voiceStatusText != null) {
                Text(
                    text = if (isListeningVoice) "••• Listening for speech (e.g., 'Gave Rahul 500 UPI')..." else voiceStatusText.orEmpty(),
                    style = CaptionStyle,
                    color = colors.textSecondary,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth().padding(bottom = 6.dp)
                )
            }

            HorizontalDivider(color = colors.divider, thickness = 1.dp)

            Spacer(modifier = Modifier.height(KhataTheme.spacing.md))

            // Date & Time pickers row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Date picker trigger
                Row(
                    modifier = Modifier
                        .clickable {
                            val now = Calendar.getInstance().apply { timeInMillis = selectedDateMillis }
                            DatePickerDialog(
                                context,
                                { _, year, month, dayOfMonth ->
                                    val sel = Calendar.getInstance().apply {
                                        set(year, month, dayOfMonth)
                                    }
                                    selectedDateMillis = sel.timeInMillis
                                },
                                now.get(Calendar.YEAR),
                                now.get(Calendar.MONTH),
                                now.get(Calendar.DAY_OF_MONTH)
                            ).show()
                        }
                        .padding(vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.CalendarToday,
                        contentDescription = "Date",
                        tint = colors.textSecondary,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = DateTimeUtils.formatDateHeader(selectedDateMillis),
                        style = BodyStyle.copy(fontSize = 14.sp),
                        color = colors.textPrimary
                    )
                }

                // Time picker trigger
                Row(
                    modifier = Modifier
                        .clickable {
                            val now = Calendar.getInstance()
                            TimePickerDialog(
                                context,
                                { _, hourOfDay, minute ->
                                    val formattedMinute = if (minute < 10) "0$minute" else "$minute"
                                    val amPm = if (hourOfDay >= 12) "PM" else "AM"
                                    val hour12 = if (hourOfDay % 12 == 0) 12 else hourOfDay % 12
                                    selectedTimeString = "$hour12:$formattedMinute $amPm"
                                },
                                now.get(Calendar.HOUR_OF_DAY),
                                now.get(Calendar.MINUTE),
                                false
                            ).show()
                        }
                        .padding(vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.AccessTime,
                        contentDescription = "Time",
                        tint = colors.textSecondary,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = selectedTimeString,
                        style = BodyStyle.copy(fontSize = 14.sp),
                        color = colors.textPrimary
                    )
                }
            }

            Spacer(modifier = Modifier.height(KhataTheme.spacing.md))

            // Payment Mode Chip Row
            Text(
                text = "Payment Mode",
                style = CaptionStyle,
                color = colors.textSecondary
            )
            Spacer(modifier = Modifier.height(6.dp))
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(paymentModes) { mode ->
                    SemanticChip(
                        text = mode,
                        isSelected = selectedPaymentMode == mode,
                        activeSurfaceColor = semanticColor,
                        activeTextColor = colors.bgSurface,
                        onClick = { selectedPaymentMode = mode },
                        testTag = "payment_mode_$mode"
                    )
                }
            }

            // Conditional Payment Reference / UTR / Cheque field
            AnimatedVisibility(visible = isRefFieldVisible) {
                Column(modifier = Modifier.fillMaxWidth().padding(top = 12.dp)) {
                    Text(
                        text = refFieldLabel,
                        style = CaptionStyle,
                        color = colors.textSecondary
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    BasicTextField(
                        value = referenceNumberText,
                        onValueChange = { referenceNumberText = it },
                        textStyle = BodyStyle.copy(
                            color = colors.textPrimary,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Medium
                        ),
                        cursorBrush = SolidColor(colors.textPrimary),
                        modifier = Modifier.fillMaxWidth(),
                        decorationBox = { inner ->
                            Box {
                                if (referenceNumberText.isEmpty()) {
                                    Text(
                                        text = "e.g., UTR129048123",
                                        style = BodyStyle.copy(color = colors.textDisabled, fontFamily = FontFamily.Monospace)
                                    )
                                }
                                inner()
                            }
                        }
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    HorizontalDivider(color = colors.divider, thickness = 1.dp)
                }
            }

            Spacer(modifier = Modifier.height(KhataTheme.spacing.md))

            // Note Field + Photo Icon
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                BasicTextField(
                    value = noteText,
                    onValueChange = { noteText = it },
                    textStyle = BodyStyle.copy(color = colors.textPrimary),
                    cursorBrush = SolidColor(colors.textPrimary),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("note_input"),
                    decorationBox = { innerTextField ->
                        Box {
                            if (noteText.isEmpty()) {
                                Text(
                                    text = "Add note (e.g., Grocery split, Bill payment)",
                                    style = BodyStyle.copy(color = colors.textDisabled)
                                )
                            }
                            innerTextField()
                        }
                    }
                )
                if (attachmentPhotoUri != null) {
                    Box(modifier = Modifier.size(40.dp)) {
                        AsyncImage(
                            model = attachmentPhotoUri,
                            contentDescription = "Attached photo",
                            modifier = Modifier.fillMaxSize().clip(KhataTheme.shapes.sm)
                        )
                        IconButton(
                            onClick = { attachmentPhotoUri = null },
                            modifier = Modifier.align(Alignment.TopEnd).size(16.dp).background(colors.bgSurface, CircleShape)
                        ) {
                            Icon(Icons.Default.Close, contentDescription = "Remove photo", tint = colors.textPrimary, modifier = Modifier.size(12.dp))
                        }
                    }
                } else {
                    IconButton(onClick = { 
                        photoPickerLauncher.launch(androidx.activity.result.PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) 
                    }) {
                        Icon(
                            imageVector = Icons.Default.CameraAlt,
                            contentDescription = "Attach photo",
                            tint = colors.textSecondary
                        )
                    }
                }
            }

            HorizontalDivider(color = colors.divider, thickness = 1.dp)

            Spacer(modifier = Modifier.height(KhataTheme.spacing.sm))

            // Set Reminder Toggle
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Set Collection Due Reminder",
                    style = BodyStyle.copy(fontSize = 14.sp),
                    color = colors.textPrimary
                )
                Switch(
                    checked = isReminderEnabled,
                    onCheckedChange = { checked ->
                        if (checked && !hasNotificationPermission && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                            // Optionally set to true if you assume grant, or let user try again. Let's just set it.
                            isReminderEnabled = true
                        } else {
                            isReminderEnabled = checked
                        }
                    },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = colors.bgSurface,
                        checkedTrackColor = semanticColor,
                        uncheckedThumbColor = colors.textSecondary,
                        uncheckedTrackColor = colors.divider
                    ),
                    modifier = Modifier.testTag("reminder_switch")
                )
            }

            AnimatedVisibility(visible = isReminderEnabled) {
                Column {
                    Spacer(modifier = Modifier.height(6.dp))
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(listOf("Tomorrow", "Next Week", "Next Month")) { option ->
                            SemanticChip(
                                text = option,
                                isSelected = reminderOption == option,
                                activeSurfaceColor = semanticColor,
                                activeTextColor = colors.bgSurface,
                                onClick = { reminderOption = option }
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(KhataTheme.spacing.lg))

            // Dynamic Action Button
            val parsedAmount = amountText.toDoubleOrNull() ?: 0.0
            val isValid = parsedAmount > 0.0

            val buttonLabel = if (editingTransaction != null) {
                "SAVE CHANGES — $activeSymbol${amountText}"
            } else if (isGot) {
                "SAVE — YOU GOT ${if (parsedAmount > 0) "$activeSymbol${amountText}" else ""}"
            } else {
                "SAVE — YOU GAVE ${if (parsedAmount > 0) "$activeSymbol${amountText}" else ""}"
            }

            PrimaryButton(
                text = buttonLabel,
                onClick = {
                    if (isValid) {
                        onSave(
                            parsedAmount,
                            currentType,
                            selectedPaymentMode,
                            noteText.ifBlank { null },
                            calculatedDueDate,
                            referenceNumberText.ifBlank { null },
                            editingTransaction?.id,
                            attachmentPhotoUri,
                            if (entryMode == "FOR_CONTACT") selectedContactId else null
                        )
                    }
                },
                enabled = isValid,
                containerColor = semanticColor,
                contentColor = colors.bgSurface,
                modifier = Modifier.fillMaxWidth(),
                testTag = "save_transaction_button"
            )

            Spacer(modifier = Modifier.height(KhataTheme.spacing.md))
        }
    }
}
