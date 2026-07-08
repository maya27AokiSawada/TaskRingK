package net.sumomo_planning.taskringk

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import dagger.hilt.android.AndroidEntryPoint
import net.sumomo_planning.taskringk.core.ui.theme.GoShoppingTheme
import net.sumomo_planning.taskringk.presentation.navigation.AppNavGraph

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
