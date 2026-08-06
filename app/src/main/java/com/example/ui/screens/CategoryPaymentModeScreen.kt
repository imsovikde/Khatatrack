package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.Unarchive
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.model.CategoryItem
import com.example.data.model.PaymentModeItem
import com.example.ui.components.PrimaryButton
import com.example.ui.theme.KhataTheme
import com.example.ui.viewmodel.KhataViewModel
import kotlinx.coroutines.launch

enum class ManagementTab {
    CATEGORIES, PAYMENT_MODES
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CategoryPaymentModeScreen(
    viewModel: KhataViewModel,
    onBack: () -> Unit
) {
    val colors = KhataTheme.colors
    val coroutineScope = rememberCoroutineScope()
    val categories by viewModel.repository.allCategories.collectAsState(initial = emptyList())
    val paymentModes by viewModel.repository.allPaymentModes.collectAsState(initial = emptyList())

    var activeTab by remember { mutableStateOf(ManagementTab.CATEGORIES) }

    var showAddCategorySheet by remember { mutableStateOf(false) }
    var showAddPaymentModeSheet by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Manage Categories & Modes",
                        style = KhataTheme.typography.titleLarge,
                        color = colors.textPrimary
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = colors.textPrimary
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = colors.surface)
            )
        },
        containerColor = colors.background
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Segmented Tab Chips
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                FilterChip(
                    selected = activeTab == ManagementTab.CATEGORIES,
                    onClick = { activeTab = ManagementTab.CATEGORIES },
                    label = { Text("Categories (${categories.size})") },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = colors.surfaceBorder,
                        selectedLabelColor = colors.textPrimary
                    )
                )
                FilterChip(
                    selected = activeTab == ManagementTab.PAYMENT_MODES,
                    onClick = { activeTab = ManagementTab.PAYMENT_MODES },
                    label = { Text("Payment Modes (${paymentModes.size})") },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = colors.surfaceBorder,
                        selectedLabelColor = colors.textPrimary
                    )
                )
            }

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                if (activeTab == ManagementTab.CATEGORIES) {
                    items(categories, key = { "cat_${it.id}" }) { category ->
                        CategoryRow(
                            category = category,
                            onToggleArchive = {
                                coroutineScope.launch {
                                    viewModel.repository.archiveCategory(category.id, !category.isArchived)
                                }
                            }
                        )
                    }
                    item {
                        DashedAddRow(
                            label = "+ Add New Category",
                            onClick = { showAddCategorySheet = true }
                        )
                    }
                } else {
                    items(paymentModes, key = { "pm_${it.id}" }) { mode ->
                        PaymentModeRow(
                            mode = mode,
                            onToggleArchive = {
                                coroutineScope.launch {
                                    viewModel.repository.archivePaymentMode(mode.id, !mode.isArchived)
                                }
                            }
                        )
                    }
                    item {
                        DashedAddRow(
                            label = "+ Add New Payment Mode",
                            onClick = { showAddPaymentModeSheet = true }
                        )
                    }
                }
            }
        }
    }

    if (showAddCategorySheet) {
        AddCategoryBottomSheet(
            onDismiss = { showAddCategorySheet = false },
            onSave = { name ->
                coroutineScope.launch {
                    viewModel.repository.addCategory(CategoryItem(name = name, sortOrder = categories.size + 1))
                    showAddCategorySheet = false
                }
            }
        )
    }

    if (showAddPaymentModeSheet) {
        AddPaymentModeBottomSheet(
            onDismiss = { showAddPaymentModeSheet = false },
            onSave = { name ->
                coroutineScope.launch {
                    viewModel.repository.addPaymentMode(PaymentModeItem(name = name, sortOrder = paymentModes.size + 1))
                    showAddPaymentModeSheet = false
                }
            }
        )
    }
}

