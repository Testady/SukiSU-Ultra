package com.sukisu.ultra.ui.screen.sulog

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.add
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.sukisu.ultra.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SulogMaterial(
    state: SulogUiState,
    actions: SulogActions,
) {
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior(rememberTopAppBarState())

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.log_viewer_title)) },
                navigationIcon = {
                    IconButton(onClick = actions.onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = null,
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                scrollBehavior = scrollBehavior
            )
        },
        contentWindowInsets = WindowInsets.systemBars.add(WindowInsets.displayCutout).only(WindowInsetsSides.Horizontal)
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxHeight()
                .padding(horizontal = 12.dp)
                .padding(innerPadding),
        ) {
            val priority = { c: Char ->
                when (c) {
                    'i' -> 0
                    'x' -> 1
                    '$' -> 2
                    else -> 3
                }
            }
            val displayed = state.entries.sortedWith(compareBy({ priority(it.sym) }, { -it.uptime }))
            items(displayed.size) { index ->
                val e = displayed[index]
                Card(modifier = Modifier.padding(vertical = 6.dp)) {
                    Column(modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp)) {
                        val bgDesc = when (e.sym) {
                            '$' -> stringResource(id = R.string.sulog_blocked_label)
                            'x' -> stringResource(id = R.string.sulog_allowed_label)
                            'i' -> stringResource(id = R.string.sulog_ioctl_label)
                            else -> stringResource(id = R.string.sulog_other_label)
                        }
                        Text(text = "$bgDesc • uid=${e.uid} • uptime=${formatDuration(e.uptime)}")
                    }
                }
            }
        }
    }
}
