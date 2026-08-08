package com.shinjikai.dictionary

import android.content.Intent
import android.content.Context
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.DownloadForOffline
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.TaskAlt
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.shinjikai.dictionary.data.AppThemeMode
import com.shinjikai.dictionary.integration.ANKIDROID_PERMISSION
import com.shinjikai.dictionary.integration.AnkiExporter
import com.shinjikai.dictionary.ui.SettingsUiState
import com.shinjikai.dictionary.ui.InstalledDictionaryUiItem
import com.shinjikai.dictionary.ui.ShinjikaiViewModel
import java.text.DateFormat
import java.util.Locale
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private data class OfflineImportStatusUi(
    val label: String,
    val color: Color
)

@Composable
@OptIn(ExperimentalMaterial3Api::class)
fun SettingsScreenContent(
    appVersionLabel: String,
    supportsDynamicColor: Boolean,
    uiState: SettingsUiState,
    viewModel: ShinjikaiViewModel,
    onOpenLocalDictionary: () -> Unit,
    onOpenAnkiExporterSettings: () -> Unit,
) {
    val context = LocalContext.current

    Scaffold(
        containerColor = Color.Transparent,
        contentWindowInsets = WindowInsets(0, 0, 0, 0)
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(
                        start = 16.dp,
                        top = 8.dp,
                        end = 16.dp,
                        bottom = ShinjikaiUi.BottomBarClearance
                    )
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                ShinjikaiPageHeader(
                    title = stringResource(R.string.settings_title),
                    icon = Icons.Filled.Settings
                )

                ShinjikaiCard(
                    modifier = Modifier.fillMaxWidth(),
                    contentPadding = 14.dp
                ) {
                    SectionLabel(text = stringResource(R.string.settings_appearance_title))
                    ThemeModeSelector(
                        selectedMode = uiState.settings.themeMode,
                        onModeSelected = viewModel::setThemeMode,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 10.dp)
                    )
                    SettingsToggleRow(
                        icon = Icons.Filled.Palette,
                        label = stringResource(R.string.settings_dynamic_color),
                        checked = uiState.settings.useDynamicColor && supportsDynamicColor,
                        onCheckedChange = viewModel::setUseDynamicColor,
                        enabled = supportsDynamicColor,
                        modifier = Modifier.padding(top = 14.dp)
                    )
                }

                ShinjikaiCard(
                    modifier = Modifier.fillMaxWidth(),
                    contentPadding = 0.dp
                ) {
                    LocalDictionarySummaryCard(
                        uiState = uiState,
                        onClick = onOpenLocalDictionary
                    )
                }

                ShinjikaiCard(
                    modifier = Modifier.fillMaxWidth(),
                    contentPadding = 14.dp
                ) {
                    SettingsLinkRow(
                        painterRes = R.drawable.ic_anki,
                        title = stringResource(R.string.settings_anki_exporter_title),
                        description = stringResource(R.string.settings_anki_exporter_description),
                        contentDescription = stringResource(R.string.settings_anki_exporter_open),
                        onClick = onOpenAnkiExporterSettings
                    )
                }

                ShinjikaiCard(
                    modifier = Modifier.fillMaxWidth(),
                    contentPadding = 14.dp
                ) {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        SettingsLinkRow(
                            icon = Icons.Filled.Public,
                            title = stringResource(R.string.settings_attribution_title),
                            description = stringResource(R.string.settings_attribution_description),
                            contentDescription = stringResource(R.string.settings_attribution_open),
                            trailingIcon = Icons.AutoMirrored.Filled.OpenInNew,
                            onClick = {
                                context.startActivity(
                                    Intent(
                                        Intent.ACTION_VIEW,
                                        Uri.parse("https://github.com/1Selxo/Shinjikai")
                                    )
                                )
                            }
                        )
                        SettingsLinkRow(
                            icon = Icons.Filled.Code,
                            title = stringResource(R.string.settings_github),
                            description = stringResource(R.string.settings_about_source_description),
                            contentDescription = stringResource(R.string.settings_open_github),
                            trailingIcon = Icons.AutoMirrored.Filled.OpenInNew,
                            onClick = {
                                context.startActivity(
                                    Intent(
                                        Intent.ACTION_VIEW,
                                        Uri.parse("https://github.com/objx0/Shinjikai-Android")
                                    )
                                )
                            }
                        )
                    }
                }

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "${stringResource(R.string.settings_about_version)}: $appVersionLabel",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.65f)
                    )
                }
            }
        }
    }
}

