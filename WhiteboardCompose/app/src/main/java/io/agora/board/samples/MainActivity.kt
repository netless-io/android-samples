package io.agora.board.samples

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.random.Random

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            WhiteboardComposeTheme {
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


@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun MainScreen() {
    val coroutineScope = rememberCoroutineScope()
    val whiteboardState = rememberWhiteboardState()
    var whiteboardShown by rememberSaveable { mutableStateOf(true) }

    // 模拟数据获取
    LaunchedEffect(true) {
        delay(2000)
        whiteboardState.whiteboardProperties = WhiteboardProperties(
            appId = Constants.SAMPLE_APP_ID,
            roomUuid = Constants.SAMPLE_ROOM_UUID,
            roomToken = Constants.SAMPLE_ROOM_TOKEN,
            uniqueUid = Constants.SAMPLE_UNIQUE_UID,
            region = Constants.SAMPLE_REGION,
        )
    }

    Box(Modifier.fillMaxSize()) {
        if (whiteboardShown) {
            Whiteboard(
                modifier = Modifier.fillMaxSize(),
                state = whiteboardState,
            )
        }

        if (!whiteboardShown) {
            Text(
                text = "Whiteboard Hidden",
                modifier = Modifier
                    .align(Alignment.Center)
                    .padding(bottom = 32.dp)
            )
        }

        FlowRow(Modifier.fillMaxWidth()) {
            OperationButton("ShowOrHide") {
                coroutineScope.launch {
                    whiteboardShown = !whiteboardShown
                }
            }

            OperationButton("Pencil") {
                // 更新教具状态
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
                // 清空白板
                whiteboardState.clearScene()
            }

            OperationButton("RoomState") {
                coroutineScope.launch {
                    val roomState = whiteboardState.getRoomState()
                    println("RoomState: $roomState")
                }
            }

            // 非核心场景，测试 Whiteboard 变更使用
            OperationButton("SwitchTest") {
                coroutineScope.launch {
                    whiteboardState.whiteboardProperties = WhiteboardProperties(
                        appId = Constants.SAMPLE_APP_ID,
                        roomUuid = Constants.SAMPLE_ROOM_UUID,
                        roomToken = Constants.SAMPLE_ROOM_TOKEN,
                        uniqueUid = Constants.SAMPLE_UNIQUE_UID + Random.nextInt(),
                        region = Constants.SAMPLE_REGION,
                    )
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