package com.ccteacher.daisai

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import com.ccteacher.daisai.ui.dice.ThreeDiceBoard
import com.ccteacher.daisai.ui.theme.DaiSaiTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            DaiSaiTheme {
                ThreeDiceBoard(modifier = Modifier.fillMaxSize())
            }
        }
    }
}
