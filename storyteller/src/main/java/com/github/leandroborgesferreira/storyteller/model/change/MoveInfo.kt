package com.github.leandroborgesferreira.storyteller.model.change

import com.github.leandroborgesferreira.storyteller.model.story.StoryStep

data class MoveInfo(
    val storyUnit: StoryStep,
    val positionFrom: List<Int>,
    val positionTo: List<Int>
)
