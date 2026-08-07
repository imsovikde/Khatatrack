package com.example.ui.components

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AttachFile
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SheetState
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.IncomeExpenseEntry
import com.example.ui.theme.BodyStyle
import com.example.ui.theme.CaptionStyle
import com.example.ui.theme.KhataTheme
import com.example.ui.theme.TitleStyle
import com.example.util.CategoryTagExtractor
import com.example.util.CurrencyFormatter
import com.example.util.CurrencyManager
import com.example.util.IntelligentParser
import com.example.util.SpeechRecognizerManager
import kotlinx.coroutines.launch
import android.Manifest
import android.os.Build
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddIncomeExpenseBottomSheet(
    isOpen: Boolean,
    onDismiss: () -> Unit,
    onSave: (IncomeExpenseEntry) -> Unit,
    editingEntry: IncomeExpenseEntry? = null,
    sheetState: SheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
) {
    if (!isOpen) return

    val context = LocalContext.current
    val colors = KhataTheme.colors
    val coroutineScope = rememberCoroutineScope()

    var type by remember(editingEntry) { mutableStateOf(editingEntry?.type ?: IncomeExpenseEntry.TYPE_EXPENSE) }
    var amountText by remember(editingEntry) { mutableStateOf(editingEntry?.amount?.let { if (it == 0.0) "" else it.toString() } ?: "") }
    var selectedCategory by remember(editingEntry) { mutableStateOf(editingEntry?.categoryTag ?: "General") }
    var selectedPaymentMode by remember(editingEntry) { mutableStateOf(editingEntry?.paymentMode ?: "Cash") }
    var referenceNumber by remember(editingEntry) { mutableStateOf(editingEntry?.transactionRefId ?: "") }
    var note by remember(editingEntry) { mutableStateOf(editingEntry?.note ?: "") }
    var attachmentUri by remember(editingEntry) { mutableStateOf(editingEntry?.attachmentPhoto) }
    var hasReminder by remember(editingEntry) { mutableStateOf(editingEntry?.collectionDueDate != null) }

    val calendar = remember { Calendar.getInstance() }
    var selectedTimestamp by remember(editingEntry) { mutableStateOf(editingEntry?.transactionDate ?: System.currentTimeMillis()) }
    var selectedTimeText by remember(editingEntry) {
        val initial = editingEntry?.transactionTime
        val initialText = if (!initial.isNull_or_blank_compat()) initial!!
        else SimpleDateFormat("hh:mm a", Locale.getDefault()).format(Date())
        mutableStateOf(initialText)
    }

    var voiceMode by remember { mutableStateOf("FREEFORM") }
    var isListeningVoice by remember { mutableStateOf(false) }
    var voiceStatusText by remember { mutableStateOf<String?>(null) }
    var activeGuidedField by remember { mutableStateOf<String?>(null) }

    val speechManager = remember { SpeechRecognizerManager(context) }

    var dynamicCategories by remember { mutableStateOf(listOf("Salary", "Freelance", "Groceries", "Rent", "Utilities", "Dining", "Shopping", "Entertainment", "Health", "General")) }
    val paymentModes = listOf("Cash", "UPI", "Bank Transfer", "Card", "Cheque", "Other")

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
            hasReminder = true
        }
    }

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) {
            attachmentUri = uri.toString()
        }
    }

    val isReferenceRequired = selectedPaymentMode == "UPI" || selectedPaymentMode == "Bank Transfer" || selectedPaymentMode == "Card"

    val datePickerDialog = remember(context) {
        val cal = Calendar.getInstance().apply { timeInMillis = selectedTimestamp }
        DatePickerDialog(
            context,
            { _, year, month, dayOfMonth ->
                val newCal = Calendar.getInstance().apply {
                    timeInMillis = selectedTimestamp
                    set(Calendar.YEAR, year)
                    set(Calendar.MONTH, month)
                    set(Calendar.DAY_OF_MONTH, dayOfMonth)
                }
                selectedTimestamp = newCal.timeInMillis
            },
            cal.get(Calendar.YEAR),
            cal.get(Calendar.MONTH),
            cal.get(Calendar.DAY_OF_MONTH)
        )
    }

    val timePickerDialog = remember(context) {
        val cal = Calendar.getInstance()
        TimePickerDialog(
            context,
            { _, hourOfDay, minute ->
                val timeCal = Calendar.getInstance().apply {
                    set(Calendar.HOUR_OF_DAY, hourOfDay)
                    set(Calendar.MINUTE, minute)
                }
                selectedTimeText = SimpleDateFormat("hh:mm a", Locale.getDefault()).format(timeCal.time)
            },
            cal.get(Calendar.HOUR_OF_DAY),
            cal.get(Calendar.MINUTE),
            false
        )
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = colors.bgSurface,
        dragHandle = {
            Box(
                modifier = Modifier
                    .padding(vertical = 10.dp)
                    .width(36.dp)
                    .height(4.dp)
                    .background(colors.divider, RoundedCornerShape(2.dp))
            )
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = KhataTheme.spacing.md)
                .verticalScroll(rememberScrollState())
                .padding(bottom = 32.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (editingEntry != null) "Edit Entry" else "Income & Expense Entry",
                    style = TitleStyle,
                    color = colors.textPrimary
                )
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.Close, contentDescription = "Close", tint = colors.textSecondary)
                }
            }

            Spacer(modifier = Modifier.height(KhataTheme.spacing.md))

            // Income / Expense Toggle
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .background(colors.bgCanvas, RoundedCornerShape(24.dp))
                    .padding(4.dp)
            ) {
                val isIncome = type == IncomeExpenseEntry.TYPE_INCOME
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(20.dp))
                        .background(if (isIncome) colors.credit else colors.bgCanvas)
                        .clickable { type = IncomeExpenseEntry.TYPE_INCOME }
                        .padding(vertical = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "INCOME",
                        style = BodyStyle.copy(fontWeight = FontWeight.Bold),
                        color = if (isIncome) colors.textPrimary else colors.textSecondary
                    )
                }
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(20.dp))
                        .background(if (!isIncome) colors.debit else colors.bgCanvas)
                        .clickable { type = IncomeExpenseEntry.TYPE_EXPENSE }
                        .padding(vertical = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "EXPENSE",
                        style = BodyStyle.copy(fontWeight = FontWeight.Bold),
                        color = if (!isIncome) colors.textPrimary else colors.textSecondary
                    )
                }
            }

            Spacer(modifier = Modifier.height(KhataTheme.spacing.md))

            // Amount Input with Mic Icon
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                val activeBorder = if (activeGuidedField == "AMOUNT") colors.credit else colors.divider
                OutlinedTextField(
                    value = amountText,
                    onValueChange = { amountText = it },
                    label = { Text("Amount (${CurrencyFormatter.getActiveCurrencySymbol()})") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier
                        .weight(1f)
                        .border(if (activeGuidedField == "AMOUNT") 2.dp else 0.dp, activeBorder, RoundedCornerShape(12.dp))
                        .testTag("entry_amount_input"),
                    shape = RoundedCornerShape(12.dp)
                )

                Spacer(modifier = Modifier.width(8.dp))

                // Mic button for Voice Entry
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
                                    voiceStatusText = "Speak Amount..."
                                } else {
                                    voiceStatusText = "Speak details..."
                                }
                                speechManager.startListening(
                                    onResult = { text ->
                                        if (voiceMode == "GUIDED") {
                                            val digits = text.replace(Regex("[^0-9.]"), "")
                                            if (digits.isNotBlank()) amountText = digits
                                            isListeningVoice = false
                                            activeGuidedField = null
                                            voiceStatusText = null
                                        } else {
                                            isListeningVoice = false
                                            coroutineScope.launch {
                                                val parsed = IntelligentParser.parseInputText(context, text, emptyList())
                                                if (parsed.amount != null) amountText = parsed.amount.toString()
                                                if (parsed.paymentMode.isNotBlank()) selectedPaymentMode = parsed.paymentMode
                                                if (!parsed.note.isNull_or_blank_compat()) {
                                                    note = parsed.note!!
                                                    val cat = CategoryTagExtractor.extractCategoryTag(note)
                                                    if (cat !in dynamicCategories) {
                                                        dynamicCategories = listOf(cat) + dynamicCategories
                                                    }
                                                    selectedCategory = cat
                                                }
                                            }
                                        }
                                    },
                                    onError = { err ->
                                        isListeningVoice = false
                                        activeGuidedField = null
                                        voiceStatusText = err
                                    }
                                )
                            }
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Default.Mic,
                            contentDescription = "Voice Entry",
                            tint = if (isListeningVoice) colors.credit else colors.textSecondary
                        )
                    }
                    Text(
                        text = if (voiceMode == "FREEFORM") "Freeform" else "Guided",
                        style = CaptionStyle.copy(fontSize = 10.sp),
                        color = colors.textSecondary,
                        modifier = Modifier.clickable {
                            voiceMode = if (voiceMode == "FREEFORM") "GUIDED" else "FREEFORM"
                        }
                    )
                }
            }

            voiceStatusText?.let { status ->
                Text(
                    text = status,
                    style = CaptionStyle,
                    color = colors.credit,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }

            Spacer(modifier = Modifier.height(KhataTheme.spacing.md))

            // Date & Time pickers
            Row(modifier = Modifier.fillMaxWidth()) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .background(colors.bgCanvas, RoundedCornerShape(12.dp))
                        .clickable { datePickerDialog.show() }
                        .padding(12.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.CalendarToday, contentDescription = "Date", tint = colors.textSecondary, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(Date(selectedTimestamp)),
                            style = BodyStyle,
                            color = colors.textPrimary
                        )
                    }
                }

                Spacer(modifier = Modifier.width(8.dp))

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .background(colors.bgCanvas, RoundedCornerShape(12.dp))
                        .clickable { timePickerDialog.show() }
                        .padding(12.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Schedule, contentDescription = "Time", tint = colors.textSecondary, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = selectedTimeText,
                            style = BodyStyle,
                            color = colors.textPrimary
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(KhataTheme.spacing.md))

            // Payment Mode chips
            Text("Payment Mode", style = CaptionStyle, color = colors.textSecondary)
            Spacer(modifier = Modifier.height(4.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                paymentModes.forEach { mode ->
                    val isSel = selectedPaymentMode == mode
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(16.dp))
                            .background(if (isSel) colors.textPrimary else colors.bgCanvas)
                            .clickable { selectedPaymentMode = mode }
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = mode,
                            style = CaptionStyle,
                            color = if (isSel) colors.bgSurface else colors.textSecondary
                        )
                    }
                }
            }

            // Conditional Reference / UTR field
            AnimatedVisibility(visible = isReferenceRequired) {
                Column {
                    Spacer(modifier = Modifier.height(KhataTheme.spacing.md))
                    OutlinedTextField(
                        value = referenceNumber,
                        onValueChange = { referenceNumber = it },
                        label = { Text("Reference / UTR Number *") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("entry_ref_input"),
                        shape = RoundedCornerShape(12.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(KhataTheme.spacing.md))

            // Category Chips
            Text("Category", style = CaptionStyle, color = colors.textSecondary)
            Spacer(modifier = Modifier.height(4.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = selectedCategory,
                    onValueChange = { 
                        selectedCategory = it
                        if (it.isNotBlank() && it !in dynamicCategories) {
                            dynamicCategories = listOf(it) + dynamicCategories
                        }
                    },
                    label = { Text("Or Type New Category") },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp)
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            LazyRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                items(dynamicCategories) { cat ->
                    val isSel = selectedCategory == cat
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(16.dp))
                            .background(if (isSel) colors.credit else colors.bgCanvas)
                            .clickable { selectedCategory = cat }
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = cat,
                            style = CaptionStyle,
                            color = if (isSel) colors.textPrimary else colors.textSecondary
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(KhataTheme.spacing.md))

            // Note
            OutlinedTextField(
                value = note,
                onValueChange = { note = it },
                label = { Text("Note / Description") },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            )

            Spacer(modifier = Modifier.height(KhataTheme.spacing.md))

            // Attachment & Reminder controls
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(colors.bgCanvas)
                        .clickable { photoPickerLauncher.launch("image/*") }
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.AttachFile, contentDescription = "Attach Image", tint = colors.textSecondary, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (attachmentUri != null) "Photo Attached ✓" else "Attach Bill Photo",
                        style = CaptionStyle,
                        color = if (attachmentUri != null) colors.credit else colors.textSecondary
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Set Reminder", style = CaptionStyle, color = colors.textSecondary)
                    Spacer(modifier = Modifier.width(8.dp))
                    Switch(
                        checked = hasReminder,
                        onCheckedChange = { checked ->
                            if (checked && !hasNotificationPermission && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                            } else {
                                hasReminder = checked
                            }
                        }
                    )
                }
            }

            Spacer(modifier = Modifier.height(KhataTheme.spacing.lg))

            // Save Button
            val amountVal = amountText.toDoubleOrNull()
            val isValid = amountVal != null && amountVal > 0 && (!isReferenceRequired || referenceNumber.isNotBlank())

            PrimaryButton(
                text = if (editingEntry != null) "UPDATE ENTRY" else "SAVE ENTRY",
                onClick = {
                    if (isValid && amountVal != null) {
                        val entry = IncomeExpenseEntry(
                            id = editingEntry?.id ?: 0,
                            type = type,
                            amount = amountVal,
                            currency = CurrencyFormatter.getActiveCurrencyCode(),
                            transactionDate = selectedTimestamp,
                            transactionTime = selectedTimeText,
                            paymentMode = selectedPaymentMode,
                            transactionRefId = referenceNumber.ifBlank { null },
                            categoryTag = selectedCategory,
                            note = note.ifBlank { null },
                            attachmentPhoto = attachmentUri,
                            collectionDueDate = if (hasReminder) selectedTimestamp + (7 * 86400000L) else null
                        )
                        onSave(entry)
                        onDismiss()
                    }
                },
                enabled = isValid,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("save_entry_button")
            )
        }
    }
}

private fun String?.isNull_or_blank_compat(): Boolean = this == null || this.isBlank()
