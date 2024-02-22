package io.agora.board.samples

import android.content.Context
import android.view.ViewGroup
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import com.herewhite.sdk.CommonCallback
import com.herewhite.sdk.Room
import com.herewhite.sdk.RoomListener
import com.herewhite.sdk.RoomParams
import com.herewhite.sdk.WhiteSdk
import com.herewhite.sdk.WhiteSdkConfiguration
import com.herewhite.sdk.WhiteboardView
import com.herewhite.sdk.domain.MemberState
import com.herewhite.sdk.domain.Promise
import com.herewhite.sdk.domain.Region
import com.herewhite.sdk.domain.RoomPhase
import com.herewhite.sdk.domain.RoomState
import com.herewhite.sdk.domain.SDKError
import org.json.JSONObject
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine

@Immutable
data class WhiteboardProperties(
    val appId: String,
    val roomUuid: String,
    val roomToken: String,
    val uniqueUid: String,
    val region: Region,
    val writable: Boolean = true
) {
    companion object {
        val UNINITIALIZED = WhiteboardProperties("", "", "", "", Region.cn)
    }
}

@Composable
fun rememberWhiteboardState(
    properties: WhiteboardProperties = WhiteboardProperties.UNINITIALIZED
): WhiteboardState = rememberSaveable(saver = WhiteboardState.Saver()) {
    WhiteboardState(properties)
}

@Stable
class WhiteboardState(properties: WhiteboardProperties) {

    var whiteboardProperties: WhiteboardProperties by mutableStateOf(properties)

    internal var whiteboardView by mutableStateOf<WhiteboardView?>(null)

    internal var room by mutableStateOf<Room?>(null)

    fun uninitialized() = whiteboardProperties == WhiteboardProperties.UNINITIALIZED

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

    fun release() {
        whiteboardView = null
        room = null
    }

    companion object {
        fun Saver(): Saver<WhiteboardState, Any> =
            listSaver(
                save = {
                    listOf(
                        it.whiteboardProperties.appId,
                        it.whiteboardProperties.roomUuid,
                        it.whiteboardProperties.roomToken,
                        it.whiteboardProperties.uniqueUid,
                        it.whiteboardProperties.region,
                        it.whiteboardProperties.writable
                    )
                },
                restore = {
                    WhiteboardState(
                        WhiteboardProperties(
                            it[0] as String,
                            it[1] as String,
                            it[2] as String,
                            it[3] as String,
                            it[4] as Region,
                            it[5] as Boolean
                        )
                    )
                }
            )
    }
}

@Composable
fun Whiteboard(
    modifier: Modifier,
    state: WhiteboardState,
    onPhaseChanged: ((phase: RoomPhase) -> Unit)? = null,
    onRoomStateChanged: ((modifyState: RoomState) -> Unit)? = null,
    onError: ((error: SDKError) -> Unit)? = null,
    onCreated: (WhiteboardView) -> Unit = {},
    onDispose: (WhiteboardView) -> Unit = {},
    factory: ((Context) -> WhiteboardView)? = null,
) {

    val wv = state.whiteboardView
    wv?.let {
        LaunchedEffect(wv, state) {
            snapshotFlow { state.whiteboardProperties }.collect {
                println("state.whiteboardProperties $it")

                val properties = state.whiteboardProperties
                if (properties == WhiteboardProperties.UNINITIALIZED)
                    return@collect

                val whiteSdkConfiguration = WhiteSdkConfiguration(properties.appId, true)
                val whiteSdk = WhiteSdk(wv, wv.context, whiteSdkConfiguration).apply {
                    setCommonCallbacks(object : CommonCallback {

                        override fun onLogger(log: JSONObject) {

                        }

                        override fun sdkSetupFail(error: SDKError) {
                            onError?.invoke(error)
                        }

                        override fun throwError(args: Any) {
                            onError?.invoke(SDKError("Uncaught sdk exception"))
                        }
                    })
                }

                val roomParams = RoomParams(
                    properties.roomUuid,
                    properties.roomToken,
                    properties.uniqueUid
                ).apply {
                    isWritable = properties.writable
                    region = properties.region
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
                        state.room = room
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
            }
        }
    }

    AndroidView(
        factory = { context ->
            (factory?.invoke(context) ?: WhiteboardView(context)).apply {
                onCreated(this)

                layoutParams = ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
                )

            }.also { state.whiteboardView = it }
        },
        modifier = modifier,
        update = {
            print("Whiteboard update call")
        },
        onRelease = {
            onDispose(it)
            it.removeAllViews()
            it.destroy()
            state.release()
        }
    )
}