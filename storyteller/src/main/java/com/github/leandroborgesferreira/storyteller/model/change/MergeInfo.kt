package com.github.leandroborgesferreira.storyteller.model.change

import com.github.leandroborgesferreira.storyteller.model.story.StoryStep

data class MergeInfo(
    val receiver: StoryStep,
    val sender: StoryStep,
    val positionFrom: List<Int>,
    val positionTo: List<Int>
)
