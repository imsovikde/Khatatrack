package com.example

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.core.content.ContextCompat
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.Transaction
import com.example.ui.components.AddTransactionBottomSheet
import com.example.ui.components.KhataBottomBar
import com.example.ui.components.NavDestination
import com.example.ui.components.QuickVoiceBottomSheet
import com.example.ui.screens.AddEditContactScreen
import com.example.ui.screens.AiAssistantScreen
import com.example.ui.screens.AppLockScreen
import com.example.ui.screens.BackupDataScreen
import com.example.ui.screens.CategoryPaymentModeScreen
import com.example.ui.screens.ContactLedgerScreen
import com.example.ui.screens.CurrencySelectionBottomSheet
import com.example.ui.screens.HomeScreen
import com.example.ui.screens.IncomeExpenseScreen
import com.example.ui.screens.RemindersScreen
import com.example.ui.screens.ReportsScreen
import com.example.ui.screens.SearchScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.screens.TraceLogScreen
import com.example.ui.screens.TrashScreen
import com.example.ui.theme.KhataTheme
import com.example.ui.theme.KhataTrackTheme
import com.example.ui.viewmodel.KhataViewModel
import com.example.ui.viewmodel.Screen
import com.example.util.CurrencyFormatter
import com.example.util.CurrencyManager

class MainActivity : ComponentActivity() {

    private val viewModel: KhataViewModel by viewModels()

    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            val uiState by viewModel.uiState.collectAsStateWithLifecycle()
            val summaryTotals by viewModel.summaryTotals.collectAsStateWithLifecycle()
            val homeContacts by viewModel.homeContacts.collectAsStateWithLifecycle()
            val activeContact by viewModel.activeContact.collectAsStateWithLifecycle()
            val activeContactTransactions by viewModel.activeContactTransactions.collectAsStateWithLifecycle()
            val allTransactions by viewModel.allTransactions.collectAsStateWithLifecycle()
            val activeContactNetBalance by viewModel.activeContactNetBalance.collectAsStateWithLifecycle()
            val searchResults by viewModel.searchResults.collectAsStateWithLifecycle()
            val pendingReminders by viewModel.pendingReminders.collectAsStateWithLifecycle()

            val snackbarHostState = remember { SnackbarHostState() }

            // ── Global Mic Permission (requested once at app launch) ──────────────
            var hasMicPermission by remember {
                mutableStateOf(
                    ContextCompat.checkSelfPermission(this@MainActivity, Manifest.permission.RECORD_AUDIO)
                        == PackageManager.PERMISSION_GRANTED
                )
            }
            val micPermissionLauncher = rememberLauncherForActivityResult(
                contract = ActivityResultContracts.RequestPermission()
            ) { granted -> hasMicPermission = granted }
            LaunchedEffect(Unit) {
                if (!hasMicPermission) {
                    micPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                }
            }

            // Add/Edit Transaction bottom sheet state
            var isAddTransactionSheetOpen by remember { mutableStateOf(false) }
            var transactionSheetInitialType by remember { mutableStateOf(Transaction.TYPE_YOU_GAVE) }
            var editingTransactionForSheet by remember { mutableStateOf<Transaction?>(null) }

            // Currency selection bottom sheet state
            var isCurrencySheetOpen by remember { mutableStateOf(false) }

            // Quick Voice Entry bottom sheet state
            var isQuickVoiceSheetOpen by remember { mutableStateOf(false) }

            val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

            // Handle snackbar messages & Undo action
            LaunchedEffect(uiState.snackbarMessage) {
                val msg = uiState.snackbarMessage
                if (msg != null) {
                    val hasTxUndo = uiState.lastDeletedTransaction != null
                    val hasContactUndo = uiState.lastDeletedContact != null
                    val hasUndo = hasTxUndo || hasContactUndo

                    val result = snackbarHostState.showSnackbar(
                        message = msg,
                        actionLabel = if (hasUndo) "Undo" else null,
                        duration = SnackbarDuration.Short
                    )
                    if (result == SnackbarResult.ActionPerformed) {
                        if (hasTxUndo) viewModel.undoDeleteTransaction()
                        else if (hasContactUndo) viewModel.undoDeleteContact()
                    }
                    viewModel.dismissSnackbar()
                }
            }

