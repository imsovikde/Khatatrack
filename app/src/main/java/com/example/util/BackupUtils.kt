package com.example.util

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.content.FileProvider
import com.example.data.model.CategoryItem
import com.example.data.model.Contact
import com.example.data.model.IncomeExpenseEntry
import com.example.data.model.PaymentModeItem
import com.example.data.model.Reminder
import com.example.data.model.TraceLog
import com.example.data.model.Transaction
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.File
import java.io.FileWriter
import java.io.InputStreamReader
import java.security.MessageDigest
import javax.crypto.Cipher
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.SecretKeySpec

data class BackupSummary(
    val contactCount: Int,
    val transactionCount: Int,
    val categoryCount: Int,
    val paymentModeCount: Int,
    val exportedAt: Long,
    val contacts: List<Contact>,
    val transactions: List<Transaction>,
    val categories: List<CategoryItem>,
    val paymentModes: List<PaymentModeItem>,
    val traceLogs: List<TraceLog>,
    val reminders: List<Reminder>,
    val incomeExpenseCount: Int,
    val incomeExpenseEntries: List<IncomeExpenseEntry>
)

object BackupUtils {

    private const val AES_KEY_STRING = "KhataTrackSecureKey2026#Encrypt!" // 32 chars for 256-bit AES
    private const val AES_IV_STRING  = "KhataTrackInitVec" // 16 chars for 128-bit IV

    private fun sha256(input: String): String {
        val digest = MessageDigest.getInstance("SHA-256")
        val hash = digest.digest(input.toByteArray(Charsets.UTF_8))
        return hash.joinToString("") { "%02x".format(it) }
    }

    private fun encryptAes(input: String): String {
        return try {
            val cipher = Cipher.getInstance("AES/CBC/PKCS5Padding")
            val keySpec = SecretKeySpec(AES_KEY_STRING.toByteArray(Charsets.UTF_8), "AES")
            val ivSpec = IvParameterSpec(AES_IV_STRING.toByteArray(Charsets.UTF_8))
            cipher.init(Cipher.ENCRYPT_MODE, keySpec, ivSpec)
            val encryptedBytes = cipher.doFinal(input.toByteArray(Charsets.UTF_8))
            android.util.Base64.encodeToString(encryptedBytes, android.util.Base64.NO_WRAP)
        } catch (e: Exception) {
            throw RuntimeException("Encryption failed", e)
        }
    }

    private fun decryptAes(input: String): String {
        return try {
            val cipher = Cipher.getInstance("AES/CBC/PKCS5Padding")
            val keySpec = SecretKeySpec(AES_KEY_STRING.toByteArray(Charsets.UTF_8), "AES")
            val ivSpec = IvParameterSpec(AES_IV_STRING.toByteArray(Charsets.UTF_8))
            cipher.init(Cipher.DECRYPT_MODE, keySpec, ivSpec)
            val decodedBytes = android.util.Base64.decode(input, android.util.Base64.NO_WRAP)
            String(cipher.doFinal(decodedBytes), Charsets.UTF_8)
        } catch (e: Exception) {
            throw RuntimeException("Decryption failed", e)
        }
    }

