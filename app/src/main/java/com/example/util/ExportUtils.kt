package com.example.util

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.content.FileProvider
import com.example.data.model.Contact
import com.example.data.model.Transaction
import java.io.File
import java.io.FileWriter

object ExportUtils {

    fun exportStatementCsv(
        context: Context,
        contact: Contact,
        transactions: List<Transaction>
    ) {
        try {
            val fileName = "KhataTrack_${contact.name.replace(" ", "_")}_Statement.csv"
            val file = File(context.cacheDir, fileName)
            val writer = FileWriter(file)

            writer.append("Date,Time,Type,Amount (INR),Payment Mode,Note\n")
            for (tx in transactions) {
                val dateStr = DateTimeUtils.formatDate(tx.transactionDate)
                val typeStr = if (tx.type == Transaction.TYPE_YOU_GOT) "YOU GOT (Credit)" else "YOU GAVE (Debit)"
                val noteClean = (tx.note ?: "").replace(",", ";")
                writer.append("$dateStr,${tx.transactionTime},$typeStr,${tx.amount},${tx.paymentMode},$noteClean\n")
            }
            writer.flush()
            writer.close()

            shareFile(context, file, "text/csv", "Share Contact Statement (CSV)")
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun exportReportsCsv(
        context: Context,
        transactions: List<Transaction>
    ) {
        try {
            val fileName = "KhataTrack_Report_${System.currentTimeMillis()}.csv"
            val file = File(context.cacheDir, fileName)
            val writer = FileWriter(file)

            writer.append("Date,Time,Type,Amount (INR),Payment Mode,Note\n")
            for (tx in transactions) {
                val dateStr = DateTimeUtils.formatDate(tx.transactionDate)
                val typeStr = if (tx.type == Transaction.TYPE_YOU_GOT) "YOU GOT" else "YOU GAVE"
                val noteClean = (tx.note ?: "").replace(",", ";")
                writer.append("$dateStr,${tx.transactionTime},$typeStr,${tx.amount},${tx.paymentMode},$noteClean\n")
            }
            writer.flush()
            writer.close()

            shareFile(context, file, "text/csv", "Share Summary Report (CSV)")
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun shareFile(context: Context, file: File, mimeType: String, title: String) {
        val uri = try {
            FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        } catch (e: Exception) {
            Uri.fromFile(file)
        }

        val intent = Intent(Intent.ACTION_SEND).apply {
            type = mimeType
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(intent, title))
    }
}
