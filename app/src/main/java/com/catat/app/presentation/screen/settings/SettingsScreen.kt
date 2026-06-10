package com.catat.app.presentation.screen.settings

import android.content.Intent
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.IosShare
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarData
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.catat.app.domain.model.ButtonPosition
import com.catat.app.domain.model.ExportFormat
import com.catat.app.domain.model.Template
import com.catat.app.presentation.theme.CatatColors
import com.catat.app.presentation.theme.CatatShapes
import com.catat.app.presentation.theme.CatatShadows
import com.catat.app.presentation.theme.CatatTextStyles
import com.catat.app.presentation.theme.cardColor
import com.catat.app.presentation.theme.iosShadow
import com.catat.app.presentation.theme.separatorColor
import com.catat.app.presentation.theme.secondaryBackground
import com.catat.app.presentation.theme.shimmer
import com.catat.app.presentation.theme.tertiaryText

@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current

    LaunchedEffect(uiState) {
        val message = (uiState as? SettingsUiState.Content)?.message
        if (!message.isNullOrBlank()) {
            snackbarHostState.showSnackbar(message)
        }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        snackbarHost = {
            SnackbarHost(snackbarHostState) { data ->
                SettingsSnackbar(data)
            }
        }
    ) { padding ->
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            color = MaterialTheme.colorScheme.background
        ) {
            when (val state = uiState) {
                SettingsUiState.Loading -> SettingsLoading()
                is SettingsUiState.Content -> {
                    SettingsContent(
                        state = state,
                        onBack = onBack,
                        onExportFormat = viewModel::updateDefaultExportFormat,
                        onScreenshotQuality = viewModel::updateScreenshotQuality,
                        onButtonPosition = viewModel::updateFloatingButtonPosition,
                        onAttachDevice = viewModel::updateAttachDeviceInfo,
                        onAttachApp = viewModel::updateAttachAppInfo,
                        onAttachNetwork = viewModel::updateAttachNetworkInfo,
                        onAttachBattery = viewModel::updateAttachBatteryInfo,
                        onAttachMemory = viewModel::updateAttachMemoryInfo,
                        onVoiceLanguage = viewModel::updateVoiceLanguage,
                        onSaveTemplate = viewModel::saveTemplate,
                        onDeleteTemplate = viewModel::deleteTemplate,
                        onClearAllReports = viewModel::clearAllReports,
                        onExportAllData = viewModel::exportAllData,
                        onShareBackup = {
                            viewModel.createBackupShareIntent()?.let { intent ->
                                context.startActivity(Intent.createChooser(intent, "Share Catat backup"))
                            }
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun SettingsContent(
    state: SettingsUiState.Content,
    onBack: () -> Unit,
    onExportFormat: (ExportFormat) -> Unit,
    onScreenshotQuality: (ScreenshotQuality) -> Unit,
    onButtonPosition: (ButtonPosition) -> Unit,
    onAttachDevice: (Boolean) -> Unit,
    onAttachApp: (Boolean) -> Unit,
    onAttachNetwork: (Boolean) -> Unit,
    onAttachBattery: (Boolean) -> Unit,
    onAttachMemory: (Boolean) -> Unit,
    onVoiceLanguage: (String) -> Unit,
    onSaveTemplate: (Long?, String, String) -> Unit,
    onDeleteTemplate: (Template) -> Unit,
    onClearAllReports: () -> Unit,
    onExportAllData: () -> Unit,
    onShareBackup: () -> Unit
) {
    var templateDialog by remember { mutableStateOf<Template?>(null) }
    var showNewTemplateDialog by remember { mutableStateOf(false) }
    var showClearConfirm by remember { mutableStateOf(false) }
    var exportFormatDialog by remember { mutableStateOf(false) }
    var qualityDialog by remember { mutableStateOf(false) }
    var voiceDialog by remember { mutableStateOf(false) }

    Column(modifier = Modifier.fillMaxSize()) {
        SettingsTopBar(onBack = onBack)

        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(22.dp)
        ) {
            SettingsSection(title = "General") {
                ValueRow(
                    label = "Default Export Format",
                    value = state.preferences.defaultExportFormat.label(),
                    onClick = { exportFormatDialog = true }
                )
                SettingsDivider()
                ValueRow(
                    label = "Screenshot Quality",
                    value = ScreenshotQuality.fromValue(state.preferences.screenshotQuality).label,
                    onClick = { qualityDialog = true }
                )
                SettingsDivider()
                SegmentedRow(
                    label = "Floating Button Position",
                    values = ButtonPosition.entries,
                    selected = state.preferences.floatingButtonPosition,
                    labelFor = { it.name.lowercase().replaceFirstChar(Char::uppercase) },
                    onSelected = onButtonPosition
                )
            }

            SettingsSection(title = "Auto-Attach") {
                ToggleRow("Device Model & OS", state.preferences.attachDeviceInfo, onAttachDevice)
                SettingsDivider()
                ToggleRow("App Name & Version", state.preferences.attachAppInfo, onAttachApp)
                SettingsDivider()
                ToggleRow("Network Type", state.preferences.attachNetworkInfo, onAttachNetwork)
                SettingsDivider()
                ToggleRow("Battery Level", state.preferences.attachBatteryInfo, onAttachBattery)
                SettingsDivider()
                ToggleRow("RAM & Storage", state.preferences.attachMemoryInfo, onAttachMemory)
            }

            SettingsSection(title = "Voice") {
                ValueRow(
                    label = "Language",
                    value = state.preferences.voiceLanguage,
                    onClick = { voiceDialog = true }
                )
            }

            SettingsSection(title = "Templates") {
                LinkRow(
                    label = "New Steps Template",
                    accent = CatatColors.Accent,
                    onClick = { showNewTemplateDialog = true },
                    leading = { Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp)) }
                )
                if (state.templates.isNotEmpty()) {
                    SettingsDivider()
                    state.templates.forEachIndexed { index, template ->
                        TemplateRow(
                            template = template,
                            onEdit = { templateDialog = template },
                            onDelete = { onDeleteTemplate(template) }
                        )
                        if (index != state.templates.lastIndex) SettingsDivider()
                    }
                }
            }

            SettingsSection(title = "Data") {
                LinkRow(
                    label = "Export All Data",
                    accent = CatatColors.Accent,
                    enabled = !state.isWorking,
                    onClick = onExportAllData
                )
                if (state.backupPath != null) {
                    SettingsDivider()
                    LinkRow(
                        label = "Share Latest Backup",
                        accent = CatatColors.Accent,
                        enabled = !state.isWorking,
                        onClick = onShareBackup,
                        leading = { Icon(Icons.Default.IosShare, contentDescription = null, modifier = Modifier.size(18.dp)) }
                    )
                }
                SettingsDivider()
                LinkRow(
                    label = "Clear All Reports",
                    accent = CatatColors.Destructive,
                    enabled = !state.isWorking,
                    onClick = { showClearConfirm = true },
                    leading = { Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(18.dp)) }
                )
            }

            SettingsSection(title = "About") {
                StaticRow(label = "Version", value = state.appVersion)
                SettingsDivider()
                StaticRow(label = "Built by", value = "Theodorus")
            }

            Spacer(Modifier.height(16.dp))
        }
    }

    if (exportFormatDialog) {
        SelectionDialog(
            title = "Default Export Format",
            values = exportFormats,
            selected = state.preferences.defaultExportFormat,
            label = { it.label() },
            onDismiss = { exportFormatDialog = false },
            onSelected = {
                onExportFormat(it)
                exportFormatDialog = false
            }
        )
    }

    if (qualityDialog) {
        SelectionDialog(
            title = "Screenshot Quality",
            values = ScreenshotQuality.entries,
            selected = ScreenshotQuality.fromValue(state.preferences.screenshotQuality),
            label = { it.label },
            onDismiss = { qualityDialog = false },
            onSelected = {
                onScreenshotQuality(it)
                qualityDialog = false
            }
        )
    }

    if (voiceDialog) {
        SelectionDialog(
            title = "Voice Language",
            values = voiceLanguages,
            selected = state.preferences.voiceLanguage,
            label = { it },
            onDismiss = { voiceDialog = false },
            onSelected = {
                onVoiceLanguage(it)
                voiceDialog = false
            }
        )
    }

    if (showNewTemplateDialog) {
        TemplateDialog(
            template = null,
            onDismiss = { showNewTemplateDialog = false },
            onSave = { id, name, content ->
                onSaveTemplate(id, name, content)
                showNewTemplateDialog = false
            }
        )
    }

    templateDialog?.let { template ->
        TemplateDialog(
            template = template,
            onDismiss = { templateDialog = null },
            onSave = { id, name, content ->
                onSaveTemplate(id, name, content)
                templateDialog = null
            }
        )
    }

    if (showClearConfirm) {
        AlertDialog(
            onDismissRequest = { showClearConfirm = false },
            title = { Text("Clear all reports") },
            text = { Text("This deletes all saved reports and screenshots.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showClearConfirm = false
                        onClearAllReports()
                    }
                ) {
                    Text("Clear", color = CatatColors.Destructive)
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearConfirm = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
private fun SettingsTopBar(onBack: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(64.dp)
            .padding(horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = onBack) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
        }
        Text(
            "Settings",
            style = CatatTextStyles.Title3
        )
    }
}

@Composable
private fun SettingsSection(title: String, content: @Composable ColumnScope.() -> Unit) {
    Column {
        Text(
            text = title.uppercase(),
            style = CatatTextStyles.Footnote,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(start = 16.dp, top = 8.dp, bottom = 8.dp)
        )
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .iosShadow(CatatShadows.shadowSm, cornerRadius = 12.dp)
                .clip(CatatShapes.md)
                .background(MaterialTheme.colorScheme.cardColor()),
            content = content
        )
    }
}

@Composable
private fun ValueRow(label: String, value: String, onClick: () -> Unit) {
    SettingsRow(
        modifier = Modifier.clickable(onClick = onClick),
        label = { Text(label, style = CatatTextStyles.Body) },
        trailing = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = value,
                    style = CatatTextStyles.Callout,
                    color = MaterialTheme.colorScheme.tertiaryText(),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Icon(
                    Icons.AutoMirrored.Filled.KeyboardArrowRight,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.tertiaryText(),
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    )
}

@Composable
private fun StaticRow(label: String, value: String) {
    SettingsRow(
        label = { Text(label, style = CatatTextStyles.Body) },
        trailing = {
            Text(
                text = value,
                style = CatatTextStyles.Callout,
                color = MaterialTheme.colorScheme.tertiaryText()
            )
        }
    )
}

@Composable
private fun <T> SegmentedRow(
    label: String,
    values: List<T>,
    selected: T,
    labelFor: (T) -> String,
    onSelected: (T) -> Unit
) {
    SettingsRow(
        label = { Text(label, style = CatatTextStyles.Body) },
        trailing = {
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                values.forEach { value ->
                    val isSelected = value == selected
                    Surface(
                        modifier = Modifier
                            .height(30.dp)
                            .clip(CatatShapes.full)
                            .clickable { onSelected(value) },
                        shape = CatatShapes.full,
                        color = if (isSelected) CatatColors.Accent else MaterialTheme.colorScheme.secondaryBackground(),
                        contentColor = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurface
                    ) {
                        Box(
                            modifier = Modifier.padding(horizontal = 12.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = labelFor(value),
                                style = CatatTextStyles.Caption1,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
            }
        }
    )
}

@Composable
private fun ToggleRow(label: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    SettingsRow(
        label = { Text(label, style = CatatTextStyles.Body) },
        trailing = {
            Switch(
                checked = checked,
                onCheckedChange = onCheckedChange,
                colors = SwitchDefaults.colors(
                    checkedThumbColor = Color.White,
                    checkedTrackColor = CatatColors.Accent
                )
            )
        }
    )
}

@Composable
private fun LinkRow(
    label: String,
    accent: Color,
    enabled: Boolean = true,
    onClick: () -> Unit,
    leading: (@Composable () -> Unit)? = null
) {
    SettingsRow(
        modifier = Modifier.clickable(enabled = enabled, onClick = onClick),
        label = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (leading != null) {
                    Box(
                        modifier = Modifier.size(22.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        leading()
                    }
                    Spacer(Modifier.width(8.dp))
                }
                Text(
                    label,
                    style = CatatTextStyles.Body,
                    color = if (enabled) accent else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        trailing = {
            if (!enabled) {
                CircularProgressIndicator(
                    modifier = Modifier.size(18.dp),
                    strokeWidth = 2.dp
                )
            }
        }
    )
}

@Composable
private fun TemplateRow(template: Template, onEdit: () -> Unit, onDelete: () -> Unit) {
    SettingsRow(
        label = {
            Column {
                Text(
                    template.name,
                    style = CatatTextStyles.Body,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    template.content,
                    style = CatatTextStyles.Footnote,
                    color = MaterialTheme.colorScheme.tertiaryText(),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        },
        trailing = {
            Row {
                IconButton(onClick = onEdit, modifier = Modifier.size(40.dp)) {
                    Icon(Icons.Default.Edit, contentDescription = "Edit template", modifier = Modifier.size(20.dp))
                }
                IconButton(onClick = onDelete, modifier = Modifier.size(40.dp)) {
                    Icon(
                        Icons.Default.Delete,
                        contentDescription = "Delete template",
                        tint = CatatColors.Destructive,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    )
}

@Composable
private fun SettingsRow(
    modifier: Modifier = Modifier,
    label: @Composable () -> Unit,
    trailing: @Composable () -> Unit
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 44.dp)
            .padding(horizontal = 16.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(modifier = Modifier.weight(1f)) {
            label()
        }
        Spacer(Modifier.width(12.dp))
        trailing()
    }
}

@Composable
private fun SettingsDivider() {
    HorizontalDivider(
        modifier = Modifier.padding(start = 16.dp),
        color = MaterialTheme.colorScheme.separatorColor(),
        thickness = 1.dp
    )
}

@Composable
private fun <T> SelectionDialog(
    title: String,
    values: List<T>,
    selected: T,
    label: (T) -> String,
    onDismiss: () -> Unit,
    onSelected: (T) -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                values.forEach { value ->
                    val isSelected = value == selected
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(44.dp)
                            .clip(CatatShapes.full)
                            .clickable { onSelected(value) },
                        color = if (isSelected) CatatColors.Accent else MaterialTheme.colorScheme.secondaryBackground(),
                        contentColor = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurface,
                        shape = CatatShapes.full,
                        border = if (isSelected) null else BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                    ) {
                        Box(
                            modifier = Modifier.padding(horizontal = 16.dp),
                            contentAlignment = Alignment.CenterStart
                        ) {
                            Text(label(value), fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal)
                        }
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
private fun TemplateDialog(
    template: Template?,
    onDismiss: () -> Unit,
    onSave: (Long?, String, String) -> Unit
) {
    var name by remember(template) { mutableStateOf(template?.name.orEmpty()) }
    var content by remember(template) { mutableStateOf(template?.content.orEmpty()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (template == null) "New template" else "Edit template") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Name") },
                    singleLine = true
                )
                OutlinedTextField(
                    value = content,
                    onValueChange = { content = it },
                    label = { Text("Steps") },
                    minLines = 4
                )
            }
        },
        confirmButton = {
            TextButton(onClick = { onSave(template?.id, name, content) }) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
private fun SettingsSnackbar(data: SnackbarData) {
    Surface(
        modifier = Modifier
            .padding(16.dp)
            .iosShadow(CatatShadows.shadowLg, cornerRadius = 16.dp)
            .clip(CatatShapes.lg),
        color = Color(0xEE1C1C1E),
        contentColor = Color.White,
        shape = CatatShapes.lg,
        shadowElevation = 0.dp
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(CatatColors.Success)
            )
            Spacer(Modifier.width(10.dp))
            Text(
                text = data.visuals.message,
                style = CatatTextStyles.Footnote
            )
        }
    }
}

@Composable
private fun SettingsLoading() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Spacer(Modifier.height(48.dp))
        repeat(5) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(120.dp)
                    .shimmer(CatatShapes.md)
            )
        }
    }
}

private fun ExportFormat.label(): String = when (this) {
    ExportFormat.Jira -> "Jira"
    ExportFormat.GitHub -> "GitHub"
    ExportFormat.Linear -> "Linear"
    ExportFormat.Markdown -> "Markdown"
}

private val exportFormats = listOf(
    ExportFormat.Jira,
    ExportFormat.GitHub,
    ExportFormat.Linear,
    ExportFormat.Markdown
)

private val voiceLanguages = listOf("en-US", "id-ID")