@Composable
@OptIn(ExperimentalMaterial3Api::class)
fun LocalDictionaryScreenContent(
    uiState: SettingsUiState,
    onPickOfflineZip: () -> Unit,
    onToggleDictionary: (String, Boolean) -> Unit,
    onDeleteDictionary: (String) -> Unit,
    onGoBack: () -> Unit
) {
    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.settings_local_dictionary)) },
                navigationIcon = {
                    IconButton(onClick = onGoBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.nav_back)
                        )
                    }
                },
                actions = {
                    FilledTonalIconButton(
                        onClick = onPickOfflineZip,
                        enabled = !uiState.isImportingOfflineData,
                        colors = IconButtonDefaults.filledTonalIconButtonColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant,
                            contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Add,
                            contentDescription = stringResource(R.string.settings_offline_import_file)
                        )
                    }
                },
                colors = shinjikaiTopAppBarColors()
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
                .padding(bottom = 12.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = stringResource(R.string.settings_dictionary_page_description),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.72f)
            )

            Text(
                text = stringResource(R.string.settings_installed_dictionaries_title),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.72f),
                letterSpacing = 1.5.sp,
                modifier = Modifier.padding(top = 8.dp)
            )

            InstalledDictionariesCard(
                dictionaries = uiState.installedDictionaries,
                isImporting = uiState.isImportingOfflineData,
                onToggleDictionary = onToggleDictionary,
                onDeleteDictionary = onDeleteDictionary,
                modifier = Modifier.fillMaxWidth()
            )

            if (uiState.isImportingOfflineData) {
                ShinjikaiCard(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                        Text(uiState.offlineImportPhase ?: stringResource(R.string.settings_loading_inline))
                    }
                    LinearProgressIndicator(
                        progress = { uiState.offlineImportProgress },
                        modifier = Modifier.fillMaxWidth().padding(top = 10.dp)
                    )
                }
            }

            uiState.offlineImportStatus?.let { status ->
                StatusMessage(message = status, isError = uiState.offlineImportError)
            }

        }
    }
}

@Composable
private fun InstalledDictionariesCard(
    dictionaries: List<InstalledDictionaryUiItem>,
    isImporting: Boolean,
    onToggleDictionary: (String, Boolean) -> Unit,
    onDeleteDictionary: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = ShinjikaiUi.CardShape,
        colors = ShinjikaiUi.cardColors(),
        border = ShinjikaiUi.cardBorder()
    ) {
        Column {
            dictionaries.forEachIndexed { index, dictionary ->
                InstalledDictionaryRow(
                    dictionary = dictionary,
                    isImporting = isImporting,
                    onToggle = { enabled -> onToggleDictionary(dictionary.id, enabled) },
                    onDelete = { onDeleteDictionary(dictionary.id) }
                )
                if (index < dictionaries.lastIndex) {
                    HorizontalDivider(
                        modifier = Modifier.padding(horizontal = 16.dp),
                        color = MaterialTheme.colorScheme.outline.copy(alpha = 0.18f)
                    )
                }
            }
        }
    }
}

@Composable
private fun InstalledDictionaryRow(
    dictionary: InstalledDictionaryUiItem,
    isImporting: Boolean,
    onToggle: (Boolean) -> Unit,
    onDelete: () -> Unit
) {
    var showDeleteConfirmation by remember(dictionary.id) { mutableStateOf(false) }
    val accent = if (dictionary.isBuiltIn) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.tertiary
    val dismissState = rememberSwipeToDismissBoxState(
        confirmValueChange = { value ->
            if (value == SwipeToDismissBoxValue.EndToStart) {
                showDeleteConfirmation = true
            }
            false
        }
    )

    SwipeToDismissBox(
        state = dismissState,
        enableDismissFromStartToEnd = false,
        enableDismissFromEndToStart = !dictionary.isBuiltIn && !isImporting,
        backgroundContent = {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.errorContainer)
                    .padding(horizontal = 20.dp),
                contentAlignment = Alignment.CenterEnd
            ) {
                Icon(
                    imageVector = Icons.Filled.Delete,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onErrorContainer
                )
            }
        },
        content = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surface)
                    .padding(horizontal = 16.dp, vertical = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(modifier = Modifier.size(12.dp).background(accent, CircleShape))
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                    Text(dictionary.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                    Text(dictionary.source, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.72f))
                    Text("ID: ${dictionary.id}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.55f))
                    dictionary.fileName?.let { fileName ->
                        Text(fileName, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.55f))
                    }
                }
                if (isImporting) {
                    CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                } else {
                    Switch(checked = dictionary.enabled, onCheckedChange = onToggle)
                }
            }
        }
    )

    if (showDeleteConfirmation) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirmation = false },
            icon = {
                Icon(
                    imageVector = Icons.Filled.Delete,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error
                )
            },
            title = { Text(stringResource(R.string.settings_delete_dictionary_title)) },
            text = {
                Text(
                    stringResource(
                        R.string.settings_delete_dictionary_message,
                        dictionary.name
                    )
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDeleteConfirmation = false
                        onDelete()
                    }
                ) {
                    Text(stringResource(R.string.settings_delete_dictionary_confirm))
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirmation = false }) {
                    Text(stringResource(R.string.action_cancel))
                }
            }
        )
    }
}

