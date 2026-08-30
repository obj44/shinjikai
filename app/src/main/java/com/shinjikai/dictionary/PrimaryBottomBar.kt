package com.shinjikai.dictionary

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
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
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
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
        indicatorColor = Color.Transparent,
        unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
        unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
    )
    val selectedIndex = when (currentScreen) {
        Screen.Browse -> 0
        Screen.History -> 1
        Screen.Search -> 2
        Screen.Bookmarks -> 3
        Screen.Settings -> 4
        else -> 2
    }

    BoxWithConstraints(
        modifier = modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surfaceContainer)
    ) {
        val itemWidth = maxWidth / PRIMARY_ITEM_COUNT
        val indicatorWidth = minOf(64.dp, itemWidth - 8.dp)
        val indicatorX by animateDpAsState(
            targetValue = itemWidth * selectedIndex + (itemWidth - indicatorWidth) / 2,
            animationSpec = spring(
                dampingRatio = 0.82f,
                stiffness = 420f
            ),
            label = "bottomBarIndicatorX"
        )

        Box(
            modifier = Modifier
                .offset(x = indicatorX, y = 12.dp)
                .size(width = indicatorWidth, height = 32.dp)
                .background(
                    color = MaterialTheme.colorScheme.secondaryContainer,
                    shape = MaterialTheme.shapes.extraLarge
                )
        )

        NavigationBar(
            containerColor = Color.Transparent,
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
}

@Composable
private fun RowScope.BottomBarItem(
    selected: Boolean,
    onClick: () -> Unit,
    icon: @Composable (Modifier) -> Unit,
    label: String,
    colors: NavigationBarItemColors
) {
    val iconScale by animateFloatAsState(
        targetValue = if (selected) 1.10f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "bottomBarIconScale"
    )

    NavigationBarItem(
        selected = selected,
        onClick = onClick,
        icon = {
            icon(
                Modifier.graphicsLayer {
                    scaleX = iconScale
                    scaleY = iconScale
                }
            )
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

private const val PRIMARY_ITEM_COUNT = 5
