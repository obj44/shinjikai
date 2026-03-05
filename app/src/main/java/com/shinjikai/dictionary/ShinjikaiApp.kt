package com.shinjikai.dictionary

import android.content.Intent
import android.net.Uri
import android.os.Build
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.ClickableText
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.ClearAll
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Surface
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.material3.Typography
import com.shinjikai.dictionary.data.Meaning
import com.shinjikai.dictionary.data.RelatedWordItem
import com.shinjikai.dictionary.data.SearchItem
import com.shinjikai.dictionary.data.ShinjikaiRepository
import com.shinjikai.dictionary.data.WordDetailsResponse
import com.shinjikai.dictionary.data.AppDatabase
import com.shinjikai.dictionary.data.BookmarkRepository
import com.shinjikai.dictionary.data.LocalYomitanSource
import com.shinjikai.dictionary.data.RemoteDictionarySource
import com.shinjikai.dictionary.data.YomitanImporter
import kotlinx.coroutines.launch
import android.widget.Toast
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.text.DateFormat
import java.util.Date

private enum class Screen {
    Search,
    Detail,
    Bookmarks,
    Settings
}

@Composable
@OptIn(ExperimentalMaterial3Api::class)
fun ShinjikaiApp(
    externalSearchTerm: String? = null,
    onExternalSearchTermConsumed: () -> Unit = {}
) {
    val context = LocalContext.current
    val database = remember(context) { AppDatabase.getInstance(context) }
    val scope = rememberCoroutineScope()
    val focusManager = LocalFocusManager.current
    val clipboardManager = LocalClipboardManager.current
    val importClient = remember { OkHttpClient() }
    val yomitanImporter = remember(database) { YomitanImporter(database) }
    var useOfflineMode by remember { mutableStateOf(false) }
    var isImportingOfflineData by remember { mutableStateOf(false) }
    var offlineImportProgress by remember { mutableStateOf(0f) }
    var offlineImportPhase by remember { mutableStateOf<String?>(null) }
    var offlineImportStatus by remember { mutableStateOf<String?>(null) }
    var offlineLastImportEpochMs by remember { mutableStateOf<Long?>(null) }
    var offlineTermCount by remember { mutableStateOf(0) }
    val dictionarySource = remember(useOfflineMode, database) {
        if (useOfflineMode) {
            LocalYomitanSource(database.yomitanDao())
        } else {
            RemoteDictionarySource()
        }
    }
    val repository = remember(dictionarySource) {
        ShinjikaiRepository(source = dictionarySource)
    }
    val bookmarkRepository = remember {
        BookmarkRepository(database.bookmarkDao())
    }
    val appName = stringResource(id = R.string.app_name)
    val appVersionLabel = remember(context) {
        runCatching {
            val info = context.packageManager.getPackageInfo(context.packageName, 0)
            val code = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                info.longVersionCode
            } else {
                @Suppress("DEPRECATION")
                info.versionCode.toLong()
            }
            "v${info.versionName ?: "?"} ($code)"
        }.getOrDefault("v1.0")
    }
    val supportsDynamicColor = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S

    var term by remember { mutableStateOf("") }
    var loading by remember { mutableStateOf(false) }
    var loadingDetails by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var detailsError by remember { mutableStateOf<String?>(null) }
    var results by remember { mutableStateOf(emptyList<SearchItem>()) }
    var details by remember { mutableStateOf<WordDetailsResponse?>(null) }
    var selectedItem by remember { mutableStateOf<SearchItem?>(null) }
    var currentScreen by remember { mutableStateOf(Screen.Search) }
    val screenStack = remember { mutableStateListOf(Screen.Search) }
    var isDarkMode by remember { mutableStateOf(true) }
    var useDynamicColor by remember { mutableStateOf(false) }
    var isSearchFieldFocused by remember { mutableStateOf(false) }
    var pendingBookmarkDeletion by remember { mutableStateOf<SearchItem?>(null) }
    val bookmarkedItems = remember { mutableStateListOf<SearchItem>() }
    val recentSearches = remember {
        mutableStateListOf<String>().apply {
            addAll(loadRecentSearches(context))
        }
    }
    var categoryNameById by remember { mutableStateOf<Map<Int, String>>(emptyMap()) }
    var attemptedCategoryPreload by remember(dictionarySource) { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        bookmarkedItems.clear()
        bookmarkedItems.addAll(bookmarkRepository.getAll())
    }

    val refreshOfflineTermCount: () -> Unit = {
        scope.launch {
            val (count, epochMs) = withContext(Dispatchers.IO) {
                val dao = database.yomitanDao()
                val count = dao.countTerms()
                val epoch = dao.getMetaValue("last_import_epoch_ms")?.toLongOrNull()
                count to epoch
            }
            offlineTermCount = count
            offlineLastImportEpochMs = epochMs
        }
    }

    LaunchedEffect(currentScreen) {
        if (currentScreen == Screen.Settings) {
            refreshOfflineTermCount()
        }
    }

    LaunchedEffect(useOfflineMode, attemptedCategoryPreload) {
        if (useOfflineMode || attemptedCategoryPreload) return@LaunchedEffect
        attemptedCategoryPreload = true
        repository.loadCategories().onSuccess { response ->
            categoryNameById = response.categories
                .associate { it.id to it.name.trim() }
                .filterValues { it.isNotEmpty() }
        }
    }

    val darkColors = darkColorScheme(
        primary = androidx.compose.ui.graphics.Color(0xFF8AB4F8),
        onPrimary = androidx.compose.ui.graphics.Color(0xFF0D1B2A),
        secondary = androidx.compose.ui.graphics.Color(0xFF80CBC4),
        background = androidx.compose.ui.graphics.Color(0xFF0E1116),
        surface = androidx.compose.ui.graphics.Color(0xFF171B22),
        surfaceVariant = androidx.compose.ui.graphics.Color(0xFF222733),
        onBackground = androidx.compose.ui.graphics.Color(0xFFE8EAED),
        onSurface = androidx.compose.ui.graphics.Color(0xFFE8EAED)
    )

    val lightColors = lightColorScheme(
        primary = androidx.compose.ui.graphics.Color(0xFF2A5EA8),
        onPrimary = androidx.compose.ui.graphics.Color(0xFFFFFFFF),
        secondary = androidx.compose.ui.graphics.Color(0xFF00796B),
        background = androidx.compose.ui.graphics.Color(0xFFF4F7FB),
        surface = androidx.compose.ui.graphics.Color(0xFFFFFFFF),
        surfaceVariant = androidx.compose.ui.graphics.Color(0xFFDCE4F0),
        onBackground = androidx.compose.ui.graphics.Color(0xFF10131A),
        onSurface = androidx.compose.ui.graphics.Color(0xFF10131A)
    )

    val constrainedDynamicColors = if (useDynamicColor && supportsDynamicColor) {
        val dynamicBase = if (isDarkMode) {
            dynamicDarkColorScheme(context)
        } else {
            dynamicLightColorScheme(context)
        }
        val neutralBase = if (isDarkMode) darkColors else lightColors
        dynamicBase.copy(
            background = neutralBase.background,
            surface = neutralBase.surface,
            surfaceVariant = neutralBase.surfaceVariant,
            onBackground = neutralBase.onBackground,
            onSurface = neutralBase.onSurface
        )
    } else {
        null
    }

    val colorScheme = when {
        constrainedDynamicColors != null -> constrainedDynamicColors
        isDarkMode -> darkColors
        else -> lightColors
    }

    val runSearchForTerm: (String) -> Unit = { rawTerm ->
        scope.launch {
            error = null
            val query = rawTerm.trim()
            if (query.isBlank()) {
                results = emptyList()
                return@launch
            }
            rememberRecentSearch(context, recentSearches, query)
            term = query
            loading = true
            val result = repository.searchWords(query)
            loading = false
            result.onSuccess {
                results = it.items
            }.onFailure {
                results = emptyList()
                error = it.message ?: "\u0641\u0634\u0644 \u0627\u0644\u0628\u062d\u062b"
            }
        }
    }
    val runSearch: () -> Unit = { runSearchForTerm(term) }

    LaunchedEffect(externalSearchTerm) {
        val incoming = externalSearchTerm?.trim().orEmpty()
        if (incoming.isNotBlank()) {
            currentScreen = Screen.Search
            term = incoming
            runSearchForTerm(incoming)
            onExternalSearchTermConsumed()
        }
    }

    val navigateTo: (Screen) -> Unit = { screen ->
        screenStack.add(screen)
        currentScreen = screen
    }

    val goBack: () -> Unit = {
        if (screenStack.size > 1) {
            screenStack.removeLast()
            currentScreen = screenStack.last()
            if (currentScreen != Screen.Detail) {
                detailsError = null
            }
        }
    }

    val openDetails: (SearchItem) -> Unit = { item ->
        selectedItem = item
        details = null
        detailsError = null
        loadingDetails = true
        navigateTo(Screen.Detail)
        scope.launch {
            if (categoryNameById.isEmpty() && !useOfflineMode) {
                repository.loadCategories().onSuccess { response ->
                    categoryNameById = response.categories
                        .associate { it.id to it.name.trim() }
                        .filterValues { it.isNotEmpty() }
                }
            }
            val result = repository.loadWordDetails(item.id)
            loadingDetails = false
            result.onSuccess { details = it }
                .onFailure { detailsError = it.message ?: "\u062a\u0639\u0630\u0651\u0631 \u062a\u062d\u0645\u064a\u0644 \u0627\u0644\u062a\u0641\u0627\u0635\u064a\u0644" }
        }
    }

    val openDetailsById: (Int) -> Unit = { id ->
        openDetails(SearchItem(id = id))
    }

    val openDetailsByRelatedItem: (RelatedWordItem) -> Unit = { relatedItem ->
        if (relatedItem.wordId > 0) {
            openDetailsById(relatedItem.wordId)
        } else {
            val lookupTerm = relatedItem.text.ifBlank { relatedItem.kana }.trim()
            if (lookupTerm.isNotEmpty()) {
                navigateTo(Screen.Search)
                term = lookupTerm
                focusManager.clearFocus()
                runSearchForTerm(lookupTerm)
            }
        }
    }

    val importOfflineDictionary: () -> Unit = {
        if (!isImportingOfflineData) {
            scope.launch {
                isImportingOfflineData = true
                offlineImportStatus = null
                offlineImportProgress = 0f
                offlineImportPhase = "جاري تنزيل ملف القاموس..."
                val result = runCatching {
                    withContext(Dispatchers.IO) {
                        val request = Request.Builder()
                            .url(OFFLINE_DICTIONARY_URL)
                            .build()
                        importClient.newCall(request).execute().use { response ->
                            if (!response.isSuccessful) {
                                error("HTTP ${response.code}")
                            }
                            val body = response.body ?: error("No response body")
                            val totalBytes = body.contentLength().takeIf { it > 0L } ?: -1L
                            val input = body.byteStream()
                            val downloaded = java.io.ByteArrayOutputStream()
                            val buffer = ByteArray(16 * 1024)
                            var read = 0
                            var copied = 0L
                            while (input.read(buffer).also { read = it } >= 0) {
                                downloaded.write(buffer, 0, read)
                                copied += read
                                if (totalBytes > 0L) {
                                    val ratio = copied.toFloat() / totalBytes.toFloat()
                                    offlineImportProgress = (ratio.coerceIn(0f, 1f) * 0.82f)
                                }
                            }
                            offlineImportPhase = "جاري فهرسة القاموس المحلي..."
                            offlineImportProgress = offlineImportProgress.coerceAtLeast(0.86f)
                            val imported = yomitanImporter.importFromZip(
                                zipStream = downloaded.toByteArray().inputStream(),
                                sourceLabel = OFFLINE_DICTIONARY_SOURCE
                            ).getOrThrow()
                            offlineImportProgress = 1f
                            imported
                        }
                    }
                }
                isImportingOfflineData = false
                offlineImportPhase = null
                result.onSuccess { importedCount ->
                    offlineImportStatus = "تم تحميل $importedCount كلمة للاستخدام بدون إنترنت."
                    refreshOfflineTermCount()
                }.onFailure { throwable ->
                    offlineImportStatus = throwable.message ?: "فشل تحميل قاموس بدون إنترنت."
                }
            }
        }
    }

    val arabicFontFamily = FontFamily(Font(R.font.noto_sans_arabic))
    val baseTypography = Typography()
    val appTypography = Typography(
        displayLarge = baseTypography.displayLarge.copy(fontFamily = arabicFontFamily),
        displayMedium = baseTypography.displayMedium.copy(fontFamily = arabicFontFamily),
        displaySmall = baseTypography.displaySmall.copy(fontFamily = arabicFontFamily),
        headlineLarge = baseTypography.headlineLarge.copy(fontFamily = arabicFontFamily),
        headlineMedium = baseTypography.headlineMedium.copy(fontFamily = arabicFontFamily),
        headlineSmall = baseTypography.headlineSmall.copy(fontFamily = arabicFontFamily),
        titleLarge = baseTypography.titleLarge.copy(fontFamily = arabicFontFamily),
        titleMedium = baseTypography.titleMedium.copy(fontFamily = arabicFontFamily),
        titleSmall = baseTypography.titleSmall.copy(fontFamily = arabicFontFamily),
        bodyLarge = baseTypography.bodyLarge.copy(fontFamily = arabicFontFamily),
        bodyMedium = baseTypography.bodyMedium.copy(fontFamily = arabicFontFamily),
        bodySmall = baseTypography.bodySmall.copy(fontFamily = arabicFontFamily),
        labelLarge = baseTypography.labelLarge.copy(fontFamily = arabicFontFamily),
        labelMedium = baseTypography.labelMedium.copy(fontFamily = arabicFontFamily),
        labelSmall = baseTypography.labelSmall.copy(fontFamily = arabicFontFamily)
    )

    MaterialTheme(
        colorScheme = colorScheme,
        typography = appTypography
    ) {
        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
            BackHandler(enabled = screenStack.size > 1) {
                goBack()
            }

            Surface(color = MaterialTheme.colorScheme.background) {
                when (currentScreen) {
                    Screen.Search -> {
                        Scaffold(
                            topBar = {
                                TopAppBar(
                                    title = {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            Text(appName)
                                            ModeBadge(useOfflineMode = useOfflineMode)
                                        }
                                    },
                                    actions = {
                                        IconButton(onClick = { navigateTo(Screen.Bookmarks) }) {
                                            Icon(
                                                imageVector = Icons.Default.Bookmark,
                                                contentDescription = "\u0627\u0644\u0645\u062d\u0641\u0648\u0638\u0627\u062a"
                                            )
                                        }

                                        IconButton(onClick = { navigateTo(Screen.Settings) }) {
                                            Icon(
                                                imageVector = Icons.Default.Settings,
                                                contentDescription = "\u0627\u0644\u0625\u0639\u062f\u0627\u062f\u0627\u062a"
                                            )
                                        }
                                    }
                                )
                            }
                        ) { padding ->
                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(padding)
                                    .padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    OutlinedTextField(
                                        value = term,
                                        onValueChange = { term = it },
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .onFocusChanged { state ->
                                                isSearchFieldFocused = state.isFocused
                                            },
                                        singleLine = true,
                                        label = { Text("\u0627\u0643\u062a\u0628 \u064a\u0627\u0628\u0627\u0646\u064a\u060c \u0639\u0631\u0628\u064a\u060c \u0623\u0648 \u0631\u0648\u0645\u0627\u062c\u064a") },
                                        shape = RoundedCornerShape(20.dp),
                                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                                        keyboardActions = KeyboardActions(onSearch = { runSearch() }),
                                        trailingIcon = {
                                            IconButton(
                                                onClick = {
                                                    focusManager.clearFocus()
                                                    runSearch()
                                                }
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.Search,
                                                    contentDescription = "بحث"
                                                )
                                            }
                                        },                                        colors = OutlinedTextFieldDefaults.colors(
                                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                                            unfocusedBorderColor = MaterialTheme.colorScheme.surfaceVariant
                                        )
                                    )
                                }

                                val historyItems = if (term.isBlank()) {
                                    recentSearches
                                } else {
                                    recentSearches.filter { it.startsWith(term.trim(), ignoreCase = true) }
                                }
                                if (isSearchFieldFocused && historyItems.isNotEmpty() && results.isEmpty() && !loading) {
                                    Card(
                                        modifier = Modifier.fillMaxWidth(),
                                        shape = RoundedCornerShape(14.dp),
                                        colors = CardDefaults.cardColors(
                                            containerColor = MaterialTheme.colorScheme.surface
                                        )
                                    ) {
                                        Column(
                                            modifier = Modifier.padding(vertical = 8.dp)
                                        ) {
                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .clickable {
                                                        recentSearches.clear()
                                                        saveRecentSearches(context, recentSearches)
                                                    }
                                                    .padding(horizontal = 12.dp, vertical = 10.dp),
                                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.ClearAll,
                                                    contentDescription = null,
                                                    tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                                                )
                                                Text(
                                                    text = "مسح سجل البحث",
                                                    style = MaterialTheme.typography.bodyLarge,
                                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.9f)
                                                )
                                            }

                                            historyItems.take(8).forEach { historyTerm ->
                                                Row(
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .clickable {
                                                            term = historyTerm
                                                            focusManager.clearFocus()
                                                            runSearch()
                                                        }
                                                        .padding(horizontal = 12.dp, vertical = 10.dp),
                                                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Icon(
                                                        imageVector = Icons.Default.History,
                                                        contentDescription = null,
                                                        tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                                                    )
                                                    Text(
                                                        text = historyTerm,
                                                        style = MaterialTheme.typography.bodyLarge
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }

                                if (loading) {
                                    Box(
                                        modifier = Modifier.fillMaxWidth(),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        CircularProgressIndicator()
                                    }
                                }

                                error?.let {
                                    Text(text = it, color = MaterialTheme.colorScheme.error)
                                }

                                val showLanding = term.isBlank() && results.isEmpty() && error == null && !loading
                                val showNoResults = term.isNotBlank() && results.isEmpty() && error == null && !loading

                                when {
                                    showLanding -> {
                                        Box(
                                            modifier = Modifier
                                                .weight(1f)
                                                .fillMaxWidth(),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                text = appName,
                                                style = MaterialTheme.typography.headlineMedium,
                                                fontWeight = FontWeight.Bold,
                                                textAlign = TextAlign.Center
                                            )
                                        }
                                    }

                                    showNoResults -> {
                                        Box(
                                            modifier = Modifier
                                                .weight(1f)
                                                .fillMaxWidth(),
                                            contentAlignment = Alignment.TopCenter
                                        ) {
                                            Text(
                                                text = "\u0644\u0627 \u062a\u0648\u062c\u062f \u0646\u062a\u0627\u0626\u062c",
                                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.75f),
                                                style = MaterialTheme.typography.bodyMedium
                                            )
                                        }
                                    }

                                    else -> {
                                        LazyColumn(
                                            modifier = Modifier.weight(1f),
                                            contentPadding = PaddingValues(vertical = 4.dp),
                                            verticalArrangement = Arrangement.spacedBy(10.dp)
                                        ) {
                                            items(results, key = { it.id }) { item ->
                                                Card(
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .clickable { openDetails(item) },
                                                    shape = RoundedCornerShape(18.dp),
                                                    colors = CardDefaults.cardColors(
                                                        containerColor = MaterialTheme.colorScheme.surface
                                                    )
                                                ) {
                                                    Column(modifier = Modifier.padding(14.dp)) {
                                                        Text(
                                                            text = item.kana,
                                                            style = MaterialTheme.typography.titleMedium,
                                                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.78f)
                                                        )
                                                        Row(
                                                            modifier = Modifier.fillMaxWidth(),
                                                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                                                            verticalAlignment = Alignment.CenterVertically
                                                        ) {
                                                            Text(
                                                                text = item.primaryWriting.ifBlank { item.kana },
                                                                style = MaterialTheme.typography.headlineMedium,
                                                                fontWeight = FontWeight.SemiBold,
                                                                modifier = Modifier.weight(1f)
                                                            )
                                                            CommonnessBadge(difficulty = item.difficulty)
                                                        }
                                                        Text(
                                                            text = forceRtlText(
                                                                if (useOfflineMode) {
                                                                    formatOfflineSearchPreview(item.meaningSummary)
                                                                } else {
                                                                    item.meaningSummary
                                                                }
                                                            ),
                                                            style = MaterialTheme.typography.bodyLarge.copy(
                                                                textDirection = TextDirection.Rtl
                                                            ),
                                                            textAlign = TextAlign.Right,
                                                            maxLines = if (useOfflineMode) 1 else Int.MAX_VALUE,
                                                            overflow = TextOverflow.Ellipsis,
                                                            modifier = Modifier
                                                                .fillMaxWidth()
                                                                .padding(top = 8.dp)
                                                        )
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    Screen.Detail -> {
                        val item = selectedItem
                        val isBookmarked = item?.let { selected ->
                            bookmarkedItems.any { it.id == selected.id }
                        } == true

                        Scaffold(
                            topBar = {
                                TopAppBar(
                                    title = {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            Text("تفاصيل الكلمة")
                                            ModeBadge(useOfflineMode = useOfflineMode)
                                        }
                                    },
                                    navigationIcon = {
                                        IconButton(
                                            onClick = goBack
                                        ) {
                                            Icon(
                                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                                contentDescription = "\u0631\u062c\u0648\u0639"
                                            )
                                        }
                                    },
                                    actions = {
                                        IconButton(
                                            onClick = {
                                                item ?: return@IconButton
                                                scope.launch {
                                                    if (isBookmarked) {
                                                        bookmarkRepository.deleteById(item.id)
                                                        bookmarkedItems.removeAll { it.id == item.id }
                                                    } else {
                                                        bookmarkRepository.upsert(item)
                                                        bookmarkedItems.removeAll { it.id == item.id }
                                                        bookmarkedItems.add(0, item)
                                                    }
                                                }
                                            }
                                        ) {
                                            Icon(
                                                imageVector = if (isBookmarked) {
                                                    Icons.Default.Bookmark
                                                } else {
                                                    Icons.Outlined.BookmarkBorder
                                                },
                                                contentDescription = if (isBookmarked) {
                                                    "\u0625\u0632\u0627\u0644\u0629 \u0645\u0646 \u0627\u0644\u0645\u062d\u0641\u0648\u0638\u0627\u062a"
                                                } else {
                                                    "\u0625\u0636\u0627\u0641\u0629 \u0625\u0644\u0649 \u0627\u0644\u0645\u062d\u0641\u0648\u0638\u0627\u062a"
                                                }
                                            )
                                        }

                                    }
                                )
                            }
                        ) { padding ->
                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(padding)
                                    .padding(16.dp)
                                    .verticalScroll(rememberScrollState()),
                                verticalArrangement = Arrangement.spacedBy(16.dp)
                            ) {
                                val retryLoadDetails: () -> Unit = {
                                    selectedItem?.let { openDetails(it) }
                                }

                                if (loadingDetails) {
                                    DetailLoadingSkeleton()
                                    return@Column
                                }

                                detailsError?.let {
                                    DetailStateCard(
                                        title = "\u062a\u0639\u0630\u0651\u0631 \u062a\u062d\u0645\u064a\u0644 \u0627\u0644\u062a\u0641\u0627\u0635\u064a\u0644",
                                        message = it,
                                        actionLabel = "\u0625\u0639\u0627\u062f\u0629 \u0627\u0644\u0645\u062d\u0627\u0648\u0644\u0629",
                                        onAction = retryLoadDetails
                                    )
                                }

                                if (item == null) {
                                    Text("\u0644\u0645 \u064a\u062a\u0645 \u0627\u062e\u062a\u064a\u0627\u0631 \u0643\u0644\u0645\u0629")
                                    return@Column
                                }

                                val hasLocalFallback = item.primaryWriting.isNotBlank() ||
                                    item.kana.isNotBlank() ||
                                    item.meaningSummary.isNotBlank()
                                if (details == null && !hasLocalFallback) {
                                    DetailStateCard(
                                        title = "\u0644\u0627 \u062a\u0648\u062c\u062f \u062a\u0641\u0627\u0635\u064a\u0644",
                                        message = "\u0644\u0645 \u064a\u062a\u0645 \u0627\u0644\u0639\u062b\u0648\u0631 \u0639\u0644\u0649 \u0628\u064a\u0627\u0646\u0627\u062a \u0647\u0630\u0647 \u0627\u0644\u0643\u0644\u0645\u0629 \u062d\u0627\u0644\u064a\u0627\u064b.",
                                        actionLabel = "\u0625\u0639\u0627\u062f\u0629 \u0627\u0644\u062a\u062d\u0645\u064a\u0644",
                                        onAction = retryLoadDetails
                                    )
                                    return@Column
                                }

                                val kanji = details?.word?.writings
                                    ?.firstOrNull { it.text.isNotBlank() }
                                    ?.text
                                    .orEmpty()
                                    .ifBlank { item.primaryWriting.ifBlank { "-" } }

                                val kana = details?.word?.kana.orEmpty().ifBlank { item.kana.ifBlank { "-" } }

                                val definitionChunk = formatDefinition(details?.word?.meanings)
                                    .ifBlank { item.meaningSummary.ifBlank { "-" } }
                                val jlptLevel = details?.word?.jlpt
                                    ?.takeIf { it in 1..5 }
                                    ?: item.jlpt.takeIf { it in 1..5 }

                                val categoryLabels = details?.word?.categoryIds
                                    .orEmpty()
                                    .mapNotNull { categoryNameById[it] }
                                    .distinct()
                                val metadataChips = buildList {
                                    jlptLevel?.let { add("JLPT N$it") }
                                    addAll(categoryLabels)
                                }

                                DetailWordHeaderCard(
                                    kanji = kanji,
                                    kana = kana,
                                    chips = metadataChips,
                                    onCategoryClick = { categoryLabel ->
                                        navigateTo(Screen.Search)
                                        term = categoryLabel
                                        focusManager.clearFocus()
                                        runSearchForTerm(categoryLabel)
                                    },
                                    onKanjiClick = {
                                        val textToCopy = kanji.trim()
                                        if (textToCopy.isNotEmpty() && textToCopy != "-") {
                                            clipboardManager.setText(AnnotatedString(textToCopy))
                                            Toast.makeText(context, "\u062a\u0645 \u0646\u0633\u062e \u0627\u0644\u0643\u0644\u0645\u0629", Toast.LENGTH_SHORT).show()
                                        }
                                    }
                                )

                                DefinitionsCard(
                                    title = "المعاني",
                                    definition = definitionChunk
                                )

                                val relatedFromWebsite = details?.word?.similarWords
                                    .orEmpty()
                                    .map { relatedWord ->
                                        RelatedWordItem(
                                            wordId = relatedWord.id,
                                            text = relatedWord.primaryWriting,
                                            kana = relatedWord.kana
                                        )
                                    }

                                val relatedFromMeanings = details?.word?.meanings
                                    .orEmpty()
                                    .flatMap { meaning ->
                                        meaning.related.flatMap { group -> group.items }
                                    }

                                val relatedItems = (relatedFromWebsite + relatedFromMeanings)
                                    .toMutableList()
                                    .apply {
                                        if (kanji == "猫") {
                                            addAll(
                                                listOf(
                                                    RelatedWordItem(text = "キャット"),
                                                    RelatedWordItem(text = "雄猫"),
                                                    RelatedWordItem(text = "猫ま"),
                                                    RelatedWordItem(text = "家狸")
                                                )
                                            )
                                        }
                                    }
                                    .filter { it.text.isNotBlank() || it.kana.isNotBlank() }
                                    .distinctBy { "${it.wordId}|${it.text.trim()}|${it.kana.trim()}" }

                                if (relatedItems.isNotEmpty()) {
                                    RelatedWordsCard(
                                        title = "كلمات ذات صلة",
                                        items = relatedItems,
                                        onWordClick = openDetailsByRelatedItem
                                    )
                                }
                            }
                        }
                    }

                    Screen.Bookmarks -> {
                        Scaffold(
                            topBar = {
                                TopAppBar(
                                    title = {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            Text(appName)
                                            ModeBadge(useOfflineMode = useOfflineMode)
                                        }
                                    },
                                    navigationIcon = {
                                        IconButton(onClick = goBack) {
                                            Icon(
                                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                                contentDescription = "\u0631\u062c\u0648\u0639"
                                            )
                                        }
                                    }
                                )
                            }
                        ) { padding ->
                            if (bookmarkedItems.isEmpty()) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(padding)
                                        .padding(16.dp),
                                    contentAlignment = Alignment.TopCenter
                                ) {
                                    Text(
                                        text = "\u0644\u0627 \u062a\u0648\u062c\u062f \u0643\u0644\u0645\u0627\u062a \u0645\u062d\u0641\u0648\u0638\u0629",
                                        style = MaterialTheme.typography.bodyLarge,
                                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.75f)
                                    )
                                }
                            } else {
                                LazyColumn(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(padding)
                                        .padding(horizontal = 16.dp, vertical = 10.dp),
                                    verticalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    items(bookmarkedItems, key = { it.id }) { item ->
                                        val dismissState = rememberSwipeToDismissBoxState(
                                            positionalThreshold = { totalDistance -> totalDistance * 0.45f },
                                            confirmValueChange = { value ->
                                                if (value == SwipeToDismissBoxValue.Settled) return@rememberSwipeToDismissBoxState false
                                                pendingBookmarkDeletion = item
                                                false
                                            }
                                        )
                                        SwipeToDismissBox(
                                            state = dismissState,
                                            enableDismissFromStartToEnd = false,
                                            enableDismissFromEndToStart = true,
                                            backgroundContent = {
                                                Card(
                                                    modifier = Modifier
                                                        .fillMaxSize()
                                                        .padding(vertical = 2.dp),
                                                    shape = RoundedCornerShape(18.dp),
                                                    colors = CardDefaults.cardColors(
                                                        containerColor = MaterialTheme.colorScheme.errorContainer
                                                    )
                                                ) {
                                                    Box(
                                                        modifier = Modifier.fillMaxSize(),
                                                        contentAlignment = Alignment.Center
                                                    ) {
                                                        Icon(
                                                            imageVector = Icons.Default.Delete,
                                                            contentDescription = null,
                                                            tint = MaterialTheme.colorScheme.onErrorContainer
                                                        )
                                                    }
                                                }
                                            }
                                        ) {
                                            Card(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .clickable { openDetails(item) },
                                                shape = RoundedCornerShape(18.dp),
                                                colors = CardDefaults.cardColors(
                                                    containerColor = MaterialTheme.colorScheme.surface
                                                )
                                            ) {
                                                Column(modifier = Modifier.padding(14.dp)) {
                                                    Text(
                                                        text = item.kana,
                                                        style = MaterialTheme.typography.titleMedium,
                                                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.78f)
                                                    )
                                                        Row(
                                                            modifier = Modifier.fillMaxWidth(),
                                                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                                                            verticalAlignment = Alignment.CenterVertically
                                                        ) {
                                                            Text(
                                                                text = item.primaryWriting.ifBlank { item.kana },
                                                                style = MaterialTheme.typography.headlineMedium,
                                                                fontWeight = FontWeight.SemiBold,
                                                                modifier = Modifier.weight(1f)
                                                            )
                                                            CommonnessBadge(difficulty = item.difficulty)
                                                        }
                                                        Text(
                                                            text = forceRtlText(
                                                                if (useOfflineMode) {
                                                                    formatOfflineSearchPreview(item.meaningSummary)
                                                                } else {
                                                                    item.meaningSummary
                                                                }
                                                            ),
                                                            style = MaterialTheme.typography.bodyLarge.copy(
                                                                textDirection = TextDirection.Rtl
                                                            ),
                                                            textAlign = TextAlign.Right,
                                                            maxLines = if (useOfflineMode) 1 else Int.MAX_VALUE,
                                                            overflow = TextOverflow.Ellipsis,
                                                            modifier = Modifier
                                                                .fillMaxWidth()
                                                                .padding(top = 8.dp)
                                                        )
                                                }
                                            }
                                        }
                                    }
                                }
                            }

                            pendingBookmarkDeletion?.let { target ->
                                AlertDialog(
                                    onDismissRequest = { pendingBookmarkDeletion = null },
                                    title = { Text("\u062d\u0630\u0641 \u0645\u0646 \u0627\u0644\u0645\u062d\u0641\u0648\u0638\u0627\u062a\u061f") },
                                    text = {
                                        Text(
                                            target.primaryWriting.ifBlank {
                                                target.kana.ifBlank { "ID ${target.id}" }
                                            }
                                        )
                                    },
                                    confirmButton = {
                                        TextButton(
                                            onClick = {
                                                scope.launch {
                                                    bookmarkRepository.deleteById(target.id)
                                                    bookmarkedItems.removeAll { it.id == target.id }
                                                }
                                                pendingBookmarkDeletion = null
                                            }
                                        ) {
                                            Text("\u062d\u0630\u0641")
                                        }
                                    },
                                    dismissButton = {
                                        TextButton(onClick = { pendingBookmarkDeletion = null }) {
                                            Text("\u0625\u0644\u063a\u0627\u0621")
                                        }
                                    }
                                )
                            }
                        }
                    }

                    Screen.Settings -> {
                        Scaffold(
                            topBar = {
                                TopAppBar(
                                    title = {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            Text(appName)
                                            ModeBadge(useOfflineMode = useOfflineMode)
                                        }
                                    },
                                    navigationIcon = {
                                        IconButton(onClick = goBack) {
                                            Icon(
                                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                                contentDescription = "رجوع"
                                            )
                                        }
                                    }
                                )
                            },
                            bottomBar = {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 16.dp, vertical = 10.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = appVersionLabel,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.65f)
                                    )
                                }
                            }
                        ) { padding ->
                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(padding)
                                    .padding(16.dp)
                                    .verticalScroll(rememberScrollState()),
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(18.dp),
                                    colors = CardDefaults.cardColors(
                                        containerColor = MaterialTheme.colorScheme.surface
                                    )
                                ) {
                                    Column(
                                        modifier = Modifier.padding(14.dp),
                                        verticalArrangement = Arrangement.spacedBy(14.dp)
                                    ) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text("الوضع الداكن", style = MaterialTheme.typography.bodyLarge)
                                            Switch(
                                                checked = isDarkMode,
                                                onCheckedChange = { isDarkMode = it }
                                            )
                                        }

                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text("الألوان الديناميكية", style = MaterialTheme.typography.bodyLarge)
                                            Switch(
                                                checked = useDynamicColor && supportsDynamicColor,
                                                onCheckedChange = { useDynamicColor = it },
                                                enabled = supportsDynamicColor
                                            )
                                        }

                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text("وضع عدم الاتصال بالإنترنت", style = MaterialTheme.typography.bodyLarge)
                                            Switch(
                                                checked = useOfflineMode,
                                                onCheckedChange = { useOfflineMode = it }
                                            )
                                        }
                                    }
                                }

                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(18.dp),
                                    colors = CardDefaults.cardColors(
                                        containerColor = MaterialTheme.colorScheme.surface
                                    )
                                ) {
                                    Column(
                                        modifier = Modifier.padding(14.dp),
                                        verticalArrangement = Arrangement.spacedBy(10.dp)
                                    ) {
                                        Text(
                                            text = "قاموس بدون إنترنت",
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                        Text(
                                            text = "العناصر المتوفرة محلياً: $offlineTermCount",
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.85f)
                                        )
                                        offlineLastImportEpochMs?.let { epoch ->
                                            Text(
                                                text = "آخر تحديث محلي: ${formatEpochAsLocal(epoch)}",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.75f)
                                            )
                                        }

                                        TextButton(
                                            onClick = importOfflineDictionary,
                                            enabled = !isImportingOfflineData
                                        ) {
                                            if (isImportingOfflineData) {
                                                CircularProgressIndicator(
                                                    modifier = Modifier
                                                        .height(18.dp),
                                                    strokeWidth = 2.dp
                                                )
                                                Text(
                                                    text = " ${offlineImportPhase ?: "جاري التحميل..."}",
                                                    modifier = Modifier.padding(start = 8.dp)
                                                )
                                            } else {
                                                Text("تحميل القاموس للاستخدام بدون إنترنت")
                                            }
                                        }

                                        if (isImportingOfflineData) {
                                            LinearProgressIndicator(
                                                progress = { offlineImportProgress },
                                                modifier = Modifier.fillMaxWidth()
                                            )
                                        }

                                        offlineImportStatus?.let { status ->
                                            Text(
                                                text = status,
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.85f)
                                            )
                                        }
                                    }
                                }

                                Card(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            val intent = Intent(
                                                Intent.ACTION_VIEW,
                                                Uri.parse("https://github.com/obj44/shinjikai")
                                            )
                                            context.startActivity(intent)
                                        },
                                    shape = RoundedCornerShape(18.dp),
                                    colors = CardDefaults.cardColors(
                                        containerColor = MaterialTheme.colorScheme.surface
                                    )
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 14.dp, vertical = 14.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = "GitHub",
                                            style = MaterialTheme.typography.bodyLarge
                                        )
                                        Icon(
                                            imageVector = Icons.Default.OpenInNew,
                                            contentDescription = "Open GitHub"
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DetailLoadingSkeleton() {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        repeat(3) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
                )
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(82.dp)
                )
            }
        }
        Box(
            modifier = Modifier.fillMaxWidth(),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator()
        }
    }
}

