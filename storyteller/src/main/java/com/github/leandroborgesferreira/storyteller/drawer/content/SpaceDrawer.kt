package com.github.leandroborgesferreira.storyteller.drawer.content

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyItemScope
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.github.leandroborgesferreira.storyteller.draganddrop.target.DropTarget
import com.github.leandroborgesferreira.storyteller.drawer.DrawInfo
import com.github.leandroborgesferreira.storyteller.drawer.StoryUnitDrawer
import com.github.leandroborgesferreira.storyteller.model.change.MoveInfo
import com.github.leandroborgesferreira.storyteller.model.story.StoryStep

/**
 * Draws a white space. This Drawer is very important for accepting drop os other Composables for
 * reorder purposes. A space create a move request when dropping Composables in it while the other
 * story units create a mergeRequest.
 */
class SpaceDrawer(private val moveRequest: (MoveInfo) -> Unit = {}) : StoryUnitDrawer {

    @Composable
    override fun LazyItemScope.Step(step: StoryStep, drawInfo: DrawInfo) {
        DropTarget { inBound, data ->
            if (inBound && data != null) {
                moveRequest(
                    MoveInfo(
                        data.storyUnit,
                        positionFrom = data.positionFrom,
                        positionTo = drawInfo.positionList
                    )
                )
            }

            val spaceBgColor =
                when {
                    inBound -> Color.LightGray
//                    BuildConfig.DEBUG -> Color.Cyan
                    else -> Color.Transparent
                }

            Box(
                modifier = Modifier
                    .height(10.dp)
                    .fillMaxWidth()
                    .background(spaceBgColor)
            )
        }
    }
}
