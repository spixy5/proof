package com.proofapp.android.ui.shopping

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.proofapp.android.data.repository.ShoppingItem
import com.proofapp.android.data.repository.ShoppingRepository
import com.proofapp.android.ui.components.EmptyState
import com.proofapp.android.ui.container
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class ShoppingViewModel(private val repo: ShoppingRepository) : ViewModel() {
    val items: StateFlow<List<ShoppingItem>?> =
        repo.items.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    fun toggle(item: ShoppingItem) = viewModelScope.launch { repo.setChecked(item, !item.checked) }
    fun delete(item: ShoppingItem) = viewModelScope.launch { repo.delete(item) }
    fun add(name: String) = viewModelScope.launch { repo.addCustom(name) }
    fun clearChecked() = viewModelScope.launch { repo.clearChecked() }

    companion object {
        val Factory = viewModelFactory { initializer { ShoppingViewModel(container.shoppingRepository) } }
    }
}

@Composable
fun ShoppingListScreen(viewModel: ShoppingViewModel = viewModel(factory = ShoppingViewModel.Factory)) {
    val items by viewModel.items.collectAsStateWithLifecycle()
    var newItem by rememberSaveable { mutableStateOf("") }
    val list = items.orEmpty()
    val remaining = list.count { !it.checked }

    Column(Modifier.fillMaxSize().statusBarsPadding()) {
        Row(Modifier.fillMaxWidth().padding(start = 20.dp, end = 8.dp, top = 20.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("Shopping list", style = MaterialTheme.typography.headlineLarge)
                if (list.isNotEmpty()) {
                    Text(
                        if (remaining == 0) "All done. Happy baking!" else "$remaining to buy",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            if (list.any { it.checked }) TextButton(onClick = { viewModel.clearChecked() }) { Text("Clear ticked") }
        }

        val submit = {
            viewModel.add(newItem)
            newItem = ""
        }
        OutlinedTextField(
            value = newItem,
            onValueChange = { newItem = it },
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            placeholder = { Text("Add an item, e.g. parchment paper") },
            singleLine = true,
            shape = CircleShape,
            trailingIcon = {
                IconButton(onClick = submit, enabled = newItem.isNotBlank()) { Icon(Icons.Rounded.Add, contentDescription = "Add item") }
            },
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(onDone = { submit() }),
            colors = OutlinedTextFieldDefaults.colors(
                unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                unfocusedBorderColor = MaterialTheme.colorScheme.surfaceContainerHigh,
            ),
        )

        if (items != null && list.isEmpty()) {
            EmptyState(
                emoji = "🧺",
                title = "Your basket is empty",
                body = "Open a recipe, scale it to your batch and tap \"Add to shopping list\". Amounts from different recipes add up.",
            )
        } else {
            LazyColumn(
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items(list, key = { it.id }) { item ->
                    ShoppingRow(item, onToggle = { viewModel.toggle(item) }, onDelete = { viewModel.delete(item) }, Modifier.animateItem())
                }
            }
        }
    }
}

@Composable
private fun ShoppingRow(item: ShoppingItem, onToggle: () -> Unit, onDelete: () -> Unit, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier.fillMaxWidth().alpha(if (item.checked) 0.55f else 1f),
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
    ) {
        Row(Modifier.clickable(onClick = onToggle).padding(start = 4.dp, end = 4.dp, top = 4.dp, bottom = 4.dp), verticalAlignment = Alignment.CenterVertically) {
            Checkbox(checked = item.checked, onCheckedChange = { onToggle() })
            Column(Modifier.weight(1f).padding(vertical = 8.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    item.amount?.let {
                        Text(it, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
                        Spacer(Modifier.width(8.dp))
                    }
                    Text(
                        item.name,
                        style = MaterialTheme.typography.bodyLarge,
                        textDecoration = if (item.checked) TextDecoration.LineThrough else null,
                    )
                }
                if (item.sources.isNotBlank()) {
                    Text(item.sources, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            IconButton(onClick = onDelete) { Icon(Icons.Rounded.Close, contentDescription = "Remove ${item.name}") }
        }
    }
}
