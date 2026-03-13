package com.sukisu.ultra.ui.screen.sulog

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.sukisu.ultra.ui.LocalUiMode
import com.sukisu.ultra.ui.UiMode
import com.sukisu.ultra.ui.navigation3.LocalNavigator
import com.sukisu.ultra.ui.navigation3.Navigator
import com.sukisu.ultra.ui.util.retrieveSulogLogs
import com.sukisu.ultra.ui.util.streamFile
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive

@Composable
fun SulogScreen() {
    val navigator = LocalNavigator.current
    var entries by remember { mutableStateOf(listOf<SulogEntry>()) }

    LaunchedEffect(true) {
        val regex = Regex("""uptime_s=(\d+)\s+uid=(\d+)\s+sym=(.)""")
        while (isActive) {
            retrieveSulogLogs()
            delay(1000)

            val streamed = streamFile("/data/adb/ksu/log/sulog.log")
            val allLines = if (streamed.isEmpty()) emptyList() else streamed.takeLast(2000)

            val parsed = mutableListOf<SulogEntry>()
            val seen = LinkedHashSet<String>()
            for (ln in allLines) {
                val lineTrim = ln.trim()
                if (lineTrim.isEmpty()) continue
                val m = regex.find(lineTrim)
                val entry = if (m != null) {
                    val uptime = m.groupValues[1].toIntOrNull() ?: 0
                    val uid = m.groupValues[2].toIntOrNull() ?: 0
                    val sym = m.groupValues[3].firstOrNull() ?: '?'
                    if (uptime == 0 && uid == 0 && sym == '?') null else SulogEntry(uptime, uid, sym, lineTrim)
                } else {
                    SulogEntry(0, 0, '?', lineTrim)
                }
                if (entry != null) {
                    val key = "${entry.uptime}|${entry.uid}|${entry.sym}|${entry.raw}"
                    if (seen.add(key)) parsed.add(entry)
                }
            }

            val map = linkedMapOf<String, SulogEntry>()
            parsed.forEach { map["${it.uptime}|${it.uid}|${it.sym}|${it.raw}"] = it }
            entries.forEach { key ->
                val k = "${key.uptime}|${key.uid}|${key.sym}|${key.raw}"
                if (!map.containsKey(k)) map[k] = key
            }
            val combined = map.values.toList()
            entries = combined

            delay(4000)
        }
    }

    val actions = SulogActions(
        onBack = { navigator.pop() }
    )

    val state = SulogUiState(entries = entries)

    when (LocalUiMode.current) {
        UiMode.Miuix -> SulogMiuix(state = state, actions = actions)
        UiMode.Material -> SulogMaterial(state = state, actions = actions)
    }
}