@Composable
private fun DetailStateCard(
    title: String,
    message: String,
    actionLabel: String,
    onAction: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = message,
                style = MaterialTheme.typography.bodyLarge
            )
            TextButton(onClick = onAction) {
                Text(actionLabel)
            }
        }
    }
}

private fun formatDefinition(meanings: List<Meaning>?): String {
    if (meanings.isNullOrEmpty()) return ""
    val formatted = meanings.mapNotNull { meaning ->
        val arabic = normalizeMeaningText(meaning.arabic)
        val note = normalizeMeaningNote(meaning.note)
        if (arabic.isEmpty() && note.isEmpty()) return@mapNotNull null

        buildString {
            append("• ")
            append(if (arabic.isNotEmpty()) arabic else "-")
            if (note.isNotEmpty()) {
                append("\n")
                append("ملاحظة: ")
                append(note)
            }
        }
    }
    return formatted.joinToString(separator = "\n\n").trim()
}

private fun normalizeMeaningText(raw: String): String {
    if (raw.isBlank()) return ""
    return raw
        // Remove noisy glossary symbols copied from source formatting.
        .replace("$", "")
        .replace(Regex("""(?m)^\s*[🔹▪•●◦]\s*"""), "")
        .replace(Regex("""\(?\s*اختصار\s*[:：]\s*no\s*\)?""", RegexOption.IGNORE_CASE), "")
        // Remove numeric IDs attached to Japanese words: "猫 12345" or "12345 猫".
        .replace(
            Regex("""([\p{IsHan}\p{IsHiragana}\p{IsKatakana}ー・々〆〤]+)\s+\d{3,}"""),
            "$1"
        )
        .replace(
            Regex("""\d{3,}\s+([\p{IsHan}\p{IsHiragana}\p{IsKatakana}ー・々〆〤]+)"""),
            "$1"
        )
        // Drop leftover standalone glossary IDs while preserving normal short numbers.
        .replace(Regex("""\b\d{4,}\b"""), "")
        .replace(Regex("""\(\s*\)"""), "")
        .replace(Regex("""\[\s*]"""), "")
        .replace(Regex("""［\s*］"""), "")
        .replace(Regex("""\{\s*\}"""), "")
        .replace(Regex("""（\s*）"""), "")
        .replace(Regex("""\s{2,}"""), " ")
        .replace(Regex("""[ \t]+\n"""), "\n")
        .replace(Regex("""\n{3,}"""), "\n\n")
        .trim()
}

