package io.github.meko123456.kharji

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import io.github.meko123456.kharji.data.fx.FxRefreshWorker
import io.github.meko123456.kharji.domain.ExpenseFromText
import io.github.meko123456.kharji.ui.HomeScreen
import io.github.meko123456.kharji.ui.theme.KharjiTheme

class MainActivity : ComponentActivity() {
    /** Text from another app for a new expense, until the editor has opened with it. */
    private var fromText by mutableStateOf<ExpenseFromText?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        FxRefreshWorker.schedule(this)
        // A fresh start only: recreated after a turn, the activity is handed the same intent again,
        // and the editor keeps what was changed in it since.
        if (savedInstanceState == null) take(intent)
        setContent {
            KharjiTheme {
                HomeScreen(fromText = fromText, onFromTextOpened = { fromText = null })
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        take(intent)
    }

    private fun take(intent: Intent?) {
        val text = when (intent?.action) {
            Intent.ACTION_PROCESS_TEXT -> intent.getCharSequenceExtra(Intent.EXTRA_PROCESS_TEXT)
            Intent.ACTION_SEND -> intent.getCharSequenceExtra(Intent.EXTRA_TEXT)
            else -> null
        }?.toString()?.takeIf { it.isNotBlank() } ?: return
        fromText = ExpenseFromText.from(text)
    }
}
