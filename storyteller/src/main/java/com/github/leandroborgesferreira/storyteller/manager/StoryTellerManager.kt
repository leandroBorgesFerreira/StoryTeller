package com.github.leandroborgesferreira.storyteller.manager

import com.github.leandroborgesferreira.storyteller.backstack.BackStackManager
import com.github.leandroborgesferreira.storyteller.backstack.BackstackHandler
import com.github.leandroborgesferreira.storyteller.backstack.BackstackInform
import com.github.leandroborgesferreira.storyteller.model.backtrack.AddStoryUnit
import com.github.leandroborgesferreira.storyteller.model.backtrack.AddText
import com.github.leandroborgesferreira.storyteller.model.change.CheckInfo
import com.github.leandroborgesferreira.storyteller.model.change.DeleteInfo
import com.github.leandroborgesferreira.storyteller.model.change.LineBreakInfo
import com.github.leandroborgesferreira.storyteller.model.change.MergeInfo
import com.github.leandroborgesferreira.storyteller.model.change.MoveInfo
import com.github.leandroborgesferreira.storyteller.model.change.TextEditInfo
import com.github.leandroborgesferreira.storyteller.model.story.StoryState
import com.github.leandroborgesferreira.storyteller.model.story.StoryStep
import com.github.leandroborgesferreira.storyteller.model.story.StoryType
import com.github.leandroborgesferreira.storyteller.normalization.builder.StepsMapNormalizationBuilder
import com.github.leandroborgesferreira.storyteller.utils.UnitsNormalizationMap
import com.github.leandroborgesferreira.storyteller.utils.extensions.toEditState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import java.util.UUID

