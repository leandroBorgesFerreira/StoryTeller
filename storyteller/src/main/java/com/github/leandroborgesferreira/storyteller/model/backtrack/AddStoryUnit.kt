package com.github.leandroborgesferreira.storyteller.model.backtrack

import com.github.leandroborgesferreira.storyteller.model.story.StoryStep

data class AddStoryUnit(val storyUnit: StoryStep, val position: List<Int>)
