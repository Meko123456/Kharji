package io.github.meko123456.kharji

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import io.github.meko123456.kharji.data.fx.FxRefreshWorker
import io.github.meko123456.kharji.ui.HomeScreen
import io.github.meko123456.kharji.ui.theme.KharjiTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        FxRefreshWorker.schedule(this)
        setContent {
            KharjiTheme {
                HomeScreen()
            }
        }
    }
}
