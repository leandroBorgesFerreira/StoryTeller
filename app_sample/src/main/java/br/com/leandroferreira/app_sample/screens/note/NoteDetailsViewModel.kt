package br.com.leandroferreira.app_sample.screens.note

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.github.leandroborgesferreira.storyteller.backstack.BackstackHandler
import com.github.leandroborgesferreira.storyteller.backstack.BackstackInform
import com.github.leandroborgesferreira.storyteller.manager.StoryTellerManager
import com.github.leandroborgesferreira.storyteller.model.document.Document
import com.github.leandroborgesferreira.storyteller.model.story.DrawState
import com.github.leandroborgesferreira.storyteller.model.story.StoryState
import com.github.leandroborgesferreira.storyteller.persistence.repository.DocumentRepositoryImpl
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.Date

class NoteDetailsViewModel(
    val storyTellerManager: StoryTellerManager,
    private val documentRepository: DocumentRepositoryImpl
) : ViewModel(),
    BackstackInform by storyTellerManager,
    BackstackHandler by storyTellerManager {

    private val _editModeState = MutableStateFlow(true)
    val editModeState: StateFlow<Boolean> = _editModeState

    val toEditState = storyTellerManager.positionsOnEdit
    val isEditState = storyTellerManager.positionsOnEdit.map { set ->
        set.isNotEmpty()
    }.stateIn(viewModelScope, started = SharingStarted.Lazily, initialValue = false)

    private val story: StateFlow<StoryState> = storyTellerManager.currentStory
    val scrollToPosition = storyTellerManager.scrollToPosition
    val toDraw = storyTellerManager.toDraw.stateIn(
        viewModelScope,
        started = SharingStarted.Lazily,
        initialValue = DrawState(emptyMap())
    )

    private val _documentState: MutableStateFlow<Document?> = MutableStateFlow(null)
    val documentState: StateFlow<Document?> = _documentState.asStateFlow()

    fun toggleEdit() {
        _editModeState.value = !_editModeState.value
    }

    fun deleteSelection() {
        storyTellerManager.deleteSelection()
    }

    fun createNewNote(documentId: String, title: String) {
        if (storyTellerManager.isInitialized()) return

        storyTellerManager.saveOnStoryChanges(documentId, documentRepository)
        storyTellerManager.newStory()

        viewModelScope.launch(Dispatchers.IO) {
            val document = Document(
                id = documentId,
                title = title,
                content = storyTellerManager.currentStory.value.stories,
                createdAt = Date(),
                lastUpdatedAt = Date()
            )

            documentRepository.saveDocument(document)
            _documentState.value = document
        }
    }

    fun requestDocumentContent(documentId: String) {
        if (storyTellerManager.isInitialized()) return

        viewModelScope.launch(Dispatchers.IO) {
            val document = documentRepository.loadDocumentBy(documentId)
            val content = document?.content
            _documentState.value = document

            if (content != null) {
                storyTellerManager.saveOnStoryChanges(documentId, documentRepository)
                storyTellerManager.initStories(content)
            }
        }
    }

    fun saveNote() {
        _documentState.value?.let { document ->
            viewModelScope.launch {
                documentRepository.saveDocument(
                    document.copy(
                        content = story.value.stories,
                        title = story.value.stories.values.firstOrNull { story ->
                            story.isTitle
                        }?.text ?: ""
                    )
                )
            }
        }
    }
}

class NoteDetailsViewModelFactory(
    private val storyTellerManager: StoryTellerManager,
    private val documentRepository: DocumentRepositoryImpl
) : ViewModelProvider.Factory {


    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(NoteDetailsViewModel::class.java)) {
            return NoteDetailsViewModel(storyTellerManager, documentRepository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
