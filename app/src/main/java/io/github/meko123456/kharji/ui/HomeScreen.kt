package io.github.meko123456.kharji.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.unit.dp
import androidx.core.app.NotificationManagerCompat
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.viewmodel.compose.viewModel
import io.github.meko123456.kharji.data.sms.BankNotificationListener
import io.github.meko123456.kharji.domain.ExpenseFromText
import io.github.meko123456.kharji.domain.KCurrency
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    viewModel: KharjiViewModel = viewModel(factory = KharjiViewModel.Factory),
    fromText: ExpenseFromText? = null,
    onFromTextOpened: () -> Unit = {},
    openScanner: Boolean = false,
    onScannerOpened: () -> Unit = {},
) {
    val entries by viewModel.entries.collectAsState()
    val pending by viewModel.pending.collectAsState()
    val categories by viewModel.categories.collectAsState()
    val rates by viewModel.rates.collectAsState()
    var showEditor by rememberSaveable { mutableStateOf(false) }
    var showScanner by rememberSaveable { mutableStateOf(false) }

    // What the editor opens with: nothing from the + button, or text picked out in another app.
    // Counted, so text arriving while the editor is open replaces what it held.
    var prefillAmount by rememberSaveable { mutableStateOf("") }
    var prefillCurrency by rememberSaveable { mutableStateOf("") }
    var prefillMerchant by rememberSaveable { mutableStateOf("") }
    var openings by rememberSaveable { mutableIntStateOf(0) }
    LaunchedEffect(fromText) {
        if (fromText != null) {
            prefillAmount = fromText.amount
            prefillCurrency = fromText.currency?.code.orEmpty()
            prefillMerchant = fromText.merchant
            openings++
            showScanner = false
            showEditor = true
            onFromTextOpened()
        }
    }
    LaunchedEffect(openScanner) {
        if (openScanner) {
            showEditor = false
            showScanner = true
            onScannerOpened()
        }
    }

    val context = LocalContext.current
    // Whether bank capture is on, read again on every resume: it is switched in Android's
    // settings, never in the app, so the button can only say what it finds when the user is back.
    var captureOn by remember { mutableStateOf(bankCaptureOn(context)) }
    LifecycleResumeEffect(Unit) {
        captureOn = bankCaptureOn(context)
        onPauseOrDispose {}
    }

    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    Scaffold(
        snackbarHost = { SnackbarHost(snackbar) },
        topBar = {
            TopAppBar(
                // Read as the app's name and as the screen's heading. TalkBack read the emoji too:
                // "Kharji, money with wings".
                title = {
                    Text(
                        "Kharji 💸",
                        modifier = Modifier.clearAndSetSemantics { contentDescription = "Kharji"; heading() },
                    )
                },
                actions = {
                    IconButton(onClick = { showScanner = true }) {
                        Text(
                            "📷",
                            style = MaterialTheme.typography.titleMedium,
                            modifier = Modifier.clearAndSetSemantics { contentDescription = "Scan receipt" },
                        )
                    }
                    // Opens Android's notification-access switch for Kharji, where bank capture is
                    // turned on and off.
                    IconButton(onClick = { openBankCaptureSettings(context) }) {
                        Text(
                            "📥",
                            style = MaterialTheme.typography.titleMedium,
                            modifier = Modifier.clearAndSetSemantics {
                                contentDescription = if (captureOn) "Bank capture is on" else "Bank capture is off"
                            },
                        )
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
            FloatingActionButton(onClick = {
                prefillAmount = ""
                prefillCurrency = ""
                prefillMerchant = ""
                openings++
                showEditor = true
            }) {
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
                    // A long-press deletes at once, so it can be taken back: a stray long-press while
                    // scrolling used to lose the expense for good.
                    onDelete = { entry ->
                        viewModel.deleteEntry(entry)
                        scope.launch {
                            snackbar.currentSnackbarData?.dismiss()
                            val result = snackbar.showSnackbar("Expense deleted", actionLabel = "Undo", duration = SnackbarDuration.Long)
                            if (result == SnackbarResult.ActionPerformed) viewModel.restoreEntry(entry)
                        }
                    },
                )
            }
        }
    }

    if (showEditor) {
        key(openings) {
            EntryEditorDialog(
                categories = categories,
                onSave = viewModel::addEntry,
                onDismiss = { showEditor = false },
                initialAmount = prefillAmount,
                initialCurrency = KCurrency.entries.firstOrNull { it.code == prefillCurrency } ?: KCurrency.GEL,
                initialMerchant = prefillMerchant,
            )
        }
    }

    if (showScanner) {
        ReceiptScannerScreen(
            onScanned = { receipt ->
                viewModel.addScannedReceipt(receipt)
                showScanner = false
            },
            onBack = { showScanner = false },
        )
    }
}

private fun bankCaptureOn(context: android.content.Context): Boolean =
    context.packageName in NotificationManagerCompat.getEnabledListenerPackages(context)

/**
 * Straight to Kharji's own notification-access switch on Android 11 and later. The list of every
 * app that can read notifications, which it used to open, is the fallback for older versions and
 * for a phone whose settings have no such page.
 */
private fun openBankCaptureSettings(context: android.content.Context) {
    val list = android.content.Intent(android.provider.Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)
    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R) {
        val listener = android.content.ComponentName(context, BankNotificationListener::class.java)
        val detail = android.content.Intent(android.provider.Settings.ACTION_NOTIFICATION_LISTENER_DETAIL_SETTINGS)
            .putExtra(android.provider.Settings.EXTRA_NOTIFICATION_LISTENER_COMPONENT_NAME, listener.flattenToString())
        if (runCatching { context.startActivity(detail) }.isSuccess) return
    }
    context.startActivity(list)
}