@Composable
@OptIn(ExperimentalMaterial3Api::class)
fun AnkiExporterSettingsScreenContent(
    selectedDeckName: String,
    onSelectDeck: (String) -> Unit,
    onGoBack: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val canRequestDirectAnkiAdd = remember(context) {
        AnkiExporter.canRequestDirectAdd(context)
    }
    var hasAnkiDatabasePermission by remember(context) {
        mutableStateOf(AnkiExporter.hasDatabasePermission(context))
    }
    var availableDecks by remember { mutableStateOf<List<String>>(emptyList()) }
    var statusMessage by remember { mutableStateOf<String?>(null) }
    var deckMenuExpanded by remember { mutableStateOf(false) }
    var isLoadingDecks by remember { mutableStateOf(false) }
    suspend fun refreshAvailableDecks() {
        statusMessage = null
        isLoadingDecks = true
        val decks = loadAnkiDeckNamesOffMain(context)
        isLoadingDecks = false
        availableDecks = decks
        statusMessage = if (decks.isEmpty()) {
            context.getString(R.string.settings_anki_no_decks)
        } else {
            null
        }
    }
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasAnkiDatabasePermission = granted
        if (granted) {
            coroutineScope.launch {
                refreshAvailableDecks()
            }
        } else {
            availableDecks = emptyList()
            isLoadingDecks = false
            statusMessage = context.getString(R.string.settings_anki_permission_required)
        }
    }

    LaunchedEffect(Unit) {
        when {
            !canRequestDirectAnkiAdd ->
                statusMessage = context.getString(R.string.settings_anki_install_required)
            hasAnkiDatabasePermission -> refreshAvailableDecks()
            else -> statusMessage = context.getString(R.string.settings_anki_allow_access)
        }
    }

    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.settings_anki_exporter_title)) },
                navigationIcon = {
                    IconButton(onClick = onGoBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.nav_back)
                        )
                    }
                },
                colors = shinjikaiTopAppBarColors()
            )
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
            ShinjikaiCard(modifier = Modifier.fillMaxWidth()) {
                SectionLabel(text = stringResource(R.string.settings_anki_selected_deck))
                Text(
                    selectedDeckName,
                    modifier = Modifier.padding(top = 8.dp),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    stringResource(R.string.settings_anki_selected_deck_description),
                    modifier = Modifier.padding(top = 8.dp),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.72f)
                )
            }

            if (!hasAnkiDatabasePermission && canRequestDirectAnkiAdd) {
                Button(
                    onClick = { permissionLauncher.launch(ANKIDROID_PERMISSION) },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(stringResource(R.string.settings_anki_allow_access_button))
                }
            }

            if (isLoadingDecks) {
                ShinjikaiCard(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                        Text(
                            text = stringResource(R.string.settings_loading_inline),
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }

            statusMessage?.let { message ->
                ShinjikaiCard(modifier = Modifier.fillMaxWidth()) {
                    Text(text = message, style = MaterialTheme.typography.bodyMedium)
                }
            }

            if (availableDecks.isNotEmpty()) {
                ShinjikaiCard(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        stringResource(R.string.settings_anki_available_decks),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    ExposedDropdownMenuBox(
                        expanded = deckMenuExpanded,
                        onExpandedChange = { deckMenuExpanded = !deckMenuExpanded },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp)
                    ) {
                        OutlinedTextField(
                            value = selectedDeckName,
                            onValueChange = {},
                            readOnly = true,
                            modifier = Modifier
                                .menuAnchor()
                                .fillMaxWidth(),
                            label = { Text(stringResource(R.string.settings_anki_deck_field_label)) },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = deckMenuExpanded) }
                        )
                        ExposedDropdownMenu(
                            expanded = deckMenuExpanded,
                            onDismissRequest = { deckMenuExpanded = false }
                        ) {
                            availableDecks.forEach { deckName ->
                                DropdownMenuItem(
                                    text = { Text(deckName) },
                                    onClick = {
                                        onSelectDeck(deckName)
                                        deckMenuExpanded = false
                                    },
                                    leadingIcon = {
                                        if (deckName == selectedDeckName) {
                                            Icon(
                                                imageVector = Icons.Filled.CheckCircle,
                                                contentDescription = null,
                                                tint = MaterialTheme.colorScheme.primary
                                            )
                                        }
                                    }
                                )
                            }
                        }
                    }
                }
            }

            OutlinedButton(
                onClick = { onSelectDeck("Shinjikai") },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(stringResource(R.string.settings_anki_use_default_deck))
            }

            Spacer(modifier = Modifier.height(128.dp))
        }
    }
}