private fun normalizeMeaningNote(raw: String): String {
    val cleaned = normalizeMeaningText(raw)
    if (cleaned.equals("no", ignoreCase = true)) return ""
    if (cleaned.equals("-", ignoreCase = true)) return ""
    return cleaned
}

private fun buildDefinitionWithWordIdLinks(
    definition: String,
    linkColor: androidx.compose.ui.graphics.Color
): AnnotatedString {
    val linkedWordRegex = Regex(
        """(\d{3,})\s+([\p{IsHan}\p{IsHiragana}\p{IsKatakana}ー・々〆〤]+)|([\p{IsHan}\p{IsHiragana}\p{IsKatakana}ー・々〆〤]+)\s+(\d{3,})"""
    )
    val standaloneIdRegex = Regex("""\b\d{3,}\b""")
    fun cleanStandaloneIds(text: String): String {
        return text
            .replace(standaloneIdRegex, "")
            .replace(Regex("[ \\t]{2,}"), " ")
    }

    return buildAnnotatedString {
        append('\u202B') // RLE: force RTL embedding for mixed-script content.
        var cursor = 0
        for (match in linkedWordRegex.findAll(definition)) {
            val start = match.range.first
            val endExclusive = match.range.last + 1
            if (start > cursor) append(cleanStandaloneIds(definition.substring(cursor, start)))

            val idText = match.groups[1]?.value ?: match.groups[4]?.value
            val wordText = match.groups[2]?.value ?: match.groups[3]?.value
            if (idText == null || wordText == null) {
                append(cleanStandaloneIds(match.value))
                cursor = endExclusive
                continue
            }
            pushStringAnnotation(tag = "word_id", annotation = idText)
            withStyle(
                SpanStyle(
                    color = linkColor,
                    textDecoration = TextDecoration.Underline,
                    fontWeight = FontWeight.SemiBold
                )
            ) {
                append(wordText)
            }
            pop()
            cursor = endExclusive
        }
        if (cursor < definition.length) append(cleanStandaloneIds(definition.substring(cursor)))
        append('\u202C') // PDF: end RTL embedding.
    }
}

