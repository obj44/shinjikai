package com.shinjikai.dictionary

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.shinjikai.dictionary.data.SearchItem

@Composable
fun DictionaryEntryCard(
    item: SearchItem,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    previewMaxLines: Int = 2,
    previewText: String? = null,
    footer: (@Composable () -> Unit)? = null
) {
    val headword = remember(item.primaryWriting, item.kana) {
        item.primaryWriting.ifBlank { item.kana }.trim()
    }
    val preview = remember(item.meaningSummary, previewText) {
        previewText ?: formatOfflineSearchPreview(item.meaningSummary)
    }
    if (headword.isBlank()) return

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .clickable(role = Role.Button, onClick = onClick),
        shape = ShinjikaiUi.CompactShape,
        color = MaterialTheme.colorScheme.surface,
        border = ShinjikaiUi.cardBorder(alpha = 0.18f)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 11.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            horizontalAlignment = Alignment.End
        ) {
            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    CommonnessBadge(difficulty = item.difficulty)
                    Column(
                        modifier = Modifier.weight(1f),
                        horizontalAlignment = Alignment.End,
                        verticalArrangement = Arrangement.spacedBy(1.dp)
                    ) {
                        HeadwordFuriganaText(
                            text = headword,
                            reading = item.kana,
                            parts = item.writings.firstOrNull()?.parts,
                            modifier = Modifier.fillMaxWidth(),
                            baseStyle = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.SemiBold),
                            rubyStyle = MaterialTheme.typography.labelSmall,
                            textAlign = TextAlign.End
                        )
                    }
                }
            }

            if (preview.isNotBlank()) {
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.34f))
                Text(
                    text = forceRtlText(preview),
                    modifier = Modifier.fillMaxWidth(),
                    style = MaterialTheme.typography.bodyMedium.copy(textDirection = TextDirection.Rtl),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Right,
                    maxLines = previewMaxLines,
                    overflow = TextOverflow.Ellipsis
                )
            }

            footer?.invoke()
        }
    }
}
