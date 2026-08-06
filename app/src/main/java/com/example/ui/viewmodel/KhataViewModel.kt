package com.example.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.db.KhataDatabase
import com.example.data.model.Contact
import com.example.data.model.ContactWithBalance
import com.example.data.model.Reminder
import com.example.data.model.Transaction
import com.example.data.repository.KhataRepository
import com.example.data.repository.SummaryTotals
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

enum class Screen {
    HOME,
    REMINDERS,
    REPORTS,
    SETTINGS,
    CONTACT_DETAIL,
    ADD_EDIT_CONTACT,
    SEARCH
}

enum class FilterOption {
    ALL,
    YOU_GET,
    YOU_PAY
}

enum class ReportDateRange {
    THIS_WEEK,
    THIS_MONTH,
    THIS_YEAR,
    CUSTOM
}

data class UiState(
    val currentScreen: Screen = Screen.HOME,
    val activeContactId: Long? = null,
    val contactForEdit: Contact? = null,
    val homeFilter: FilterOption = FilterOption.ALL,
    val searchQuery: String = "",
    val reportRange: ReportDateRange = ReportDateRange.THIS_MONTH,
    val isAppLockEnabled: Boolean = false,
    val isAppLocked: Boolean = false,
    val securityPin: String = "1234",
    val isDarkMode: Boolean = false,
    val snackbarMessage: String? = null,
    val lastDeletedTransaction: Transaction? = null
)

class KhataViewModel(application: Application) : AndroidViewModel(application) {

    private val db = KhataDatabase.getDatabase(application)
    val repository = KhataRepository(db.contactDao(), db.transactionDao(), db.reminderDao())

