package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
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
import com.example.ui.screens.AddEditContactScreen
import com.example.ui.screens.AppLockScreen
import com.example.ui.screens.ContactLedgerScreen
import com.example.ui.screens.HomeScreen
import com.example.ui.screens.RemindersScreen
import com.example.ui.screens.ReportsScreen
import com.example.ui.screens.SearchScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.theme.KhataTheme
import com.example.ui.theme.KhataTrackTheme
import com.example.ui.viewmodel.KhataViewModel
import com.example.ui.viewmodel.Screen

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
            val activeContactNetBalance by viewModel.activeContactNetBalance.collectAsStateWithLifecycle()
            val searchResults by viewModel.searchResults.collectAsStateWithLifecycle()
            val pendingReminders by viewModel.pendingReminders.collectAsStateWithLifecycle()

            val snackbarHostState = remember { SnackbarHostState() }

            // Add Transaction bottom sheet state
            var isAddTransactionSheetOpen by remember { mutableStateOf(false) }
            var transactionSheetInitialType by remember { mutableStateOf(Transaction.TYPE_YOU_GAVE) }
            val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

            // Handle snackbar messages & Undo action
            LaunchedEffect(uiState.snackbarMessage) {
                val msg = uiState.snackbarMessage
                if (msg != null) {
                    val hasUndo = uiState.lastDeletedTransaction != null
                    val result = snackbarHostState.showSnackbar(
                        message = msg,
                        actionLabel = if (hasUndo) "Undo" else null,
                        duration = SnackbarDuration.Short
                    )
                    if (result == SnackbarResult.ActionPerformed && hasUndo) {
                        viewModel.undoDeleteTransaction()
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
                        Screen.REMINDERS,
                        Screen.REPORTS,
                        Screen.SETTINGS
                    )

                    Scaffold(
                        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
                        bottomBar = {
                            if (showBottomNav) {
                                val currentRoute = when (currentScreen) {
                                    Screen.HOME -> NavDestination.HOME.route
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
                                    HomeScreen(
                                        summaryTotals = summaryTotals,
                                        contacts = homeContacts,
                                        currentFilter = uiState.homeFilter,
                                        onFilterSelect = { viewModel.setFilter(it) },
                                        onContactClick = { contactId -> viewModel.openContactDetail(contactId) },
                                        onAddContactClick = { viewModel.openAddContact() },
                                        onSearchClick = { viewModel.navigateTo(Screen.SEARCH) }
                                    )
                                }

                                Screen.CONTACT_DETAIL -> {
                                    ContactLedgerScreen(
                                        contact = activeContact,
                                        transactions = activeContactTransactions,
                                        netBalance = activeContactNetBalance,
                                        onBackClick = { viewModel.navigateTo(Screen.HOME) },
                                        onEditContact = { viewModel.openEditContact(it) },
                                        onDeleteContact = { viewModel.deleteContact(it) },
                                        onDeleteTransaction = { viewModel.deleteTransaction(it) },
                                        onOpenAddTransaction = { type ->
                                            transactionSheetInitialType = type
                                            isAddTransactionSheetOpen = true
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
                                    ReportsScreen(
                                        transactions = activeContactTransactions.ifEmpty {
                                            // Fallback to all transactions if no contact selected
                                            val allList = mutableListOf<Transaction>()
                                            activeContactTransactions.forEach { allList.add(it) }
                                            allList
                                        },
                                        selectedRange = uiState.reportRange,
                                        onRangeSelect = { viewModel.setReportRange(it) }
                                    )
                                }

                                Screen.SEARCH -> {
                                    SearchScreen(
                                        query = uiState.searchQuery,
                                        onQueryChange = { viewModel.setSearchQuery(it) },
                                        searchResults = searchResults,
                                        onBackClick = { viewModel.navigateTo(Screen.HOME) },
                                        onContactClick = { contactId -> viewModel.openContactDetail(contactId) }
                                    )
                                }

                                Screen.SETTINGS -> {
                                    SettingsScreen(
                                        isDarkMode = uiState.isDarkMode,
                                        onDarkModeToggle = { viewModel.toggleDarkMode(it) },
                                        isAppLockEnabled = uiState.isAppLockEnabled,
                                        onAppLockToggle = { viewModel.toggleAppLock(it) }
                                    )
                                }
                            }
                        }

                        // Add Transaction Bottom Sheet (Sub-5-second fast entry)
                        if (isAddTransactionSheetOpen && activeContact != null) {
                            AddTransactionBottomSheet(
                                sheetState = sheetState,
                                initialType = transactionSheetInitialType,
                                contactName = activeContact!!.name,
                                onDismiss = { isAddTransactionSheetOpen = false },
                                onSave = { amount, type, paymentMode, note, dueDate ->
                                    viewModel.addTransaction(amount, type, paymentMode, note, dueDate)
                                    isAddTransactionSheetOpen = false
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}
