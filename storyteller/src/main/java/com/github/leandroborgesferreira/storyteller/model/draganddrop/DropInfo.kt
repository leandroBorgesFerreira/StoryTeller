package com.github.leandroborgesferreira.storyteller.model.draganddrop

import com.github.leandroborgesferreira.storyteller.model.story.StoryStep

data class DropInfo(val storyUnit: StoryStep, val positionFrom: List<Int>)
