package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.Science
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import com.example.data.local.BseDatabase
import com.example.data.repository.BseStockRepository
import com.example.ui.screens.HomeScreen
import com.example.ui.screens.IpoTrackerScreen
import com.example.ui.screens.ProjectionLabScreen
import com.example.ui.screens.StockDetailScreen
import com.example.ui.screens.WatchlistScreen
import com.example.ui.theme.DalalBlueLight
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.viewmodel.AppScreen
import com.example.ui.viewmodel.BseViewModel
import com.example.ui.viewmodel.BseViewModelFactory

class MainActivity : ComponentActivity() {

    private val viewModel: BseViewModel by viewModels {
        val database = BseDatabase.getDatabase(applicationContext)
        val repository = BseStockRepository(database.bseDao())
        BseViewModelFactory(repository)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme(darkTheme = true) {
                BseAppRoot(viewModel = viewModel)
            }
        }
    }
}

data class NavItem(
    val screen: AppScreen,
    val title: String,
    val icon: ImageVector,
    val tag: String
)

@Composable
fun BseAppRoot(viewModel: BseViewModel) {
    val currentScreen by viewModel.currentScreen.collectAsState()

    val navItems = listOf(
        NavItem(AppScreen.MARKET_OVERVIEW, "BSE Market", Icons.Default.TrendingUp, "nav_market"),
        NavItem(AppScreen.PROJECTION_LAB, "5Y Models", Icons.Default.Science, "nav_projection_lab"),
        NavItem(AppScreen.IPO_RESEARCH, "IPO Audit", Icons.Default.Assessment, "nav_ipo_research"),
        NavItem(AppScreen.WATCHLIST_ALERTS, "Watchlist", Icons.Default.Bookmark, "nav_watchlist")
    )

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        bottomBar = {
            if (currentScreen != AppScreen.STOCK_DETAIL) {
                NavigationBar(
                    modifier = Modifier
                        .windowInsetsPadding(WindowInsets.navigationBars)
                        .testTag("bottom_nav_bar"),
                    containerColor = MaterialTheme.colorScheme.surface,
                    contentColor = MaterialTheme.colorScheme.onSurface
                ) {
                    navItems.forEach { item ->
                        val isSelected = currentScreen == item.screen
                        NavigationBarItem(
                            selected = isSelected,
                            onClick = { viewModel.navigateTo(item.screen) },
                            icon = { Icon(item.icon, contentDescription = item.title) },
                            label = { Text(item.title) },
                            modifier = Modifier.testTag(item.tag),
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = DalalBlueLight,
                                selectedTextColor = DalalBlueLight,
                                indicatorColor = DalalBlueLight.copy(alpha = 0.15f),
                                unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        when (currentScreen) {
            AppScreen.MARKET_OVERVIEW -> HomeScreen(
                viewModel = viewModel,
                modifier = Modifier.padding(innerPadding)
            )
            AppScreen.STOCK_DETAIL -> StockDetailScreen(
                viewModel = viewModel,
                modifier = Modifier.padding(innerPadding)
            )
            AppScreen.PROJECTION_LAB -> ProjectionLabScreen(
                viewModel = viewModel,
                modifier = Modifier.padding(innerPadding)
            )
            AppScreen.IPO_RESEARCH -> IpoTrackerScreen(
                viewModel = viewModel,
                modifier = Modifier.padding(innerPadding)
            )
            AppScreen.WATCHLIST_ALERTS -> WatchlistScreen(
                viewModel = viewModel,
                modifier = Modifier.padding(innerPadding)
            )
        }
    }
}
