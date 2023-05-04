package br.com.leandroferreira.storyteller.utils.extensions

import br.com.leandroferreira.storyteller.model.StoryUnit

fun Map<Int, StoryUnit>.toEditState(): MutableMap<Int, List<StoryUnit>> =
    mapValues { (_, story) -> listOf(story) }.toMutableMap()

fun <T> Iterable<T>.associateWithPosition(): Map<Int, T> {
    var acc = -1

    return associateBy { ++acc }
}