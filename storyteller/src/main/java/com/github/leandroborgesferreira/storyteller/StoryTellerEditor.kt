package com.github.leandroborgesferreira.storyteller

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.github.leandroborgesferreira.storyteller.draganddrop.target.DraggableScreen
import com.github.leandroborgesferreira.storyteller.drawer.DrawInfo
import com.github.leandroborgesferreira.storyteller.drawer.StoryUnitDrawer
import com.github.leandroborgesferreira.storyteller.model.story.StoryState

@Composable
fun StoryTellerEditor(
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(0.dp),
    editable: Boolean,
    storyState: StoryState,
    drawers: Map<String, StoryUnitDrawer>,
    listState: LazyListState = rememberLazyListState()
) {
    DraggableScreen(modifier = modifier) {
        LazyColumn(
            modifier = modifier,
            contentPadding = contentPadding,
            state = listState,
            content = {
                itemsIndexed(
                    storyState.stories.values.toList(),
                    key = { _, storyUnit -> storyUnit.key },
                    itemContent = {index, storyUnit ->
                        drawers[storyUnit.type]?.run {
                            Step(
                                step = storyUnit,
                                drawInfo = DrawInfo(
                                    editable = editable,
                                    focusId = storyState.focusId,
                                    positionList = listOf(index),
                                    extraData = mapOf("listSize" to storyState.stories.size)
                                )
                            )
                        }
                    }
                )
            })
    }
}
