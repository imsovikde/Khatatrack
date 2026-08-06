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
    SEARCH,
    TRASH,
    TRACE_LOG,
    CATEGORY_PAYMENT_MODE,
    BACKUP_DATA
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
    val lastDeletedTransaction: Transaction? = null,
    val lastDeletedContact: Contact? = null,
    val traceTargetEntityType: String? = null,
    val traceTargetEntityId: Long? = null
)

class KhataViewModel(application: Application) : AndroidViewModel(application) {

    private val db = KhataDatabase.getDatabase(application)
    val repository = KhataRepository(
        contactDao = db.contactDao(),
        transactionDao = db.transactionDao(),
        reminderDao = db.reminderDao(),
        traceLogDao = db.traceLogDao(),
        categoryDao = db.categoryDao(),
        paymentModeDao = db.paymentModeDao()
    )

    private val _uiState = MutableStateFlow(UiState())
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            repository.seedSampleDataIfEmpty()
            val retentionDays = com.example.util.TrashRetentionManager.getRetentionDays(application)
            repository.purgeOldTrash(retentionDays)
            com.example.util.TrashRetentionManager.scheduleDailyCleanupWork(application)
        }
    }

    fun getTrashRetentionDays(): Int {
        return com.example.util.TrashRetentionManager.getRetentionDays(getApplication())
    }

    fun setTrashRetentionDays(days: Int) {
        com.example.util.TrashRetentionManager.setRetentionDays(getApplication(), days)
        viewModelScope.launch {
            repository.purgeOldTrash(days)
        }
        showSnackbar("Trash retention window updated to $days days")
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

    fun openSingleTraceLog(entityType: String, entityId: Long) {
        _uiState.value = _uiState.value.copy(
            traceTargetEntityType = entityType,
            traceTargetEntityId = entityId,
            currentScreen = Screen.TRACE_LOG
        )
    }

    fun openFullTraceLog() {
        _uiState.value = _uiState.value.copy(
            traceTargetEntityType = null,
            traceTargetEntityId = null,
            currentScreen = Screen.TRACE_LOG
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
                repository.updateContact(updated, editContact)
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

    fun togglePinContact(contactId: Long, currentIsPinned: Boolean, name: String = "Contact") {
        viewModelScope.launch {
            val newPinnedState = !currentIsPinned
            repository.setContactPinned(contactId, newPinnedState, name)
            val msg = if (newPinnedState) "'$name' pinned to top" else "'$name' unpinned"
            showSnackbar(msg)
        }
    }

    fun softDeleteContact(contactId: Long, name: String = "Contact") {
        viewModelScope.launch {
            val contact = repository.getContactByIdSync(contactId)
            repository.softDeleteContact(contactId, name)
            _uiState.value = _uiState.value.copy(
                lastDeletedContact = contact,
                activeContactId = null,
                currentScreen = Screen.HOME,
                snackbarMessage = "'$name' moved to Trash"
            )
        }
    }

    fun restoreContact(contactId: Long, name: String = "Contact") {
        viewModelScope.launch {
            repository.restoreContact(contactId, name)
            showSnackbar("Restored '$name' from Trash")
        }
    }

    fun deleteContactPermanently(contactId: Long, name: String = "Contact") {
        viewModelScope.launch {
            repository.deleteContactPermanently(contactId, name)
            showSnackbar("Permanently deleted '$name'")
        }
    }

    fun saveOrUpdateTransaction(
        amount: Double,
        type: String,
        paymentMode: String,
        note: String?,
        dueDate: Long?,
        referenceNumber: String?,
        editingTxId: Long?
    ) {
        val contactId = _uiState.value.activeContactId ?: return
        viewModelScope.launch {
            val contact = repository.getContactByIdSync(contactId)
            val cName = contact?.name ?: ""

            if (editingTxId != null) {
                val oldTx = repository.getTransactionById(editingTxId)
                if (oldTx != null) {
                    val updated = oldTx.copy(
                        amount = amount,
                        type = type,
                        paymentMode = paymentMode,
                        note = note,
                        referenceNumber = referenceNumber,
                        collectionDueDate = dueDate
                    )
                    repository.updateTransaction(updated, oldTx, cName)
                    showSnackbar("Transaction updated")
                }
            } else {
                val tx = Transaction(
                    contactId = contactId,
                    type = type,
                    amount = amount,
                    paymentMode = paymentMode,
                    note = note,
                    referenceNumber = referenceNumber,
                    collectionDueDate = dueDate
                )
                repository.addTransaction(tx, cName)
                showSnackbar("Transaction added successfully")
            }
        }
    }

    fun softDeleteTransaction(transaction: Transaction) {
        viewModelScope.launch {
            repository.softDeleteTransaction(transaction.id, "Tx #${transaction.id}")
            _uiState.value = _uiState.value.copy(
                lastDeletedTransaction = transaction,
                snackbarMessage = "Transaction moved to Trash"
            )
        }
    }

    fun restoreTransaction(id: Long, desc: String = "") {
        viewModelScope.launch {
            repository.restoreTransaction(id, desc)
            showSnackbar("Transaction restored from Trash")
        }
    }

    fun deleteTransactionPermanently(id: Long, desc: String = "") {
        viewModelScope.launch {
            repository.deleteTransactionPermanently(id, desc)
            showSnackbar("Transaction deleted permanently")
        }
    }

    fun undoDeleteTransaction() {
        val lastTx = _uiState.value.lastDeletedTransaction ?: return
        viewModelScope.launch {
            repository.restoreTransaction(lastTx.id)
            _uiState.value = _uiState.value.copy(
                lastDeletedTransaction = null,
                snackbarMessage = "Transaction restored"
            )
        }
    }

    fun undoDeleteContact() {
        val lastC = _uiState.value.lastDeletedContact ?: return
        viewModelScope.launch {
            repository.restoreContact(lastC.id, lastC.name)
            _uiState.value = _uiState.value.copy(
                lastDeletedContact = null,
                snackbarMessage = "Contact restored"
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
