package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.data.Item
import com.example.ui.theme.DangerColor

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AutocompleteItemDropdown(
    query: String,
    onQueryChanged: (String) -> Unit,
    isExpanded: Boolean,
    onExpandedChanged: (Boolean) -> Unit,
    filteredItems: List<Item>,
    onItemSelected: (Item) -> Unit,
    modifier: Modifier = Modifier
) {
    ExposedDropdownMenuBox(
        expanded = isExpanded,
        onExpandedChange = { onExpandedChanged(it) },
        modifier = modifier
    ) {
        OutlinedTextField(
            value = query,
            onValueChange = { },
            readOnly = true,
            singleLine = true,
            label = { Text("Nama Barang") },
            modifier = Modifier.menuAnchor().fillMaxWidth(),
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = isExpanded) },
            colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors()
        )

        if (filteredItems.isNotEmpty()) {
            ExposedDropdownMenu(
                expanded = isExpanded,
                onDismissRequest = { onExpandedChanged(false) }
            ) {
                filteredItems.forEach { item ->
                    DropdownMenuItem(
                        text = { Text(item.name) },
                        onClick = {
                            onQueryChanged(item.name)
                            onItemSelected(item)
                            onExpandedChanged(false)
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun ConfirmDialog(
    title: String,
    text: String,
    confirmText: String = "HAPUS",
    dismissText: String = "BATAL",
    isDestructive: Boolean = true,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(text = title, style = MaterialTheme.typography.titleMedium)
        },
        text = {
            Text(text = text, style = MaterialTheme.typography.bodyMedium)
        },
        confirmButton = {
            TextButton(
                onClick = onConfirm,
                colors = ButtonDefaults.textButtonColors(
                    contentColor = if (isDestructive) DangerColor else MaterialTheme.colorScheme.primary
                )
            ) {
                Text(confirmText)
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                colors = ButtonDefaults.textButtonColors(
                    contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                )
            ) {
                Text(dismissText)
            }
        }
    )
}

@Composable
fun PrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    isLoading: Boolean = false
) {
    Button(
        onClick = onClick,
        modifier = modifier.fillMaxWidth().height(48.dp),
        enabled = enabled && !isLoading,
        shape = RoundedCornerShape(8.dp)
    ) {
        if (isLoading) {
            // Placeholder for loading
            Text("Memuat...", style = MaterialTheme.typography.labelLarge)
        } else {
            Text(text, style = MaterialTheme.typography.labelLarge)
        }
    }
}

// Simple placeholder for skeleton loader since actual shimmer can be complex
@Composable
fun SkeletonLoader(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .background(Color.LightGray.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
            .clip(RoundedCornerShape(8.dp))
    )
}
