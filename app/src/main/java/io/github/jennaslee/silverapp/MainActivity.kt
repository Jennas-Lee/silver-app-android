package io.github.jennaslee.silverapp

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import io.github.jennaslee.silverapp.ui.HomeScreen
import io.github.jennaslee.silverapp.ui.theme.SilverTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            SilverTheme {
                HomeScreen()
            }
        }
    }
}
