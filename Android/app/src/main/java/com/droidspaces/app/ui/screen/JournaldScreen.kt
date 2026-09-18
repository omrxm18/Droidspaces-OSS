package com.droidspaces.app.ui.screen

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
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

    LaunchedEffect(containerName, unitName) {
        viewModel.loadLogs(containerName, unitName)
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
                        IconButton(
                            onClick = { viewModel.loadLogs(containerName, unitName) },
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
                    is JournaldState.Error -> JournaldError(onRetry = { viewModel.loadLogs(containerName, unitName) })
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
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(12.dp)
        ) {
            items(logs) { line ->
                Text(
                    text = line,
                    style = MaterialTheme.typography.bodySmall.copy(fontFamily = JetBrainsMono)
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
