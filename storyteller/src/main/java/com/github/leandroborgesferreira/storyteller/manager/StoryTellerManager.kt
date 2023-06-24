package com.github.leandroborgesferreira.storyteller.manager

import android.util.Log
import com.github.leandroborgesferreira.storyteller.backstack.BackStackManager
import com.github.leandroborgesferreira.storyteller.backstack.BackstackHandler
import com.github.leandroborgesferreira.storyteller.backstack.BackstackInform
import com.github.leandroborgesferreira.storyteller.model.backtrack.AddStoryUnit
import com.github.leandroborgesferreira.storyteller.model.backtrack.AddText
import com.github.leandroborgesferreira.storyteller.model.change.BulkDelete
import com.github.leandroborgesferreira.storyteller.model.change.CheckInfo
import com.github.leandroborgesferreira.storyteller.model.change.DeleteInfo
import com.github.leandroborgesferreira.storyteller.model.change.LineBreakInfo
import com.github.leandroborgesferreira.storyteller.model.change.MergeInfo
import com.github.leandroborgesferreira.storyteller.model.change.MoveInfo
import com.github.leandroborgesferreira.storyteller.model.change.TextEditInfo
import com.github.leandroborgesferreira.storyteller.model.story.DrawState
import com.github.leandroborgesferreira.storyteller.model.story.DrawStory
import com.github.leandroborgesferreira.storyteller.model.story.Focus
import com.github.leandroborgesferreira.storyteller.model.story.StoryState
import com.github.leandroborgesferreira.storyteller.model.story.StoryStep
import com.github.leandroborgesferreira.storyteller.model.story.StoryType
import com.github.leandroborgesferreira.storyteller.normalization.builder.StepsMapNormalizationBuilder
import com.github.leandroborgesferreira.storyteller.utils.alias.UnitsNormalizationMap
import com.github.leandroborgesferreira.storyteller.utils.extensions.toEditState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.flatMapLatest
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
    ),
    private val focusHandler: FocusHandler = FocusHandler()
) : BackstackHandler, BackstackInform by backStackManager {

    private val _scrollToPosition: MutableStateFlow<Int?> = MutableStateFlow(null)
    val scrollToPosition: StateFlow<Int?> = _scrollToPosition.asStateFlow()

    private val _currentStory: MutableStateFlow<StoryState> = MutableStateFlow(
        StoryState(stories = emptyMap())
    )

    private val _positionsOnEdit = MutableStateFlow(setOf<Int>())
    val positionsOnEdit = _positionsOnEdit.asStateFlow()

    val currentStory: StateFlow<StoryState> = _currentStory.asStateFlow()

//    val toDraw = currentStory.map { storyState ->
//        val focus = storyState.focusId
//        val toDrawStories = storyState.stories.mapValues { (position, storyStep) ->
//            DrawStory(storyStep, _positionsOnEdit.value.contains(position))
//        }
//
//        DrawState(toDrawStories, focus)
//    }

    @OptIn(ExperimentalCoroutinesApi::class)
    val toDraw = _positionsOnEdit.flatMapLatest { positions ->
        Log.d("Manager", "new edit positions: ${positions.joinToString()}")
        currentStory.map { storyState ->
            val focus = storyState.focus

            val toDrawStories = storyState.stories.mapValues { (position, storyStep) ->
                DrawStory(storyStep, positions.contains(position))
            }

            DrawState(toDrawStories, focus)
        }
    }

    private val isOnSelection: Boolean
        get() = _positionsOnEdit.value.isNotEmpty()

    fun saveOnStoryChanges(
        coroutineScope: CoroutineScope,
        documentId: String,
        documentRepository: DocumentRepository
    ) {
        coroutineScope.launch(Dispatchers.IO) {
            _currentStory.map { storyState ->
                storyState.stories
            }.collectLatest { content ->
                documentRepository.save(documentId, content)
            }
        }
    }

    fun isInitialized(): Boolean = _currentStory.value.stories.isNotEmpty()

    fun newStory() {
        val firstMessage = StoryStep(
            localId = UUID.randomUUID().toString(),
            type = StoryType.TITLE.type
        )
        val stories: Map<Int, StoryStep> = mapOf(0 to firstMessage)
        val normalized = stepsNormalizer(stories.toEditState())

        _currentStory.value = StoryState(
            normalized + normalized,
            Focus(firstMessage.id)
        )
    }

    //Todo: Add unit test fot this!!
    fun nextFocusOrCreate(position: Int) {
        val storyMap = _currentStory.value.stories
        val nextFocus = focusHandler.findNextFocus(position, _currentStory.value.stories)
        if (nextFocus != null) {
            val (nextPosition, storyStep) = nextFocus
            val mutable = storyMap.toMutableMap()
            mutable[nextPosition] = storyStep.copy(localId = UUID.randomUUID().toString())
            _currentStory.value =
                _currentStory.value.copy(stories = mutable, focus = Focus(storyStep.id))
        }
    }

    fun initStories(stories: Map<Int, StoryStep>) {
        if (isInitialized()) return

        _currentStory.value = StoryState(stepsNormalizer(stories.toEditState()), null)
        val normalized = stepsNormalizer(stories.toEditState())

        _currentStory.value = StoryState(normalized)
    }

    fun mergeRequest(info: MergeInfo) {
        if (isOnSelection) {
            cancelSelection()
        }

        val movedStories = movementHandler.merge(_currentStory.value.stories, info)
        _currentStory.value = StoryState(stories = stepsNormalizer(movedStories))
    }

    fun moveRequest(moveInfo: MoveInfo) {
        if (isOnSelection) {
            cancelSelection()
        }

        val newStory = movementHandler.move(_currentStory.value.stories, moveInfo)
        _currentStory.value = StoryState(stepsNormalizer(newStory.toEditState()))
    }

    /**
     * At the moment it is only possible to check items not inside groups. Todo: Fix it!
     */
    fun checkRequest(checkInfo: CheckInfo) {
        if (isOnSelection) {
            cancelSelection()
        }

        val storyUnit = checkInfo.storyUnit
        val newStep = (storyUnit as? StoryStep)?.copy(checked = checkInfo.checked) ?: return

        updateStory(checkInfo.position, newStep)
    }

    fun createCheckItem(position: Int) {
        if (isOnSelection) {
            cancelSelection()
        }

        _currentStory.value = contentHandler.createCheckItem(_currentStory.value.stories, position)
    }

    fun onTextEdit(text: String, position: Int) {
        if (isOnSelection) {
            cancelSelection()
        }

        _currentStory.value.stories[position]?.copy(text = text)?.let { newStory ->
            updateStory(position, newStory)
            backStackManager.addAction(TextEditInfo(text, position))
        }
    }

    fun onLineBreak(lineBreakInfo: LineBreakInfo) {
        if (isOnSelection) {
            cancelSelection()
        }

        contentHandler.onLineBreak(_currentStory.value.stories, lineBreakInfo)
            ?.let { (info, newState) ->
                // Todo: Fix this when the inner position are completed
                backStackManager.addAction(AddStoryUnit(info.second, position = info.first))

                _currentStory.value = newState
                _scrollToPosition.value = info.first
            }
    }

    fun onSelected(isSelected: Boolean, position: Int) {
        if (_currentStory.value.stories[position] != null) {
            val newOnEdit = if (isSelected) {
                _positionsOnEdit.value + position
            } else {
                _positionsOnEdit.value - position
            }

            _positionsOnEdit.value = newOnEdit
        }
    }

    fun clickAtTheEnd() {
        val stories = _currentStory.value.stories
        val lastContentStory = stories[stories.size - 3]

        if (lastContentStory?.type == StoryType.MESSAGE.type) {
            val newState = _currentStory.value.copy(focus = Focus(lastContentStory.id))
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

            _currentStory.value = StoryState(newStories, Focus(newLastMessage.id))
        }
    }

    override fun undo() {
        when (val backAction = backStackManager.undo()) {
            is DeleteInfo -> {
                _currentStory.value = revertDelete(backAction)
            }

            is AddStoryUnit -> {
                revertAddStory(backAction)
            }

            is BulkDelete -> {
                val newState = contentHandler.addNewContentBulk(
                    _currentStory.value.stories,
                    backAction.deletedUnits,
                    addInBetween = {
                        StoryStep(type = StoryType.SPACE.type)
                    }
                ).let { newStories ->
                    StoryState(stepsNormalizer(newStories.toEditState()))
                }

                _currentStory.value = newState
            }

            is AddText -> {
                revertAddText(_currentStory.value.stories, backAction)
            }

            else -> return
        }
    }


    override fun redo() {
        when (val action = backStackManager.redo()) {
            is DeleteInfo -> {
                contentHandler.deleteStory(action, _currentStory.value.stories)
            }

            is AddStoryUnit -> {
                val newStory = contentHandler.addNewContent(
                    _currentStory.value.stories,
                    action.storyUnit,
                    action.position
                )
                _currentStory.value = StoryState(
                    newStory,
                    newStory[action.position]?.id?.let(::Focus)
                )

                _scrollToPosition.value = action.position
            }

            is AddText -> {
                redoAddText(_currentStory.value.stories, action)
            }

            else -> return
        }
    }


    fun onDelete(deleteInfo: DeleteInfo) {
        contentHandler.deleteStory(deleteInfo, _currentStory.value.stories)?.let { newState ->
            _currentStory.value = newState
        }

        backStackManager.addAction(deleteInfo)
    }

    fun deleteSelection() {
        val (newStories, deletedStories) = contentHandler.bulkDeletion(
            _positionsOnEdit.value,
            _currentStory.value.stories
        )

//        Log.d("manager", "deleted: ${deletedStories.values.joinToString { it.text.toString() }} " +
//                "positions: ${_positionsOnEdit.value.joinToString { it.toString() }}")

        backStackManager.addAction(BulkDelete(deletedStories))
        _positionsOnEdit.value = emptySet()

        _currentStory.value =
            _currentStory.value.copy(stories = newStories)
    }

    private fun updateStory(position: Int, newStep: StoryStep, focus: Focus? = null) {
        val newMap = _currentStory.value.stories.toMutableMap()
        newMap[position] = newStep
        _currentStory.value = StoryState(newMap, focus)
    }

    private fun revertAddText(currentStory: Map<Int, StoryStep>, addText: AddText) {
        val mutableSteps = currentStory.toMutableMap()
        val revertStep = currentStory[addText.position]
        val currentText = revertStep?.text

        if (!currentText.isNullOrEmpty()) {
            val newText = if (currentText.length <= addText.text.length) {
                ""
            } else {
                currentText.substring(currentText.length - addText.text.length)
            }

            mutableSteps[addText.position] =
                revertStep.copy(localId = UUID.randomUUID().toString(), text = newText)

            _currentStory.value =
                StoryState(mutableSteps, focus = Focus(revertStep.id, atEnd = true))
        }
    }

    private fun redoAddText(currentStory: Map<Int, StoryStep>, addText: AddText) {
        val position = addText.position
        val mutableSteps = currentStory.toMutableMap()
        mutableSteps[position]?.let { editStep ->
            val newText = "${editStep.text.toString()}${addText.text}"
            mutableSteps[position] = editStep.copy(text = newText)
            _currentStory.value = StoryState(mutableSteps, focus = Focus(editStep.id, atEnd = true))
        }
    }

    private fun revertAddStory(addStoryUnit: AddStoryUnit) {
        contentHandler.deleteStory(
            DeleteInfo(addStoryUnit.storyUnit, position = addStoryUnit.position),
            _currentStory.value.stories
        )?.let { newState ->
            _currentStory.value = newState
        }
    }

    private fun revertDelete(deleteInfo: DeleteInfo): StoryState {
        val newStory = contentHandler.addNewContent(
            _currentStory.value.stories,
            deleteInfo.storyUnit,
            deleteInfo.position
        )

        return StoryState(
            stories = newStory,
            focus = Focus(deleteInfo.storyUnit.id, atEnd = true)
        )
    }

    private fun cancelSelection() {
        Log.d("Manager", "Cancelling selection")
        _positionsOnEdit.value = emptySet()
    }

}
