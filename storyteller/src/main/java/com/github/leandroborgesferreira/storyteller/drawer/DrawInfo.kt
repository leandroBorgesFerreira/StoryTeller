package com.github.leandroborgesferreira.storyteller.drawer

import com.github.leandroborgesferreira.storyteller.model.story.Focus

data class DrawInfo(
    val editable: Boolean,
    val focus: Focus? = null,
    val position: Int,
    val extraData: Map<String, Any> = emptyMap(),
    val selectMode: Boolean = false
)