class StoryTellerManager(
    private val stepsNormalizer: UnitsNormalizationMap =
        StepsMapNormalizationBuilder.reduceNormalizations {
            defaultNormalizers()
        },
    private val backStackManager: BackStackManager = BackStackManager(),
    private val movementHandler: MovementHandler = MovementHandler(),
    private val contentHandler: ContentHandler = ContentHandler(
        focusableTypes = setOf(
            StoryType.CHECK_ITEM.type,
            StoryType.MESSAGE.type,
            StoryType.MESSAGE_BOX.type,
        ),
        stepsNormalizer = stepsNormalizer
    )
) : BackstackHandler, BackstackInform by backStackManager {

    private val _scrollToPosition: MutableStateFlow<Int?> = MutableStateFlow(null)
    val scrollToPosition: StateFlow<Int?> = _scrollToPosition.asStateFlow()

    private val textChanges: MutableMap<Int, String> = mutableMapOf()

    private val _currentStory: MutableStateFlow<StoryState> = MutableStateFlow(
        StoryState(stories = emptyMap())
    )

    val currentStory: StateFlow<StoryState> = _currentStory.asStateFlow()

    fun saveOnStoryChanges(
        coroutineScope: CoroutineScope,
        documentId: String,
        storyStateSaver: StoryStateSaver
    ) {
        coroutineScope.launch(Dispatchers.IO) {
            currentStory.map { storyState ->
                storyState.stories
            }.collectLatest { content ->
                storyStateSaver.saveState(documentId, content)
            }
        }
    }

    fun isInitialized(): Boolean = _currentStory.value.stories.isNotEmpty()

    fun newStory() {
        val firstMessage = StoryStep(
            localId = UUID.randomUUID().toString(),
            type = "message"
        )
        val stories: Map<Int, StoryStep> = mapOf(0 to firstMessage)
        val normalized = stepsNormalizer(stories.toEditState())

        _currentStory.value = StoryState(
            normalized + normalized,
            firstMessage.id
        )
    }

    fun initStories(stories: Map<Int, StoryStep>) {
        if (isInitialized()) return

        _currentStory.value = StoryState(stepsNormalizer(stories.toEditState()), null)
        val normalized = stepsNormalizer(stories.toEditState())

        _currentStory.value = StoryState(normalized)
    }

    fun mergeRequest(info: MergeInfo) {
        val movedStories = movementHandler.merge(_currentStory.value.stories, info)
        _currentStory.value = StoryState(stories = stepsNormalizer(movedStories))
    }

    fun moveRequest(moveInfo: MoveInfo) {
        val newStory = movementHandler.move(_currentStory.value.stories, moveInfo)
        _currentStory.value = StoryState(stepsNormalizer(newStory.toEditState()))
    }

    /**
     * At the moment it is only possible to check items not inside groups. Todo: Fix it!
     */
    fun checkRequest(checkInfo: CheckInfo) {
        updateState()
        val storyUnit = checkInfo.storyUnit

        val newStep = (storyUnit as? StoryStep)?.copy(checked = checkInfo.checked) ?: return
        val newMap = _currentStory.value.stories.toMutableMap()

        newMap[checkInfo.position] = newStep

        _currentStory.value = StoryState(newMap)
    }

    fun createCheckItem(position: List<Int>) {
        updateState()
        _currentStory.value = contentHandler.createCheckItem(_currentStory.value.stories, position)
    }

    fun onTextEdit(text: String, position: List<Int>) {
        textChanges[position] = text
        backStackManager.addAction(TextEditInfo(text, position))
    }

    fun onLineBreak(lineBreakInfo: LineBreakInfo) {
        val updatedStories = updateTexts(_currentStory.value.stories)

        contentHandler.onLineBreak(updatedStories, lineBreakInfo)?.let { (info, newState) ->
            // Todo: Fix this when the inner position are completed
            backStackManager.addAction(AddStoryUnit(info.second, position = info.first))

            _currentStory.value = newState
            _scrollToPosition.value = info.first
        }
    }

    fun clickAtTheEnd() {
        val stories = _currentStory.value.stories
        val lastContentStory = stories[stories.size - 3]

        if (lastContentStory?.type == StoryType.MESSAGE.type) {
            val newState = _currentStory.value.copy(focusId = lastContentStory.id)
            _currentStory.value = newState
        } else {
            var acc = stories.size - 1
            val newLastMessage = StoryStep(type = StoryType.MESSAGE.type)

            //Todo: It should be possible to customize which steps are add
            val newStories = stories + mapOf(
                acc++ to newLastMessage,
                acc++ to StoryStep(type = StoryType.SPACE.type),
                acc to StoryStep(type = StoryType.LARGE_SPACE.type),
            )

            _currentStory.value = StoryState(newStories, newLastMessage.id)
        }
    }

    fun updateState() {
        _currentStory.value = StoryState(updateTexts(_currentStory.value.stories))
    }

    override fun undo() {
        when (val backAction = backStackManager.undo()) {
            is DeleteInfo -> {
                _currentStory.value = revertDelete(backAction)
            }

            is AddStoryUnit -> {
                revertAddStory(backAction)
            }

            is AddText -> {
                revertAddText(currentStory.value.stories, backAction)
            }

            else -> return
        }
    }


    override fun redo() {
        when (val action = backStackManager.redo()) {
            is DeleteInfo -> {
                contentHandler.deleteStory(action, currentStory.value.stories)
            }

            is AddStoryUnit -> {
                val (position, newStory) = contentHandler.addNewContent(
                    currentStory.value.stories,
                    action.storyUnit,
                    action.position - 1
                )
                _currentStory.value = StoryState(
                    newStory,
                    newStory[position]?.id
                )

                _scrollToPosition.value = position
            }

            is AddText -> {
                redoAddText(currentStory.value.stories, action)
            }

            else -> return
        }
    }

    private fun revertAddText(currentStory: Map<Int, StoryStep>, addText: AddText) {
        val mutableSteps = currentStory.toMutableMap()
        val revertStep = mutableSteps[addText.position]
        val currentText = revertStep?.text

        if (!currentText.isNullOrEmpty()) {
            val newText = if (currentText.length <= addText.text.length) {
                ""
            } else {
                currentText.substring(currentText.length - addText.text.length)
            }

            mutableSteps[addText.position] =
                revertStep.copy(localId = UUID.randomUUID().toString(), text = newText)

            _currentStory.value = StoryState(mutableSteps, focusId = revertStep.id)
        }
    }

    private fun redoAddText(currentStory: Map<Int, StoryStep>, addText: AddText) {
        val position = addText.position
        val mutableSteps = currentStory.toMutableMap()
        mutableSteps[position]?.let { editStep ->
            val newText = "${editStep.text.toString()}${addText.text}"
            mutableSteps[position] = editStep.copy(text = newText)
            _currentStory.value = StoryState(mutableSteps, focusId = editStep.id)
        }
    }

    private fun revertAddStory(addStoryUnit: AddStoryUnit) {
        contentHandler.deleteStory(
            DeleteInfo(addStoryUnit.storyUnit, positionList = addStoryUnit.position),
            currentStory.value.stories
        )?.let { newState ->
            _currentStory.value = newState
        }
    }

    private fun revertDelete(deleteInfo: DeleteInfo): StoryState {
        val (_, newStory) = contentHandler.addNewContent(
            currentStory.value.stories,
            deleteInfo.storyUnit,
            deleteInfo.positionList
        )

        return StoryState(
            stories = newStory,
            focusId = deleteInfo.storyUnit.id
        )
    }

    private fun updateTexts(stepMap: Map<Int, StoryStep>): Map<Int, StoryStep> {
        val mutableSteps = stepMap.toMutableMap()

        textChanges.forEach { (position, text) ->
            val editStep = mutableSteps[position]

            editStep?.copy(text = text)?.let { step ->
                mutableSteps[position] = step
            }
        }

        textChanges.clear()

        return mutableSteps
    }

    fun onDelete(deleteInfo: DeleteInfo) {
        val newSteps = updateTexts(_currentStory.value.stories)

        contentHandler.deleteStory(deleteInfo, newSteps)?.let { newState ->
            _currentStory.value = newState
        }

        backStackManager.addAction(deleteInfo)
    }
}
