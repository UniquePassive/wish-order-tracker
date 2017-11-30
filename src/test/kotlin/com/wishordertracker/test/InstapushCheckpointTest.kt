package com.wishordertracker.test

import com.wishordertracker.WishOrderTracker
import com.wishordertracker.site.WishSite
import instapush.Instapush
import org.junit.Test

class InstapushCheckpointTest {
    companion object {
        // Get at https://instapush.im/
        private const val APP_ID = ""
        private const val APP_SECRET = ""
    }

    @Test
    fun example() {
        val wish = WishSite()
        run {
            val error = wish.login("", "")

            if (error != null) {
                throw IllegalStateException(error)
            }
        }

        val tracker = WishOrderTracker(wish)

        tracker.addListener({
            item, checkpoint ->
            val ip = Instapush(APP_ID, APP_SECRET)

            ip.push("update",
                    Pair("name", item.name),
                    Pair("status", "(" + checkpoint.country + ") " + checkpoint.message))
        })

        // Check every 30 minutes
        tracker.start(30 * 60 * 1000)

        // Run the test for 5 minutes
        Thread.sleep(5 * 60 * 1000)

        tracker.cancel()
    }
}
