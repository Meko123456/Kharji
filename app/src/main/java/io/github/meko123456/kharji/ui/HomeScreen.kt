package io.github.meko123456.kharji.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.IconButton
import androidx.compose.ui.platform.LocalContext
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(viewModel: KharjiViewModel = viewModel(factory = KharjiViewModel.Factory)) {
    val entries by viewModel.entries.collectAsState()
    val pending by viewModel.pending.collectAsState()
    val categories by viewModel.categories.collectAsState()
    val rates by viewModel.rates.collectAsState()
    var showEditor by remember { mutableStateOf(false) }

    val context = LocalContext.current

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Kharji 💸") },
                actions = {
                    // Opens system settings so the user can opt in to bank-notification capture.
                    IconButton(onClick = {
                        context.startActivity(
                            android.content.Intent("android.settings.ACTION_NOTIFICATION_LISTENER_SETTINGS"),
                        )
                    }) {
                        Text("📥", style = MaterialTheme.typography.titleMedium)
                    }
                    if (entries.isNotEmpty()) {
                        IconButton(onClick = { shareCsv(context, entries, categories) }) {
                            Icon(Icons.Default.Share, contentDescription = "Export CSV")
                        }
                    }
                },
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { showEditor = true }) {
                Icon(Icons.Default.Add, contentDescription = "Add expense")
            }
        },
    ) { innerPadding ->
        Column(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
            PendingReview(
                pending = pending,
                onConfirm = { viewModel.confirmPending(it) },
                onDiscard = { viewModel.discardPending(it) },
            )
            if (entries.isEmpty()) {
                Column(
                    modifier = Modifier.fillMaxSize().padding(32.dp),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text("No expenses yet", style = MaterialTheme.typography.headlineSmall)
                    Text(
                        "Tap + to add your first — lari or dirham.",
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(top = 8.dp),
                    )
                }
            } else {
                EntryList(
                    entries = entries,
                    categories = categories,
                    rates = rates,
                    onDelete = { viewModel.deleteEntry(it) },
                )
            }
        }
    }

    if (showEditor) {
        EntryEditorDialog(
            categories = categories,
            onSave = viewModel::addEntry,
            onDismiss = { showEditor = false },
        )
    }
}