    fun generateBackupJson(
        contacts: List<Contact>,
        transactions: List<Transaction>,
        categories: List<CategoryItem>,
        paymentModes: List<PaymentModeItem>,
        traceLogs: List<TraceLog>,
        reminders: List<Reminder>,
        incomeExpenseEntries: List<IncomeExpenseEntry>,
        encrypt: Boolean
    ): String {
        val payloadObj = JSONObject()

        // Contacts
        val contactsArray = JSONArray()
        for (c in contacts) {
            val cObj = JSONObject()
            cObj.put("id", c.id)
            cObj.put("name", c.name)
            cObj.put("mobileNumber", c.mobileNumber ?: "")
            cObj.put("email", c.email ?: "")
            cObj.put("profilePhoto", c.profilePhoto ?: "")
            cObj.put("addressNotes", c.addressNotes ?: "")
            cObj.put("categoryTag", c.categoryTag)
            cObj.put("createdAt", c.createdAt)
            cObj.put("updatedAt", c.updatedAt)
            cObj.put("isPinned", c.isPinned)
            cObj.put("isArchived", c.isArchived)
            cObj.put("isDeleted", c.isDeleted)
            cObj.put("deletedAt", c.deletedAt ?: 0L)
            contactsArray.put(cObj)
        }
        payloadObj.put("contacts", contactsArray)

        // Transactions
        val txArray = JSONArray()
        for (t in transactions) {
            val tObj = JSONObject()
            tObj.put("id", t.id)
            tObj.put("contactId", t.contactId)
            tObj.put("type", t.type)
            tObj.put("amount", t.amount)
            tObj.put("transactionDate", t.transactionDate)
            tObj.put("transactionTime", t.transactionTime)
            tObj.put("paymentMode", t.paymentMode)
            tObj.put("note", t.note ?: "")
            tObj.put("attachmentPhoto", t.attachmentPhoto ?: "")
            tObj.put("referenceNumber", t.referenceNumber ?: "")
            tObj.put("collectionDueDate", t.collectionDueDate ?: 0L)
            tObj.put("createdAt", t.createdAt)
            tObj.put("updatedAt", t.updatedAt)
            tObj.put("isDeleted", t.isDeleted)
            tObj.put("deletedAt", t.deletedAt ?: 0L)
            txArray.put(tObj)
        }
        payloadObj.put("transactions", txArray)

        // Categories
        val catArray = JSONArray()
        for (cat in categories) {
            val catObj = JSONObject()
            catObj.put("id", cat.id)
            catObj.put("name", cat.name)
            catObj.put("iconName", cat.iconName)
            catObj.put("tagColor", cat.tagColor)
            catObj.put("sortOrder", cat.sortOrder)
            catObj.put("isArchived", cat.isArchived)
            catObj.put("isDeleted", cat.isDeleted)
            catArray.put(catObj)
        }
        payloadObj.put("categories", catArray)

        // Payment Modes
        val pmArray = JSONArray()
        for (pm in paymentModes) {
            val pmObj = JSONObject()
            pmObj.put("id", pm.id)
            pmObj.put("name", pm.name)
            pmObj.put("iconName", pm.iconName)
            pmObj.put("sortOrder", pm.sortOrder)
            pmObj.put("isArchived", pm.isArchived)
            pmObj.put("isDeleted", pm.isDeleted)
            pmArray.put(pmObj)
        }
        payloadObj.put("paymentModes", pmArray)

        // Trace Logs
        val traceArray = JSONArray()
        for (tr in traceLogs) {
            val trObj = JSONObject()
            trObj.put("id", tr.id)
            trObj.put("entityType", tr.entityType)
            trObj.put("entityId", tr.entityId)
            trObj.put("entityName", tr.entityName)
            trObj.put("action", tr.action)
            trObj.put("fieldChanged", tr.fieldChanged ?: "")
            trObj.put("oldValue", tr.oldValue ?: "")
            trObj.put("newValue", tr.newValue ?: "")
            trObj.put("timestamp", tr.timestamp)
            traceArray.put(trObj)
        }
        payloadObj.put("traceLogs", traceArray)

        // Reminders
        val remArray = JSONArray()
        for (r in reminders) {
            val rObj = JSONObject()
            rObj.put("id", r.id)
            rObj.put("contactId", r.contactId)
            rObj.put("transactionId", r.transactionId ?: 0L)
            rObj.put("reminderDate", r.reminderDate)
            rObj.put("status", r.status)
            rObj.put("createdAt", r.createdAt)
            remArray.put(rObj)
        }
        payloadObj.put("reminders", remArray)

        
        // Income/Expense
        val incArray = JSONArray()
        for (ie in incomeExpenseEntries) {
            val ieObj = JSONObject()
            ieObj.put("id", ie.id)
            ieObj.put("type", ie.type)
            ieObj.put("amount", ie.amount)
            ieObj.put("currency", ie.currency)
            ieObj.put("transactionDate", ie.transactionDate)
            ieObj.put("transactionTime", ie.transactionTime)
            ieObj.put("paymentMode", ie.paymentMode)
            ieObj.put("transactionRefId", ie.transactionRefId ?: "")
            ieObj.put("categoryTag", ie.categoryTag)
            ieObj.put("note", ie.note ?: "")
            ieObj.put("attachmentPhoto", ie.attachmentPhoto ?: "")
            ieObj.put("collectionDueDate", ie.collectionDueDate ?: 0L)
            ieObj.put("createdAt", ie.createdAt)
            ieObj.put("updatedAt", ie.updatedAt)
            ieObj.put("isDeleted", ie.isDeleted)
            ieObj.put("deletedAt", ie.deletedAt ?: 0L)
            incArray.put(ieObj)
        }
        payloadObj.put("incomeExpenseEntries", incArray)
        
        val rawPayloadString = payloadObj.toString()
        val checksumHash = sha256(rawPayloadString)

        val finalPayload = if (encrypt) encryptAes(rawPayloadString) else rawPayloadString

        val root = JSONObject()
        root.put("app", "KhataTrack")
        root.put("schemaVersion", 1)
        root.put("exportedAt", System.currentTimeMillis())
        root.put("encrypted", encrypt)
        root.put("checksum", checksumHash)
        root.put("payload", finalPayload)

        return root.toString(2)
    }