            KhataTrackTheme(darkTheme = uiState.isDarkMode) {
                if (uiState.isAppLockEnabled && uiState.isAppLocked) {
                    AppLockScreen(onUnlock = { viewModel.unlockApp() })
                } else {
                    val currentScreen = uiState.currentScreen
                    val showBottomNav = currentScreen in listOf(
                        Screen.HOME,
                        Screen.INCOME_EXPENSE,
                        Screen.REMINDERS,
                        Screen.REPORTS,
                        Screen.SETTINGS
                    )

                    // Intercept system back to navigate within the custom back-stack.
                    // BackHandler is only enabled when NOT on HOME to allow the OS to
                    // handle back naturally (i.e. close the app) from the root screen.
                    BackHandler(enabled = currentScreen != Screen.HOME) {
                        when (currentScreen) {
                            Screen.CONTACT_DETAIL -> viewModel.navigateTo(Screen.HOME)
                            Screen.ADD_EDIT_CONTACT -> {
                                if (uiState.activeContactId != null) {
                                    viewModel.navigateTo(Screen.CONTACT_DETAIL)
                                } else {
                                    viewModel.navigateTo(Screen.HOME)
                                }
                            }
                            Screen.SEARCH -> viewModel.navigateTo(Screen.HOME)
                            Screen.TRASH -> viewModel.navigateTo(Screen.SETTINGS)
                            Screen.TRACE_LOG -> {
                                if (uiState.traceTargetEntityId != null) {
                                    viewModel.navigateTo(Screen.CONTACT_DETAIL)
                                } else {
                                    viewModel.navigateTo(Screen.SETTINGS)
                                }
                            }
                            Screen.CATEGORY_PAYMENT_MODE -> viewModel.navigateTo(Screen.SETTINGS)
                            Screen.BACKUP_DATA -> viewModel.navigateTo(Screen.SETTINGS)
                            Screen.INCOME_EXPENSE -> viewModel.navigateTo(Screen.HOME)
                            Screen.AI_HUB -> viewModel.navigateTo(Screen.HOME)
                            else -> viewModel.navigateTo(Screen.HOME)
                        }
                    }

                    Scaffold(
                        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
                        bottomBar = {
                            if (showBottomNav) {
                                val currentRoute = when (currentScreen) {
                                    Screen.HOME -> NavDestination.HOME.route
                                    Screen.INCOME_EXPENSE -> NavDestination.INCOME_EXPENSE.route
                                    Screen.REMINDERS -> NavDestination.REMINDERS.route
                                    Screen.REPORTS -> NavDestination.REPORTS.route
                                    Screen.SETTINGS -> NavDestination.SETTINGS.route
                                    else -> NavDestination.HOME.route
                                }
                                KhataBottomBar(
                                    currentRoute = currentRoute,
                                    onNavigate = { destination ->
                                        when (destination) {
                                            NavDestination.HOME -> viewModel.navigateTo(Screen.HOME)
                                            NavDestination.INCOME_EXPENSE -> viewModel.navigateTo(Screen.INCOME_EXPENSE)
                                            NavDestination.REMINDERS -> viewModel.navigateTo(Screen.REMINDERS)
                                            NavDestination.REPORTS -> viewModel.navigateTo(Screen.REPORTS)
                                            NavDestination.SETTINGS -> viewModel.navigateTo(Screen.SETTINGS)
                                        }
                                    }
                                )
                            }
                        },
                        containerColor = KhataTheme.colors.bgCanvas,
                        modifier = Modifier.fillMaxSize()
                    ) { innerPadding ->
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(innerPadding)
                        ) {
                            when (currentScreen) {
                                Screen.HOME -> {
                                    val quickEntries by viewModel.quickEntries.collectAsStateWithLifecycle()
                                    HomeScreen(
                                        summaryTotals = summaryTotals,
                                        contacts = homeContacts,
                                        quickEntries = quickEntries,
                                        currentFilter = uiState.homeFilter,
                                        onFilterSelect = { viewModel.setFilter(it) },
                                        onContactClick = { contactId -> viewModel.openContactDetail(contactId) },
                                        onAddContactClick = { viewModel.openAddContact() },
                                        onQuickVoiceClick = { isQuickVoiceSheetOpen = true },
                                        onQuickLedgerEntryClick = {
                                            editingTransactionForSheet = null
                                            transactionSheetInitialType = Transaction.TYPE_YOU_GAVE
                                            isAddTransactionSheetOpen = true
                                        },
                                        onSearchClick = { viewModel.navigateTo(Screen.SEARCH) },
                                        onNavigateToIncomeExpense = { viewModel.navigateTo(Screen.INCOME_EXPENSE) },
                                        onTogglePin = { contactId, isPinned, name -> viewModel.togglePinContact(contactId, isPinned, name) },
                                        onDeleteContact = { contactId, name -> viewModel.softDeleteContact(contactId, name) }
                                    )
                                }

                                Screen.INCOME_EXPENSE -> {
                                    val ieEntries by viewModel.allIncomeExpenseEntries.collectAsStateWithLifecycle()
                                    IncomeExpenseScreen(
                                        entries = ieEntries,
                                        onAddEntry = { viewModel.addIncomeExpenseEntry(it) },
                                        onUpdateEntry = { viewModel.updateIncomeExpenseEntry(it) },
                                        onDeleteEntry = { viewModel.deleteIncomeExpenseEntry(it) },
                                        onBack = { viewModel.navigateTo(Screen.HOME) }
                                    )
                                }

                                Screen.AI_HUB -> {
                                    AiAssistantScreen(
                                        contacts = homeContacts,
                                        summaryTotals = summaryTotals,
                                        traceLogs = emptyList(),
                                        onBackClick = { viewModel.navigateTo(Screen.HOME) }
                                    )
                                }

                                Screen.CONTACT_DETAIL -> {
                                    ContactLedgerScreen(
                                        contact = activeContact,
                                        transactions = activeContactTransactions,
                                        netBalance = activeContactNetBalance,
                                        onBackClick = { viewModel.navigateTo(Screen.HOME) },
                                        onEditContact = { viewModel.openEditContact(it) },
                                        onDeleteContact = { contactId ->
                                            activeContact?.let { c ->
                                                viewModel.softDeleteContact(contactId, c.name)
                                            }
                                        },
                                        onEditTransaction = { tx ->
                                            editingTransactionForSheet = tx
                                            transactionSheetInitialType = tx.type
                                            isAddTransactionSheetOpen = true
                                        },
                                        onDeleteTransaction = { viewModel.softDeleteTransaction(it) },
                                        onOpenAddTransaction = { type ->
                                            editingTransactionForSheet = null
                                            transactionSheetInitialType = type
                                            isAddTransactionSheetOpen = true
                                        },
                                        onViewHistory = { id ->
                                            viewModel.openSingleTraceLog("CONTACT", id)
                                        }
                                    )
                                }

                                Screen.ADD_EDIT_CONTACT -> {
                                    AddEditContactScreen(
                                        contactToEdit = uiState.contactForEdit,
                                        onBackClick = {
                                            if (uiState.activeContactId != null) {
                                                viewModel.navigateTo(Screen.CONTACT_DETAIL)
                                            } else {
                                                viewModel.navigateTo(Screen.HOME)
                                            }
                                        },
                                        onSaveContact = { name, mobile, email, tag, notes ->
                                            viewModel.saveContact(name, mobile, email, tag, notes)
                                        }
                                    )
                                }

                                Screen.REMINDERS -> {
                                    RemindersScreen(
                                        reminders = pendingReminders,
                                        contactsWithBalances = homeContacts,
                                        onContactClick = { contactId -> viewModel.openContactDetail(contactId) }
                                    )
                                }

                                Screen.REPORTS -> {
                                    val ieEntries by viewModel.allIncomeExpenseEntries.collectAsStateWithLifecycle()
                                    ReportsScreen(
                                        transactions = allTransactions,
                                        incomeExpenseEntries = ieEntries,
                                        selectedRange = uiState.reportRange,
                                        onRangeSelect = { viewModel.setReportRange(it) }
                                    )
                                }

                                Screen.SEARCH -> {
                                    val ieSearchResults by viewModel.incomeExpenseSearchResults.collectAsStateWithLifecycle()
                                    val allContacts by viewModel.allContacts.collectAsStateWithLifecycle()
                                    val contactsMap = remember(allContacts) {
                                        allContacts.associate { it.id to it.name }
                                    }
                                    SearchScreen(
                                        query = uiState.searchQuery,
                                        onQueryChange = { viewModel.setSearchQuery(it) },
                                        searchResults = searchResults,
                                        transactionSearchResults = viewModel.transactionSearchResults.collectAsStateWithLifecycle().value,
                                        incomeExpenseResults = ieSearchResults,
                                        contactsMap = contactsMap,
                                        onBackClick = { viewModel.navigateTo(Screen.HOME) },
                                        onContactClick = { contactId -> viewModel.openContactDetail(contactId) }
                                    )
                                }

                                 Screen.SETTINGS -> {
                                    SettingsScreen(
                                        isDarkMode = uiState.isDarkMode,
                                        onDarkModeToggle = { viewModel.toggleDarkMode(it) },
                                        isAppLockEnabled = uiState.isAppLockEnabled,
                                        onAppLockToggle = { viewModel.toggleAppLock(it) },
                                        retentionDays = viewModel.getTrashRetentionDays(),
                                        onRetentionDaysChange = { viewModel.setTrashRetentionDays(it) },
                                        onOpenCurrency = { isCurrencySheetOpen = true },
                                        onOpenTrash = { viewModel.navigateTo(Screen.TRASH) },
                                        onOpenTraceLog = { viewModel.openFullTraceLog() },
                                        onOpenCategoryManagement = { viewModel.navigateTo(Screen.CATEGORY_PAYMENT_MODE) },
                                        onOpenBackupData = { viewModel.navigateTo(Screen.BACKUP_DATA) }
                                    )
                                }

                                Screen.TRASH -> {
                                    TrashScreen(
                                        viewModel = viewModel,
                                        onBack = { viewModel.navigateTo(Screen.SETTINGS) }
                                    )
                                }

                                Screen.TRACE_LOG -> {
                                    TraceLogScreen(
                                        viewModel = viewModel,
                                        entityType = uiState.traceTargetEntityType,
                                        entityId = uiState.traceTargetEntityId,
                                        onBack = {
                                            if (uiState.traceTargetEntityId != null) {
                                                viewModel.navigateTo(Screen.CONTACT_DETAIL)
                                            } else {
                                                viewModel.navigateTo(Screen.SETTINGS)
                                            }
                                        }
                                    )
                                }

                                Screen.CATEGORY_PAYMENT_MODE -> {
                                    CategoryPaymentModeScreen(
                                        viewModel = viewModel,
                                        onBack = { viewModel.navigateTo(Screen.SETTINGS) }
                                    )
                                }

                                Screen.BACKUP_DATA -> {
                                    BackupDataScreen(
                                        viewModel = viewModel,
                                        onBack = { viewModel.navigateTo(Screen.SETTINGS) }
                                    )
                                }
                            }
                        }

                        // Add / Edit Transaction Bottom Sheet
                        if (isAddTransactionSheetOpen) {
                            AddTransactionBottomSheet(
                                sheetState = sheetState,
                                initialType = transactionSheetInitialType,
                                contactName = activeContact?.name ?: "",
                                contactsList = homeContacts.map { it.contact },
                                editingTransaction = editingTransactionForSheet,
                                onDismiss = {
                                    isAddTransactionSheetOpen = false
                                    editingTransactionForSheet = null
                                },
                                onSave = { amount, type, paymentMode, note, dueDate, referenceNumber, editingTxId, photoUri, contactIdOverride ->
                                    viewModel.saveOrUpdateTransaction(
                                        amount = amount,
                                        type = type,
                                        paymentMode = paymentMode,
                                        note = note,
                                        dueDate = dueDate,
                                        referenceNumber = referenceNumber,
                                        editingTxId = editingTxId,
                                        attachmentPhotoUri = photoUri,
                                        contactIdOverride = contactIdOverride ?: activeContact?.id
                                    )
                                    isAddTransactionSheetOpen = false
                                    editingTransactionForSheet = null
                                }
                            )
                        }

                        // Currency Selection Sheet
                        if (isCurrencySheetOpen) {
                            CurrencySelectionBottomSheet(
                                onDismiss = { isCurrencySheetOpen = false },
                                onCurrencySelected = { currency ->
                                    CurrencyManager.setSelectedCurrency(this@MainActivity, currency.code)
                                    CurrencyFormatter.updateActiveCurrency(currency.symbol, currency.code)
                                    isCurrencySheetOpen = false
                                    viewModel.showSnackbar("Currency updated to ${currency.symbol} (${currency.code})")
                                }
                            )
                        }

                        // Quick Voice Entry Sheet
                        if (isQuickVoiceSheetOpen) {
                            QuickVoiceBottomSheet(
                                contacts = homeContacts.map { it.contact },
                                onDismiss = { isQuickVoiceSheetOpen = false },
                                onSaveTransaction = { contactId, type, amount, paymentMode, note, categoryTag, collectionDueDate, referenceNumber ->
                                    viewModel.addTransactionForContact(
                                        contactId = contactId,
                                        type = type,
                                        amount = amount,
                                        paymentMode = paymentMode,
                                        note = note,
                                        categoryTag = categoryTag,
                                        dueDate = collectionDueDate,
                                        referenceNumber = referenceNumber
                                    )
                                    isQuickVoiceSheetOpen = false
                                },
                                onAddNewContactAndSave = { contactName, type, amount, paymentMode, note, categoryTag, collectionDueDate, referenceNumber ->
                                    viewModel.addContactWithInitialTransaction(
                                        contactName = contactName,
                                        type = type,
                                        amount = amount,
                                        paymentMode = paymentMode,
                                        note = note,
                                        categoryTag = categoryTag,
                                        dueDate = collectionDueDate,
                                        referenceNumber = referenceNumber
                                    )
                                    isQuickVoiceSheetOpen = false
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}
