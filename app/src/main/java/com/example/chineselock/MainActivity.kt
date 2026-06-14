package com.example.chineselock

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.navArgument
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.chineselock.feature.add.AddScreen
import com.example.chineselock.feature.capture.CaptureMode
import com.example.chineselock.feature.capture.CaptureScreen
import com.example.chineselock.feature.conversation.ConversationScreen
import com.example.chineselock.feature.favorites.FavoritesScreen
import com.example.chineselock.feature.home.HomeScreen
import com.example.chineselock.feature.search.WordSearchScreen
import com.example.chineselock.feature.vocab.VocabScreen
import com.example.chineselock.ui.AdBanner
import com.example.chineselock.ui.theme.ChineseLockTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { ChineseLockTheme { AppRoot() } }
    }
}

private enum class Tab(val route: String, val label: String, val icon: ImageVector) {
    Home("home", "홈", Icons.Filled.Home),
    Vocab("vocab", "단어장", Icons.AutoMirrored.Filled.MenuBook),
    Conversation("conversation", "회화", Icons.AutoMirrored.Filled.Chat),
    Favorites("favorites", "즐겨찾기", Icons.Filled.Star),
}

@Composable
private fun AppRoot() {
    val nav = rememberNavController()
    val backStack by nav.currentBackStackEntryAsState()
    val current = backStack?.destination?.route
    val showBottomBar = Tab.entries.any { it.route == current }

    Scaffold(
        bottomBar = {
            if (showBottomBar) {
                Column {
                    AdBanner()
                    NavigationBar {
                        Tab.entries.forEach { tab ->
                            NavigationBarItem(
                                selected = current == tab.route,
                                onClick = {
                                    nav.navigate(tab.route) {
                                        popUpTo(nav.graph.findStartDestination().id) { saveState = true }
                                        launchSingleTop = true
                                        restoreState = true
                                    }
                                },
                                icon = { Icon(tab.icon, tab.label) },
                                label = { Text(tab.label) },
                            )
                        }
                    }
                }
            }
        },
    ) { padding ->
        NavHost(
            navController = nav,
            startDestination = Tab.Home.route,
            modifier = Modifier.padding(padding),
        ) {
            composable(Tab.Home.route) {
                HomeScreen(
                    onAddVocab = { nav.navigate("add") },
                    onAddDialogue = { nav.navigate("capture/dialogue") },
                    onSearchClick = { nav.navigate("search") },
                )
            }
            composable(Tab.Vocab.route) { VocabScreen() }
            composable(Tab.Conversation.route) {
                ConversationScreen(
                    onCaptureTranslation = { unitId -> nav.navigate("capture/translation/$unitId") },
                )
            }
            composable(Tab.Favorites.route) { FavoritesScreen() }
            composable("search") { WordSearchScreen(onBack = { nav.popBackStack() }) }
            composable("add") {
                AddScreen(
                    onBack = { nav.popBackStack() },
                    onCapture = { nav.navigate("capture/vocab") },
                )
            }
            composable("capture/vocab") {
                CaptureScreen(onBack = { nav.popBackStack() }, mode = CaptureMode.VOCAB)
            }
            composable("capture/dialogue") {
                CaptureScreen(onBack = { nav.popBackStack() }, mode = CaptureMode.DIALOGUE)
            }
            composable(
                "capture/translation/{unitId}",
                arguments = listOf(navArgument("unitId") { type = NavType.LongType }),
            ) { entry ->
                CaptureScreen(
                    onBack = { nav.popBackStack() },
                    mode = CaptureMode.TRANSLATION,
                    unitId = entry.arguments?.getLong("unitId"),
                )
            }
        }
    }
}
