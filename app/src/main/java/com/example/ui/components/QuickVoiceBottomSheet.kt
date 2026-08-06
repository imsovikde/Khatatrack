package com.example.ui.components

import android.content.Context
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
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Badge
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.SheetState
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.Contact
import com.example.data.model.Transaction
import com.example.ui.theme.BodyStyle
import com.example.ui.theme.CaptionStyle
import com.example.ui.theme.DisplayStyle
import com.example.ui.theme.KhataTheme
import com.example.ui.theme.LabelStyle
import com.example.ui.theme.TitleStyle
import com.example.util.CategoryTagExtractor
import com.example.util.CurrencyFormatter
import com.example.util.DateTimeUtils
import com.example.util.IntelligentParser
import com.example.util.SpeechRecognizerManager
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuickVoiceBottomSheet(
    contacts: List<Contact>,
    sheetState: SheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
    onDismiss: () -> Unit,
    onSaveTransaction: (
        contactId: Long,
        type: String,
        amount: Double,
        paymentMode: String,
        note: String,
        categoryTag: String,
        collectionDueDate: Long?,
        referenceNumber: String?
    ) -> Unit,
    onAddNewContactAndSave: (
        contactName: String,
        type: String,
        amount: Double,
        paymentMode: String,
        note: String,
        categoryTag: String,
        collectionDueDate: Long?,
        referenceNumber: String?
    ) -> Unit
) {
    val context = LocalContext.current
    val colors = KhataTheme.colors
    val coroutineScope = rememberCoroutineScope()

    val speechManager = remember { SpeechRecognizerManager(context) }
    var isListening by remember { mutableStateOf(false) }
    var rawInputText by remember { mutableStateOf("") }
    var parserSourceText by remember { mutableStateOf<String?>(null) }

    // Parsed Fields
    var selectedContact by remember { mutableStateOf<Contact?>(contacts.firstOrNull()) }
    var newContactNameInput by remember { mutableStateOf("") }
    var currentType by remember { mutableStateOf(Transaction.TYPE_YOU_GAVE) }
    var amountText by remember { mutableStateOf("") }
    var selectedPaymentMode by remember { mutableStateOf("Cash") }
    var noteText by remember { mutableStateOf("") }
    var categoryTagText by remember { mutableStateOf("General") }
    var referenceNumberText by remember { mutableStateOf("") }
    var collectionDueDate by remember { mutableStateOf<Long?>(null) }

    var errorMessage by remember { mutableStateOf<String?>(null) }
    var showContactDropdown by remember { mutableStateOf(false) }

    val paymentModes = listOf("Cash", "UPI", "Bank Transfer", "Cheque", "Card", "Other")

    fun triggerParsing(text: String) {
        if (text.isBlank()) return
        coroutineScope.launch {
            val res = IntelligentParser.parseInputText(context, text, contacts)
            if (res.amount != null) {
                amountText = if (res.amount % 1.0 == 0.0) res.amount.toLong().toString() else res.amount.toString()
            }
            if (res.intent.isNotBlank()) {
                currentType = res.intent
            }
            if (res.paymentMode.isNotBlank()) {
                selectedPaymentMode = res.paymentMode
            }
            if (!res.note.isNullOrBlank()) {
                noteText = res.note
                categoryTagText = CategoryTagExtractor.extractCategoryTag(res.note)
            }
            if (!res.referenceNumber.isNullOrBlank()) {
                referenceNumberText = res.referenceNumber
            }
            if (res.collectionDueDate != null) {
                collectionDueDate = res.collectionDueDate
            }
            if (!res.contactName.isNullOrBlank()) {
                val matched = contacts.firstOrNull { it.name.equals(res.contactName, ignoreCase = true) }
                if (matched != null) {
                    selectedContact = matched
                    newContactNameInput = ""
                } else {
                    selectedContact = null
                    newContactNameInput = res.contactName
                }
            }
            parserSourceText = "Parsed via ${res.parsedSource}"
        }
    }

    // Auto-start speech recognition on sheet opening
    LaunchedEffect(Unit) {
        isListening = true
        speechManager.startListening(
            onResult = { recognizedText ->
                isListening = false
                rawInputText = recognizedText
                triggerParsing(recognizedText)
            },
            onError = { err ->
                isListening = false
                parserSourceText = err
            }
        )
    }

    DisposableEffect(Unit) {
        onDispose {
            speechManager.stopListening()
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = colors.bgSurface,
        scrimColor = colors.textPrimary.copy(alpha = 0.4f),
        dragHandle = null
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(KhataTheme.spacing.lg)
                .verticalScroll(rememberScrollState())
        ) {
            // Header Bar
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.AutoAwesome,
                        contentDescription = "Voice Entry",
                        tint = colors.textPrimary,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Quick Voice Entry",
                        style = TitleStyle.copy(fontSize = 18.sp, fontWeight = FontWeight.Bold),
                        color = colors.textPrimary
                    )
                }
                IconButton(onClick = onDismiss, modifier = Modifier.testTag("close_voice_sheet")) {
                    Icon(imageVector = Icons.Default.Close, contentDescription = "Close", tint = colors.textSecondary)
                }
            }

            Spacer(modifier = Modifier.height(KhataTheme.spacing.md))

            // Speech Control Waveform Box
            Card(
                shape = KhataTheme.shapes.md,
                colors = CardDefaults.cardColors(
                    containerColor = if (isListening) colors.bgSurfaceElevated else colors.bgCanvas
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(
                        width = 1.dp,
                        color = if (isListening) colors.textPrimary else colors.divider,
                        shape = KhataTheme.shapes.md
                    )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(KhataTheme.spacing.md),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        modifier = Modifier
                            .size(56.dp)
                            .clip(CircleShape)
                            .background(if (isListening) colors.textPrimary else colors.divider)
                            .clickable {
                                if (isListening) {
                                    isListening = false
                                    speechManager.stopListening()
                                } else {
                                    isListening = true
                                    speechManager.startListening(
                                        onResult = { text ->
                                            isListening = false
                                            rawInputText = text
                                            triggerParsing(text)
                                        },
                                        onError = { err ->
                                            isListening = false
                                            parserSourceText = err
                                        }
                                    )
                                }
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (isListening) Icons.Default.MicOff else Icons.Default.Mic,
                            contentDescription = "Tap to speak",
                            tint = if (isListening) colors.bgSurface else colors.textPrimary,
                            modifier = Modifier.size(28.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = if (isListening) "Listening... Speak now!" else "Tap microphone to speak or edit text below",
                        style = LabelStyle,
                        color = if (isListening) colors.textPrimary else colors.textSecondary,
                        textAlign = TextAlign.Center
                    )

                    if (!parserSourceText.isNullOrBlank()) {
                        Text(
                            text = parserSourceText.orEmpty(),
                            style = CaptionStyle,
                            color = colors.textSecondary,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(KhataTheme.spacing.md))

            // Spoken/Typed Text Input Area
            OutlinedTextField(
                value = rawInputText,
                onValueChange = { text ->
                    rawInputText = text
                    triggerParsing(text)
                },
                label = { Text("Spoken or Typed Sentence", style = CaptionStyle) },
                placeholder = { Text("e.g., 'Gave Rahul 500 via UPI for groceries due tomorrow'", style = CaptionStyle) },
                textStyle = BodyStyle.copy(color = colors.textPrimary),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = colors.textPrimary,
                    unfocusedBorderColor = colors.divider,
                    focusedContainerColor = colors.bgSurface,
                    unfocusedContainerColor = colors.bgSurface
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("voice_raw_input")
            )

            Spacer(modifier = Modifier.height(KhataTheme.spacing.md))
            HorizontalDivider(color = colors.divider)
            Spacer(modifier = Modifier.height(KhataTheme.spacing.md))

            // PARSED PREVIEW FIELDS

            // 1. Transaction Type Toggle (YOU GAVE / YOU GOT)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(KhataTheme.shapes.sm)
                    .background(colors.bgCanvas)
                    .padding(4.dp),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                val isGave = currentType == Transaction.TYPE_YOU_GAVE
                val isGot = currentType == Transaction.TYPE_YOU_GOT

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(KhataTheme.shapes.sm)
                        .background(if (isGave) colors.debit else colors.bgCanvas)
                        .clickable { currentType = Transaction.TYPE_YOU_GAVE }
                        .padding(vertical = 10.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "YOU GAVE (Debit)",
                        style = LabelStyle.copy(fontWeight = FontWeight.Bold),
                        color = if (isGave) colors.bgSurface else colors.textSecondary
                    )
                }

                Spacer(modifier = Modifier.width(4.dp))

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(KhataTheme.shapes.sm)
                        .background(if (isGot) colors.credit else colors.bgCanvas)
                        .clickable { currentType = Transaction.TYPE_YOU_GOT }
                        .padding(vertical = 10.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "YOU GOT (Credit)",
                        style = LabelStyle.copy(fontWeight = FontWeight.Bold),
                        color = if (isGot) colors.bgSurface else colors.textSecondary
                    )
                }
            }

            Spacer(modifier = Modifier.height(KhataTheme.spacing.md))

            // 2. Amount Input
            OutlinedTextField(
                value = amountText,
                onValueChange = { input ->
                    if (input.isEmpty() || input.matches(Regex("^\\d*\\.?\\d{0,2}$"))) {
                        amountText = input
                    }
                },
                label = { Text("Amount (₹)", style = CaptionStyle) },
                textStyle = DisplayStyle.copy(fontSize = 28.sp, color = if (currentType == Transaction.TYPE_YOU_GAVE) colors.debit else colors.credit),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = colors.textPrimary,
                    unfocusedBorderColor = colors.divider
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("parsed_amount_input")
            )

            Spacer(modifier = Modifier.height(KhataTheme.spacing.md))

            // 3. Contact Picker / Creator
            Box(modifier = Modifier.fillMaxWidth()) {
                OutlinedTextField(
                    value = selectedContact?.name ?: newContactNameInput,
                    onValueChange = { name ->
                        selectedContact = null
                        newContactNameInput = name
                    },
                    label = { Text("Customer / Contact Name", style = CaptionStyle) },
                    trailingIcon = {
                        IconButton(onClick = { showContactDropdown = !showContactDropdown }) {
                            Icon(imageVector = Icons.Default.Person, contentDescription = "Select Contact")
                        }
                    },
                    textStyle = BodyStyle.copy(color = colors.textPrimary),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = colors.textPrimary,
                        unfocusedBorderColor = colors.divider
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("parsed_contact_input")
                )

                DropdownMenu(
                    expanded = showContactDropdown,
                    onDismissRequest = { showContactDropdown = false },
                    modifier = Modifier.fillMaxWidth(0.9f)
                ) {
                    contacts.forEach { contact ->
                        DropdownMenuItem(
                            text = { Text(contact.name, style = BodyStyle, color = colors.textPrimary) },
                            onClick = {
                                selectedContact = contact
                                newContactNameInput = ""
                                showContactDropdown = false
                            }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(KhataTheme.spacing.md))

            // 4. Payment Mode Chips
            Text(text = "Payment Mode", style = CaptionStyle, color = colors.textSecondary)
            Spacer(modifier = Modifier.height(4.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                paymentModes.forEach { mode ->
                    val isSel = selectedPaymentMode == mode
                    SemanticChip(
                        text = mode,
                        isSelected = isSel,
                        onClick = { selectedPaymentMode = mode }
                    )
                }
            }

            Spacer(modifier = Modifier.height(KhataTheme.spacing.md))

            // 5. Note & Category Tag
            OutlinedTextField(
                value = noteText,
                onValueChange = { text ->
                    noteText = text
                    categoryTagText = CategoryTagExtractor.extractCategoryTag(text)
                },
                label = { Text("Note / Description", style = CaptionStyle) },
                textStyle = BodyStyle.copy(color = colors.textPrimary),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = colors.textPrimary,
                    unfocusedBorderColor = colors.divider
                ),
                modifier = Modifier.fillMaxWidth()
            )

            if (categoryTagText.isNotBlank()) {
                Text(
                    text = "Category Tag: #$categoryTagText",
                    style = CaptionStyle,
                    color = colors.textSecondary,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }

            Spacer(modifier = Modifier.height(KhataTheme.spacing.md))

            // Collection Due Date Badge if present
            if (collectionDueDate != null) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(colors.bgCanvas, shape = KhataTheme.shapes.sm)
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Collection Due: ${DateTimeUtils.formatDate(collectionDueDate!!)}",
                        style = LabelStyle,
                        color = colors.textPrimary
                    )
                    IconButton(onClick = { collectionDueDate = null }, modifier = Modifier.size(24.dp)) {
                        Icon(imageVector = Icons.Default.Close, contentDescription = "Clear Due Date", tint = colors.textSecondary)
                    }
                }
                Spacer(modifier = Modifier.height(KhataTheme.spacing.md))
            }

            if (errorMessage != null) {
                Text(
                    text = errorMessage.orEmpty(),
                    style = CaptionStyle,
                    color = colors.debit,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
            }

            // Save Action Button
            PrimaryButton(
                text = "Save Transaction",
                onClick = {
                    val amt = amountText.toDoubleOrNull()
                    if (amt == null || amt <= 0.0) {
                        errorMessage = "Please enter a valid amount"
                        return@PrimaryButton
                    }
                    val targetContact = selectedContact
                    val targetName = newContactNameInput.trim()

                    if (targetContact == null && targetName.isBlank()) {
                        errorMessage = "Please select or enter a contact name"
                        return@PrimaryButton
                    }

                    if (targetContact != null) {
                        onSaveTransaction(
                            targetContact.id,
                            currentType,
                            amt,
                            selectedPaymentMode,
                            noteText,
                            categoryTagText,
                            collectionDueDate,
                            referenceNumberText.ifBlank { null }
                        )
                    } else {
                        onAddNewContactAndSave(
                            targetName,
                            currentType,
                            amt,
                            selectedPaymentMode,
                            noteText,
                            categoryTagText,
                            collectionDueDate,
                            referenceNumberText.ifBlank { null }
                        )
                    }
                    onDismiss()
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("save_voice_transaction_button")
            )
        }
    }
}
