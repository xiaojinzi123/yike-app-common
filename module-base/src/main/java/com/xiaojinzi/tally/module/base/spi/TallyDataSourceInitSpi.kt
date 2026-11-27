package com.xiaojinzi.tally.module.base.spi

import com.xiaojinzi.support.annotation.StateHotObservable
import kotlinx.coroutines.flow.Flow

interface TallyDataSourceInitSpi {

    /**
     * 是否被初始化
     */
    @StateHotObservable
    val isInitStateOb: Flow<Boolean>

    /**
     * 初始化记账的数据库
     */
    suspend fun initTallyDataBase(userId: String)

}