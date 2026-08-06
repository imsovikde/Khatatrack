package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.ContactWithBalance
import com.example.ui.components.ContactCard
import com.example.ui.components.EmptyState
import com.example.ui.theme.BodyStyle
import com.example.ui.theme.CaptionStyle
import com.example.ui.theme.KhataTheme
import com.example.data.model.Transaction
import com.example.ui.components.TransactionRow
import com.example.util.SearchHistoryManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.material3.FilterChip
import androidx.compose.material3.TextButton
import androidx.compose.foundation.clickable

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchScreen(
    query: String,
    onQueryChange: (String) -> Unit,
    searchResults: List<ContactWithBalance>,
    transactionSearchResults: List<Transaction>,
    onBackClick: () -> Unit,
    onContactClick: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = KhataTheme.colors
    val context = LocalContext.current
    val focusRequester = remember { FocusRequester() }
    
    // Search history state
    var searchHistory by remember { mutableStateOf<List<String>>(SearchHistoryManager.getSearchHistory(context)) }

    LaunchedEffect(Unit) {
        try {
            focusRequester.requestFocus()
        } catch (e: Exception) {
            // ignore
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        BasicTextField(
                            value = query,
                            onValueChange = onQueryChange,
                            textStyle = BodyStyle.copy(color = colors.textPrimary, fontSize = 18.sp),
                            cursorBrush = SolidColor(colors.textPrimary),
                            singleLine = true,
                            modifier = Modifier
                                .weight(1f)
                                .focusRequester(focusRequester)
                                .testTag("search_query_input"),
                            decorationBox = { innerTextField ->
                                Box {
                                    if (query.isEmpty()) {
                                        Text(
                                            text = "Search by name, phone, or notes...",
                                            style = BodyStyle.copy(color = colors.textDisabled, fontSize = 18.sp)
                                        )
                                    }
                                    innerTextField()
                                }
                            }
                        )
                        if (query.isNotEmpty()) {
                            IconButton(onClick = { onQueryChange("") }) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Clear search",
                                    tint = colors.textSecondary
                                )
                            }
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBackClick, modifier = Modifier.testTag("back_button")) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = colors.textPrimary
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = colors.bgCanvas)
            )
        },
        containerColor = colors.bgCanvas,
        modifier = modifier.fillMaxSize()
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            HorizontalDivider(color = colors.divider, thickness = 1.dp)

            if (query.isEmpty()) {
                if (searchHistory.isNotEmpty()) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "RECENT SEARCHES",
                            style = CaptionStyle,
                            color = colors.textSecondary,
                            modifier = Modifier.weight(1f)
                        )
                        TextButton(onClick = { 
                            SearchHistoryManager.clearSearchHistory(context)
                            searchHistory = emptyList<String>()
                        }) {
                            Text("Clear", style = CaptionStyle, color = colors.debit)
                        }
                    }
                    LazyRow(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
                        items(searchHistory) { historyItem ->
                            FilterChip(
                                selected = false,
                                onClick = { onQueryChange(historyItem) },
                                label = { Text(historyItem) },
                                modifier = Modifier.padding(end = 8.dp)
                            )
                        }
                    }
                    HorizontalDivider(color = colors.divider, thickness = 1.dp, modifier = Modifier.padding(top = 8.dp))
                }

                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    EmptyState(
                        message = "Search KhataTrack",
                        subMessage = "Type a name, mobile number, or note keyword.",
                        icon = Icons.Default.Search
                    )
                }
            } else if (searchResults.isEmpty() && transactionSearchResults.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    EmptyState(
                        message = "No matching results found",
                        subMessage = "Check your spelling or try searching for another term.",
                        icon = Icons.Default.Search
                    )
                }
            } else {
                LaunchedEffect(query) {
                    if (searchResults.isNotEmpty() || transactionSearchResults.isNotEmpty()) {
                        kotlinx.coroutines.delay(1000)
                        SearchHistoryManager.addSearchQuery(context, query)
                    }
                }

                LazyColumn(modifier = Modifier.fillMaxSize()) {
                    if (searchResults.isNotEmpty()) {
                        item {
                            Text(
                                text = "CONTACTS (${searchResults.size})",
                                style = CaptionStyle,
                                color = colors.textSecondary,
                                modifier = Modifier.padding(horizontal = KhataTheme.spacing.md, vertical = KhataTheme.spacing.sm)
                            )
                        }
                        items(searchResults, key = { "contact_${it.contact.id}" }) { item ->
                            ContactCard(
                                contactWithBalance = item,
                                onClick = { 
                                    SearchHistoryManager.addSearchQuery(context, query)
                                    onContactClick(item.contact.id) 
                                }
                            )
                        }
                    }

                    if (transactionSearchResults.isNotEmpty()) {
                        item {
                            Text(
                                text = "ENTRIES (${transactionSearchResults.size})",
                                style = CaptionStyle,
                                color = colors.textSecondary,
                                modifier = Modifier.padding(horizontal = KhataTheme.spacing.md, vertical = KhataTheme.spacing.sm)
                            )
                        }
                        items(transactionSearchResults, key = { "tx_${it.id}" }) { item ->
                            TransactionRow(
                                transaction = item,
                                highlightQuery = query,
                                onClick = { 
                                    SearchHistoryManager.addSearchQuery(context, query)
                                    item.contactId?.let { onContactClick(it) }
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}
