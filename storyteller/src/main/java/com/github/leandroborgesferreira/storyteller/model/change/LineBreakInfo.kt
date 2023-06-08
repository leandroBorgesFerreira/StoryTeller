package com.github.leandroborgesferreira.storyteller.model.change

import com.github.leandroborgesferreira.storyteller.model.story.StoryStep

data class LineBreakInfo(val storyStep: StoryStep, val positionList: List<Int>)
