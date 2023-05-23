package com.github.leandroborgesferreira.storyteller.normalization.merge

import com.github.leandroborgesferreira.storyteller.model.story.GroupStep
import com.github.leandroborgesferreira.storyteller.model.story.StoryUnit

interface StepMerger {

    fun merge(step1: StoryUnit, step2: StoryUnit, type: String): StoryUnit

    fun groupsMerger(step1: GroupStep, step2: GroupStep): StoryUnit

}