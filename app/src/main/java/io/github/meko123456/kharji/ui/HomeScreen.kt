package io.github.meko123456.kharji.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
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
    val categories by viewModel.categories.collectAsState()
    var showEditor by remember { mutableStateOf(false) }

    Scaffold(
        topBar = { TopAppBar(title = { Text("Kharji 💸") }) },
        floatingActionButton = {
            FloatingActionButton(onClick = { showEditor = true }) {
                Icon(Icons.Default.Add, contentDescription = "Add expense")
            }
        },
    ) { innerPadding ->
        if (entries.isEmpty()) {
            Column(
                modifier = Modifier.fillMaxSize().padding(innerPadding).padding(32.dp),
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
                onDelete = { viewModel.deleteEntry(it) },
                modifier = Modifier.padding(innerPadding),
            )
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