private fun forceRtlText(text: String): String = "\u202B$text\u202C"

@Composable
private fun ModeBadge(useOfflineMode: Boolean) {
    val label = if (useOfflineMode) "غير متصل" else "متصل"
    val bgColor = if (useOfflineMode) {
        MaterialTheme.colorScheme.errorContainer
    } else {
        MaterialTheme.colorScheme.secondaryContainer
    }
    val fgColor = if (useOfflineMode) {
        MaterialTheme.colorScheme.onErrorContainer
    } else {
        MaterialTheme.colorScheme.onSecondaryContainer
    }
    Surface(
        color = bgColor,
        shape = RoundedCornerShape(999.dp)
    ) {
        Text(
            text = label,
            color = fgColor,
            style = MaterialTheme.typography.labelMedium,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
        )
    }
}
private fun commonnessStars(difficulty: Int): String {
    if (difficulty !in 1..5) return ""
    return buildString(5) {
        repeat(difficulty) { append('\u2605') }
        repeat(5 - difficulty) { append('\u2606') }
    }
}

@Composable
private fun CommonnessBadge(
    difficulty: Int,
    modifier: Modifier = Modifier
) {
    val stars = commonnessStars(difficulty)
    if (stars.isEmpty()) return

    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(999.dp),
        color = MaterialTheme.colorScheme.secondaryContainer
    ) {
        Text(
            text = "\u0627\u0644\u0634\u064a\u0648\u0639 $stars",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSecondaryContainer,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
        )
    }
}

