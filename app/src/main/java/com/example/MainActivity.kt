package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.with
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.OfficeScreen
import com.example.ui.OfficeViewModel
import com.example.ui.screens.DashboardScreen
import com.example.ui.screens.DocEditorScreen
import com.example.ui.screens.HscEditorScreen
import com.example.ui.screens.SheetEditorScreen
import com.example.ui.screens.SlideEditorScreen

class MainActivity : ComponentActivity() {
    private val viewModel: OfficeViewModel by viewModels()

    @OptIn(ExperimentalAnimationApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    Surface(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding)
                    ) {
                        AnimatedContent(
                            targetState = viewModel.currentScreen,
                            transitionSpec = {
                                slideInHorizontally(initialOffsetX = { it }) with 
                                slideOutHorizontally(targetOffsetX = { -it })
                            }
                        ) { screen ->
                            when (screen) {
                                is OfficeScreen.Dashboard -> DashboardScreen(viewModel = viewModel)
                                is OfficeScreen.DocEditor -> DocEditorScreen(viewModel = viewModel, docId = screen.docId)
                                is OfficeScreen.SheetEditor -> SheetEditorScreen(viewModel = viewModel, docId = screen.docId)
                                is OfficeScreen.SlideEditor -> SlideEditorScreen(viewModel = viewModel, docId = screen.docId)
                                is OfficeScreen.HscEditor -> HscEditorScreen(viewModel = viewModel, docId = screen.docId)
                            }
                        }
                    }
                }
            }
        }
    }
}
