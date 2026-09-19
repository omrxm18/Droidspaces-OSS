package com.droidspaces.app.ui.screen

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.droidspaces.app.R
import com.droidspaces.app.ui.theme.JetBrainsMono
import com.droidspaces.app.ui.util.FullScreenLoading
import com.droidspaces.app.ui.viewmodel.JournaldState
import com.droidspaces.app.ui.viewmodel.JournaldViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun JournaldScreen(
    containerName: String,
    unitName: String,
    onNavigateBack: () -> Unit,
    viewModel: JournaldViewModel = viewModel()
) {
    val context = LocalContext.current
    val state = viewModel.state
    var selectedLineCount by remember { mutableStateOf("100") }
    var customLineCount by remember { mutableStateOf("") }

    val lineCount = if (selectedLineCount == "custom") {
        customLineCount.toIntOrNull() ?: 100
    } else {
        selectedLineCount.toIntOrNull() ?: 100
    }

    LaunchedEffect(containerName, unitName) {
        viewModel.loadLogs(containerName, unitName, selectedLineCount.toIntOrNull() ?: 100)
    }

    Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                        Column {
                            Text(
                                context.getString(R.string.logs_title, unitName),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1
                            )
                            Text(
                                containerName,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    },
                    navigationIcon = {
                        IconButton(onClick = onNavigateBack) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, context.getString(R.string.back))
                        }
                    },
                    actions = {
                        LineCountSelector(
                            selected = selectedLineCount,
                            onSelected = { selectedLineCount = it }
                        )
                        if (selectedLineCount == "custom") {
                            OutlinedTextField(
                                value = customLineCount,
                                onValueChange = { customLineCount = it.filter(Char::isDigit) },
                                modifier = Modifier.width(80.dp),
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                            )
                        }
                        IconButton(
                            onClick = { viewModel.loadLogs(containerName, unitName, lineCount) },
                            enabled = state !is JournaldState.Loading
                        ) {
                            Icon(Icons.Default.Refresh, context.getString(R.string.refresh))
                        }
                    },
                    colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = Color.Transparent)
                )
            },
            containerColor = Color.Transparent
        ) { padding ->
            Box(modifier = Modifier.padding(padding).fillMaxSize()) {
                when (val s = state) {
                    is JournaldState.Loading -> FullScreenLoading(message = context.getString(R.string.fetching_services))
                    is JournaldState.Error -> JournaldError(onRetry = { viewModel.loadLogs(containerName, unitName, lineCount) })
                    is JournaldState.Ready -> JournaldContent(s.logs)
                }
            }
        }
    }
}

@Composable
private fun JournaldContent(logs: List<String>) {
    Surface(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHighest,
        shape = RoundedCornerShape(12.dp)
    ) {
        val listState = rememberLazyListState()

        LaunchedEffect(logs) {
            if (logs.isNotEmpty()) {
                listState.scrollToItem(logs.size - 1)
            }
        }

        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(12.dp)
        ) {
            items(logs) { line ->
                Text(
                    text = line,
                    style = MaterialTheme.typography.bodySmall.copy(fontFamily = JetBrainsMono),
                    color = when {
                        line.contains("error", ignoreCase = true) || line.contains("fail", ignoreCase = true) -> Color(0xFFEF5350)
                        line.contains("warn", ignoreCase = true) -> Color(0xFFFFCA28)
                        else -> MaterialTheme.colorScheme.onSurface
                    }
                )
            }
        }
    }
}

@Composable
private fun JournaldError(onRetry: () -> Unit) {
    val context = LocalContext.current
    Column(
        modifier = Modifier.fillMaxSize().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            context.getString(R.string.unit_detail_load_error_title),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )
        Spacer(Modifier.height(8.dp))
        Text(
            context.getString(R.string.unit_detail_load_error_message),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(16.dp))
        FilledTonalButton(onClick = onRetry) {
            Text(context.getString(R.string.repo_retry))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun LineCountSelector(
    selected: String,
    onSelected: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    val options = listOf("10", "100", "1000", "custom")

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = it }
    ) {
        TextField(
            value = selected,
            onValueChange = {},
            readOnly = true,
            label = { Text("Lines") },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier.menuAnchor()
        )
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            options.forEach { option ->
                DropdownMenuItem(
                    text = { Text(option) },
                    onClick = {
                        onSelected(option)
                        expanded = false
                    }
                )
            }
        }
    }
}
