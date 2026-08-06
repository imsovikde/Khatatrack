package com.example.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.ui.theme.KhataTheme
import com.example.util.CurrencyInfo
import com.example.util.CurrencyManager

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CurrencySelectionBottomSheet(
    onDismiss: () -> Unit,
    onCurrencySelected: (CurrencyInfo) -> Unit
) {
    val context = LocalContext.current
    val colors = KhataTheme.colors
    val sheetState = rememberModalBottomSheetState()

    var searchQuery by remember { mutableStateOf("") }
    var pendingCurrency by remember { mutableStateOf<CurrencyInfo?>(null) }

    val currentCurrency = remember { CurrencyManager.getSelectedCurrency(context) }

    val filteredCurrencies = remember(searchQuery) {
        val q = searchQuery.trim().lowercase()
        if (q.isEmpty()) {
            CurrencyManager.CURRENCIES
        } else {
            CurrencyManager.CURRENCIES.filter {
                it.code.lowercase().contains(q) ||
                it.name.lowercase().contains(q) ||
                it.symbol.lowercase().contains(q)
            }
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = colors.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
        ) {
            Text(
                text = "Select Currency",
                style = KhataTheme.typography.titleLarge,
                color = colors.textPrimary
            )
            Spacer(modifier = Modifier.height(12.dp))
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                label = { Text("Search currency...") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(12.dp))

            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 350.dp)
            ) {
                items(filteredCurrencies, key = { it.code }) { currency ->
                    val isSelected = currency.code == currentCurrency.code
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { pendingCurrency = currency }
                            .padding(vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = isSelected,
                            onClick = { pendingCurrency = currency },
                            colors = RadioButtonDefaults.colors(selectedColor = colors.textPrimary)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "${currency.symbol}  ${currency.code}",
                            style = KhataTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Bold,
                            color = colors.textPrimary
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = currency.name,
                            style = KhataTheme.typography.bodyMedium,
                            color = colors.textSecondary
                        )
                    }
                }
            }
        }
    }

    pendingCurrency?.let { target ->
        AlertDialog(
            onDismissRequest = { pendingCurrency = null },
            title = { Text("Change Display Currency?") },
            text = { Text("This changes how amounts are displayed (${target.symbol} ${target.code}). Existing amounts are not converted.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        onCurrencySelected(target)
                        pendingCurrency = null
                    }
                ) {
                    Text("Confirm")
                }
            },
            dismissButton = {
                TextButton(onClick = { pendingCurrency = null }) {
                    Text("Cancel")
                }
            }
        )
    }
}
