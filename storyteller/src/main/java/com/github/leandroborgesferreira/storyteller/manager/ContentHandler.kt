package com.github.leandroborgesferreira.storyteller.manager

import com.github.leandroborgesferreira.storyteller.model.backtrack.AddStoryUnit
import com.github.leandroborgesferreira.storyteller.model.change.DeleteInfo
import com.github.leandroborgesferreira.storyteller.model.change.LineBreakInfo
import com.github.leandroborgesferreira.storyteller.model.story.StoryState
import com.github.leandroborgesferreira.storyteller.model.story.StoryStep
import com.github.leandroborgesferreira.storyteller.model.story.StoryType
import com.github.leandroborgesferreira.storyteller.utils.StoryStepFactory
import com.github.leandroborgesferreira.storyteller.utils.UnitsNormalizationMap
import com.github.leandroborgesferreira.storyteller.utils.extensions.associateWithPosition
import com.github.leandroborgesferreira.storyteller.utils.extensions.toEditState
import java.util.UUID

/**
 * Class dedicated to handle adding and/or deleting new StorySteps
 */
class ContentHandler(
    private val focusableTypes: Set<String>,
    private val stepsNormalizer: UnitsNormalizationMap
) {

    fun createCheckItem(currentStory: Map<Int, StoryStep>, position: List<Int>): StoryState {
        val newMap = currentStory.toMutableMap()
        val newCheck = StoryStep(
            id = UUID.randomUUID().toString(),
            localId = UUID.randomUUID().toString(),
            type = StoryType.CHECK_ITEM.type,
        )
        newMap[position] = newCheck

        return StoryState(newMap, newCheck.id)
    }

    fun addNewContent(
        currentStory: Map<Int, StoryStep>,
        newStoryUnit: StoryStep,
        position: List<Int>
    ): Pair<Int, Map<Int, StoryStep>> {
        val mutable = currentStory.values.toMutableList()
        var acc = position

        mutable.add(acc++, StoryStepFactory.space())
        mutable.add(acc, newStoryUnit)

        return acc to mutable.associateWithPosition()
    }

    fun onLineBreak(
        currentStory: Map<Int, StoryStep>,
        lineBreakInfo: LineBreakInfo
    ): Pair<Pair<Int, StoryStep>, StoryState>? {
        val storyStep = lineBreakInfo.storyStep

        return storyStep.text?.split("\n", limit = 2)?.let { list ->
            val secondText = list.elementAtOrNull(1) ?: ""
            val secondMessage = StoryStep(
                localId = UUID.randomUUID().toString(),
                type = storyStep.type,
                text = secondText,
            )

            val position = lineBreakInfo.position + 1

            val (addedPosition, newStory) = addNewContent(
                currentStory,
                secondMessage,
                position
            )

            (addedPosition to secondMessage) to StoryState(
                stories = newStory,
                focusId = secondMessage.id
            )
        }
    }

    fun deleteStory(deleteInfo: DeleteInfo, history: Map<Int, StoryStep>): StoryState? {
        val step = deleteInfo.storyUnit
        val parentId = step.parentId
        val mutableSteps = history.toMutableMap()

        return if (parentId == null) {
            mutableSteps.remove(deleteInfo.position)
            val previousFocus =
                FindStory.previousFocus(
                    history.values.toList(),
                    deleteInfo.position,
                    focusableTypes
                )

            val normalized = stepsNormalizer(mutableSteps.toEditState())
            StoryState(normalized, focusId = previousFocus?.id)
        } else {
            mutableSteps[deleteInfo.position]?.let { group ->
                val newSteps = group.steps.filter { storyUnit ->
                    storyUnit.localId != step.localId
                }

                val newStoryUnit = if (newSteps.size == 1) {
                    newSteps.first()
                } else {
                    group.copy(steps = newSteps)
                }

                mutableSteps[deleteInfo.position] = newStoryUnit.copy(parentId = null)
                StoryState(stepsNormalizer(mutableSteps.toEditState()))
            }
        }

    }
}