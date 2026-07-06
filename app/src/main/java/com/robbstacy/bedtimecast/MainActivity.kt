package com.robbstacy.bedtimecast

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.Composable
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.robbstacy.bedtimecast.data.ProfilesStore
import com.robbstacy.bedtimecast.ui.CastScreen
import com.robbstacy.bedtimecast.ui.LibraryScreen
import com.robbstacy.bedtimecast.ui.ProfilesScreen
import com.robbstacy.bedtimecast.ui.RecordScreen
import com.robbstacy.bedtimecast.ui.StoryScreen
import com.robbstacy.bedtimecast.ui.theme.BedtimeCastTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        ProfilesStore.init(this)
        setContent {
            BedtimeCastTheme {
                AppNavHost()
            }
        }
    }
}

@Composable
fun AppNavHost() {
    val nav = rememberNavController()
    NavHost(navController = nav, startDestination = "library") {
        composable("library") {
            LibraryScreen(nav)
        }
        composable(
            route = "story/{storyId}",
            arguments = listOf(navArgument("storyId") { type = NavType.StringType }),
        ) { entry ->
            StoryScreen(nav, entry.arguments?.getString("storyId").orEmpty())
        }
        composable(
            route = "record/{storyId}?page={page}&seg={seg}",
            arguments = listOf(
                navArgument("storyId") { type = NavType.StringType },
                navArgument("page") { type = NavType.IntType; defaultValue = 0 },
                navArgument("seg") { type = NavType.IntType; defaultValue = 0 },
            ),
        ) { entry ->
            RecordScreen(
                nav,
                entry.arguments?.getString("storyId").orEmpty(),
                entry.arguments?.getInt("page") ?: 0,
                entry.arguments?.getInt("seg") ?: 0,
            )
        }
        composable("cast") {
            CastScreen(nav)
        }
        composable("profiles") {
            ProfilesScreen(nav)
        }
    }
}
