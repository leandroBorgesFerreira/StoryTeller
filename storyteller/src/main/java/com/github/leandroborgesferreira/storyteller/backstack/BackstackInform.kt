package com.github.leandroborgesferreira.storyteller.backstack

import kotlinx.coroutines.flow.StateFlow

interface BackstackInform {

    val canUndo: StateFlow<Boolean>

    val canRedo: StateFlow<Boolean>
}
