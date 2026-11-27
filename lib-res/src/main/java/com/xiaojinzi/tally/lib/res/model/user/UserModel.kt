package com.xiaojinzi.tally.lib.res.model.user

import androidx.annotation.Keep
import com.xiaojinzi.support.annotation.MillisecondTime
import com.xiaojinzi.support.annotation.ModelDto
import com.xiaojinzi.support.annotation.ModelForJson

@Keep
@ModelForJson
data class UserInfoRes(
    val id: String,
    val name: String,
    val timeCreate: Long,
)

@Keep
@ModelDto
data class UserInfoDto(
    val id: String,
    val name: String,
    val timeCreate: Long,
) {

    companion object {
        // 要固定的
        const val TEST_ID = "opensourceUserId"
    }

}

@Keep
data class UserInfoCacheDto(
    val id: String,
    val name: String?,
    // 过期时间
    @MillisecondTime
    val timeExpire: Long,
)

fun UserInfoRes.toDto(): UserInfoDto {
    return UserInfoDto(
        id = id,
        name = name,
        timeCreate = timeCreate,
    )
}