    private val _uiState = MutableStateFlow(UiState())
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            repository.seedSampleDataIfEmpty()
        }
    }

    // Summary totals flow
    val summaryTotals: StateFlow<SummaryTotals> = repository.summaryTotals
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = SummaryTotals(0.0, 0.0, 0.0)
        )

    // Filtered contacts with balance for Home dashboard
    val homeContacts: StateFlow<List<ContactWithBalance>> = combine(
        repository.contactsWithBalances,
        _uiState
    ) { list, state ->
        when (state.homeFilter) {
            FilterOption.ALL -> list
            FilterOption.YOU_GET -> list.filter { it.netBalance > 0 }
            FilterOption.YOU_PAY -> list.filter { it.netBalance < 0 }
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    // Current selected contact for detail screen
    val activeContact: StateFlow<Contact?> = _uiState.flatMapLatest { state ->
        val id = state.activeContactId
        if (id != null) {
            repository.getContactById(id)
        } else {
            flowOf(null)
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = null
    )

    // Active contact transactions
    val activeContactTransactions: StateFlow<List<Transaction>> = _uiState.flatMapLatest { state ->
        val id = state.activeContactId
        if (id != null) {
            repository.getTransactionsForContact(id)
        } else {
            flowOf(emptyList())
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    // Active contact calculated net balance
    val activeContactNetBalance: StateFlow<Double> = activeContactTransactions.map { list ->
        var net = 0.0
        for (tx in list) {
            if (tx.type == Transaction.TYPE_YOU_GOT) {
                net += tx.amount
            } else {
                net -= tx.amount
            }
        }
        net
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = 0.0
    )

    // Global search contacts matching query
    val searchResults: StateFlow<List<ContactWithBalance>> = combine(
        repository.contactsWithBalances,
        _uiState
    ) { list, state ->
        val q = state.searchQuery.trim().lowercase()
        if (q.isEmpty()) {
            emptyList()
        } else {
            list.filter { item ->
                item.contact.name.lowercase().contains(q) ||
                (item.contact.mobileNumber ?: "").lowercase().contains(q) ||
                (item.contact.addressNotes ?: "").lowercase().contains(q)
            }
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    // Reminders flow
    val pendingReminders: StateFlow<List<Reminder>> = repository.pendingReminders
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    fun navigateTo(screen: Screen) {
        _uiState.value = _uiState.value.copy(currentScreen = screen)
    }

    fun openContactDetail(contactId: Long) {
        _uiState.value = _uiState.value.copy(
            activeContactId = contactId,
            currentScreen = Screen.CONTACT_DETAIL
        )
    }

    fun openAddContact() {
        _uiState.value = _uiState.value.copy(
            contactForEdit = null,
            currentScreen = Screen.ADD_EDIT_CONTACT
        )
    }

    fun openEditContact(contact: Contact) {
        _uiState.value = _uiState.value.copy(
            contactForEdit = contact,
            currentScreen = Screen.ADD_EDIT_CONTACT
        )
    }

    fun setFilter(filter: FilterOption) {
        _uiState.value = _uiState.value.copy(homeFilter = filter)
    }

    fun setSearchQuery(query: String) {
        _uiState.value = _uiState.value.copy(searchQuery = query)
    }

    fun setReportRange(range: ReportDateRange) {
        _uiState.value = _uiState.value.copy(reportRange = range)
    }

    fun toggleDarkMode(enabled: Boolean) {
        _uiState.value = _uiState.value.copy(isDarkMode = enabled)
    }

    fun toggleAppLock(enabled: Boolean) {
        _uiState.value = _uiState.value.copy(isAppLockEnabled = enabled)
    }

    fun unlockApp() {
        _uiState.value = _uiState.value.copy(isAppLocked = false)
    }

    fun saveContact(
        name: String,
        mobileNumber: String?,
        email: String?,
        categoryTag: String,
        notes: String?
    ) {
        viewModelScope.launch {
            val editContact = _uiState.value.contactForEdit
            if (editContact != null) {
                val updated = editContact.copy(
                    name = name,
                    mobileNumber = mobileNumber,
                    email = email,
                    categoryTag = categoryTag,
                    addressNotes = notes,
                    updatedAt = System.currentTimeMillis()
                )
                repository.updateContact(updated)
                _uiState.value = _uiState.value.copy(
                    activeContactId = updated.id,
                    currentScreen = Screen.CONTACT_DETAIL
                )
            } else {
                val newContact = Contact(
                    name = name,
                    mobileNumber = mobileNumber,
                    email = email,
                    categoryTag = categoryTag,
                    addressNotes = notes
                )
                val newId = repository.addContact(newContact)
                _uiState.value = _uiState.value.copy(
                    activeContactId = newId,
                    currentScreen = Screen.CONTACT_DETAIL
                )
            }
        }
    }

    fun togglePinContact(contactId: Long, currentIsPinned: Boolean) {
        viewModelScope.launch {
            repository.setContactPinned(contactId, !currentIsPinned)
        }
    }

    fun deleteContact(contactId: Long) {
        viewModelScope.launch {
            repository.deleteContactPermanently(contactId)
            _uiState.value = _uiState.value.copy(
                activeContactId = null,
                currentScreen = Screen.HOME,
                snackbarMessage = "Contact deleted"
            )
        }
    }

    fun addTransaction(
        amount: Double,
        type: String,
        paymentMode: String,
        note: String?,
        dueDate: Long?
    ) {
        val contactId = _uiState.value.activeContactId ?: return
        viewModelScope.launch {
            val tx = Transaction(
                contactId = contactId,
                type = type,
                amount = amount,
                paymentMode = paymentMode,
                note = note,
                collectionDueDate = dueDate
            )
            repository.addTransaction(tx)
            showSnackbar("Transaction added successfully")
        }
    }

    fun deleteTransaction(transaction: Transaction) {
        viewModelScope.launch {
            repository.deleteTransaction(transaction.id)
            _uiState.value = _uiState.value.copy(
                lastDeletedTransaction = transaction,
                snackbarMessage = "Transaction deleted"
            )
        }
    }

    fun undoDeleteTransaction() {
        val lastTx = _uiState.value.lastDeletedTransaction ?: return
        viewModelScope.launch {
            repository.addTransaction(lastTx.copy(id = 0))
            _uiState.value = _uiState.value.copy(
                lastDeletedTransaction = null,
                snackbarMessage = "Transaction restored"
            )
        }
    }

    fun showSnackbar(msg: String) {
        _uiState.value = _uiState.value.copy(snackbarMessage = msg)
    }

    fun dismissSnackbar() {
        _uiState.value = _uiState.value.copy(snackbarMessage = null)
    }
}