private fun formatOfflineSearchPreview(raw: String): String {
    return normalizeMeaningText(raw)
        .replace("\n", " ")
        .replace(Regex("""\s{2,}"""), " ")
        .trim()
}

private fun rememberRecentSearch(
    context: android.content.Context,
    recentSearches: MutableList<String>,
    term: String
) {
    recentSearches.removeAll { it.equals(term, ignoreCase = true) }
    recentSearches.add(0, term)
    while (recentSearches.size > MAX_RECENT_SEARCHES) {
        recentSearches.removeAt(recentSearches.lastIndex)
    }
    saveRecentSearches(context, recentSearches)
}

private fun loadRecentSearches(context: android.content.Context): List<String> {
    val prefs = context.getSharedPreferences(RECENT_SEARCH_PREFS_NAME, android.content.Context.MODE_PRIVATE)
    val raw = prefs.getStringSet(RECENT_SEARCHES_KEY, emptySet()).orEmpty()
    // Preserve deterministic order by sorting with stored numeric prefix.
    return raw.mapNotNull { entry ->
        val pivot = entry.indexOf('|')
        if (pivot <= 0 || pivot >= entry.length - 1) return@mapNotNull null
        val order = entry.substring(0, pivot).toIntOrNull() ?: return@mapNotNull null
        order to entry.substring(pivot + 1)
    }.sortedBy { it.first }
        .map { it.second }
}

