package cz.parizmat.gitcraft.feature.toolwindow.model

enum class PopupVerticalSide { ABOVE, BELOW }

data class SuggestionPopupPlacement(
    val x: Int,
    val y: Int,
    val side: PopupVerticalSide,
)

object SuggestionPopupPositioner {
    fun place(
        anchorX: Int,
        anchorTop: Int,
        anchorBottom: Int,
        popupWidth: Int,
        popupHeight: Int,
        viewportLeft: Int,
        viewportTop: Int,
        viewportRight: Int,
        viewportBottom: Int,
    ): SuggestionPopupPlacement {
        val spaceAbove = (anchorTop - viewportTop).coerceAtLeast(0)
        val spaceBelow = (viewportBottom - anchorBottom).coerceAtLeast(0)
        val side = when {
            popupHeight <= spaceBelow -> PopupVerticalSide.BELOW
            popupHeight <= spaceAbove -> PopupVerticalSide.ABOVE
            spaceBelow >= spaceAbove -> PopupVerticalSide.BELOW
            else -> PopupVerticalSide.ABOVE
        }
        val desiredY = when (side) {
            PopupVerticalSide.ABOVE -> anchorTop - popupHeight
            PopupVerticalSide.BELOW -> anchorBottom
        }
        val maximumX = (viewportRight - popupWidth).coerceAtLeast(viewportLeft)
        val maximumY = (viewportBottom - popupHeight).coerceAtLeast(viewportTop)
        return SuggestionPopupPlacement(
            x = anchorX.coerceIn(viewportLeft, maximumX),
            y = desiredY.coerceIn(viewportTop, maximumY),
            side = side,
        )
    }
}
