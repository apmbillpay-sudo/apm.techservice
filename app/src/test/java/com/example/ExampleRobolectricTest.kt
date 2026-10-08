package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.BseDatabase
import com.example.data.local.WatchlistEntity
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

    @Test
    fun `read string from context`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("BSE Pulse", appName)
    }

    @Test
    fun `test watchlist room database operations`() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val db = BseDatabase.getDatabase(context)
        val dao = db.bseDao()

        val item = WatchlistEntity(
            scripCode = "500325",
            scripId = "RELIANCE",
            companyName = "Reliance Industries Ltd",
            targetAlertPrice = 3000.0,
            alertCondition = "ABOVE",
            researchNotes = "Core green hydrogen holding"
        )

        dao.addToWatchlist(item)
        val watched = dao.getWatchlistItem("500325")
        assertNotNull(watched)
        assertEquals("RELIANCE", watched?.scripId)
        assertEquals(3000.0, watched?.targetAlertPrice)

        val list = dao.getAllWatchlist().first()
        assertEquals(1, list.size)

        dao.removeFromWatchlistByCode("500325")
        val emptyList = dao.getAllWatchlist().first()
        assertEquals(0, emptyList.size)
    }
}
