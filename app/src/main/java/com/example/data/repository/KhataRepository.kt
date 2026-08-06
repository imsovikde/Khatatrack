package com.example.data.repository

import com.example.data.dao.ContactDao
import com.example.data.dao.ReminderDao
import com.example.data.dao.TransactionDao
import com.example.data.model.Contact
import com.example.data.model.ContactWithBalance
import com.example.data.model.Reminder
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
    private val reminderDao: ReminderDao
) {
    val allContacts: Flow<List<Contact>> = contactDao.getAllContacts()
    val allTransactions: Flow<List<Transaction>> = transactionDao.getAllTransactions()
    val pendingReminders: Flow<List<Reminder>> = reminderDao.getPendingReminders()

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

    suspend fun addContact(contact: Contact): Long {
        return contactDao.insertContact(contact)
    }

    suspend fun updateContact(contact: Contact) {
        contactDao.updateContact(contact)
    }

    suspend fun setContactPinned(contactId: Long, isPinned: Boolean) {
        contactDao.setPinned(contactId, isPinned)
    }

    suspend fun archiveContact(contactId: Long) {
        contactDao.archiveContact(contactId)
    }

    suspend fun deleteContactPermanently(contactId: Long) {
        contactDao.deleteContactPermanently(contactId)
    }

    suspend fun addTransaction(transaction: Transaction): Long {
        val id = transactionDao.insertTransaction(transaction)
        // If collection due date set, create pending reminder
        if (transaction.collectionDueDate != null) {
            reminderDao.insertReminder(
                Reminder(
                    contactId = transaction.contactId,
                    transactionId = id,
                    reminderDate = transaction.collectionDueDate
                )
            )
        }
        return id
    }

    suspend fun updateTransaction(transaction: Transaction) {
        transactionDao.updateTransaction(transaction)
    }

    suspend fun deleteTransaction(id: Long) {
        transactionDao.deleteTransaction(id)
    }

    suspend fun getTransactionById(id: Long): Transaction? {
        return transactionDao.getTransactionById(id)
    }

    fun getTransactionsInRange(startDate: Long, endDate: Long): Flow<List<Transaction>> {
        return transactionDao.getTransactionsInRange(startDate, endDate)
    }

    suspend fun addReminder(reminder: Reminder): Long {
        return reminderDao.insertReminder(reminder)
    }

    suspend fun updateReminder(reminder: Reminder) {
        reminderDao.updateReminder(reminder)
    }

    suspend fun deleteReminder(id: Long) {
        reminderDao.deleteReminder(id)
    }

    suspend fun seedSampleDataIfEmpty() {
        // Seed initial data if empty
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

            // Seed transactions for Rahul (Net: You'll Get ₹2,500)
            transactionDao.insertTransaction(
                Transaction(
                    contactId = c1Id,
                    type = Transaction.TYPE_YOU_GAVE,
                    amount = 5000.0,
                    transactionDate = now - (5 * dayMillis),
                    transactionTime = "10:30 AM",
                    paymentMode = "UPI",
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
                    note = "Partial repayment via GPay"
                )
            )

            // Seed transactions for Priya (Net: You'll Get ₹12,000)
            transactionDao.insertTransaction(
                Transaction(
                    contactId = c2Id,
                    type = Transaction.TYPE_YOU_GAVE,
                    amount = 15000.0,
                    transactionDate = now - (10 * dayMillis),
                    transactionTime = "11:00 AM",
                    paymentMode = "Bank Transfer",
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
                    note = "Milestone payment 1"
                )
            )

            // Seed transactions for Ramesh (Net: You'll Pay ₹1,850)
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

            // Seed reminder
            reminderDao.insertReminder(
                Reminder(
                    contactId = c2Id,
                    reminderDate = now + (2 * dayMillis),
                    status = "PENDING"
                )
            )
        }
    }
}
