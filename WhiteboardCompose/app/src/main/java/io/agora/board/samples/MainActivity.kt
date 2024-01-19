package io.agora.board.samples

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.herewhite.sdk.domain.MemberState
import io.agora.board.samples.ui.theme.WhiteboardComposeTheme
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            WhiteboardComposeTheme {
                // A surface container using the 'background' color from the theme
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    MainScreen()
                }
            }
        }
    }
}

@Composable
private fun MainScreen() {

    var showWhiteboard by rememberSaveable { mutableStateOf(false) }
    val whiteboardState = rememberWhiteboardState()
    val coroutineScope = rememberCoroutineScope()

    Box(Modifier.fillMaxSize()) {
        if (showWhiteboard) {
            Whiteboard(
                modifier = Modifier.fillMaxSize(),
                whiteboardState = whiteboardState
            )
        }

        OutlinedButton(
            onClick = { showWhiteboard = !showWhiteboard },
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 32.dp)
        ) {
            Text(text = if (showWhiteboard) "Dismiss Whiteboard" else "Show Whiteboard")
        }

        LazyRow(Modifier.padding(top = 32.dp)) {
            item {
                OperationButton("Pencil") {
                    val memberState = MemberState().apply {
                        currentApplianceName = "pencil"
                    }
                    whiteboardState.setMemberState(memberState)
                }

                OperationButton("Eraser") {
                    val memberState = MemberState().apply {
                        currentApplianceName = "eraser"
                    }
                    whiteboardState.setMemberState(memberState)
                }

                OperationButton("Clear") {
                    whiteboardState.clearScene()
                }

                OperationButton("RoomState") {
                    coroutineScope.launch {
                        val roomState = whiteboardState.getRoomState()
                        println("RoomState: $roomState")
                    }
                }
            }
        }
    }
}

@Composable
private fun OperationButton(text: String, onClick: () -> Unit) {
    OutlinedButton(
        onClick = onClick,
        modifier = Modifier.padding(horizontal = 6.dp)
    ) {
        Text(text = text)
    }
}

@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
    WhiteboardComposeTheme {
        MainScreen()
    }
}