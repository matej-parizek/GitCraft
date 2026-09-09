package cz.parizmat.gitcraft.feature.toolwindow.model

import kotlin.test.Test
import kotlin.test.assertEquals

class SuggestionPopupPositionerTest {
    @Test
    fun `places popup below a caret near the top`() {
        val placement = place(anchorTop = 60, anchorBottom = 80)

        assertEquals(PopupVerticalSide.BELOW, placement.side)
        assertEquals(80, placement.y)
    }

    @Test
    fun `flips popup above a caret near the bottom`() {
        val placement = place(anchorTop = 720, anchorBottom = 740)

        assertEquals(PopupVerticalSide.ABOVE, placement.side)
        assertEquals(420, placement.y)
    }

    @Test
    fun `uses the larger side and stays inside the viewport when neither side fits`() {
        val placement = place(anchorTop = 330, anchorBottom = 350, popupHeight = 500)

        assertEquals(PopupVerticalSide.BELOW, placement.side)
        assertEquals(300, placement.y)
    }

    private fun place(
        anchorTop: Int,
        anchorBottom: Int,
        popupHeight: Int = 300,
    ): SuggestionPopupPlacement = SuggestionPopupPositioner.place(
        anchorX = 900,
        anchorTop = anchorTop,
        anchorBottom = anchorBottom,
        popupWidth = 440,
        popupHeight = popupHeight,
        viewportLeft = 0,
        viewportTop = 0,
        viewportRight = 1_000,
        viewportBottom = 800,
    )
}
