package com.robbstacy.bedtimecast.ui

import androidx.compose.foundation.layout.RowScope
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.navigation.NavController
import com.robbstacy.bedtimecast.data.Story
import com.robbstacy.bedtimecast.ui.theme.CHARACTER_COLORS

fun characterColor(story: Story, speakerId: String): Color {
    val index = story.characters.indexOfFirst { it.id == speakerId }
    return CHARACTER_COLORS[(if (index < 0) 0 else index) % CHARACTER_COLORS.size]
}

/** Parses a story's cover color like "#7FB069". */
fun coverColor(story: Story): Color =
    runCatching { Color(android.graphics.Color.parseColor(story.coverColor)) }
        .getOrDefault(Color(0xFF7FB069))

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppTopBar(
    title: String,
    nav: NavController? = null,
    containerColor: Color = MaterialTheme.colorScheme.background,
    titleColor: Color = MaterialTheme.colorScheme.onSurface,
    actions: @Composable RowScope.() -> Unit = {},
) {
    TopAppBar(
        title = { Text(title, fontWeight = FontWeight.Bold) },
        navigationIcon = {
            if (nav != null) {
                TextButton(onClick = { nav.popBackStack() }) {
                    Text("‹ Back", color = MaterialTheme.colorScheme.primary)
                }
            }
        },
        actions = actions,
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = containerColor,
            titleContentColor = titleColor,
        ),
    )
}
