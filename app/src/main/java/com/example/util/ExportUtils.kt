package com.example.util

import android.content.Context
import android.content.Intent
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import android.net.Uri
import androidx.core.content.FileProvider
import com.example.data.model.Contact
import com.example.data.model.Transaction
import java.io.File
import java.io.FileOutputStream
import java.io.FileWriter

object ExportUtils {

    fun exportStatementCsv(
        context: Context,
        contact: Contact,
        transactions: List<Transaction>,
        currencySymbol: String = CurrencyFormatter.getActiveCurrencySymbol()
    ) {
        try {
            val fileName = "KhataTrack_${contact.name.replace(" ", "_")}_Statement.csv"
            val file = File(context.cacheDir, fileName)
            val writer = FileWriter(file)

            writer.append("Date,Time,Contact,Type,Amount ($currencySymbol),Payment Mode,Reference Number,Note,Running Balance ($currencySymbol)\n")

            var runningBalance = 0.0
            val sortedTxs = transactions.sortedBy { it.transactionDate }

            for (tx in sortedTxs) {
                if (tx.type == Transaction.TYPE_YOU_GOT) {
                    runningBalance += tx.amount
                } else {
                    runningBalance -= tx.amount
                }

                val dateStr = DateTimeUtils.formatDate(tx.transactionDate)
                val typeStr = if (tx.type == Transaction.TYPE_YOU_GOT) "YOU GOT (Credit)" else "YOU GAVE (Debit)"
                val noteClean = (tx.note ?: "").replace(",", ";")
                val refClean = (tx.referenceNumber ?: "").replace(",", ";")

                writer.append("$dateStr,${tx.transactionTime},${contact.name},$typeStr,${tx.amount},$currencySymbol,${tx.paymentMode},$refClean,$noteClean,$runningBalance\n")
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
        transactions: List<Transaction>,
        contactsMap: Map<Long, String> = emptyMap(),
        currencySymbol: String = CurrencyFormatter.getActiveCurrencySymbol()
    ) {
        try {
            val fileName = "KhataTrack_Report_${System.currentTimeMillis()}.csv"
            val file = File(context.cacheDir, fileName)
            val writer = FileWriter(file)

            writer.append("Date,Time,Contact,Type,Amount ($currencySymbol),Payment Mode,Reference Number,Note\n")
            for (tx in transactions) {
                val dateStr = DateTimeUtils.formatDate(tx.transactionDate)
                val typeStr = if (tx.type == Transaction.TYPE_YOU_GOT) "YOU GOT" else "YOU GAVE"
                val contactName = contactsMap[tx.contactId] ?: "Contact #${tx.contactId}"
                val noteClean = (tx.note ?: "").replace(",", ";")
                val refClean = (tx.referenceNumber ?: "").replace(",", ";")

                writer.append("$dateStr,${tx.transactionTime},$contactName,$typeStr,${tx.amount},$currencySymbol,${tx.paymentMode},$refClean,$noteClean\n")
            }
            writer.flush()
            writer.close()

            shareFile(context, file, "text/csv", "Share Summary Report (CSV)")
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun exportStatementPdf(
        context: Context,
        contact: Contact,
        transactions: List<Transaction>,
        currencySymbol: String = CurrencyFormatter.getActiveCurrencySymbol()
    ) {
        try {
            val pdfDocument = PdfDocument()
            val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create() // Standard A4 (pt)
            val page = pdfDocument.startPage(pageInfo)
            val canvas = page.canvas

            val paint = Paint()
            val textPaint = Paint().apply { isAntiAlias = true }

            // Colors
            val primaryColor = Color.parseColor("#1C1B1F")
            val secondaryColor = Color.parseColor("#49454F")
            val creditColor = Color.parseColor("#1B5E20") // Green
            val debitColor = Color.parseColor("#B71C1C") // Red
            val dividerColor = Color.parseColor("#E0E0E0")
            val headerBgColor = Color.parseColor("#F5F5F7")

            var y = 40f

            // Header Banner
            textPaint.apply {
                color = primaryColor
                textSize = 20f
                isFakeBoldText = true
            }
            canvas.drawText("KhataTrack", 40f, y, textPaint)

            textPaint.apply {
                color = secondaryColor
                textSize = 10f
                isFakeBoldText = false
            }
            canvas.drawText("Account Statement", 480f, y, textPaint)

            y += 24f
            // Hairline divider
            paint.apply {
                color = dividerColor
                strokeWidth = 1f
            }
            canvas.drawLine(40f, y, 555f, y, paint)

            y += 25f
            // Contact info
            textPaint.apply {
                color = primaryColor
                textSize = 14f
                isFakeBoldText = true
            }
            canvas.drawText(contact.name, 40f, y, textPaint)

            if (!contact.mobileNumber.isNullOrEmpty()) {
                y += 16f
                textPaint.apply {
                    color = secondaryColor
                    textSize = 10f
                    isFakeBoldText = false
                }
                canvas.drawText("Mobile: ${contact.mobileNumber}", 40f, y, textPaint)
            }

            y += 16f
            val nowStr = DateTimeUtils.formatDate(System.currentTimeMillis())
            canvas.drawText("Generated on: $nowStr", 40f, y, textPaint)

            // Calculate totals
            var totalGave = 0.0
            var totalGot = 0.0
            val sortedTxs = transactions.sortedBy { it.transactionDate }
            for (t in sortedTxs) {
                if (t.type == Transaction.TYPE_YOU_GOT) totalGot += t.amount else totalGave += t.amount
            }
            val netBalance = totalGot - totalGave

            y += 25f
            // Table Header Box
            paint.color = headerBgColor
            canvas.drawRect(40f, y, 555f, y + 24f, paint)

            textPaint.apply {
                color = primaryColor
                textSize = 10f
                isFakeBoldText = true
            }
            canvas.drawText("Date & Time", 48f, y + 16f, textPaint)
            canvas.drawText("Details", 160f, y + 16f, textPaint)
            canvas.drawText("You Gave (-)", 310f, y + 16f, textPaint)
            canvas.drawText("You Got (+)", 400f, y + 16f, textPaint)
            canvas.drawText("Balance", 490f, y + 16f, textPaint)

            y += 24f
            var runningBalance = 0.0

            textPaint.isFakeBoldText = false

            for (tx in sortedTxs) {
                if (y > 720f) {
                    // Page limit protection
                    break
                }
                y += 20f

                if (tx.type == Transaction.TYPE_YOU_GOT) {
                    runningBalance += tx.amount
                } else {
                    runningBalance -= tx.amount
                }

                val dateStr = "${DateTimeUtils.formatDate(tx.transactionDate)} ${tx.transactionTime}"
                textPaint.color = secondaryColor
                canvas.drawText(dateStr, 48f, y, textPaint)

                val details = tx.paymentMode + (if (!tx.note.isNullOrEmpty()) " (${tx.note})" else "")
                val truncatedDetails = if (details.length > 25) details.take(22) + "..." else details
                canvas.drawText(truncatedDetails, 160f, y, textPaint)

                if (tx.type == Transaction.TYPE_YOU_GAVE) {
                    textPaint.color = debitColor
                    canvas.drawText("$currencySymbol ${CurrencyFormatter.formatRupee(tx.amount, false)}", 310f, y, textPaint)
                } else {
                    textPaint.color = creditColor
                    canvas.drawText("$currencySymbol ${CurrencyFormatter.formatRupee(tx.amount, false)}", 400f, y, textPaint)
                }

                textPaint.color = if (runningBalance >= 0) creditColor else debitColor
                canvas.drawText("$currencySymbol ${CurrencyFormatter.formatRupee(Math.abs(runningBalance), false)}", 490f, y, textPaint)

                y += 6f
                paint.color = dividerColor
                canvas.drawLine(40f, y, 555f, y, paint)
            }

            // Summary Footer Card
            y += 30f
            paint.color = headerBgColor
            canvas.drawRect(40f, y, 555f, y + 60f, paint)

            textPaint.apply {
                color = secondaryColor
                textSize = 9f
                isFakeBoldText = false
            }
            canvas.drawText("Total You Gave", 60f, y + 20f, textPaint)
            canvas.drawText("Total You Got", 230f, y + 20f, textPaint)
            canvas.drawText("Net Balance", 400f, y + 20f, textPaint)

            textPaint.apply {
                textSize = 12f
                isFakeBoldText = true
            }
            textPaint.color = debitColor
            canvas.drawText("$currencySymbol ${CurrencyFormatter.formatRupee(totalGave, false)}", 60f, y + 42f, textPaint)

            textPaint.color = creditColor
            canvas.drawText("$currencySymbol ${CurrencyFormatter.formatRupee(totalGot, false)}", 230f, y + 42f, textPaint)

            textPaint.color = if (netBalance >= 0) creditColor else debitColor
            val netStr = if (netBalance > 0) "You'll Get $currencySymbol ${CurrencyFormatter.formatRupee(netBalance, false)}"
            else if (netBalance < 0) "You'll Pay $currencySymbol ${CurrencyFormatter.formatRupee(Math.abs(netBalance), false)}"
            else "Settled Up"
            canvas.drawText(netStr, 400f, y + 42f, textPaint)

            pdfDocument.finishPage(page)

            val fileName = "KhataTrack_${contact.name.replace(" ", "_")}_Statement.pdf"
            val file = File(context.cacheDir, fileName)
            val outputStream = FileOutputStream(file)
            pdfDocument.writeTo(outputStream)
            outputStream.close()
            pdfDocument.close()

            shareFile(context, file, "application/pdf", "Share Statement PDF")

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
