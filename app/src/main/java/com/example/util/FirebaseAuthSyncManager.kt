package com.example.util

import android.content.Context
import com.example.data.model.Contact
import com.example.data.model.ContactWithBalance
import com.example.data.model.Transaction
import com.example.data.repository.KhataRepository
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

data class UserSyncState(
    val isSignedIn: Boolean = false,
    val userId: String? = null,
    val email: String? = null,
    val isAnonymous: Boolean = true,
    val syncStatus: String = "Local Mode",
    val isFirebaseInitialized: Boolean = false
)

object FirebaseAuthSyncManager {

    private var firebaseAuth: FirebaseAuth? = null
    private var firestore: FirebaseFirestore? = null

    fun isInitialized(context: Context): Boolean {
        return try {
            if (FirebaseApp.getApps(context).isNotEmpty()) {
                if (firebaseAuth == null) {
                    firebaseAuth = FirebaseAuth.getInstance()
                }
                if (firestore == null) {
                    firestore = FirebaseFirestore.getInstance()
                }
                true
            } else {
                false
            }
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    fun getSyncState(context: Context): UserSyncState {
        val hasFirebase = isInitialized(context)
        if (!hasFirebase || firebaseAuth == null) {
            return UserSyncState(
                isSignedIn = false,
                syncStatus = "Local Mode (Room DB active)",
                isFirebaseInitialized = false
            )
        }

        val currentUser = firebaseAuth?.currentUser
        return if (currentUser != null) {
            UserSyncState(
                isSignedIn = true,
                userId = currentUser.uid,
                email = currentUser.email ?: "Anonymous User",
                isAnonymous = currentUser.isAnonymous,
                syncStatus = "Synced with Firestore",
                isFirebaseInitialized = true
            )
        } else {
            UserSyncState(
                isSignedIn = false,
                syncStatus = "Signed Out",
                isFirebaseInitialized = true
            )
        }
    }

    suspend fun signInAnonymously(context: Context): Result<String> = withContext(Dispatchers.IO) {
        if (!isInitialized(context)) {
            return@withContext Result.failure(Exception("Firebase app is not initialized. Operating in local Room DB mode."))
        }
        try {
            val result = firebaseAuth?.signInAnonymously()?.await()
            val uid = result?.user?.uid ?: "unknown"
            Result.success(uid)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun signInWithEmail(context: Context, email: String, pass: String): Result<String> = withContext(Dispatchers.IO) {
        if (!isInitialized(context)) {
            return@withContext Result.failure(Exception("Firebase app is not initialized."))
        }
        try {
            val result = firebaseAuth?.signInWithEmailAndPassword(email, pass)?.await()
            val uid = result?.user?.uid ?: "unknown"
            Result.success(uid)
        } catch (e: Exception) {
            try {
                val createResult = firebaseAuth?.createUserWithEmailAndPassword(email, pass)?.await()
                val uid = createResult?.user?.uid ?: "unknown"
                Result.success(uid)
            } catch (createErr: Exception) {
                Result.failure(e)
            }
        }
    }

    fun signOut(context: Context) {
        if (isInitialized(context)) {
            firebaseAuth?.signOut()
        }
    }

    // --- FIRESTORE TWO-WAY SYNC FOR ROOM ENTITIES (CONTACTS & TRANSACTIONS) ---

    suspend fun pushLocalToFirestore(
        context: Context,
        repository: KhataRepository
    ): Result<String> = withContext(Dispatchers.IO) {
        if (!isInitialized(context)) {
            return@withContext Result.success("Saved to local Room DB (Local mode).")
        }
        val user = firebaseAuth?.currentUser
            ?: return@withContext Result.failure(Exception("Not signed in to Firebase Cloud"))

        try {
            val db = firestore ?: FirebaseFirestore.getInstance()
            val userRef = db.collection("users").document(user.uid)

            val contacts = repository.getAllContactsForSync()
            val transactions = repository.getAllTransactionsForSync()

            val batch = db.batch()

            // Push contacts
            for (c in contacts) {
                val docRef = userRef.collection("contacts").document(c.id.toString())
                val contactMap = mapOf(
                    "id" to c.id,
                    "name" to c.name,
                    "mobileNumber" to (c.mobileNumber ?: ""),
                    "email" to (c.email ?: ""),
                    "profilePhoto" to (c.profilePhoto ?: ""),
                    "addressNotes" to (c.addressNotes ?: ""),
                    "categoryTag" to c.categoryTag,
                    "createdAt" to c.createdAt,
                    "updatedAt" to c.updatedAt,
                    "isPinned" to c.isPinned,
                    "isArchived" to c.isArchived,
                    "isDeleted" to c.isDeleted,
                    "deletedAt" to (c.deletedAt ?: 0L)
                )
                batch.set(docRef, contactMap, SetOptions.merge())
            }

            // Push transactions
            for (t in transactions) {
                val docRef = userRef.collection("transactions").document(t.id.toString())
                val txMap = mapOf(
                    "id" to t.id,
                    "contactId" to t.contactId,
                    "type" to t.type,
                    "amount" to t.amount,
                    "transactionDate" to t.transactionDate,
                    "transactionTime" to t.transactionTime,
                    "paymentMode" to t.paymentMode,
                    "note" to (t.note ?: ""),
                    "attachmentPhoto" to (t.attachmentPhoto ?: ""),
                    "referenceNumber" to (t.referenceNumber ?: ""),
                    "collectionDueDate" to (t.collectionDueDate ?: 0L),
                    "createdAt" to t.createdAt,
                    "updatedAt" to t.updatedAt,
                    "isDeleted" to t.isDeleted,
                    "deletedAt" to (t.deletedAt ?: 0L)
                )
                batch.set(docRef, txMap, SetOptions.merge())
            }

            // Update parent user document metadata
            val userMetadata = mapOf(
                "lastSyncedAt" to System.currentTimeMillis(),
                "contactCount" to contacts.size,
                "transactionCount" to transactions.size,
                "email" to (user.email ?: "Anonymous")
            )
            batch.set(userRef, userMetadata, SetOptions.merge())

            batch.commit().await()

            Result.success("Pushed ${contacts.size} contacts and ${transactions.size} transactions to Firestore!")
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun pullFirestoreToLocal(
        context: Context,
        repository: KhataRepository
    ): Result<String> = withContext(Dispatchers.IO) {
        if (!isInitialized(context)) {
            return@withContext Result.failure(Exception("Firebase app is not initialized."))
        }
        val user = firebaseAuth?.currentUser
            ?: return@withContext Result.failure(Exception("Not signed in to Firebase"))

        try {
            val db = firestore ?: FirebaseFirestore.getInstance()
            val userRef = db.collection("users").document(user.uid)

            // 1. Pull contacts
            val contactsSnap = userRef.collection("contacts").get().await()
            val pulledContacts = mutableListOf<Contact>()
            for (doc in contactsSnap.documents) {
                val data = doc.data ?: continue
                val id = (data["id"] as? Number)?.toLong() ?: doc.id.toLongOrNull() ?: continue
                val name = data["name"] as? String ?: "Contact"
                val mobileNumber = (data["mobileNumber"] as? String)?.ifBlank { null }
                val email = (data["email"] as? String)?.ifBlank { null }
                val profilePhoto = (data["profilePhoto"] as? String)?.ifBlank { null }
                val addressNotes = (data["addressNotes"] as? String)?.ifBlank { null }
                val categoryTag = data["categoryTag"] as? String ?: "Friend"
                val createdAt = (data["createdAt"] as? Number)?.toLong() ?: System.currentTimeMillis()
                val updatedAt = (data["updatedAt"] as? Number)?.toLong() ?: System.currentTimeMillis()
                val isPinned = data["isPinned"] as? Boolean ?: false
                val isArchived = data["isArchived"] as? Boolean ?: false
                val isDeleted = data["isDeleted"] as? Boolean ?: false
                val deletedAt = (data["deletedAt"] as? Number)?.toLong()?.let { if (it == 0L) null else it }

                pulledContacts.add(
                    Contact(
                        id = id,
                        name = name,
                        mobileNumber = mobileNumber,
                        email = email,
                        profilePhoto = profilePhoto,
                        addressNotes = addressNotes,
                        categoryTag = categoryTag,
                        createdAt = createdAt,
                        updatedAt = updatedAt,
                        isPinned = isPinned,
                        isArchived = isArchived,
                        isDeleted = isDeleted,
                        deletedAt = deletedAt
                    )
                )
            }

            // 2. Pull transactions
            val txSnap = userRef.collection("transactions").get().await()
            val pulledTransactions = mutableListOf<Transaction>()
            for (doc in txSnap.documents) {
                val data = doc.data ?: continue
                val id = (data["id"] as? Number)?.toLong() ?: doc.id.toLongOrNull() ?: continue
                val contactId = (data["contactId"] as? Number)?.toLong() ?: continue
                val type = data["type"] as? String ?: Transaction.TYPE_YOU_GAVE
                val amount = (data["amount"] as? Number)?.toDouble() ?: 0.0
                val transactionDate = (data["transactionDate"] as? Number)?.toLong() ?: System.currentTimeMillis()
                val transactionTime = data["transactionTime"] as? String ?: ""
                val paymentMode = data["paymentMode"] as? String ?: "Cash"
                val note = (data["note"] as? String)?.ifBlank { null }
                val attachmentPhoto = (data["attachmentPhoto"] as? String)?.ifBlank { null }
                val referenceNumber = (data["referenceNumber"] as? String)?.ifBlank { null }
                val collectionDueDate = (data["collectionDueDate"] as? Number)?.toLong()?.let { if (it == 0L) null else it }
                val createdAt = (data["createdAt"] as? Number)?.toLong() ?: System.currentTimeMillis()
                val updatedAt = (data["updatedAt"] as? Number)?.toLong() ?: System.currentTimeMillis()
                val isDeleted = data["isDeleted"] as? Boolean ?: false
                val deletedAt = (data["deletedAt"] as? Number)?.toLong()?.let { if (it == 0L) null else it }

                pulledTransactions.add(
                    Transaction(
                        id = id,
                        contactId = contactId,
                        type = type,
                        amount = amount,
                        transactionDate = transactionDate,
                        transactionTime = transactionTime,
                        paymentMode = paymentMode,
                        note = note,
                        attachmentPhoto = attachmentPhoto,
                        referenceNumber = referenceNumber,
                        collectionDueDate = collectionDueDate,
                        createdAt = createdAt,
                        updatedAt = updatedAt,
                        isDeleted = isDeleted,
                        deletedAt = deletedAt
                    )
                )
            }

            // Insert pulled data into local Room database
            if (pulledContacts.isNotEmpty()) {
                repository.insertContactsFromSync(pulledContacts)
            }
            if (pulledTransactions.isNotEmpty()) {
                repository.insertTransactionsFromSync(pulledTransactions)
            }

            Result.success("Downloaded ${pulledContacts.size} contacts and ${pulledTransactions.size} transactions from Firestore!")
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun fullSyncWithFirestore(
        context: Context,
        repository: KhataRepository
    ): Result<String> = withContext(Dispatchers.IO) {
        if (!isInitialized(context)) {
            return@withContext Result.success("Room database active locally.")
        }
        val user = firebaseAuth?.currentUser
        if (user == null) {
            // Auto sign in anonymously if not signed in
            val authRes = signInAnonymously(context)
            if (authRes.isFailure) {
                return@withContext Result.failure(authRes.exceptionOrNull() ?: Exception("Authentication failed"))
            }
        }

        // 1. First pull from cloud to update local Room DB with cloud changes
        val pullRes = pullFirestoreToLocal(context, repository)

        // 2. Then push local Room DB to cloud to ensure all local changes are backed up
        val pushRes = pushLocalToFirestore(context, repository)

        if (pushRes.isSuccess) {
            val pulledInfo = pullRes.getOrDefault("Local synced.")
            val pushedInfo = pushRes.getOrThrow()
            Result.success("$pulledInfo | $pushedInfo")
        } else {
            pushRes
        }
    }

    suspend fun syncLedgerSummaryToFirestore(
        context: Context,
        contacts: List<ContactWithBalance>
    ): Result<String> = withContext(Dispatchers.IO) {
        if (!isInitialized(context)) {
            return@withContext Result.success("Synced to local database (Local Room DB active).")
        }
        val user = firebaseAuth?.currentUser
            ?: return@withContext Result.failure(Exception("Not signed in to Firebase"))

        try {
            val db = firestore ?: FirebaseFirestore.getInstance()
            val userDoc = db.collection("users").document(user.uid)

            val contactsMapList = contacts.map { c ->
                mapOf(
                    "id" to c.contact.id,
                    "name" to c.contact.name,
                    "mobileNumber" to (c.contact.mobileNumber ?: ""),
                    "netBalance" to c.netBalance,
                    "isPinned" to c.contact.isPinned,
                    "lastActivity" to c.lastActivityTime
                )
            }

            userDoc.set(
                mapOf(
                    "updatedAt" to System.currentTimeMillis(),
                    "totalContacts" to contacts.size,
                    "contacts" to contactsMapList
                ),
                SetOptions.merge()
            ).await()

            Result.success("Successfully backed up ${contacts.size} contacts summary to Firestore!")
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