    fun exportBackupFile(context: Context, jsonContent: String, contacts: List<Contact>, transactions: List<Transaction>) {
        try {
            val fileName = "KhataTrack_Backup_${DateTimeUtils.formatDate(System.currentTimeMillis()).replace(" ", "_")}.ktb.zip"
            val file = File(context.cacheDir, fileName)
            
            val fos = java.io.FileOutputStream(file)
            val zos = java.util.zip.ZipOutputStream(fos)
            
            // 1. JSON Payload
            zos.putNextEntry(java.util.zip.ZipEntry("backup.json"))
            zos.write(jsonContent.toByteArray(Charsets.UTF_8))
            zos.closeEntry()
            
            // 2. Attachments
            val mediaUris = mutableSetOf<String>()
            contacts.forEach { it.profilePhoto?.let { p -> mediaUris.add(p) } }
            transactions.forEach { it.attachmentPhoto?.let { p -> mediaUris.add(p) } }
            
            for (uriStr in mediaUris) {
                try {
                    val uri = Uri.parse(uriStr)
                    val inputStream = context.contentResolver.openInputStream(uri)
                    if (inputStream != null) {
                        val hashName = sha256(uriStr)
                        zos.putNextEntry(java.util.zip.ZipEntry("attachments/$hashName"))
                        inputStream.copyTo(zos)
                        zos.closeEntry()
                        inputStream.close()
                    }
                } catch (e: Exception) {
                    // Ignore missing files
                }
            }
            zos.close()
            fos.close()

            val uri = try {
                FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
            } catch (e: Exception) {
                Uri.fromFile(file)
            }

            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "application/zip"
                putExtra(Intent.EXTRA_STREAM, uri)
                putExtra(Intent.EXTRA_SUBJECT, "KhataTrack Backup")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(Intent.createChooser(intent, "Save or Share KhataTrack Backup"))
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun parseAndValidateBackup(context: Context, uri: Uri): Pair<BackupSummary?, String?> {
        try {
            val inputStream = context.contentResolver.openInputStream(uri)
                ?: return Pair(null, "Unable to read selected file.")
            
            val zis = java.util.zip.ZipInputStream(inputStream)
            var jsonString = ""
            val extractedFiles = mutableMapOf<String, File>()
            
            try {
                var entry = zis.nextEntry
                while (entry != null) {
                    if (entry.name == "backup.json") {
                        jsonString = String(zis.readBytes(), Charsets.UTF_8)
                    } else if (entry.name.startsWith("attachments/") && !entry.isDirectory) {
                        val tempFile = File(context.cacheDir, entry.name.substringAfterLast("/"))
                        val fos = java.io.FileOutputStream(tempFile)
                        zis.copyTo(fos)
                        fos.close()
                        extractedFiles[tempFile.name] = tempFile
                    }
                    zis.closeEntry()
                    entry = zis.nextEntry
                }
            } catch (e: Exception) {
                // Not a zip or corrupted
            }
            zis.close()
            
            if (jsonString.isEmpty()) {
                // Fallback to legacy JSON format
                val rawStream = context.contentResolver.openInputStream(uri) ?: return Pair(null, "Unable to read.")
                val reader = BufferedReader(InputStreamReader(rawStream))
                try {
                    jsonString = reader.readText()
                } finally {
                    reader.close()
                    rawStream.close()
                }
            }


            val rootObj = JSONObject(jsonString)
            val appSig = rootObj.optString("app")
            if (appSig != "KhataTrack") {
                return Pair(null, "This file can't be imported — it wasn't exported from KhataTrack or has been modified.")
            }

            val checksum = rootObj.optString("checksum")
            val isEncrypted = rootObj.optBoolean("encrypted", false)
            val rawPayload = rootObj.optString("payload")

            val payloadJsonString = if (isEncrypted) decryptAes(rawPayload) else rawPayload

            val computedChecksum = sha256(payloadJsonString)
            if (checksum.isNotEmpty() && checksum != computedChecksum) {
                return Pair(null, "This file can't be imported — it wasn't exported from KhataTrack or has been modified.")
            }

            val payloadObj = JSONObject(payloadJsonString)
            val exportedAt = rootObj.optLong("exportedAt", System.currentTimeMillis())

            val contacts = mutableListOf<Contact>()
            val cArr = payloadObj.optJSONArray("contacts") ?: JSONArray()
            for (i in 0 until cArr.length()) {
                val o = cArr.getJSONObject(i)
                var pPhoto = o.optString("profilePhoto").ifEmpty { null }
                if (pPhoto != null) {
                    val hash = sha256(pPhoto)
                    if (extractedFiles.containsKey(hash)) {
                        pPhoto = Uri.fromFile(extractedFiles[hash]).toString()
                    } else {
                        pPhoto = null
                    }
                }
                contacts.add(
                    Contact(
                        id = o.optLong("id", 0L),
                        name = o.optString("name"),
                        mobileNumber = o.optString("mobileNumber").ifEmpty { null },
                        email = o.optString("email").ifEmpty { null },
                        profilePhoto = pPhoto,
                        addressNotes = o.optString("addressNotes").ifEmpty { null },
                        categoryTag = o.optString("categoryTag", "Friend"),
                        createdAt = o.optLong("createdAt"),
                        updatedAt = o.optLong("updatedAt"),
                        isPinned = o.optBoolean("isPinned", false),
                        isArchived = o.optBoolean("isArchived", false),
                        isDeleted = o.optBoolean("isDeleted", false),
                        deletedAt = if (o.has("deletedAt") && o.getLong("deletedAt") != 0L) o.getLong("deletedAt") else null
                    )
                )
            }

            val transactions = mutableListOf<Transaction>()
            val tArr = payloadObj.optJSONArray("transactions") ?: JSONArray()
            for (i in 0 until tArr.length()) {
                val o = tArr.getJSONObject(i)
                var aPhoto = o.optString("attachmentPhoto").ifEmpty { null }
                if (aPhoto != null) {
                    val hash = sha256(aPhoto)
                    if (extractedFiles.containsKey(hash)) {
                        aPhoto = Uri.fromFile(extractedFiles[hash]).toString()
                    } else {
                        aPhoto = null
                    }
                }
                transactions.add(
                    Transaction(
                        id = o.optLong("id", 0L),
                        contactId = o.optLong("contactId"),
                        type = o.optString("type"),
                        amount = o.optDouble("amount"),
                        transactionDate = o.optLong("transactionDate"),
                        transactionTime = o.optString("transactionTime"),
                        paymentMode = o.optString("paymentMode", "Cash"),
                        note = o.optString("note").ifEmpty { null },
                        attachmentPhoto = aPhoto,
                        referenceNumber = o.optString("referenceNumber").ifEmpty { null },
                        collectionDueDate = if (o.has("collectionDueDate") && o.getLong("collectionDueDate") != 0L) o.getLong("collectionDueDate") else null,
                        createdAt = o.optLong("createdAt"),
                        updatedAt = o.optLong("updatedAt"),
                        isDeleted = o.optBoolean("isDeleted", false),
                        deletedAt = if (o.has("deletedAt") && o.getLong("deletedAt") != 0L) o.getLong("deletedAt") else null
                    )
                )
            }

            // Parse Categories
            val categories = mutableListOf<CategoryItem>()
            val catArr = payloadObj.optJSONArray("categories") ?: JSONArray()
            for (i in 0 until catArr.length()) {
                val o = catArr.getJSONObject(i)
                categories.add(
                    CategoryItem(
                        id = o.optLong("id", 0L),
                        name = o.optString("name"),
                        iconName = o.optString("iconName", "Category"),
                        tagColor = o.optString("tagColor", "Gray"),
                        sortOrder = o.optInt("sortOrder", 0),
                        isArchived = o.optBoolean("isArchived", false),
                        isDeleted = o.optBoolean("isDeleted", false)
                    )
                )
            }

            // Parse Payment Modes
            val paymentModes = mutableListOf<PaymentModeItem>()
            val pmArr = payloadObj.optJSONArray("paymentModes") ?: JSONArray()
            for (i in 0 until pmArr.length()) {
                val o = pmArr.getJSONObject(i)
                paymentModes.add(
                    PaymentModeItem(
                        id = o.optLong("id", 0L),
                        name = o.optString("name"),
                        iconName = o.optString("iconName", "Payments"),
                        sortOrder = o.optInt("sortOrder", 0),
                        isArchived = o.optBoolean("isArchived", false),
                        isDeleted = o.optBoolean("isDeleted", false)
                    )
                )
            }

            // Parse Trace Logs
            val traceLogs = mutableListOf<TraceLog>()
            val trArr = payloadObj.optJSONArray("traceLogs") ?: JSONArray()
            for (i in 0 until trArr.length()) {
                val o = trArr.getJSONObject(i)
                traceLogs.add(
                    TraceLog(
                        id = o.optLong("id", 0L),
                        entityType = o.optString("entityType"),
                        entityId = o.optLong("entityId"),
                        entityName = o.optString("entityName"),
                        action = o.optString("action"),
                        fieldChanged = o.optString("fieldChanged").ifEmpty { null },
                        oldValue = o.optString("oldValue").ifEmpty { null },
                        newValue = o.optString("newValue").ifEmpty { null },
                        timestamp = o.optLong("timestamp")
                    )
                )
            }

            // Parse Reminders
            val reminders = mutableListOf<Reminder>()
            val rArr = payloadObj.optJSONArray("reminders") ?: JSONArray()
            for (i in 0 until rArr.length()) {
                val o = rArr.getJSONObject(i)
                reminders.add(
                    Reminder(
                        id = o.optLong("id", 0L),
                        contactId = o.optLong("contactId"),
                        transactionId = if (o.has("transactionId") && o.getLong("transactionId") != 0L) o.getLong("transactionId") else null,
                        reminderDate = o.optLong("reminderDate"),
                        status = o.optString("status", "PENDING"),
                        createdAt = o.optLong("createdAt")
                    )
                )
            }

            // Parse Income/Expense Entries
            val incomeExpenseEntries = mutableListOf<com.example.data.model.IncomeExpenseEntry>()
            val ieArr = payloadObj.optJSONArray("incomeExpenseEntries") ?: JSONArray()
            for (i in 0 until ieArr.length()) {
                val o = ieArr.getJSONObject(i)
                incomeExpenseEntries.add(
                    com.example.data.model.IncomeExpenseEntry(
                        id = o.optLong("id", 0L),
                        type = o.optString("type"),
                        amount = o.optDouble("amount"),
                        currency = o.optString("currency", "INR"),
                        transactionDate = o.optLong("transactionDate"),
                        transactionTime = o.optString("transactionTime", ""),
                        paymentMode = o.optString("paymentMode", "Cash"),
                        transactionRefId = o.optString("transactionRefId").ifEmpty { null },
                        categoryTag = o.optString("categoryTag", "General"),
                        note = o.optString("note").ifEmpty { null },
                        attachmentPhoto = o.optString("attachmentPhoto").ifEmpty { null },
                        collectionDueDate = if (o.has("collectionDueDate") && o.getLong("collectionDueDate") != 0L) o.getLong("collectionDueDate") else null,
                        createdAt = o.optLong("createdAt"),
                        updatedAt = o.optLong("updatedAt"),
                        isDeleted = o.optBoolean("isDeleted", false),
                        deletedAt = if (o.has("deletedAt") && o.getLong("deletedAt") != 0L) o.getLong("deletedAt") else null
                    )
                )
            }

            val summary = BackupSummary(
                contactCount = contacts.size,
                transactionCount = transactions.size,
                categoryCount = categories.size,
                paymentModeCount = paymentModes.size,
                exportedAt = exportedAt,
                contacts = contacts,
                transactions = transactions,
                categories = categories,
                paymentModes = paymentModes,
                traceLogs = traceLogs,
                reminders = reminders,
                incomeExpenseCount = incomeExpenseEntries.size,
                incomeExpenseEntries = incomeExpenseEntries
            )

            return Pair(summary, null)

        } catch (e: Exception) {
            e.printStackTrace()
            return Pair(null, "This file can't be imported — it wasn't exported from KhataTrack or has been modified.")
        }
    }
}
