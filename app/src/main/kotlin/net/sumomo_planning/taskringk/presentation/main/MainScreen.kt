package net.sumomo_planning.taskringk.presentation.main

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import net.sumomo_planning.taskringk.presentation.auth.AuthViewModel
import net.sumomo_planning.taskringk.presentation.group.SharedGroupScreen
import net.sumomo_planning.taskringk.presentation.settings.SettingsPlaceholderScreen
import net.sumomo_planning.taskringk.presentation.shoppinglist.SharedListScreen

/**
 * Main screen with a bottom navigation bar (Phase 3+).
 *
 * Tabs (porting_spec §3-1):
 *  Tab 0 — 買い物リスト   (placeholder; Phase 4)
 *  Tab 1 — グループ管理  (SharedGroupScreen; Phase 3)
 *  Tab 2 — 設定          (placeholder; Phase 9)
 */
@Composable
fun MainScreen(
    onSignOut: () -> Unit,
    modifier: Modifier = Modifier,
    authViewModel: AuthViewModel = hiltViewModel(),
) {
    var selectedTab by rememberSaveable { mutableIntStateOf(1) }

    Scaffold(
        modifier = modifier,
        bottomBar = {
            NavigationBar {
                NavigationBarItem(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    icon = { Icon(Icons.Default.ShoppingCart, contentDescription = null) },
                    label = { Text("リスト") },
                )
                NavigationBarItem(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    icon = { Icon(Icons.Default.Group, contentDescription = null) },
                    label = { Text("グループ") },
                )
                NavigationBarItem(
                    selected = selectedTab == 2,
                    onClick = { selectedTab = 2 },
                    icon = { Icon(Icons.Default.Settings, contentDescription = null) },
                    label = { Text("設定") },
                )
            }
        },
    ) { innerPadding ->
        when (selectedTab) {
            0 -> SharedListScreen(
                modifier = Modifier.padding(innerPadding),
            )
            1 -> SharedGroupScreen(
                modifier = Modifier.padding(innerPadding),
            )
            else -> SettingsPlaceholderScreen(
                onSignOut = {
                    authViewModel.signOut()
                    onSignOut()
                },
                modifier = Modifier.padding(innerPadding),
            )
        }
    }
}
