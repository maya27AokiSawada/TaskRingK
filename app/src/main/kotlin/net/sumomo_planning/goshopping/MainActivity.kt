package net.sumomo_planning.goshopping

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.google.firebase.Firebase
import com.google.firebase.app
import dagger.hilt.android.AndroidEntryPoint
import net.sumomo_planning.goshopping.core.ui.theme.GoShoppingTheme

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            GoShoppingTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    SetupStatusScreen(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding)
                    )
                }
            }
        }
    }
}

@Composable
private fun SetupStatusScreen(modifier: Modifier = Modifier) {
    val firebaseReady = remember {
        runCatching { Firebase.app.name }.isSuccess
    }

    Column(
        modifier = modifier.padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Go Shopping (Kotlin)",
            style = MaterialTheme.typography.headlineMedium,
            textAlign = TextAlign.Center
        )
        Text(
            text = "Flavor: ${BuildConfig.FLAVOR} · v${BuildConfig.VERSION_NAME}",
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center
        )
        Text(
            text = if (firebaseReady) "Firebase initialized ✓" else "Firebase NOT initialized ✗",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.primary,
            textAlign = TextAlign.Center
        )
        Text(
            text = "Phase 0 scaffold ready.",
            style = MaterialTheme.typography.bodySmall,
            textAlign = TextAlign.Center
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun SetupStatusScreenPreview() {
    GoShoppingTheme {
        SetupStatusScreen()
    }
}
