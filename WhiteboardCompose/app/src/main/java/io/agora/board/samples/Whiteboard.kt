package io.agora.board.samples

import android.view.ViewGroup
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import com.herewhite.sdk.Room
import com.herewhite.sdk.RoomListener
import com.herewhite.sdk.RoomParams
import com.herewhite.sdk.WhiteSdk
import com.herewhite.sdk.WhiteSdkConfiguration
import com.herewhite.sdk.WhiteboardView
import com.herewhite.sdk.domain.MemberState
import com.herewhite.sdk.domain.Promise
import com.herewhite.sdk.domain.RoomPhase
import com.herewhite.sdk.domain.RoomState
import com.herewhite.sdk.domain.SDKError
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine

@Composable
fun rememberWhiteboardState(
    key: String? = null,
    init: WhiteboardState.() -> Unit = {},
): WhiteboardState = rememberSaveable(key = key, saver = WhiteboardState.Saver()) {
    WhiteboardState().apply(init)
}

@Stable
class WhiteboardState {

    private var room by mutableStateOf<Room?>(null)

    internal fun initRoom(r: Room) {
        room = r
    }

    suspend fun getRoomState(): RoomState? = suspendCoroutine { cont ->
        room?.getRoomState(object : Promise<RoomState> {
            override fun then(state: RoomState) {
                cont.resume(value = state)
            }

            override fun catchEx(t: SDKError) {
                cont.resumeWithException(t)
            }
        }) ?: cont.resume(value = null)
    }

    suspend fun setWritable(writable: Boolean): Boolean? = suspendCoroutine { cont ->
        room?.setWritable(writable, object : Promise<Boolean> {
            override fun then(value: Boolean) {
                cont.resume(value = value)
            }

            override fun catchEx(t: SDKError) {
                cont.resumeWithException(t)
            }
        }) ?: cont.resume(value = null)
    }

    fun setMemberState(state: MemberState) {
        room?.memberState = state
    }

    suspend fun disconnect() = suspendCoroutine<Boolean> { cont ->
        room?.disconnect(object : Promise<Any> {
            override fun then(state: Any) {
                cont.resume(value = true)
            }

            override fun catchEx(t: SDKError) {
                cont.resumeWithException(t)
            }

        })
    }

    fun clearScene() {
        room?.cleanScene(true)
    }

    companion object {
        fun Saver(): Saver<WhiteboardState, String> =
            Saver(
                save = { "" },
                restore = { WhiteboardState() }
            )
    }
}

@Composable
fun Whiteboard(
    modifier: Modifier,
    whiteboardState: WhiteboardState = rememberWhiteboardState(),
    onPhaseChanged: ((phase: RoomPhase) -> Unit)? = null,
    onRoomStateChanged: ((modifyState: RoomState) -> Unit)? = null,
    onError: ((error: SDKError) -> Unit)? = null
) {

    var whiteboardView by remember { mutableStateOf<WhiteboardView?>(null) }

    DisposableEffect(
        AndroidView(
            factory = { ctx ->
                whiteboardView = WhiteboardView(ctx).apply {
                    layoutParams = ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT
                    )
                }

                val whiteSdkConfiguration = WhiteSdkConfiguration(Constants.SAMPLE_APP_ID, true)
                val whiteSdk = WhiteSdk(whiteboardView, ctx, whiteSdkConfiguration)

                val roomParams = RoomParams(
                    /* uuid = */ Constants.SAMPLE_ROOM_UUID,
                    /* token = */ Constants.SAMPLE_ROOM_TOKEN,
                    /* uid = */ Constants.SAMPLE_UNIQUE_UID,
                ).apply {
                    // 设置可写
                    isWritable = true
                    region = Constants.SAMPLE_REGION
                }

                whiteSdk.joinRoom(roomParams, object : RoomListener {
                    override fun onPhaseChanged(phase: RoomPhase) {
                        onPhaseChanged?.invoke(phase)
                    }

                    override fun onDisconnectWithError(e: Exception) {

                    }

                    override fun onKickedWithReason(reason: String) {

                    }

                    override fun onRoomStateChanged(modifyState: RoomState) {
                        onRoomStateChanged?.invoke(modifyState)
                    }

                    override fun onCanUndoStepsUpdate(canUndoSteps: Long) {

                    }

                    override fun onCanRedoStepsUpdate(canRedoSteps: Long) {

                    }

                    override fun onCatchErrorWhenAppendFrame(userId: Long, error: Exception) {

                    }

                }, object : Promise<Room> {
                    override fun then(room: Room) {
                        whiteboardState.initRoom(room)

                        // 开启本地序列化, 触发 onCanUndoStepsUpdate, onCanRedoStepsUpdate 回调
                        room.disableSerialization(false)
                        // 禁止客户端视角操作
                        room.disableCameraTransform(true)
                    }

                    override fun catchEx(error: SDKError) {
                        onError?.invoke(error)
                    }
                }
                )

                return@AndroidView whiteboardView!!
            },

            modifier = modifier
        )
    ) {
        onDispose {
            whiteboardView?.run {
                removeAllViews()
                destroy()
                whiteboardView = null
            }
        }
    }
}