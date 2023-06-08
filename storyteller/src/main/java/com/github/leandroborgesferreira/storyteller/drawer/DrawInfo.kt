package com.github.leandroborgesferreira.storyteller.drawer

data class DrawInfo(
    val editable: Boolean,
    val focusId: String?,
    val positionList: List<Int>,
    val extraData: Map<String, Any>,
)