private suspend fun loadAnkiDeckNamesOffMain(context: Context): List<String> =
    withContext(Dispatchers.IO) {
        runCatching { AnkiExporter.loadDeckNames(context) }.getOrDefault(emptyList())
    }

@Composable
private fun SettingsLinkRow(
    icon: ImageVector? = null,
    painterRes: Int? = null,
    title: String,
    description: String,
    contentDescription: String,
    trailingIcon: ImageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(role = Role.Button, onClick = onClick)
            .padding(vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            modifier = Modifier.weight(1f),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            when {
                painterRes != null -> SettingsLeadingPainterIcon(painterRes = painterRes)
                icon != null -> SettingsLeadingIcon(icon = icon)
            }
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.75f)
                )
            }
        }
        Icon(
            imageVector = trailingIcon,
            contentDescription = contentDescription,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(24.dp)
        )
    }
}

@Composable
private fun ThemeModeSelector(
    selectedMode: AppThemeMode,
    onModeSelected: (AppThemeMode) -> Unit,
    modifier: Modifier = Modifier
) {
    val options = listOf(
        ThemeModeOption(
            mode = AppThemeMode.System,
            icon = Icons.Filled.Public,
            label = stringResource(R.string.settings_theme_system)
        ),
        ThemeModeOption(
            mode = AppThemeMode.Light,
            icon = Icons.Filled.Palette,
            label = stringResource(R.string.settings_theme_light)
        ),
        ThemeModeOption(
            mode = AppThemeMode.Dark,
            icon = Icons.Filled.DarkMode,
            label = stringResource(R.string.settings_theme_dark)
        )
    )

    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = stringResource(R.string.settings_theme_mode),
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            options.forEach { option ->
                val selected = selectedMode == option.mode
                Surface(
                    onClick = { onModeSelected(option.mode) },
                    modifier = Modifier
                        .weight(1f)
                        .height(76.dp),
                    shape = ShinjikaiUi.CompactShape,
                    color = if (selected) {
                        MaterialTheme.colorScheme.primaryContainer
                    } else {
                        ShinjikaiUi.panelColor(alpha = 0.24f)
                    },
                    border = if (selected) {
                        ShinjikaiUi.cardBorder(alpha = 0.76f)
                    } else {
                        ShinjikaiUi.cardBorder(alpha = 0.24f)
                    }
                ) {
                    Column(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 10.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(7.dp)
                    ) {
                        Icon(
                            imageVector = option.icon,
                            contentDescription = option.label,
                            tint = if (selected) {
                                MaterialTheme.colorScheme.onPrimaryContainer
                            } else {
                                MaterialTheme.colorScheme.primary
                            }
                        )
                        Text(
                            text = option.label,
                            style = MaterialTheme.typography.labelMedium,
                            color = if (selected) {
                                MaterialTheme.colorScheme.onPrimaryContainer
                            } else {
                                MaterialTheme.colorScheme.onSurface
                            },
                            maxLines = 1
                        )
                    }
                }
            }
        }
    }
}

private data class ThemeModeOption(
    val mode: AppThemeMode,
    val icon: ImageVector,
    val label: String
)

