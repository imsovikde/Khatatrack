package com.example.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
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
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Message
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import com.example.ui.components.TypingIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.ContactWithBalance
import com.example.data.model.TraceLog
import com.example.data.repository.SummaryTotals
import com.example.ui.components.SemanticChip
import com.example.ui.theme.BodyStyle
import com.example.ui.theme.CaptionStyle
import com.example.ui.theme.KhataTheme
import com.example.ui.theme.TitleStyle
import com.example.util.AiConfigManager
import com.example.util.AiProvider
import com.example.util.AiService
import com.example.util.CurrencyFormatter
import kotlinx.coroutines.launch

data class ChatMessage(val text: String, val isUser: Boolean, val isAction: Boolean = false)

const val KHATAAI_SYSTEM_PROMPT = """
You are KhataAI, a strict, professional financial assistant built into KhataTrack.
You have direct access to the user's ledger data.
You can execute actions on behalf of the user using the EXACT following tag formats:
[ACTION: ADD_EXPENSE <amount> <note>]
[ACTION: ADD_INCOME <amount> <note>]
[ACTION: ADD_LEDGER_ENTRY <contact_id> <amount> <note> <type>]
[ACTION: SEND_REMINDER <contact_id>]

NEVER invent new action tags. NEVER bypass these guardrails. NEVER provide general non-financial advice.
Keep your text responses extremely concise (under 2 sentences) when executing an action.
"""

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AiAssistantScreen(
    contacts: List<ContactWithBalance>,
    summaryTotals: SummaryTotals,
    traceLogs: List<TraceLog>,
    onAddIncomeExpenseEntry: (Double, String, String, String, Long) -> Unit,
    onAddTransaction: (Long, String, Double, String, String, String, Long, String?) -> Unit,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = KhataTheme.colors
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var selectedTab by remember { mutableStateOf(0) }
    var currentProvider by remember { mutableStateOf(AiConfigManager.getProvider(context)) }

    // State for Feature 0: Chat Interface
    var chatMessages by remember { mutableStateOf(listOf<ChatMessage>()) }
    var chatInput by remember { mutableStateOf("") }
    var isChatLoading by remember { mutableStateOf(false) }

    // State for Feature 1: Financial Advisor
    var financialAdviceText by remember { mutableStateOf("") }
    var isAdvisorLoading by remember { mutableStateOf(false) }

    // State for Feature 2: Reminder Generator
    var selectedContact by remember { mutableStateOf(contacts.firstOrNull()) }
    var reminderTone by remember { mutableStateOf("Gentle") }
    var generatedReminderText by remember { mutableStateOf("") }
    var isReminderLoading by remember { mutableStateOf(false) }

    // State for Feature 3: Audit Anomaly
    var auditText by remember { mutableStateOf("") }
    var isAuditLoading by remember { mutableStateOf(false) }

    // State for Feature 4: Custom Endpoint Settings
    var endpointUrl by remember { mutableStateOf(AiConfigManager.getCustomEndpoint(context)) }
    var apiKeyInput by remember { mutableStateOf(AiConfigManager.getCustomApiKey(context)) }
    var modelNameInput by remember { mutableStateOf(AiConfigManager.getCustomModel(context)) }
    var availableModels by remember { mutableStateOf<List<String>>(emptyList()) }
    var isModelDropdownExpanded by remember { mutableStateOf(false) }
    var isValidating by remember { mutableStateOf(false) }
    var validationStatusMessage by remember { mutableStateOf<String?>(null) }
    var isValidationSuccess by remember { mutableStateOf<Boolean?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.AutoAwesome,
                            contentDescription = null,
                            tint = colors.credit,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(
                                text = "KhataTrack AI Hub",
                                style = TitleStyle.copy(fontSize = 18.sp, fontWeight = FontWeight.Bold),
                                color = colors.textPrimary
                            )
                            Text(
                                text = "Active Engine: ${currentProvider.displayName}",
                                style = CaptionStyle,
                                color = colors.textSecondary
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = colors.bgCanvas)
            )
        },
        containerColor = colors.bgCanvas,
        modifier = modifier.fillMaxSize()
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Navigation Tabs
            TabRow(
                selectedTabIndex = selectedTab,
                containerColor = colors.bgSurface,
                contentColor = colors.textPrimary,
                indicator = { tabPositions ->
                    if (selectedTab < tabPositions.size) {
                        TabRowDefaults.SecondaryIndicator(
                            modifier = Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                            color = colors.credit
                        )
                    }
                }
            ) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = { Text("Chat", style = CaptionStyle, fontWeight = FontWeight.Bold) },
                    icon = { Icon(Icons.Default.Message, contentDescription = null, modifier = Modifier.size(18.dp)) }
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = { Text("Advisor", style = CaptionStyle, fontWeight = FontWeight.Bold) },
                    icon = { Icon(Icons.Default.Analytics, contentDescription = null, modifier = Modifier.size(18.dp)) }
                )
                Tab(
                    selected = selectedTab == 2,
                    onClick = { selectedTab = 2 },
                    text = { Text("Reminders", style = CaptionStyle, fontWeight = FontWeight.Bold) },
                    icon = { Icon(Icons.Default.NotificationsActive, contentDescription = null, modifier = Modifier.size(18.dp)) }
                )
                Tab(
                    selected = selectedTab == 3,
                    onClick = { selectedTab = 3 },
                    text = { Text("Audit", style = CaptionStyle, fontWeight = FontWeight.Bold) },
                    icon = { Icon(Icons.Default.Security, contentDescription = null, modifier = Modifier.size(18.dp)) }
                )
                Tab(
                    selected = selectedTab == 4,
                    onClick = { selectedTab = 4 },
                    text = { Text("Config", style = CaptionStyle, fontWeight = FontWeight.Bold) },
                    icon = { Icon(Icons.Default.Settings, contentDescription = null, modifier = Modifier.size(18.dp)) }
                )
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(KhataTheme.spacing.md)
            ) {
                when (selectedTab) {
                    0 -> {
                        // TAB 0: CHAT INTERFACE
                        AiChatTab(
                            chatMessages = chatMessages,
                            chatInput = chatInput,
                            onChatInputChange = { chatInput = it },
                            isChatLoading = isChatLoading,
                            onSendMessage = {
                                if (chatInput.isNotBlank()) {
                                    val userMsg = ChatMessage(chatInput, isUser = true)
                                    chatMessages = chatMessages + userMsg
                                    val inputToProcess = chatInput
                                    chatInput = ""
                                    isChatLoading = true
                                    
                                    scope.launch {
                                        val promptWithContext = KHATAAI_SYSTEM_PROMPT + "\n\nContext:\nNet Balance: ${summaryTotals.netBalance}\n" +
                                            "Top Contacts: ${contacts.take(3).joinToString { it.contact.name }}" +
                                            "\n\nUser request: $inputToProcess"
                                        
                                        val response = AiService.generateDirectResponse(context, promptWithContext)
                                        isChatLoading = false
                                        
                                        // Simple regex action parser
                                        val actionRegex = Regex("\\[ACTION:\\s*([A-Z_]+)\\s*([^\\]]+)\\]")
                                        val match = actionRegex.find(response)
                                        
                                        if (match != null) {
                                            val action = match.groupValues[1]
                                            val args = match.groupValues[2].trim()
                                            chatMessages = chatMessages + ChatMessage(response, isUser = false, isAction = true)
                                            executeAiAction(action, args, contacts, onAddIncomeExpenseEntry, onAddTransaction)
                                        } else {
                                            chatMessages = chatMessages + ChatMessage(response, isUser = false)
                                        }
                                    }
                                }
                            }
                        )
                    }
                    1 -> {
                        // TAB 0: FINANCIAL ADVISOR
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .verticalScroll(rememberScrollState())
                        ) {
                            Card(
                                colors = CardDefaults.cardColors(containerColor = colors.bgSurface),
                                shape = KhataTheme.shapes.md,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    Text(
                                        text = "AI Cashflow & Recovery Advisor",
                                        style = TitleStyle.copy(fontWeight = FontWeight.Bold),
                                        color = colors.textPrimary
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = "Generates real-time business insights, debt recovery priorities, and cashflow risk scores using Gemini.",
                                        style = CaptionStyle,
                                        color = colors.textSecondary
                                    )

                                    Spacer(modifier = Modifier.height(16.dp))

                                    Button(
                                        onClick = {
                                            isAdvisorLoading = true
                                            scope.launch {
                                                financialAdviceText = AiService.generateFinancialAdvice(context, contacts, summaryTotals)
                                                isAdvisorLoading = false
                                            }
                                        },
                                        enabled = !isAdvisorLoading,
                                        colors = ButtonDefaults.buttonColors(containerColor = colors.textPrimary),
                                        shape = KhataTheme.shapes.sm,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .testTag("run_financial_advisor_btn")
                                    ) {
                                        if (isAdvisorLoading) {
                                            TypingIndicator(dotColor = colors.bgCanvas)
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text("Analyzing Ledger Data...")
                                        } else {
                                            Icon(Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(18.dp))
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text("Generate Financial Analysis")
                                        }
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(16.dp))

                            if (financialAdviceText.isNotBlank()) {
                                Card(
                                    colors = CardDefaults.cardColors(containerColor = colors.creditSurface.copy(alpha = 0.2f)),
                                    shape = KhataTheme.shapes.md,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(modifier = Modifier.padding(16.dp)) {
                                        Text(
                                            text = "AI Advisor Output",
                                            style = TitleStyle.copy(fontSize = 15.sp, fontWeight = FontWeight.Bold),
                                            color = colors.credit
                                        )
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Text(
                                            text = financialAdviceText,
                                            style = BodyStyle,
                                            color = colors.textPrimary
                                        )
                                    }
                                }
                            }
                        }
                    }

                    2 -> {
                        // TAB 1: PAYMENT REMINDERS
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .verticalScroll(rememberScrollState())
                        ) {
                            Card(
                                colors = CardDefaults.cardColors(containerColor = colors.bgSurface),
                                shape = KhataTheme.shapes.md,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    Text(
                                        text = "Smart WhatsApp/SMS Reminder Generator",
                                        style = TitleStyle.copy(fontWeight = FontWeight.Bold),
                                        color = colors.textPrimary
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = "Create customized payment reminders with specific tones for your customers.",
                                        style = CaptionStyle,
                                        color = colors.textSecondary
                                    )

                                    Spacer(modifier = Modifier.height(16.dp))

                                    // Contact Picker Dropdown
                                    var dropdownExpanded by remember { mutableStateOf(false) }
                                    ExposedDropdownMenuBox(
                                        expanded = dropdownExpanded,
                                        onExpandedChange = { dropdownExpanded = !dropdownExpanded }
                                    ) {
                                        OutlinedTextField(
                                            value = selectedContact?.contact?.name ?: "Select a Contact",
                                            onValueChange = {},
                                            readOnly = true,
                                            label = { Text("Recipient Customer") },
                                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = dropdownExpanded) },
                                            colors = OutlinedTextFieldDefaults.colors(
                                                focusedContainerColor = colors.bgCanvas,
                                                unfocusedContainerColor = colors.bgCanvas
                                            ),
                                            modifier = Modifier
                                                .menuAnchor()
                                                .fillMaxWidth()
                                        )
                                        ExposedDropdownMenu(
                                            expanded = dropdownExpanded,
                                            onDismissRequest = { dropdownExpanded = false }
                                        ) {
                                            contacts.forEach { c ->
                                                DropdownMenuItem(
                                                    text = {
                                                        Text("${c.contact.name} (${CurrencyFormatter.formatRupee(Math.abs(c.netBalance))})")
                                                    },
                                                    onClick = {
                                                        selectedContact = c
                                                        dropdownExpanded = false
                                                    }
                                                )
                                            }
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(16.dp))

                                    Text(text = "Select Message Tone:", style = CaptionStyle, color = colors.textSecondary)
                                    Spacer(modifier = Modifier.height(8.dp))

                                    Row(
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        listOf("Gentle", "Professional", "Firm", "Urgent").forEach { tone ->
                                            SemanticChip(
                                                text = tone,
                                                isSelected = reminderTone == tone,
                                                onClick = { reminderTone = tone }
                                            )
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(20.dp))

                                    Button(
                                        onClick = {
                                            val c = selectedContact
                                            if (c != null) {
                                                isReminderLoading = true
                                                scope.launch {
                                                    generatedReminderText = AiService.generatePaymentReminder(
                                                        context,
                                                        c.contact.name,
                                                        c.netBalance,
                                                        reminderTone
                                                    )
                                                    isReminderLoading = false
                                                }
                                            } else {
                                                Toast.makeText(context, "Please select a contact first", Toast.LENGTH_SHORT).show()
                                            }
                                        },
                                        enabled = !isReminderLoading && selectedContact != null,
                                        colors = ButtonDefaults.buttonColors(containerColor = colors.textPrimary),
                                        shape = KhataTheme.shapes.sm,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .testTag("generate_reminder_btn")
                                    ) {
                                        if (isReminderLoading) {
                                            TypingIndicator(dotColor = colors.bgCanvas)
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text("Drafting Message...")
                                        } else {
                                            Icon(Icons.Default.Send, contentDescription = null, modifier = Modifier.size(18.dp))
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text("Generate $reminderTone Message")
                                        }
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(16.dp))

                            if (generatedReminderText.isNotBlank()) {
                                Card(
                                    colors = CardDefaults.cardColors(containerColor = colors.bgSurface),
                                    shape = KhataTheme.shapes.md,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(modifier = Modifier.padding(16.dp)) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = "Generated $reminderTone Message",
                                                style = TitleStyle.copy(fontSize = 15.sp, fontWeight = FontWeight.Bold),
                                                color = colors.textPrimary
                                            )
                                            IconButton(
                                                onClick = {
                                                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                                    val clip = ClipData.newPlainText("Khata Reminder", generatedReminderText)
                                                    clipboard.setPrimaryClip(clip)
                                                    Toast.makeText(context, "Copied to clipboard!", Toast.LENGTH_SHORT).show()
                                                }
                                            ) {
                                                Icon(Icons.Default.ContentCopy, contentDescription = "Copy", tint = colors.credit)
                                            }
                                        }
                                        Spacer(modifier = Modifier.height(8.dp))
                                        OutlinedTextField(
                                            value = generatedReminderText,
                                            onValueChange = { generatedReminderText = it },
                                            modifier = Modifier.fillMaxWidth().height(150.dp),
                                            textStyle = BodyStyle.copy(color = colors.textPrimary),
                                            colors = OutlinedTextFieldDefaults.colors(
                                                focusedContainerColor = colors.bgCanvas,
                                                unfocusedContainerColor = colors.bgCanvas
                                            ),
                                            shape = KhataTheme.shapes.sm
                                        )
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            val words = generatedReminderText.trim().split("\\s+".toRegex()).size
                                            val readTime = maxOf(1, words / 200)
                                            Text(
                                                text = "${generatedReminderText.length} chars • ~${readTime} min read",
                                                style = CaptionStyle,
                                                color = colors.textSecondary
                                            )
                                            Button(
                                                onClick = {
                                                    val intent = android.content.Intent(android.content.Intent.ACTION_VIEW).apply {
                                                        data = android.net.Uri.parse("https://wa.me/?text=${android.net.Uri.encode(generatedReminderText)}")
                                                    }
                                                    context.startActivity(intent)
                                                },
                                                colors = ButtonDefaults.buttonColors(containerColor = androidx.compose.ui.graphics.Color(0xFF25D366)),
                                                shape = KhataTheme.shapes.sm
                                            ) {
                                                Text("Share via WhatsApp", color = androidx.compose.ui.graphics.Color.White)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    3 -> {
                        // TAB 2: AUDIT & ANOMALY DETECTOR
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .verticalScroll(rememberScrollState())
                        ) {
                            Card(
                                colors = CardDefaults.cardColors(containerColor = colors.bgSurface),
                                shape = KhataTheme.shapes.md,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    Text(
                                        text = "AI Ledger Audit & Anomaly Detector",
                                        style = TitleStyle.copy(fontWeight = FontWeight.Bold),
                                        color = colors.textPrimary
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = "Scans all accounts and trace logs to highlight suspicious entries, duplicate entries, or long overdue balances.",
                                        style = CaptionStyle,
                                        color = colors.textSecondary
                                    )

                                    Spacer(modifier = Modifier.height(16.dp))

                                    Button(
                                        onClick = {
                                            isAuditLoading = true
                                            scope.launch {
                                                auditText = AiService.detectAuditAnomalies(context, contacts, traceLogs)
                                                isAuditLoading = false
                                            }
                                        },
                                        enabled = !isAuditLoading,
                                        colors = ButtonDefaults.buttonColors(containerColor = colors.debit),
                                        shape = KhataTheme.shapes.sm,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .testTag("run_audit_scan_btn")
                                    ) {
                                        if (isAuditLoading) {
                                            TypingIndicator(dotColor = colors.bgCanvas)
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text("Scanning Database...")
                                        } else {
                                            Icon(Icons.Default.Security, contentDescription = null, modifier = Modifier.size(18.dp))
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text("Run Audit Scan")
                                        }
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(16.dp))

                            if (auditText.isNotBlank()) {
                                Card(
                                    colors = CardDefaults.cardColors(containerColor = colors.debitSurface.copy(alpha = 0.2f)),
                                    shape = KhataTheme.shapes.md,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(modifier = Modifier.padding(16.dp)) {
                                        Text(
                                            text = "Audit Insights & Risk Report",
                                            style = TitleStyle.copy(fontSize = 15.sp, fontWeight = FontWeight.Bold),
                                            color = colors.debit
                                        )
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Text(
                                            text = auditText,
                                            style = BodyStyle,
                                            color = colors.textPrimary
                                        )
                                    }
                                }
                            }
                        }
                    }

                    4 -> {
                        // TAB 3: CUSTOM ENDPOINTS & MULTI-MODEL AI CONFIG
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .verticalScroll(rememberScrollState())
                        ) {
                            Card(
                                colors = CardDefaults.cardColors(containerColor = colors.bgSurface),
                                shape = KhataTheme.shapes.md,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    Text(
                                        text = "AI Model Engine & Endpoint Configuration",
                                        style = TitleStyle.copy(fontWeight = FontWeight.Bold),
                                        color = colors.textPrimary
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = "Switch between Built-in Gemini 3.5 Flash or paste custom OpenAI/Anthropic/Nvidia endpoints.",
                                        style = CaptionStyle,
                                        color = colors.textSecondary
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    val currentConfigText = if (currentProvider == AiProvider.GEMINI) {
                                        "Currently using Free Tier Built-in Model"
                                    } else {
                                        "Currently using ${currentProvider.displayName} - Model: ${modelNameInput}"
                                    }
                                    Text(
                                        text = currentConfigText,
                                        style = CaptionStyle,
                                        color = colors.credit
                                    )

                                    Spacer(modifier = Modifier.height(16.dp))

                                    // Provider Selection Radio Group
                                    AiProvider.values().forEach { provider ->
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clip(RoundedCornerShape(8.dp))
                                                .clickable {
                                                    currentProvider = provider
                                                    AiConfigManager.setProvider(context, provider)
                                                }
                                                .padding(vertical = 8.dp, horizontal = 4.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            RadioButton(
                                                selected = currentProvider == provider,
                                                onClick = {
                                                    currentProvider = provider
                                                    AiConfigManager.setProvider(context, provider)
                                                },
                                                colors = RadioButtonDefaults.colors(selectedColor = colors.credit)
                                            )
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text(
                                                text = provider.displayName,
                                                style = BodyStyle,
                                                fontWeight = if (currentProvider == provider) FontWeight.Bold else FontWeight.Normal,
                                                color = colors.textPrimary
                                            )
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(16.dp))

                                    // Fields for Custom Endpoint
                                    if (currentProvider != AiProvider.GEMINI) {
                                        OutlinedTextField(
                                            value = endpointUrl,
                                            onValueChange = { endpointUrl = it },
                                            label = { Text("Base Endpoint URL") },
                                            placeholder = { Text(if (currentProvider == AiProvider.OPENAI) "https://api.openai.com/v1/chat/completions" else "https://api.anthropic.com/v1/messages") },
                                            colors = OutlinedTextFieldDefaults.colors(
                                                focusedContainerColor = colors.bgCanvas,
                                                unfocusedContainerColor = colors.bgCanvas
                                            ),
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .testTag("custom_endpoint_url_input")
                                        )

                                        Spacer(modifier = Modifier.height(12.dp))
                                        
                                        if (availableModels.isNotEmpty()) {
                                            ExposedDropdownMenuBox(
                                                expanded = isModelDropdownExpanded,
                                                onExpandedChange = { isModelDropdownExpanded = !isModelDropdownExpanded }
                                            ) {
                                                OutlinedTextField(
                                                    value = modelNameInput,
                                                    onValueChange = { modelNameInput = it },
                                                    label = { Text("Selected Model") },
                                                    readOnly = false,
                                                    trailingIcon = {
                                                        ExposedDropdownMenuDefaults.TrailingIcon(expanded = isModelDropdownExpanded)
                                                    },
                                                    colors = OutlinedTextFieldDefaults.colors(
                                                        focusedContainerColor = colors.bgCanvas,
                                                        unfocusedContainerColor = colors.bgCanvas
                                                    ),
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .menuAnchor()
                                                )
                                                ExposedDropdownMenu(
                                                    expanded = isModelDropdownExpanded,
                                                    onDismissRequest = { isModelDropdownExpanded = false }
                                                ) {
                                                    availableModels.forEach { model ->
                                                        DropdownMenuItem(
                                                            text = { Text(text = model) },
                                                            onClick = {
                                                                modelNameInput = model
                                                                isModelDropdownExpanded = false
                                                                AiConfigManager.saveCustomConfig(context, endpointUrl, apiKeyInput, modelNameInput)
                                                            }
                                                        )
                                                    }
                                                }
                                            }
                                        } else {
                                            OutlinedTextField(
                                                value = modelNameInput,
                                                onValueChange = { modelNameInput = it },
                                                label = { Text("Model Name (e.g. gpt-4o-mini, claude-3-haiku, nvidia/llama-3.1)") },
                                                colors = OutlinedTextFieldDefaults.colors(
                                                    focusedContainerColor = colors.bgCanvas,
                                                    unfocusedContainerColor = colors.bgCanvas
                                                ),
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .testTag("custom_model_name_input")
                                            )
                                        }

                                        Spacer(modifier = Modifier.height(12.dp))
                                    }

                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        OutlinedTextField(
                                            value = apiKeyInput,
                                            onValueChange = { apiKeyInput = it },
                                            label = { Text("API Key (leave empty for default Gemini Studio key)") },
                                            visualTransformation = PasswordVisualTransformation(),
                                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                                            colors = OutlinedTextFieldDefaults.colors(
                                                focusedContainerColor = colors.bgCanvas,
                                                unfocusedContainerColor = colors.bgCanvas
                                            ),
                                            modifier = Modifier
                                                .weight(1f)
                                                .testTag("custom_api_key_input")
                                        )
                                        if (apiKeyInput.isNotBlank()) {
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Button(
                                                onClick = {
                                                    apiKeyInput = ""
                                                    AiConfigManager.saveCustomConfig(context, endpointUrl, apiKeyInput, modelNameInput)
                                                },
                                                colors = ButtonDefaults.buttonColors(containerColor = colors.debit),
                                                shape = KhataTheme.shapes.sm,
                                                modifier = Modifier.padding(top = 8.dp)
                                            ) {
                                                Text("Clear")
                                            }
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(16.dp))

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                                    ) {
                                        Button(
                                            onClick = {
                                                AiConfigManager.saveCustomConfig(context, endpointUrl, apiKeyInput, modelNameInput)
                                                isValidating = true
                                                validationStatusMessage = null
                                                isValidationSuccess = null

                                                scope.launch {
                                                    val res = AiConfigManager.validateConnection(
                                                        provider = currentProvider,
                                                        endpoint = endpointUrl,
                                                        apiKey = apiKeyInput,
                                                        model = modelNameInput
                                                    )
                                                    
                                                    res.fold(
                                                        onSuccess = { msg ->
                                                            isValidationSuccess = true
                                                            validationStatusMessage = msg
                                                            
                                                            // Also attempt to fetch models
                                                            val modelRes = AiConfigManager.fetchAvailableModels(currentProvider, endpointUrl, apiKeyInput)
                                                            modelRes.onSuccess { models ->
                                                                if (models.isNotEmpty()) {
                                                                    availableModels = models
                                                                }
                                                            }
                                                        },
                                                        onFailure = { err ->
                                                            isValidationSuccess = false
                                                            validationStatusMessage = err.localizedMessage
                                                        }
                                                    )
                                                    isValidating = false
                                                }
                                            },
                                            enabled = !isValidating,
                                            colors = ButtonDefaults.buttonColors(containerColor = colors.textPrimary),
                                            shape = KhataTheme.shapes.sm,
                                            modifier = Modifier
                                                .weight(1f)
                                                .testTag("test_ai_connection_btn")
                                        ) {
                                            if (isValidating) {
                                                TypingIndicator(dotColor = colors.bgCanvas)
                                                Spacer(modifier = Modifier.width(8.dp))
                                                Text("Testing & Loading...")
                                            } else {
                                                Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(18.dp))
                                                Spacer(modifier = Modifier.width(8.dp))
                                                Text("Validate & Load Models")
                                            }
                                        }
                                    }

                                    AnimatedVisibility(visible = validationStatusMessage != null) {
                                        val isOk = isValidationSuccess == true
                                        val statusColor = if (isOk) colors.credit else colors.debit
                                        val icon = if (isOk) Icons.Default.CheckCircle else Icons.Default.ErrorOutline

                                        Card(
                                            colors = CardDefaults.cardColors(
                                                containerColor = if (isOk) colors.creditSurface else colors.debitSurface
                                            ),
                                            shape = KhataTheme.shapes.sm,
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(top = 16.dp)
                                        ) {
                                            Row(
                                                modifier = Modifier.padding(12.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Icon(
                                                    imageVector = icon,
                                                    contentDescription = null,
                                                    tint = statusColor,
                                                    modifier = Modifier.size(20.dp)
                                                )
                                                Spacer(modifier = Modifier.width(8.dp))
                                                Text(
                                                    text = validationStatusMessage ?: "",
                                                    style = CaptionStyle.copy(fontWeight = FontWeight.Bold),
                                                    color = statusColor
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun AiChatTab(
    chatMessages: List<ChatMessage>,
    chatInput: String,
    onChatInputChange: (String) -> Unit,
    isChatLoading: Boolean,
    onSendMessage: () -> Unit
) {
    val colors = KhataTheme.colors
    val listState = androidx.compose.foundation.lazy.rememberLazyListState()

    androidx.compose.runtime.LaunchedEffect(chatMessages.size, isChatLoading) {
        if (chatMessages.isNotEmpty()) {
            listState.animateScrollToItem(chatMessages.size - 1)
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        // Chat Messages
        androidx.compose.foundation.lazy.LazyColumn(
            state = listState,
            modifier = Modifier.weight(1f).fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = PaddingValues(bottom = 16.dp)
        ) {
            items(chatMessages.size) { index ->
                val msg = chatMessages[index]
                val alignment = if (msg.isUser) Alignment.CenterEnd else Alignment.CenterStart
                val bgColor = if (msg.isUser) colors.creditSurface else colors.bgSurface
                val textColor = if (msg.isUser) colors.credit else colors.textPrimary

                Box(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = alignment
                ) {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = bgColor),
                        shape = KhataTheme.shapes.md,
                        modifier = Modifier.padding(horizontal = 8.dp).fillMaxWidth(0.85f)
                    ) {
                        Text(
                            text = if (msg.isAction) "⚙️ Action Executed: ${msg.text}" else msg.text,
                            style = BodyStyle,
                            color = textColor,
                            modifier = Modifier.padding(12.dp)
                        )
                    }
                }
            }
            if (isChatLoading) {
                item {
                    Box(
                        modifier = Modifier.fillMaxWidth().padding(8.dp),
                        contentAlignment = Alignment.CenterStart
                    ) {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = colors.bgSurface),
                            shape = KhataTheme.shapes.md
                        ) {
                            TypingIndicator(modifier = Modifier.padding(16.dp))
                        }
                    }
                }
            }
        }

        // Quick Actions
        androidx.compose.foundation.lazy.LazyRow(
            modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            val suggestions = listOf("Add Expense", "Check Balance", "Log Income", "Send Reminder")
            items(suggestions.size) { index ->
                SemanticChip(
                    text = suggestions[index],
                    onClick = { 
                        onChatInputChange(suggestions[index])
                    },
                    type = com.example.ui.components.ChipType.NEUTRAL
                )
            }
        }

        // Input Row
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            OutlinedTextField(
                value = chatInput,
                onValueChange = onChatInputChange,
                placeholder = { Text("Ask KhataAI...") },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = colors.bgSurface,
                    unfocusedContainerColor = colors.bgSurface
                ),
                shape = KhataTheme.shapes.md,
                modifier = Modifier.weight(1f)
            )
            Spacer(modifier = Modifier.width(8.dp))
            IconButton(
                onClick = onSendMessage,
                modifier = Modifier
                    .size(48.dp)
                    .background(colors.credit, androidx.compose.foundation.shape.CircleShape)
            ) {
                Icon(
                    imageVector = Icons.Default.Send,
                    contentDescription = "Send",
                    tint = colors.bgCanvas
                )
            }
        }
    }
}

fun executeAiAction(
    action: String,
    args: String,
    contacts: List<ContactWithBalance>,
    onAddIncomeExpenseEntry: (Double, String, String, String, Long) -> Unit,
    onAddTransaction: (Long, String, Double, String, String, String, Long, String?) -> Unit
) {
    val argsList = args.split(" ").filter { it.isNotBlank() }
    when (action) {
        "ADD_EXPENSE" -> {
            if (argsList.size >= 2) {
                val amount = argsList[0].toDoubleOrNull() ?: return
                val note = argsList.drop(1).joinToString(" ")
                onAddIncomeExpenseEntry(amount, note, "EXPENSE", "AI_GENERATED", System.currentTimeMillis())
            }
        }
        "ADD_INCOME" -> {
            if (argsList.size >= 2) {
                val amount = argsList[0].toDoubleOrNull() ?: return
                val note = argsList.drop(1).joinToString(" ")
                onAddIncomeExpenseEntry(amount, note, "INCOME", "AI_GENERATED", System.currentTimeMillis())
            }
        }
        "ADD_LEDGER_ENTRY" -> {
            if (argsList.size >= 4) {
                val contactId = argsList[0].toLongOrNull() ?: return
                val amount = argsList[1].toDoubleOrNull() ?: return
                val type = argsList.last()
                val note = argsList.drop(2).dropLast(1).joinToString(" ")
                onAddTransaction(contactId, type, amount, "AI_GENERATED", note, "AI_GENERATED", System.currentTimeMillis(), null)
            }
        }
        "SEND_REMINDER" -> {
            // Usually we might trigger the reminder tab, but for now we can just log it or handle similarly
            // A robust implementation would switch to the reminder tab or fire an intent directly.
        }
    }
}

