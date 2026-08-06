package com.example.ui.screens

import android.content.Intent
import android.provider.ContactsContract
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.Contact
import com.example.ui.components.PrimaryButton
import com.example.ui.components.SemanticChip
import com.example.ui.theme.BodyStyle
import com.example.ui.theme.CaptionStyle
import com.example.ui.theme.KhataTheme
import com.example.ui.theme.LabelStyle
import com.example.ui.theme.TitleStyle

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditContactScreen(
    contactToEdit: Contact?,
    onBackClick: () -> Unit,
    onSaveContact: (name: String, mobile: String?, email: String?, tag: String, notes: String?) -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = KhataTheme.colors
    val context = LocalContext.current

    var name by remember { mutableStateOf(contactToEdit?.name ?: "") }
    var mobileNumber by remember { mutableStateOf(contactToEdit?.mobileNumber ?: "") }
    var email by remember { mutableStateOf(contactToEdit?.email ?: "") }
    var selectedCategory by remember { mutableStateOf(contactToEdit?.categoryTag ?: "Friend") }
    var notes by remember { mutableStateOf(contactToEdit?.addressNotes ?: "") }

    val categories = listOf("Friend", "Family", "Customer", "Supplier", "Other")

    // Contact picker launcher
    val contactPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickContact()
    ) { uri ->
        if (uri != null) {
            try {
                val cursor = context.contentResolver.query(uri, null, null, null, null)
                cursor?.use {
                    if (it.moveToFirst()) {
                        val nameIndex = it.getColumnIndex(ContactsContract.Contacts.DISPLAY_NAME)
                        if (nameIndex >= 0) {
                            val pickedName = it.getString(nameIndex)
                            if (!pickedName.isNull_or_blank()) {
                                name = pickedName
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = if (contactToEdit == null) "Add New Contact" else "Edit Contact",
                        style = TitleStyle,
                        color = colors.textPrimary
                    )
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
        bottomBar = {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(colors.bgSurface)
                    .padding(KhataTheme.spacing.md)
                    .navigationBarsPadding()
            ) {
                val isValid = name.trim().isNotEmpty()
                PrimaryButton(
                    text = if (contactToEdit == null) "SAVE CONTACT" else "UPDATE CONTACT",
                    onClick = {
                        if (isValid) {
                            onSaveContact(
                                name.trim(),
                                mobileNumber.ifBlank { null },
                                email.ifBlank { null },
                                selectedCategory,
                                notes.ifBlank { null }
                            )
                        }
                    },
                    enabled = isValid,
                    modifier = Modifier.fillMaxWidth(),
                    testTag = "save_contact_button"
                )
            }
        },
        containerColor = colors.bgCanvas,
        modifier = modifier.fillMaxSize()
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(KhataTheme.spacing.md),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(KhataTheme.spacing.md))

            // Avatar picker centered top (80dp circle)
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .clip(CircleShape)
                    .background(colors.divider)
                    .clickable { /* Photo picker */ },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Person,
                    contentDescription = "Profile Photo",
                    tint = colors.textSecondary,
                    modifier = Modifier.size(40.dp)
                )
                Box(
                    modifier = Modifier
                        .size(24.dp)
                        .clip(CircleShape)
                        .background(colors.textPrimary)
                        .align(Alignment.BottomEnd),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.CameraAlt,
                        contentDescription = "Edit photo",
                        tint = colors.bgCanvas,
                        modifier = Modifier.size(12.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(KhataTheme.spacing.xl))

            // NAME FIELD (Required, underlined minimal input style)
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "Full Name *",
                    style = CaptionStyle,
                    color = colors.textSecondary
                )
                Spacer(modifier = Modifier.height(6.dp))
                BasicTextField(
                    value = name,
                    onValueChange = { name = it },
                    textStyle = BodyStyle.copy(color = colors.textPrimary, fontSize = 18.sp, fontWeight = FontWeight.Medium),
                    cursorBrush = SolidColor(colors.textPrimary),
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("name_input"),
                    decorationBox = { innerTextField ->
                        Box(modifier = Modifier.padding(vertical = 4.dp)) {
                            if (name.isEmpty()) {
                                Text(
                                    text = "e.g., Rahul Sharma",
                                    style = BodyStyle.copy(color = colors.textDisabled, fontSize = 18.sp)
                                )
                            }
                            innerTextField()
                        }
                    }
                )
                HorizontalDivider(color = colors.textPrimary, thickness = 1.5.dp)
            }

            Spacer(modifier = Modifier.height(KhataTheme.spacing.lg))

            // MOBILE NUMBER FIELD with inline "Import from Contacts" text button
            Column(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Mobile Number",
                        style = CaptionStyle,
                        color = colors.textSecondary
                    )
                    Text(
                        text = "Import from Contacts",
                        style = LabelStyle.copy(fontSize = 12.sp),
                        color = colors.credit,
                        modifier = Modifier.clickable {
                            contactPickerLauncher.launch(null)
                        }
                    )
                }
                Spacer(modifier = Modifier.height(6.dp))
                BasicTextField(
                    value = mobileNumber,
                    onValueChange = { mobileNumber = it },
                    textStyle = BodyStyle.copy(color = colors.textPrimary),
                    cursorBrush = SolidColor(colors.textPrimary),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("mobile_input"),
                    decorationBox = { innerTextField ->
                        Box(modifier = Modifier.padding(vertical = 4.dp)) {
                            if (mobileNumber.isEmpty()) {
                                Text(
                                    text = "+91 98765 43210",
                                    style = BodyStyle.copy(color = colors.textDisabled)
                                )
                            }
                            innerTextField()
                        }
                    }
                )
                HorizontalDivider(color = colors.divider, thickness = 1.dp)
            }

            Spacer(modifier = Modifier.height(KhataTheme.spacing.lg))

            // EMAIL FIELD
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "Email (Optional)",
                    style = CaptionStyle,
                    color = colors.textSecondary
                )
                Spacer(modifier = Modifier.height(6.dp))
                BasicTextField(
                    value = email,
                    onValueChange = { email = it },
                    textStyle = BodyStyle.copy(color = colors.textPrimary),
                    cursorBrush = SolidColor(colors.textPrimary),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    decorationBox = { innerTextField ->
                        Box(modifier = Modifier.padding(vertical = 4.dp)) {
                            if (email.isEmpty()) {
                                Text(
                                    text = "name@example.com",
                                    style = BodyStyle.copy(color = colors.textDisabled)
                                )
                            }
                            innerTextField()
                        }
                    }
                )
                HorizontalDivider(color = colors.divider, thickness = 1.dp)
            }

            Spacer(modifier = Modifier.height(KhataTheme.spacing.lg))

            // CATEGORY TAG CHIPS
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "Category Tag",
                    style = CaptionStyle,
                    color = colors.textSecondary
                )
                Spacer(modifier = Modifier.height(8.dp))
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(categories) { category ->
                        SemanticChip(
                            text = category,
                            isSelected = selectedCategory == category,
                            onClick = { selectedCategory = category }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(KhataTheme.spacing.lg))

            // NOTES / ADDRESS MULTILINE FIELD
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "Notes / Address",
                    style = CaptionStyle,
                    color = colors.textSecondary
                )
                Spacer(modifier = Modifier.height(6.dp))
                BasicTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    textStyle = BodyStyle.copy(color = colors.textPrimary),
                    cursorBrush = SolidColor(colors.textPrimary),
                    minLines = 3,
                    modifier = Modifier.fillMaxWidth(),
                    decorationBox = { innerTextField ->
                        Box(modifier = Modifier.padding(vertical = 4.dp)) {
                            if (notes.isEmpty()) {
                                Text(
                                    text = "Add shop address or notes...",
                                    style = BodyStyle.copy(color = colors.textDisabled)
                                )
                            }
                            innerTextField()
                        }
                    }
                )
                HorizontalDivider(color = colors.divider, thickness = 1.dp)
            }
        }
    }
}

private fun String?.isNull_or_blank(): Boolean {
    return this == null || this.trim().isEmpty()
}
