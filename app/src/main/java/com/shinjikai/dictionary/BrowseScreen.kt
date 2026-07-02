package com.shinjikai.dictionary

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.paging.LoadState
import androidx.paging.PagingData
import androidx.paging.compose.collectAsLazyPagingItems
import com.shinjikai.dictionary.data.SearchItem
import com.shinjikai.dictionary.ui.ShinjikaiViewModel
import kotlinx.coroutines.flow.Flow

@Composable
@OptIn(ExperimentalMaterial3Api::class)
fun BrowseScreenContent(
    viewModel: ShinjikaiViewModel,
    browseFlow: Flow<PagingData<SearchItem>>,
    totalEntries: Int,
    onOpenDetails: (SearchItem) -> Unit
) {
    val entries = browseFlow.collectAsLazyPagingItems()
    val refreshState = entries.loadState.refresh

    Scaffold(
        containerColor = Color.Transparent
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp),
            contentPadding = PaddingValues(top = 8.dp, bottom = 16.dp),
            verticalArrangement = Arrangement.spacedBy(5.dp)
        ) {
            item(key = "browse-header") {
                ShinjikaiPageHeader(
                    title = stringResource(R.string.browse_title),
                    subtitle = "${stringResource(R.string.browse_subtitle)} · ${
                        stringResource(R.string.browse_count, totalEntries)
                    }",
                    icon = Icons.AutoMirrored.Filled.MenuBook,
                    action = {
                        FilledTonalIconButton(
                            onClick = { viewModel.openRandomDictionaryEntry(onOpenDetails) }
                        ) {
                            Icon(
                                imageVector = Icons.Default.Shuffle,
                                contentDescription = stringResource(R.string.browse_random)
                            )
                        }
                    }
                )
            }

            if (refreshState is LoadState.Loading) {
                item(key = "browse-loading") {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 28.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                }
            }

            if (refreshState is LoadState.NotLoading && entries.itemCount == 0) {
                item(key = "browse-empty") {
                    ShinjikaiEmptyState(
                        title = stringResource(R.string.browse_empty),
                        icon = Icons.AutoMirrored.Filled.MenuBook,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            (refreshState as? LoadState.Error)?.let { error ->
                item(key = "browse-refresh-error") {
                    TextButton(
                        onClick = { entries.retry() },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(error.error.message ?: stringResource(R.string.detail_retry))
                    }
                }
            }

            items(
                count = entries.itemCount,
                key = { index -> entries[index]?.id ?: "browse-$index" },
                contentType = { "dictionary-entry" }
            ) { index ->
                val item = entries[index] ?: return@items
                DictionaryEntryCard(
                    item = item,
                    onClick = { onOpenDetails(item) },
                    previewMaxLines = 2
                )
            }

            if (entries.loadState.append is LoadState.Loading) {
                item(key = "browse-append-loading") {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 12.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                }
            }

            (entries.loadState.append as? LoadState.Error)?.let { appendError ->
                item(key = "browse-append-error") {
                    TextButton(
                        onClick = { entries.retry() },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(appendError.error.message ?: stringResource(R.string.detail_retry))
                    }
                }
            }
        }
    }
}
