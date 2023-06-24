package com.github.leandroborgesferreira.storyteller.drawer.content

import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import com.github.leandroborgesferreira.storyteller.drawer.DrawInfo
import com.github.leandroborgesferreira.storyteller.drawer.StoryUnitDrawer
import com.github.leandroborgesferreira.storyteller.model.story.StoryStep

/**
 * Draws a scrollable list of images accordingly with the imageStepDrawer provided.
 */
class ImageGroupDrawer(
    private val imageStepDrawer: StoryUnitDrawer,
    private val modifier: Modifier = Modifier
) : StoryUnitDrawer {

    @Composable
    override fun Step(step: StoryStep, drawInfo: DrawInfo) {
        val steps = step.steps
        val focusRequester = remember { FocusRequester() }

        LaunchedEffect(drawInfo.focus?.id) {
            if (drawInfo.focus?.id == step.localId) {
                focusRequester.requestFocus()
            }
        }

        LazyRow(modifier = modifier.focusRequester(focusRequester)) {
            items(steps) { storyStep ->
                imageStepDrawer.Step(storyStep, drawInfo = drawInfo.copy(focus = null))
            }
        }
    }
}
