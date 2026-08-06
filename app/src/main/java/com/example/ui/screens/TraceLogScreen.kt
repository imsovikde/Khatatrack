package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.TraceLog
import com.example.ui.components.EmptyState
import com.example.ui.theme.KhataTheme
import com.example.util.DateTimeUtils
import com.example.ui.viewmodel.KhataViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TraceLogScreen(
    viewModel: KhataViewModel,
    entityType: String? = null,
    entityId: Long? = null,
    onBack: () -> Unit
) {
    val colors = KhataTheme.colors

    val tracesFlow = if (entityType != null && entityId != null) {
        viewModel.repository.getTracesForEntity(entityType, entityId)
    } else {
        viewModel.repository.allTraces
    }

    val traces by tracesFlow.collectAsState(initial = emptyList())

    val groupedTraces = traces.groupBy { DateTimeUtils.formatDate(it.timestamp) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = if (entityType != null) "Activity History" else "Activity Trace",
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
        if (traces.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                EmptyState(
                    message = "No Activity Logs Yet",
                    subMessage = "All create, edit, delete, and restore actions will be recorded here for auditability."
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                groupedTraces.forEach { (dateHeader, traceList) ->
                    item {
                        Text(
                            text = dateHeader,
                            style = KhataTheme.typography.labelSmall,
                            color = colors.textDisabled,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(vertical = 4.dp)
                        )
                    }

                    items(traceList, key = { it.id }) { trace ->
                        TraceLogRow(trace = trace)
                    }
                }
            }
        }
    }
}

@Composable
fun TraceLogRow(trace: TraceLog) {
    val colors = KhataTheme.colors

    val actionColor = when (trace.action) {
        "CREATE" -> Color(0xFF2E7D32) // Soft Green
        "EDIT" -> Color(0xFF0288D1) // Soft Blue
        "DELETE" -> colors.debitRed
        "RESTORE" -> Color(0xFF7B1FA2) // Soft Purple
        "PURGE" -> Color(0xFF616161) // Neutral Gray
        else -> colors.textSecondary
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(containerColor = colors.surface),
        border = androidx.compose.foundation.BorderStroke(1.dp, colors.surfaceBorder)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .background(actionColor.copy(alpha = 0.15f), RoundedCornerShape(4.dp))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = trace.action,
                            style = KhataTheme.typography.labelSmall,
                            color = actionColor,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = trace.entityName.ifEmpty { "${trace.entityType} #${trace.entityId}" },
                        style = KhataTheme.typography.bodyLarge,
                        color = colors.textPrimary,
                        fontWeight = FontWeight.Medium
                    )
                }
                Text(
                    text = DateTimeUtils.formatTimeOnly(trace.timestamp),
                    style = KhataTheme.typography.bodyMedium,
                    color = colors.textDisabled
                )
            }

            if (!trace.fieldChanged.isNullOrEmpty()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Changed: ${trace.fieldChanged}",
                    style = KhataTheme.typography.bodyMedium,
                    color = colors.textSecondary
                )
            }

            if (!trace.oldValue.isNullOrEmpty() || !trace.newValue.isNullOrEmpty()) {
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "'${trace.oldValue ?: ""}' ➔ '${trace.newValue ?: ""}'",
                    style = KhataTheme.typography.bodyMedium,
                    color = colors.textDisabled
                )
            }
        }
    }
}
