package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.model.Transaction
import com.example.ui.theme.KhataTheme
import com.example.util.CurrencyFormatter
import com.example.util.DateTimeUtils

@Composable
fun PdfPreviewDialog(
    contactName: String,
    transactions: List<Transaction>,
    onDismiss: () -> Unit,
    onDownload: () -> Unit
) {
    val colors = KhataTheme.colors

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .fillMaxHeight(0.9f),
            shape = KhataTheme.shapes.lg,
            color = colors.bgCanvas
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // Toolbar
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "PDF Preview",
                        style = KhataTheme.typography.titleLarge,
                        color = colors.textPrimary,
                        fontWeight = FontWeight.Bold
                    )
                    TextButton(onClick = onDismiss) {
                        Text("Close", color = colors.textSecondary)
                    }
                }

                HorizontalDivider(color = colors.divider)

                // PDF Mockup
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .padding(16.dp)
                        .background(Color.White)
                ) {
                    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("KhataTrack", fontWeight = FontWeight.Bold, color = Color.Black, fontSize = 18.sp)
                            Text("Account Statement", color = Color.Gray, fontSize = 12.sp)
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        HorizontalDivider(color = Color.LightGray)
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(contactName, fontWeight = FontWeight.Bold, color = Color.Black, fontSize = 14.sp)
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("Generated on: ${DateTimeUtils.formatDate(System.currentTimeMillis())}", color = Color.Gray, fontSize = 10.sp)
                        
                        Spacer(modifier = Modifier.height(16.dp))

                        // Table Header
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Color(0xFFF5F5F7))
                                .padding(8.dp)
                        ) {
                            Text("Date", modifier = Modifier.weight(1.5f), fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color.Black)
                            Text("Details", modifier = Modifier.weight(2f), fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color.Black)
                            Text("Gave(-)", modifier = Modifier.weight(1f), fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color.Black)
                            Text("Got(+)", modifier = Modifier.weight(1f), fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color.Black)
                            Text("Bal", modifier = Modifier.weight(1f), fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color.Black)
                        }

                        // Table Body
                        LazyColumn(modifier = Modifier.weight(1f)) {
                            var runningBal = 0.0
                            val sortedTxs = transactions.sortedBy { it.transactionDate }
                            items(sortedTxs) { tx ->
                                if (tx.type == Transaction.TYPE_YOU_GOT) runningBal += tx.amount else runningBal -= tx.amount
                                val debitStr = if (tx.type == Transaction.TYPE_YOU_GAVE) CurrencyFormatter.formatRupee(tx.amount) else ""
                                val creditStr = if (tx.type == Transaction.TYPE_YOU_GOT) CurrencyFormatter.formatRupee(tx.amount) else ""
                                
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 4.dp, horizontal = 8.dp)
                                ) {
                                    Text(DateTimeUtils.formatDate(tx.transactionDate), modifier = Modifier.weight(1.5f), fontSize = 9.sp, color = Color.DarkGray)
                                    Text(tx.paymentMode + if (!tx.note.isNullOrEmpty()) " (${tx.note})" else "", modifier = Modifier.weight(2f), fontSize = 9.sp, color = Color.DarkGray, maxLines = 1)
                                    Text(debitStr, modifier = Modifier.weight(1f), fontSize = 9.sp, color = Color(0xFFB71C1C))
                                    Text(creditStr, modifier = Modifier.weight(1f), fontSize = 9.sp, color = Color(0xFF1B5E20))
                                    Text(CurrencyFormatter.formatRupee(Math.abs(runningBal)), modifier = Modifier.weight(1f), fontSize = 9.sp, color = if(runningBal>=0) Color(0xFF1B5E20) else Color(0xFFB71C1C))
                                }
                                HorizontalDivider(color = Color.LightGray, thickness = 0.5.dp)
                            }
                        }

                        // Summary
                        var tg = 0.0; var tc = 0.0
                        transactions.forEach { if (it.type == Transaction.TYPE_YOU_GOT) tc+=it.amount else tg+=it.amount }
                        val net = tc - tg
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Color(0xFFF5F5F7))
                                .padding(16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text("Total You Gave", fontSize = 10.sp, color = Color.Gray)
                                Text(CurrencyFormatter.formatRupee(tg), fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFFB71C1C))
                            }
                            Column {
                                Text("Total You Got", fontSize = 10.sp, color = Color.Gray)
                                Text(CurrencyFormatter.formatRupee(tc), fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFF1B5E20))
                            }
                            Column {
                                Text("Net Balance", fontSize = 10.sp, color = Color.Gray)
                                Text(CurrencyFormatter.formatRupee(Math.abs(net)), fontSize = 12.sp, fontWeight = FontWeight.Bold, color = if (net>=0) Color(0xFF1B5E20) else Color(0xFFB71C1C))
                            }
                        }
                    }
                }

                HorizontalDivider(color = colors.divider)
                
                // Actions
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.End
                ) {
                    PrimaryButton(
                        text = "Download PDF",
                        onClick = onDownload,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }
    }
}