@Composable
fun CategoryRow(category: CategoryItem, onToggleArchive: () -> Unit) {
    val colors = KhataTheme.colors
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (category.isArchived) colors.surfaceBorder.copy(alpha = 0.5f) else colors.surface
        ),
        border = androidx.compose.foundation.BorderStroke(1.dp, colors.surfaceBorder)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.Category,
                contentDescription = null,
                tint = if (category.isArchived) colors.textDisabled else colors.textPrimary,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = category.name,
                    style = KhataTheme.typography.bodyLarge,
                    color = if (category.isArchived) colors.textDisabled else colors.textPrimary,
                    fontWeight = FontWeight.Medium
                )
                if (category.isArchived) {
                    Text(
                        text = "Archived",
                        style = KhataTheme.typography.bodyMedium,
                        color = colors.textDisabled
                    )
                }
            }
            IconButton(onClick = onToggleArchive) {
                Icon(
                    imageVector = if (category.isArchived) Icons.Default.Unarchive else Icons.Default.Archive,
                    contentDescription = "Archive",
                    tint = colors.textSecondary
                )
            }
        }
    }
}

@Composable
fun PaymentModeRow(mode: PaymentModeItem, onToggleArchive: () -> Unit) {
    val colors = KhataTheme.colors
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (mode.isArchived) colors.surfaceBorder.copy(alpha = 0.5f) else colors.surface
        ),
        border = androidx.compose.foundation.BorderStroke(1.dp, colors.surfaceBorder)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.Payments,
                contentDescription = null,
                tint = if (mode.isArchived) colors.textDisabled else colors.textPrimary,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = mode.name,
                    style = KhataTheme.typography.bodyLarge,
                    color = if (mode.isArchived) colors.textDisabled else colors.textPrimary,
                    fontWeight = FontWeight.Medium
                )
                if (mode.isArchived) {
                    Text(
                        text = "Archived",
                        style = KhataTheme.typography.bodyMedium,
                        color = colors.textDisabled
                    )
                }
            }
            IconButton(onClick = onToggleArchive) {
                Icon(
                    imageVector = if (mode.isArchived) Icons.Default.Unarchive else Icons.Default.Archive,
                    contentDescription = "Archive",
                    tint = colors.textSecondary
                )
            }
        }
    }
}

@Composable
fun DashedAddRow(label: String, onClick: () -> Unit) {
    val colors = KhataTheme.colors
    val strokeColor = colors.surfaceBorder

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(52.dp)
            .clip(RoundedCornerShape(10.dp))
            .drawWithContent {
                drawContent()
                val stroke = Stroke(
                    width = 3f,
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(12f, 8f), 0f)
                )
                drawRoundRect(
                    color = strokeColor,
                    style = stroke
                )
            }
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            style = KhataTheme.typography.bodyLarge,
            color = colors.textSecondary,
            fontWeight = FontWeight.Medium
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddCategoryBottomSheet(onDismiss: () -> Unit, onSave: (String) -> Unit) {
    val colors = KhataTheme.colors
    var name by remember { mutableStateOf("") }
    val sheetState = rememberModalBottomSheetState()

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
                text = "Add New Category",
                style = KhataTheme.typography.titleLarge,
                color = colors.textPrimary
            )
            Spacer(modifier = Modifier.height(16.dp))
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Category Name") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(20.dp))
            PrimaryButton(
                text = "Save Category",
                onClick = {
                    if (name.isNotBlank()) {
                        onSave(name.trim())
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = name.isNotBlank()
            )
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddPaymentModeBottomSheet(onDismiss: () -> Unit, onSave: (String) -> Unit) {
    val colors = KhataTheme.colors
    var name by remember { mutableStateOf("") }
    val sheetState = rememberModalBottomSheetState()

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
                text = "Add New Payment Mode",
                style = KhataTheme.typography.titleLarge,
                color = colors.textPrimary
            )
            Spacer(modifier = Modifier.height(16.dp))
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Payment Mode Name") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(20.dp))
            PrimaryButton(
                text = "Save Payment Mode",
                onClick = {
                    if (name.isNotBlank()) {
                        onSave(name.trim())
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = name.isNotBlank()
            )
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}
