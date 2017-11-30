package com.wishordertracker

import com.wishordertracker.site.WishSite
import com.wishordertracker.site.models.WishItemCheckpointModel
import com.wishordertracker.site.models.WishItemModel
import java.util.*
import kotlin.concurrent.schedule

class WishOrderTracker(private val wish: WishSite) {
    private var timer: Timer? = null

    private val checkpointListener = mutableListOf<(WishItemModel, WishItemCheckpointModel) -> Unit>()
    private val latestItemMessages = mutableMapOf<String, String>()

    fun start(period: Long, delay: Long = 0) {
        timer?.cancel()

        // Let's not spam requests
        var adjustedPeriod = period
        if (adjustedPeriod < 60 * 1000) {
            adjustedPeriod = 60 * 1000
        }

        timer = Timer(false)
        timer?.schedule(delay, adjustedPeriod, {
            checkpoints()
            // More checks can be added here
        })
    }

    private fun checkpoints() {
        checkpointListener.forEach({
            listener ->
            val (history, error) = wish.orderHistory()

            if (error != null) {
                throw IllegalStateException(error)
            }

            history?.orders?.forEach {
                it.items.forEach({
                    item ->
                    if (item.checkpoints.isNotEmpty()) {
                        val localMessage = latestItemMessages[item.name]

                        val checkpoint = item.checkpoints[item.checkpoints.size - 1]
                        val remoteMessage = checkpoint.message

                        if (localMessage == null || localMessage != remoteMessage) {
                            latestItemMessages[item.name] = remoteMessage
                            listener(item, checkpoint)
                        }
                    }
                })
            }
        })
    }

    fun cancel() {
        timer?.cancel()
    }

    fun addListener(listener: (WishItemModel, WishItemCheckpointModel) -> Unit) {
        checkpointListener.add(listener)
    }
}
