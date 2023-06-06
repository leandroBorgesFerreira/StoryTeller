package com.github.leandroborgesferreira.storyteller.model.change

import com.github.leandroborgesferreira.storyteller.model.story.StoryStep

data class CheckInfo(val storyUnit: StoryStep, val position: Int, val checked: Boolean)