@Composable
private fun SettingsLeadingPainterIcon(painterRes: Int) {
    Box(
        modifier = Modifier.size(36.dp),
        contentAlignment = Alignment.Center
    ) {
        Surface(
            shape = CircleShape,
            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
        ) {
            Box(
                modifier = Modifier.size(36.dp),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    painter = painterResource(painterRes),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

@Composable
private fun LocalDictionarySummaryCard(
    uiState: SettingsUiState,
    onClick: () -> Unit
) {
    val hasOfflineDictionary = uiState.offlineTermCount > 0
    val summary = when {
        uiState.isImportingOfflineData ->
            uiState.offlineImportPhase ?: stringResource(R.string.settings_loading_inline)
        uiState.offlineImportError ->
            uiState.offlineImportStatus ?: stringResource(R.string.offline_import_failure)
        hasOfflineDictionary ->
            stringResource(R.string.settings_local_summary_ready)
        else ->
            stringResource(R.string.settings_local_summary_missing)
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(role = Role.Button, onClick = onClick)
            .padding(14.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.Top
    ) {
        SettingsLeadingIcon(icon = Icons.Filled.DownloadForOffline)
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = stringResource(R.string.settings_local_dictionary),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = summary,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.75f)
            )
        }
        Icon(
            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
            contentDescription = stringResource(R.string.settings_local_dictionary),
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(24.dp)
        )
    }
}

@Composable
private fun OfflineImporterCard(
    uiState: SettingsUiState,
    onPickOfflineZip: () -> Unit,
    onOpenDownloads: () -> Unit
) {
    val hasOfflineDictionary = uiState.offlineTermCount > 0
    val statusUi = offlineImportStatusUi(uiState, hasOfflineDictionary)
    val locale = Locale.getDefault()
    val importDateFormatter = remember(locale) {
        DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT, locale)
    }
    val summaryText = when {
        uiState.isImportingOfflineData ->
            uiState.offlineImportPhase ?: stringResource(R.string.settings_loading_inline)
        hasOfflineDictionary ->
            stringResource(R.string.settings_offline_import_summary_ready, uiState.offlineTermCount)
        else ->
            stringResource(R.string.settings_offline_import_summary_empty)
    }
    val latestUpdateText = uiState.offlineLastImportEpochMs
        ?.let { epochMs -> formatEpochAsLocal(epochMs, importDateFormatter) }
        ?: stringResource(R.string.settings_last_import_never)
    val latestSourceText = uiState.offlineLastImportSource?.let(::formatImportSourceName)
        ?: stringResource(R.string.settings_last_import_source_none)

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = ShinjikaiUi.CardShape,
        colors = ShinjikaiUi.cardColors(),
        border = ShinjikaiUi.cardBorder()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .background(
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.10f),
                            shape = ShinjikaiUi.CompactShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Filled.DownloadForOffline,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = stringResource(R.string.settings_local_dictionary),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    StatusBadge(
                        label = statusUi.label,
                        color = statusUi.color
                    )
                }
            }

            Text(
                text = summaryText,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.74f)
            )

            ImporterMetricRow(
                title = stringResource(R.string.settings_local_count, uiState.offlineTermCount),
                subtitle = stringResource(R.string.settings_offline_metric_count_subtitle)
            )

            ImporterMetricRow(
                title = stringResource(R.string.settings_offline_metric_last_update_title),
                subtitle = latestUpdateText
            )

            ImporterMetricRow(
                title = stringResource(R.string.settings_offline_metric_source_title),
                subtitle = latestSourceText
            )

            ImporterMetricRow(
                title = stringResource(R.string.settings_offline_metric_supported_files_title),
                subtitle = stringResource(R.string.settings_offline_metric_supported_files_subtitle)
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Button(
                    onClick = onPickOfflineZip,
                    enabled = !uiState.isImportingOfflineData,
                    modifier = Modifier.weight(1f)
                ) {
                    if (uiState.isImportingOfflineData) {
                        Row(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(18.dp),
                                    strokeWidth = 2.dp,
                                    color = MaterialTheme.colorScheme.onPrimary
                                )
                                Text(text = stringResource(R.string.settings_offline_indexing_inline))
                            }
                    } else {
                        Text(stringResource(R.string.settings_offline_import_file))
                    }
                }
                OutlinedButton(
                    onClick = onOpenDownloads,
                    modifier = Modifier.weight(1f),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.35f))
                ) {
                    Text(stringResource(R.string.settings_offline_archives_title))
                }
            }

            if (uiState.isImportingOfflineData) {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = uiState.offlineImportPhase ?: stringResource(R.string.settings_loading_inline),
                            style = MaterialTheme.typography.bodySmall
                        )
                        Text(
                            text = "${(uiState.offlineImportProgress * 100).toInt()}%",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                        )
                    }
                    LinearProgressIndicator(
                        progress = { uiState.offlineImportProgress },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            uiState.offlineImportStatus?.let { status ->
                StatusMessage(
                    message = status,
                    isError = uiState.offlineImportError
                )
            }
        }
    }
}

