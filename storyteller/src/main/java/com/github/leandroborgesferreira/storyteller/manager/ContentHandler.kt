package com.github.leandroborgesferreira.storyteller.manager

import com.github.leandroborgesferreira.storyteller.model.change.DeleteInfo
import com.github.leandroborgesferreira.storyteller.model.change.LineBreakInfo
import com.github.leandroborgesferreira.storyteller.model.story.Focus
import com.github.leandroborgesferreira.storyteller.model.story.StoryState
import com.github.leandroborgesferreira.storyteller.model.story.StoryStep
import com.github.leandroborgesferreira.storyteller.model.story.StoryType
import com.github.leandroborgesferreira.storyteller.utils.StoryStepFactory
import com.github.leandroborgesferreira.storyteller.utils.alias.UnitsNormalizationMap
import com.github.leandroborgesferreira.storyteller.utils.extensions.toEditState
import com.github.leandroborgesferreira.storyteller.utils.iterables.MapOperations
import java.util.UUID

/**
 * Class dedicated to handle adding and/or deleting new StorySteps
 */
class ContentHandler(
    private val focusableTypes: Set<String> = setOf(
        StoryType.TITLE.type,
        StoryType.MESSAGE.type,
        StoryType.CHECK_ITEM.type
    ),
    private val nonDuplicatableTypes: Map<String, String> = mapOf(
        StoryType.TITLE.type to StoryType.MESSAGE.type
    ),
    private val stepsNormalizer: UnitsNormalizationMap
) {

    fun createCheckItem(currentStory: Map<Int, StoryStep>, position: Int): StoryState {
        val newMap = currentStory.toMutableMap()
        val newCheck = StoryStep(
            id = UUID.randomUUID().toString(),
            localId = UUID.randomUUID().toString(),
            type = StoryType.CHECK_ITEM.type,
        )
        newMap[position] = newCheck

        return StoryState(newMap, Focus(newCheck.id))
    }

    //Todo: Add unit test
    fun addNewContent(
        currentStory: Map<Int, StoryStep>,
        newStoryUnit: StoryStep,
        position: Int
    ): Map<Int, StoryStep> =
        MapOperations.addElementInPosition(
            currentStory,
            newStoryUnit,
            StoryStepFactory.space(),
            position
        )

    fun addNewContentBulk(
        currentStory: Map<Int, StoryStep>,
        newStory: Map<Int, StoryStep>,
        addInBetween: () -> StoryStep
    ): Map<Int, StoryStep> = MapOperations.mergeSortedMaps(currentStory, newStory, addInBetween)

    fun onLineBreak(
        currentStory: Map<Int, StoryStep>,
        lineBreakInfo: LineBreakInfo
    ): Pair<Pair<Int, StoryStep>, StoryState>? {
        val storyStep = lineBreakInfo.storyStep

        return storyStep.text?.split("\n", limit = 2)?.let { list ->
            val firstText = list.elementAtOrNull(0) ?: ""
            val secondText = list.elementAtOrNull(1) ?: ""
            val secondMessage = StoryStep(
                localId = UUID.randomUUID().toString(),
                type = getStoryType(storyStep.type),
                text = secondText,
            )

            val addPosition = lineBreakInfo.position + 2

            //Todo: Cover this in unit tests!
            if (currentStory[addPosition]?.type == StoryType.SPACE.type) {
                throw IllegalStateException(
                    "it should not be possible to add content in the place of a space"
                )
            }

            val mutable = currentStory.toMutableMap().apply {
                this[lineBreakInfo.position] = storyStep.copy(text = firstText)
            }

            val newStory = addNewContent(
                mutable,
                secondMessage,
                addPosition
            )

            (addPosition to secondMessage) to StoryState(
                stories = newStory,
                focus = Focus(secondMessage.id)
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
            StoryState(normalized, focus = previousFocus?.id?.let(::Focus))
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

    fun bulkDeletion(
        positions: Iterable<Int>,
        stories: Map<Int, StoryStep>
    ): Pair<Map<Int, StoryStep>, Map<Int, StoryStep>> {
        val deleted = mutableMapOf<Int, StoryStep>()
        val mutable = stories.toMutableMap()
        positions.forEach { position ->
            mutable.remove(position)?.let { deletedStory ->
                deleted[position] = deletedStory
            }

            mutable.remove(position + 1)
        }

        return mutable to deleted
    }

    //Uses the preset conversion (example: Title becomes Message) of simply duplicate the type
    private fun getStoryType(type: String) = nonDuplicatableTypes[type] ?: type
}