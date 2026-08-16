package com.pranav.flipbook

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.rememberNavController
import com.pranav.flipbook.navigation.FlipBookNavGraph
import com.pranav.flipbook.ui.theme.FlipBookTheme
import com.pranav.flipbook.viewmodel.LibraryViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            FlipBookTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val navController = rememberNavController()
                    val libraryViewModel: LibraryViewModel = viewModel()

                    // Handle PDF intent
                    LaunchedEffect(intent) {
                        handleIncomingPdf(intent, libraryViewModel)
                    }

                    FlipBookNavGraph(navController = navController)
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
    }

    private fun handleIncomingPdf(intent: Intent?, viewModel: LibraryViewModel) {
        if (intent?.action == Intent.ACTION_VIEW && intent.data != null) {
            intent.data?.let { uri ->
                viewModel.importPdf(uri)
            }
        }
    }
}