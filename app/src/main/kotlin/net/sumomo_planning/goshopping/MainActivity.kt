package net.sumomo_planning.goshopping

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import dagger.hilt.android.AndroidEntryPoint
import net.sumomo_planning.goshopping.core.ui.theme.GoShoppingTheme
import net.sumomo_planning.goshopping.presentation.navigation.AppNavGraph

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            GoShoppingTheme {
                AppNavGraph(modifier = Modifier.fillMaxSize())
            }
        }
    }
}
