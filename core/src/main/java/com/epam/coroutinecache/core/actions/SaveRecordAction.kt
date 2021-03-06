package com.epam.coroutinecache.core.actions

import com.epam.coroutinecache.core.Memory
import com.epam.coroutinecache.core.Persistence
import com.epam.coroutinecache.core.Record
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.async
import org.koin.core.parameter.parametersOf
import org.koin.standalone.KoinComponent
import org.koin.standalone.inject

/**
 * Action that save record in the cache
 */
class SaveRecordAction(
        private val diskCache: Persistence,
        private val memory: Memory,
        private val maxMbCacheSize: Int,
        private val scope: CoroutineScope
): KoinComponent {

    private val deleteExpirableRecordsAction: DeleteExpirableRecordsAction by inject { parametersOf(maxMbCacheSize, scope) }

    /**
     * Async function that saves data in the cache and memory. After saving delete all expirable data from cache
     * if memory limit is reached. If memory limit is reached, bot there is nothing to delete from cache, then print message that record can't be saved
     */
    suspend fun save(key: String, data: Any?, lifeTimeMillis: Long = 0, isExpirable: Boolean = true) = scope.async {
        val record = Record(data, isExpirable, lifeTimeMillis)

        memory.saveRecord(key, record)

        if (diskCache.storedMB() >= maxMbCacheSize) {
            System.out.println("Record can not be persisted because it would exceed the max limit megabytes settled down")
        } else {
            diskCache.saveRecord(key, record)
        }

        deleteExpirableRecordsAction.deleteExpirableRecords().await()
    }
}