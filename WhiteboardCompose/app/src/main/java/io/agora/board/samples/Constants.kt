package io.agora.board.samples

import com.herewhite.sdk.domain.Region
import java.util.UUID

object Constants {
    const val SAMPLE_APP_ID = "283/VGiScM9Wiw2HJg"
    const val SAMPLE_ROOM_UUID = "eb2c4540b67511eebe0729a238197850"
    const val SAMPLE_ROOM_TOKEN =
        "NETLESSROOM_YWs9eTBJOWsxeC1IVVo4VGh0NyZub25jZT0xNzA1NjMyODA3NjQwMDAmcm9sZT0wJnNpZz1iZjZiZTRkODM4YzNjZjYyZGVhZDlmM2YwM2ExNWE1ZDdjZWM0Yzg4ZTQxZWI3YjQ4MjZiY2M1NjUwNjlmMzQwJnV1aWQ9ZWIyYzQ1NDBiNjc1MTFlZWJlMDcyOWEyMzgxOTc4NTA"

    val SAMPLE_UNIQUE_UID: String by lazy { UUID.randomUUID().toString() }
    val SAMPLE_REGION: Region by lazy { Region.cn }
}
