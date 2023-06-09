package br.com.leandroferreira.note_menu.ui.dto

import com.github.leandroborgesferreira.storyteller.model.story.StoryStep

class DocumentCard(
    val documentId: String,
    val title: String,
    val lastEdit: String,
    val preview: List<StoryStep>
)