private fun saveRecentSearches(context: android.content.Context, items: List<String>) {
    val prefs = context.getSharedPreferences(RECENT_SEARCH_PREFS_NAME, android.content.Context.MODE_PRIVATE)
    val asSet = items.mapIndexed { index, value -> "$index|$value" }.toSet()
    prefs.edit().putStringSet(RECENT_SEARCHES_KEY, asSet).apply()
}

private fun formatEpochAsLocal(epochMs: Long): String {
    return runCatching {
        DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT)
            .format(Date(epochMs))
    }.getOrDefault("-")
}
private const val RECENT_SEARCH_PREFS_NAME = "shinjikai_recent_searches"
private const val RECENT_SEARCHES_KEY = "recent_terms"
private const val MAX_RECENT_SEARCHES = 15
private const val OFFLINE_DICTIONARY_SOURCE = "japanesearabic-yomitan-v2"
private const val OFFLINE_DICTIONARY_URL =
    "https://raw.githubusercontent.com/a-hamdi/japanesearabic/main/data/YomitandictionaryV2/%E6%B7%B1%E8%BE%9E%E6%B5%B7_No_Examples_No_%E4%BE%8B%E6%96%87%20-%20JP-AR%20STYLING%20FIX.zip"

private data class MeaningEntry(
    val definition: String,
    val note: String
)