private fun formatImportSourceName(source: String): String {
    return when (source) {
        "japanesearabic-yomitan-v2" -> "Yomitan V2"
        "raw-shinjikai-jp-ar-picked-zip" -> "Raw Shinjikai (ZIP)"
        "raw-shinjikai-jp-ar-picked-tar-xz" -> "Raw Shinjikai (TAR.XZ)"
        "raw-shinjikai-jp-ar-jsonl" -> "Raw Shinjikai (JSONL)"
        "bundled-1selxo-shinjikai-jsonl" -> "Bundled 1Selxo/Shinjikai"
        else -> source
    }
}

@Composable
private fun offlineImportStatusUi(
    uiState: SettingsUiState,
    hasOfflineDictionary: Boolean = uiState.offlineTermCount > 0
): OfflineImportStatusUi {
    return when {
        uiState.isImportingOfflineData -> OfflineImportStatusUi(
            label = stringResource(R.string.settings_offline_status_importing),
            color = MaterialTheme.colorScheme.primary
        )
        uiState.offlineImportError -> OfflineImportStatusUi(
            label = stringResource(R.string.settings_offline_status_retry),
            color = MaterialTheme.colorScheme.error
        )
        hasOfflineDictionary -> OfflineImportStatusUi(
            label = stringResource(R.string.settings_offline_status_ready),
            color = ShinjikaiUi.SuccessColor
        )
        else -> OfflineImportStatusUi(
            label = stringResource(R.string.settings_offline_status_missing),
            color = MaterialTheme.colorScheme.tertiary
        )
    }
}

@Composable
private fun StatusBadge(
    label: String,
    color: Color
) {
    Surface(
        shape = CircleShape,
        color = color.copy(alpha = 0.14f)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Filled.TaskAlt,
                contentDescription = null,
                tint = color,
                modifier = Modifier.size(14.dp)
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = color,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

@Composable
private fun ImporterMetricRow(
    title: String,
    subtitle: String
) {
    Surface(
        shape = ShinjikaiUi.CompactShape,
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.22f)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.74f)
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Normal,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.84f)
            )
        }
    }
}

@Composable
private fun StatusMessage(
    message: String,
    isError: Boolean
) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    val tint = if (isError) MaterialTheme.colorScheme.error else ShinjikaiUi.SuccessColor
    val background = tint.copy(alpha = 0.12f)
    val icon = if (isError) Icons.Filled.ErrorOutline else Icons.Filled.CheckCircle

    Surface(
        shape = ShinjikaiUi.CompactShape,
        color = background
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.Top
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = tint,
                modifier = Modifier.padding(top = 2.dp)
            )
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = message,
                    style = MaterialTheme.typography.bodySmall,
                    color = tint
                )
                if (isError) {
                    OutlinedButton(
                        onClick = {
                            clipboardManager.setText(AnnotatedString(message))
                            Toast.makeText(
                                context,
                                context.getString(R.string.settings_copy_message_toast),
                                Toast.LENGTH_SHORT
                            ).show()
                        },
                        border = BorderStroke(1.dp, tint.copy(alpha = 0.35f))
                    ) {
                        Icon(
                            imageVector = Icons.Filled.ContentCopy,
                            contentDescription = null,
                            tint = tint,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = stringResource(R.string.settings_copy_message_action),
                            color = tint,
                            modifier = Modifier.padding(start = 8.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SettingsToggleRow(
    icon: ImageVector,
    label: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    enabled: Boolean = true,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            modifier = Modifier.weight(1f),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            SettingsLeadingIcon(icon = icon)
            Text(label, style = MaterialTheme.typography.bodyLarge)
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            enabled = enabled
        )
    }
}

@Composable
private fun SettingsLeadingIcon(icon: ImageVector) {
    Box(
        modifier = Modifier.size(36.dp),
        contentAlignment = Alignment.Center
    ) {
        Card(
            shape = CircleShape,
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
            )
        ) {
            Box(
                modifier = Modifier.size(36.dp),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}
