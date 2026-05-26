package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.data.AppDatabase
import com.example.data.GameRecordRepository
import com.example.ui.MahjongScreen
import com.example.ui.SoundManager
import com.example.ui.theme.MyApplicationTheme
import com.example.viewmodel.MahjongViewModel

class MainActivity : ComponentActivity() {
  private lateinit var database: AppDatabase
  private lateinit var repository: GameRecordRepository

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    
    // Initialize database and repository once at the Activity level, preventing recreation and connection leaks on recompositions
    database = AppDatabase.getDatabase(applicationContext)
    repository = GameRecordRepository(database.gameRecordDao())

    // Initialize sound engine with TTS support
    SoundManager.init(applicationContext)

    enableEdgeToEdge()
    setContent {
      MyApplicationTheme {
        val mahjongViewModel: MahjongViewModel = viewModel(
          factory = MahjongViewModel.provideFactory(repository)
        )

        Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
          Box(modifier = Modifier.padding(innerPadding)) {
            MahjongScreen(viewModel = mahjongViewModel)
          }
        }
      }
    }
  }

  override fun onDestroy() {
    super.onDestroy()
    SoundManager.release()
  }
}

