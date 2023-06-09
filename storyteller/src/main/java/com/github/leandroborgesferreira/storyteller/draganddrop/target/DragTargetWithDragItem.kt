package com.github.leandroborgesferreira.storyteller.draganddrop.target

import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DragIndicator
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.github.leandroborgesferreira.storyteller.R
import com.github.leandroborgesferreira.storyteller.model.draganddrop.DropInfo

//Todo: Review this name
@Composable
fun DragTargetWithDragItem(
    modifier: Modifier = Modifier,
    dataToDrop: DropInfo,
    content: @Composable () -> Unit
) {
    var currentPosition by remember { mutableStateOf(Offset.Zero) }
    val currentState = LocalDragTargetInfo.current
    val haptic = LocalHapticFeedback.current

    Row(modifier = modifier
        .onGloballyPositioned { layoutCoordinates ->
            // Todo: Offset.Zero Is wrong!
            currentPosition = layoutCoordinates.localToWindow(Offset.Zero)
        },
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Spacer(modifier = Modifier.width(10.dp))

        Icon(
            modifier = Modifier
                .size(25.dp)
                .pointerInput(Unit) {
                    detectDragGestures(onDragStart = { offset ->
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)

                        currentState.dataToDrop = dataToDrop
                        currentState.isDragging = true
                        currentState.dragPosition = currentPosition + offset
                        currentState.draggableComposable = content
                    }, onDrag = { change, dragAmount ->
                        change.consume()
                        currentState.dragOffset += Offset(dragAmount.x, dragAmount.y)
                    }, onDragEnd = {
                        currentState.isDragging = false
                        currentState.dragOffset = Offset.Zero
                    }, onDragCancel = {
                        currentState.dragOffset = Offset.Zero
                        currentState.isDragging = false
                    })
                },
            imageVector = Icons.Default.DragIndicator,
            contentDescription = stringResource(R.string.drag_icon)
        )

        content()
    }
}
