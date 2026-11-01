package kr.mycatdid0.quicktimestamplog00

import kr.mycatdid0.quicktimestamplog00.data.RecentTitleHistory
import org.junit.Assert.assertEquals
import org.junit.Test

class RecentTitleHistoryTest {
    @Test
    fun addMovesExistingTitleToFrontAndTrimsWhitespace() {
        assertEquals(
            listOf("Meeting", "Walk"),
            RecentTitleHistory.add(listOf("Walk", "Meeting"), " Meeting ")
        )
    }

    @Test
    fun addIgnoresBlankTitle() {
        val titles = listOf("Walk")
        assertEquals(titles, RecentTitleHistory.add(titles, "  "))
    }

    @Test
    fun addKeepsOnlyMaximumNumberOfTitles() {
        val titles = (1..RecentTitleHistory.MAX_TITLES).map { "Title $it" }
        val result = RecentTitleHistory.add(titles, "Newest")

        assertEquals(RecentTitleHistory.MAX_TITLES, result.size)
        assertEquals("Newest", result.first())
    }

    @Test
    fun removeDeletesSelectedTitle() {
        assertEquals(
            listOf("Walk"),
            RecentTitleHistory.remove(listOf("Meeting", "Walk"), "Meeting")
        )
    }
}
