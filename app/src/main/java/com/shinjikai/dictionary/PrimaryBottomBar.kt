package com.shinjikai.dictionary

import androidx.compose.foundation.layout.RowScope
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarDefaults
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemColors
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.shinjikai.dictionary.ui.Screen

@Composable
fun PrimaryBottomBar(
    currentScreen: Screen,
    onSearchClick: () -> Unit,
    onBrowseClick: () -> Unit,
    onHistoryClick: () -> Unit,
    onBookmarksClick: () -> Unit,
    onSettingsClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val itemColors = NavigationBarItemDefaults.colors(
        selectedIconColor = MaterialTheme.colorScheme.onSecondaryContainer,
        selectedTextColor = MaterialTheme.colorScheme.onSurface,
        indicatorColor = MaterialTheme.colorScheme.secondaryContainer,
        unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
        unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
    )

    NavigationBar(
        modifier = modifier,
        containerColor = MaterialTheme.colorScheme.surfaceContainer,
        contentColor = MaterialTheme.colorScheme.onSurface,
        windowInsets = NavigationBarDefaults.windowInsets
    ) {
                BottomBarItem(
                    selected = currentScreen == Screen.Browse,
                    onClick = onBrowseClick,
                    icon = { modifier ->
                        Icon(
                            Icons.AutoMirrored.Filled.MenuBook,
                            contentDescription = stringResource(R.string.nav_browse),
                            modifier = modifier
                        )
                    },
                    label = stringResource(R.string.nav_browse),
                    colors = itemColors
                )
                BottomBarItem(
                    selected = currentScreen == Screen.History,
                    onClick = onHistoryClick,
                    icon = { modifier ->
                        Icon(
                            Icons.Default.History,
                            contentDescription = stringResource(R.string.history_title),
                            modifier = modifier
                        )
                    },
                    label = stringResource(R.string.history_title),
                    colors = itemColors
                )
                BottomBarItem(
                    selected = currentScreen == Screen.Search,
                    onClick = onSearchClick,
                    icon = { modifier ->
                        Icon(
                            Icons.Default.Search,
                            contentDescription = stringResource(R.string.nav_search),
                            modifier = modifier
                        )
                    },
                    label = stringResource(R.string.nav_search),
                    colors = itemColors
                )
                BottomBarItem(
                    selected = currentScreen == Screen.Bookmarks,
                    onClick = onBookmarksClick,
                    icon = { modifier ->
                        Icon(
                            Icons.Default.Bookmark,
                            contentDescription = stringResource(R.string.nav_bookmarks),
                            modifier = modifier
                        )
                    },
                    label = stringResource(R.string.nav_bookmarks),
                    colors = itemColors
                )
                BottomBarItem(
                    selected = currentScreen == Screen.Settings,
                    onClick = onSettingsClick,
                    icon = { modifier ->
                        Icon(
                            Icons.Default.Settings,
                            contentDescription = stringResource(R.string.nav_settings),
                            modifier = modifier
                        )
                    },
                    label = stringResource(R.string.nav_settings),
                    colors = itemColors
                )
        }
}

@Composable
private fun RowScope.BottomBarItem(
    selected: Boolean,
    onClick: () -> Unit,
    icon: @Composable (Modifier) -> Unit,
    label: String,
    colors: NavigationBarItemColors
) {
    NavigationBarItem(
        selected = selected,
        onClick = onClick,
        icon = {
            icon(Modifier)
        },
        label = {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall
            )
        },
        alwaysShowLabel = true,
        colors = colors
    )
}