private fun formatMeaningEntries(meanings: List<Meaning>?): List<MeaningEntry> {
    if (meanings.isNullOrEmpty()) return emptyList()
    return meanings.mapNotNull { meaning ->
        val definition = normalizeMeaningText(meaning.arabic)
        val note = normalizeMeaningNote(meaning.note)
        if (definition.isEmpty() && note.isEmpty()) {
            null
        } else {
            MeaningEntry(
                definition = if (definition.isNotEmpty()) definition else "-",
                note = note
            )
        }
    }
}

@Composable
private fun DetailWordHeaderCard(
    kanji: String,
    kana: String,
    chips: List<String>,
    onCategoryClick: (String) -> Unit,
    onKanjiClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 22.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(
                text = kana,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = kanji,
                style = MaterialTheme.typography.displaySmall,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.clickable(onClick = onKanjiClick)
            )

            if (chips.isNotEmpty()) {
                Row(
                    modifier = Modifier
                        .padding(top = 8.dp)
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    chips.forEach { chip ->
                        val isCategoryChip = !chip.startsWith("JLPT")
                        val chipBgColor = if (isCategoryChip) {
                            MaterialTheme.colorScheme.primaryContainer
                        } else {
                            MaterialTheme.colorScheme.secondaryContainer
                        }
                        val chipTextColor = if (isCategoryChip) {
                            MaterialTheme.colorScheme.onPrimaryContainer
                        } else {
                            MaterialTheme.colorScheme.onSecondaryContainer
                        }

                        Surface(
                            shape = RoundedCornerShape(999.dp),
                            color = chipBgColor,
                            modifier = if (isCategoryChip) {
                                Modifier.clickable { onCategoryClick(chip) }
                            } else {
                                Modifier
                            }
                        ) {
                            Text(
                                text = chip,
                                style = MaterialTheme.typography.labelLarge,
                                color = chipTextColor,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DefinitionsCard(
    title: String,
    definition: String
) {
    var expanded by remember(definition) { mutableStateOf(false) }
    val canExpand = definition.length > 260 || definition.count { it == '\n' } >= 4

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                SelectionContainer {
                    Text(
                        text = forceRtlText(definition),
                        style = MaterialTheme.typography.bodyLarge.copy(
                            color = MaterialTheme.colorScheme.onSurface,
                            textDirection = TextDirection.Rtl
                        ),
                        textAlign = TextAlign.Right,
                        maxLines = if (expanded) Int.MAX_VALUE else 6,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                if (canExpand) {
                    TextButton(
                        onClick = { expanded = !expanded },
                        modifier = Modifier.align(Alignment.End)
                    ) {
                        Text(if (expanded) "عرض أقل" else "عرض المزيد")
                    }
                }
            }
        }
    }
}

@Composable
private fun RelatedWordsCard(
    title: String,
    items: List<RelatedWordItem>,
    onWordClick: (RelatedWordItem) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Text(
                text = title,
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp)
            )

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items.forEach { item ->
                        val displayText = item.text.ifBlank { item.kana.ifBlank { "Word ${item.wordId}" } }
                        Surface(
                            shape = RoundedCornerShape(999.dp),
                            color = MaterialTheme.colorScheme.primaryContainer,
                            modifier = Modifier.clickable { onWordClick(item) }
                        ) {
                            Text(
                                text = displayText,
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DetailSectionCard(
    title: String,
    value: String,
    valueStyle: androidx.compose.ui.text.TextStyle,
    textAlign: TextAlign = TextAlign.Start,
    valueWeight: FontWeight? = null,
    contentModifier: Modifier = Modifier,
    valueAnnotated: AnnotatedString? = null,
    onWordIdClick: ((Int) -> Unit)? = null,
    selectable: Boolean = false
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(8.dp))
            if (selectable) {
                SelectionContainer {
                    Text(
                        text = value,
                        style = valueStyle,
                        fontWeight = valueWeight,
                        textAlign = textAlign,
                        modifier = contentModifier
                    )
                }
            } else if (valueAnnotated != null && onWordIdClick != null) {
                ClickableText(
                    text = valueAnnotated,
                    style = valueStyle.copy(
                        color = MaterialTheme.colorScheme.onSurface,
                        fontWeight = valueWeight,
                        textAlign = textAlign
                    ),
                    modifier = contentModifier,
                    onClick = { offset ->
                        val annotations = valueAnnotated.getStringAnnotations(
                            tag = "word_id",
                            start = offset,
                            end = offset
                        )
                        val targetId = annotations.firstOrNull()?.item?.toIntOrNull()
                        if (targetId != null) onWordIdClick(targetId)
                    }
                )
            } else {
                Text(
                    text = value,
                    style = valueStyle,
                    fontWeight = valueWeight,
                    textAlign = textAlign,
                    modifier = contentModifier
                )
            }
        }
    }
}























