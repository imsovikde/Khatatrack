package com.example.data.repository

import com.example.data.dao.CategoryDao
import com.example.data.dao.ContactDao
import com.example.data.dao.PaymentModeDao
import com.example.data.dao.ReminderDao
import com.example.data.dao.TraceLogDao
import com.example.data.dao.TransactionDao
import com.example.data.model.CategoryItem
import com.example.data.model.Contact
import com.example.data.model.ContactWithBalance
import com.example.data.model.PaymentModeItem
import com.example.data.model.Reminder
import com.example.data.model.TraceLog
import com.example.data.model.Transaction
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map

data class SummaryTotals(
    val totalGet: Double, // You'll Get (Credit)
    val totalPay: Double, // You'll Pay (Debit)
    val netBalance: Double // Net balance = totalGet - totalPay
)

class KhataRepository(
    private val contactDao: ContactDao,
    private val transactionDao: TransactionDao,
    private val reminderDao: ReminderDao,
    private val traceLogDao: TraceLogDao,
    private val categoryDao: CategoryDao,
    private val paymentModeDao: PaymentModeDao
) {
    val allContacts: Flow<List<Contact>> = contactDao.getAllContacts()
    val trashContacts: Flow<List<Contact>> = contactDao.getTrashContacts()

    val allTransactions: Flow<List<Transaction>> = transactionDao.getAllTransactions()
    val trashTransactions: Flow<List<Transaction>> = transactionDao.getTrashTransactions()

    val pendingReminders: Flow<List<Reminder>> = reminderDao.getPendingReminders()
    val allTraces: Flow<List<TraceLog>> = traceLogDao.getAllTraces()
    val allCategories: Flow<List<CategoryItem>> = categoryDao.getAllCategories()
    val allPaymentModes: Flow<List<PaymentModeItem>> = paymentModeDao.getAllPaymentModes()

    // Reactive list of contacts paired with their calculated running balance and last activity date
    val contactsWithBalances: Flow<List<ContactWithBalance>> = combine(
        contactDao.getAllContacts(),
        transactionDao.getAllTransactions()
    ) { contacts, transactions ->
        val txMap = transactions.groupBy { it.contactId }
        contacts.map { contact ->
            val contactTxs = txMap[contact.id] ?: emptyList()
            var net = 0.0
            var latestTime = contact.createdAt
            for (tx in contactTxs) {
                if (tx.type == Transaction.TYPE_YOU_GOT) {
                    net += tx.amount
                } else {
                    net -= tx.amount
                }
                if (tx.transactionDate > latestTime) {
                    latestTime = tx.transactionDate
                }
            }
            ContactWithBalance(
                contact = contact,
                netBalance = net,
                lastActivityTime = latestTime
            )
        }
    }

    // Reactive summary card totals
    val summaryTotals: Flow<SummaryTotals> = contactsWithBalances.map { list ->
        var totalGet = 0.0
        var totalPay = 0.0
        for (item in list) {
            if (item.netBalance > 0) {
                totalGet += item.netBalance
            } else if (item.netBalance < 0) {
                totalPay += Math.abs(item.netBalance)
            }
        }
        SummaryTotals(
            totalGet = totalGet,
            totalPay = totalPay,
            netBalance = totalGet - totalPay
        )
    }

    fun getTransactionsForContact(contactId: Long): Flow<List<Transaction>> {
        return transactionDao.getTransactionsForContact(contactId)
    }

    fun getContactById(contactId: Long): Flow<Contact?> {
        return contactDao.getContactById(contactId)
    }

    suspend fun getContactByIdSync(contactId: Long): Contact? {
        return contactDao.getContactByIdSync(contactId)
    }

    fun getTracesForEntity(entityType: String, entityId: Long): Flow<List<TraceLog>> {
        return traceLogDao.getTracesForEntity(entityType, entityId)
    }

    // --- CONTACT OPERATIONS ---

    suspend fun addContact(contact: Contact): Long {
        val id = contactDao.insertContact(contact)
        traceLogDao.insertTrace(
            TraceLog(
                entityType = "CONTACT",
                entityId = id,
                entityName = contact.name,
                action = "CREATE"
            )
        )
        return id
    }

    suspend fun updateContact(newContact: Contact, oldContact: Contact? = null) {
        contactDao.updateContact(newContact)
        val old = oldContact ?: contactDao.getContactByIdSync(newContact.id)
        if (old != null) {
            val changes = mutableListOf<String>()
            if (old.name != newContact.name) changes.add("Name: '${old.name}' -> '${newContact.name}'")
            if (old.mobileNumber != newContact.mobileNumber) changes.add("Mobile: '${old.mobileNumber ?: ""}' -> '${newContact.mobileNumber ?: ""}'")
            if (old.email != newContact.email) changes.add("Email: '${old.email ?: ""}' -> '${newContact.email ?: ""}'")
            if (old.categoryTag != newContact.categoryTag) changes.add("Category: '${old.categoryTag}' -> '${newContact.categoryTag}'")
            if (old.addressNotes != newContact.addressNotes) changes.add("Notes: '${old.addressNotes ?: ""}' -> '${newContact.addressNotes ?: ""}'")

            for (change in changes) {
                traceLogDao.insertTrace(
                    TraceLog(
                        entityType = "CONTACT",
                        entityId = newContact.id,
                        entityName = newContact.name,
                        action = "EDIT",
                        fieldChanged = change
                    )
                )
            }
        }
    }

    suspend fun setContactPinned(contactId: Long, isPinned: Boolean, name: String = "Contact") {
        contactDao.setPinned(contactId, isPinned)
        traceLogDao.insertTrace(
            TraceLog(
                entityType = "CONTACT",
                entityId = contactId,
                entityName = name,
                action = if (isPinned) "PIN" else "UNPIN",
                fieldChanged = "isPinned: ${!isPinned} -> $isPinned"
            )
        )
    }

    suspend fun archiveContact(contactId: Long) {
        contactDao.archiveContact(contactId)
    }

    suspend fun softDeleteContact(contactId: Long, name: String = "Contact") {
        val now = System.currentTimeMillis()
        contactDao.softDeleteContact(contactId, now)
        transactionDao.softDeleteTransactionsForContact(contactId, now)
        traceLogDao.insertTrace(
            TraceLog(
                entityType = "CONTACT",
                entityId = contactId,
                entityName = name,
                action = "DELETE"
            )
        )
    }

    suspend fun restoreContact(contactId: Long, name: String = "Contact") {
        contactDao.restoreContact(contactId)
        transactionDao.restoreTransactionsForContact(contactId)
        traceLogDao.insertTrace(
            TraceLog(
                entityType = "CONTACT",
                entityId = contactId,
                entityName = name,
                action = "RESTORE"
            )
        )
    }

    suspend fun deleteContactPermanently(contactId: Long, name: String = "Contact") {
        transactionDao.deleteTransactionsForContact(contactId)
        contactDao.deleteContactPermanently(contactId)
        traceLogDao.insertTrace(
            TraceLog(
                entityType = "CONTACT",
                entityId = contactId,
                entityName = name,
                action = "PURGE"
            )
        )
    }

    // --- TRANSACTION OPERATIONS ---

    suspend fun addTransaction(transaction: Transaction, contactName: String = ""): Long {
        val id = transactionDao.insertTransaction(transaction)
        if (transaction.collectionDueDate != null) {
            reminderDao.insertReminder(
                Reminder(
                    contactId = transaction.contactId,
                    transactionId = id,
                    reminderDate = transaction.collectionDueDate
                )
            )
        }
        val typeLabel = if (transaction.type == Transaction.TYPE_YOU_GOT) "YOU GOT" else "YOU GAVE"
        traceLogDao.insertTrace(
            TraceLog(
                entityType = "TRANSACTION",
                entityId = id,
                entityName = "$typeLabel ${transaction.amount} ($contactName)",
                action = "CREATE"
            )
        )
        return id
    }

    suspend fun updateTransaction(newTx: Transaction, oldTx: Transaction? = null, contactName: String = "") {
        transactionDao.updateTransaction(newTx)
        val old = oldTx ?: transactionDao.getTransactionById(newTx.id)
        if (old != null) {
            val changes = mutableListOf<Pair<String, Pair<String, String>>>()
            if (old.amount != newTx.amount) changes.add("amount" to (old.amount.toString() to newTx.amount.toString()))
            if (old.type != newTx.type) changes.add("type" to (old.type to newTx.type))
            if (old.paymentMode != newTx.paymentMode) changes.add("paymentMode" to (old.paymentMode to newTx.paymentMode))
            if (old.referenceNumber != newTx.referenceNumber) changes.add("referenceNumber" to ((old.referenceNumber ?: "") to (newTx.referenceNumber ?: "")))
            if (old.note != newTx.note) changes.add("note" to ((old.note ?: "") to (newTx.note ?: "")))

            for ((field, vals) in changes) {
                traceLogDao.insertTrace(
                    TraceLog(
                        entityType = "TRANSACTION",
                        entityId = newTx.id,
                        entityName = "Tx #${newTx.id} ($contactName)",
                        action = "EDIT",
                        fieldChanged = field,
                        oldValue = vals.first,
                        newValue = vals.second
                    )
                )
            }
        }
    }

    suspend fun softDeleteTransaction(id: Long, desc: String = "") {
        val now = System.currentTimeMillis()
        transactionDao.softDeleteTransaction(id, now)
        traceLogDao.insertTrace(
            TraceLog(
                entityType = "TRANSACTION",
                entityId = id,
                entityName = desc.ifEmpty { "Transaction #$id" },
                action = "DELETE"
            )
        )
    }

    suspend fun restoreTransaction(id: Long, desc: String = "") {
        transactionDao.restoreTransaction(id)
        traceLogDao.insertTrace(
            TraceLog(
                entityType = "TRANSACTION",
                entityId = id,
                entityName = desc.ifEmpty { "Transaction #$id" },
                action = "RESTORE"
            )
        )
    }

    suspend fun deleteTransactionPermanently(id: Long, desc: String = "") {
        transactionDao.deleteTransactionPermanently(id)
        traceLogDao.insertTrace(
            TraceLog(
                entityType = "TRANSACTION",
                entityId = id,
                entityName = desc.ifEmpty { "Transaction #$id" },
                action = "PURGE"
            )
        )
    }

    suspend fun getTransactionById(id: Long): Transaction? {
        return transactionDao.getTransactionById(id)
    }

    fun getTransactionsInRange(startDate: Long, endDate: Long): Flow<List<Transaction>> {
        return transactionDao.getTransactionsInRange(startDate, endDate)
    }

    suspend fun purgeOldTrash(retentionDays: Int) {
        val cutoff = System.currentTimeMillis() - (retentionDays.toLong() * 86400000L)

        // Find expired contacts to log in TraceLog before deletion
        val expiredContacts = contactDao.getExpiredDeletedContactsSync(cutoff)
        for (contact in expiredContacts) {
            traceLogDao.insertTrace(
                TraceLog(
                    entityType = "CONTACT",
                    entityId = contact.id,
                    entityName = contact.name,
                    action = "PURGE",
                    fieldChanged = "Auto-purged by WorkManager CleanupWorker (> $retentionDays days)"
                )
            )
        }

        // Find expired transactions to log in TraceLog before deletion
        val expiredTxs = transactionDao.getExpiredDeletedTransactionsSync(cutoff)
        for (tx in expiredTxs) {
            traceLogDao.insertTrace(
                TraceLog(
                    entityType = "TRANSACTION",
                    entityId = tx.id,
                    entityName = "Tx #${tx.id} (${tx.type} ${tx.amount})",
                    action = "PURGE",
                    fieldChanged = "Auto-purged by WorkManager CleanupWorker (> $retentionDays days)"
                )
            )
        }

        contactDao.purgeOldDeletedContacts(cutoff)
        transactionDao.purgeOldDeletedTransactions(cutoff)
    }

    // --- CATEGORY & PAYMENT MODE OPERATIONS ---

    suspend fun addCategory(category: CategoryItem) {
        categoryDao.insertCategory(category)
    }

    suspend fun updateCategory(category: CategoryItem) {
        categoryDao.updateCategory(category)
    }

    suspend fun archiveCategory(id: Long, isArchived: Boolean) {
        categoryDao.setArchived(id, isArchived)
    }

    suspend fun addPaymentMode(mode: PaymentModeItem) {
        paymentModeDao.insertPaymentMode(mode)
    }

    suspend fun updatePaymentMode(mode: PaymentModeItem) {
        paymentModeDao.updatePaymentMode(mode)
    }

    suspend fun archivePaymentMode(id: Long, isArchived: Boolean) {
        paymentModeDao.setArchived(id, isArchived)
    }

    // --- BACKUP / RESTORE COMMIT ---

    suspend fun restoreBackupData(
        contacts: List<Contact>,
        transactions: List<Transaction>,
        categories: List<CategoryItem>,
        paymentModes: List<PaymentModeItem>,
        traceLogs: List<TraceLog>,
        reminders: List<Reminder>,
        replaceExisting: Boolean
    ) {
        if (replaceExisting) {
            contactDao.clearAllContacts()
            transactionDao.clearAllTransactions()
            traceLogDao.clearAllTraces()
        }

        contactDao.insertContacts(contacts)
        transactionDao.insertTransactions(transactions)
        if (categories.isNotEmpty()) categoryDao.insertCategories(categories)
        if (paymentModes.isNotEmpty()) paymentModeDao.insertPaymentModes(paymentModes)
        if (traceLogs.isNotEmpty()) traceLogDao.insertTraces(traceLogs)

        traceLogDao.insertTrace(
            TraceLog(
                entityType = "SYSTEM",
                entityId = 0,
                entityName = "Full Database Restore",
                action = "RESTORE",
                newValue = "${contacts.size} contacts, ${transactions.size} transactions"
            )
        )
    }

    suspend fun seedSampleDataIfEmpty() {
        // Seed initial categories & payment modes if empty
        val seedCategories = listOf(
            CategoryItem(name = "Friend", iconName = "Person", tagColor = "Blue", sortOrder = 1),
            CategoryItem(name = "Family", iconName = "Home", tagColor = "Green", sortOrder = 2),
            CategoryItem(name = "Customer", iconName = "Store", tagColor = "Purple", sortOrder = 3),
            CategoryItem(name = "Supplier", iconName = "LocalShipping", tagColor = "Orange", sortOrder = 4),
            CategoryItem(name = "Other", iconName = "Category", tagColor = "Gray", sortOrder = 5)
        )
        val seedPaymentModes = listOf(
            PaymentModeItem(name = "Cash", iconName = "Payments", sortOrder = 1),
            PaymentModeItem(name = "UPI", iconName = "QrCode", sortOrder = 2),
            PaymentModeItem(name = "Bank Transfer", iconName = "AccountBalance", sortOrder = 3),
            PaymentModeItem(name = "Cheque", iconName = "Article", sortOrder = 4),
            PaymentModeItem(name = "Card", iconName = "CreditCard", sortOrder = 5),
            PaymentModeItem(name = "Other", iconName = "MoreHoriz", sortOrder = 6)
        )

        categoryDao.insertCategories(seedCategories)
        paymentModeDao.insertPaymentModes(seedPaymentModes)

        val existing = contactDao.getContactByIdSync(1)
        if (existing == null) {
            val now = System.currentTimeMillis()
            val dayMillis = 86400000L

            val c1Id = contactDao.insertContact(
                Contact(
                    name = "Rahul Sharma",
                    mobileNumber = "+91 98765 43210",
                    email = "rahul@example.com",
                    categoryTag = "Friend",
                    addressNotes = "Quarter 4B, MG Road",
                    isPinned = true
                )
            )
            val c2Id = contactDao.insertContact(
                Contact(
                    name = "Priya Patel",
                    mobileNumber = "+91 91234 56789",
                    email = "priya.design@gmail.com",
                    categoryTag = "Customer",
                    addressNotes = "Design Studio, Block C",
                    isPinned = false
                )
            )
            val c3Id = contactDao.insertContact(
                Contact(
                    name = "Ramesh Kumar (Kirana)",
                    mobileNumber = "+91 99887 76655",
                    categoryTag = "Supplier",
                    addressNotes = "Main Market Shop #12",
                    isPinned = false
                )
            )

            transactionDao.insertTransaction(
                Transaction(
                    contactId = c1Id,
                    type = Transaction.TYPE_YOU_GAVE,
                    amount = 5000.0,
                    transactionDate = now - (5 * dayMillis),
                    transactionTime = "10:30 AM",
                    paymentMode = "UPI",
                    referenceNumber = "UPI/4238910023",
                    note = "Dinner & cabs split"
                )
            )
            transactionDao.insertTransaction(
                Transaction(
                    contactId = c1Id,
                    type = Transaction.TYPE_YOU_GOT,
                    amount = 2500.0,
                    transactionDate = now - (2 * dayMillis),
                    transactionTime = "04:15 PM",
                    paymentMode = "UPI",
                    referenceNumber = "GPay-8839201",
                    note = "Partial repayment via GPay"
                )
            )

            transactionDao.insertTransaction(
                Transaction(
                    contactId = c2Id,
                    type = Transaction.TYPE_YOU_GAVE,
                    amount = 15000.0,
                    transactionDate = now - (10 * dayMillis),
                    transactionTime = "11:00 AM",
                    paymentMode = "Bank Transfer",
                    referenceNumber = "IMPS9984729102",
                    note = "Advance for UI consultancy",
                    collectionDueDate = now + (2 * dayMillis)
                )
            )
            transactionDao.insertTransaction(
                Transaction(
                    contactId = c2Id,
                    type = Transaction.TYPE_YOU_GOT,
                    amount = 3000.0,
                    transactionDate = now - (3 * dayMillis),
                    transactionTime = "02:30 PM",
                    paymentMode = "UPI",
                    referenceNumber = "UPI/9984729105",
                    note = "Milestone payment 1"
                )
            )

            transactionDao.insertTransaction(
                Transaction(
                    contactId = c3Id,
                    type = Transaction.TYPE_YOU_GOT,
                    amount = 1850.0,
                    transactionDate = now - (1 * dayMillis),
                    transactionTime = "07:45 PM",
                    paymentMode = "Cash",
                    note = "Monthly groceries on credit"
                )
            )

            reminderDao.insertReminder(
                Reminder(
                    contactId = c2Id,
                    reminderDate = now + (2 * dayMillis),
                    status = "PENDING"
                )
            )

            traceLogDao.insertTrace(TraceLog(entityType = "CONTACT", entityId = c1Id, entityName = "Rahul Sharma", action = "CREATE"))
            traceLogDao.insertTrace(TraceLog(entityType = "CONTACT", entityId = c2Id, entityName = "Priya Patel", action = "CREATE"))
            traceLogDao.insertTrace(TraceLog(entityType = "CONTACT", entityId = c3Id, entityName = "Ramesh Kumar (Kirana)", action = "CREATE"))
        }
    }
}
