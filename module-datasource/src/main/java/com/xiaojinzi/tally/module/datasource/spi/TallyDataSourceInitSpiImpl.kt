package com.xiaojinzi.tally.module.datasource.spi

import com.xiaojinzi.component.anno.ServiceAnno
import com.xiaojinzi.support.ktx.MutableSharedStateFlow
import com.xiaojinzi.tally.module.base.spi.TallyDataSourceInitSpi
import com.xiaojinzi.tally.module.base.support.AppServices
import com.xiaojinzi.tally.module.datasource.storage.db.tally.TallyDb
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

@ServiceAnno(TallyDataSourceInitSpi::class)
class TallyDataSourceInitSpiImpl : TallyDataSourceInitSpi {

    private val initMutex = Mutex()

    override val isInitStateOb = MutableSharedStateFlow(
        initValue = false,
    )

    override suspend fun initTallyDataBase(
        userId: String,
    ) {
        initMutex.withLock {
            withContext(Dispatchers.IO) {
                TallyDb.initDataBase(userId = userId)
                AppServices.tallyDataSourceSpi.initLocalData(userId = userId)
                AppServices.tallyDataSourceSpi.resetSelectedBook()
                isInitStateOb.emit(value = true)
            }
        }
    }

}