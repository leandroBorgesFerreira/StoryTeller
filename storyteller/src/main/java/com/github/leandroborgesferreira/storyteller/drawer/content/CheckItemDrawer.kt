package com.github.leandroborgesferreira.storyteller.drawer.content

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.LocalMinimumInteractiveComponentEnforcement
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import com.github.leandroborgesferreira.storyteller.draganddrop.target.DragTarget
import com.github.leandroborgesferreira.storyteller.draganddrop.target.DragTargetWithDragItem
import com.github.leandroborgesferreira.storyteller.drawer.DrawInfo
import com.github.leandroborgesferreira.storyteller.drawer.StoryUnitDrawer
import com.github.leandroborgesferreira.storyteller.drawer.common.SwipeToCommandBox
import com.github.leandroborgesferreira.storyteller.drawer.modifier.callOnEmptyErase
import com.github.leandroborgesferreira.storyteller.model.change.CheckInfo
import com.github.leandroborgesferreira.storyteller.model.change.DeleteInfo
import com.github.leandroborgesferreira.storyteller.model.draganddrop.DropInfo
import com.github.leandroborgesferreira.storyteller.model.story.StoryStep
import com.github.leandroborgesferreira.storyteller.text.edition.TextCommandHandler

class CheckItemDrawer(
    private val onCheckedChange: (CheckInfo) -> Unit = {},
    private val onTextEdit: (String, Int) -> Unit = { _, _ -> },
    private val onDeleteRequest: (DeleteInfo) -> Unit = {},
    private val onSelected: (Boolean, Int) -> Unit = { _, _ -> },
    private val commandHandler: TextCommandHandler = TextCommandHandler(emptyMap()),
    private val customBackgroundColor: Color? = null,
    private val clickable: Boolean = true
) : StoryUnitDrawer {

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    override fun Step(step: StoryStep, drawInfo: DrawInfo) {
        val dropInfo = DropInfo(step, drawInfo.position)
        val focusRequester = remember { FocusRequester() }
        var hasFocus by remember { mutableStateOf(false) }

        Box(modifier = Modifier.padding(horizontal = 6.dp)) {
            SwipeToCommandBox(
                modifier = Modifier
                    .clip(RoundedCornerShape(3.dp))
                    .apply {
                        if (clickable) {
                            clickable {
                                focusRequester.requestFocus()
                            }
                        }
                    },
                defaultColor = customBackgroundColor ?: MaterialTheme.colorScheme.background,
                activeColor = MaterialTheme.colorScheme.primary,
                state = drawInfo.selectMode,
                listener = { isSelected ->
                    onSelected(isSelected, drawInfo.position)
                }
            ) {
                DragTargetWithDragItem(dataToDrop = dropInfo) {
                    DragTarget(dataToDrop = dropInfo) {
                        Row(
                            modifier = Modifier
                                .padding(horizontal = 2.dp)
                                .fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            var inputText by remember {
                                val text = step.text ?: ""
                                mutableStateOf(TextFieldValue(text, TextRange(text.length)))
                            }

                            val textStyle = if (step.checked == true) {
                                TextStyle(
                                    textDecoration = TextDecoration.LineThrough,
                                    color = MaterialTheme.colorScheme.onBackground
                                )
                            } else {
                                TextStyle(color = MaterialTheme.colorScheme.onBackground)
                            }

                            LaunchedEffect(drawInfo.focus?.id) {
                                if (drawInfo.focus?.id == step.id) {
                                    focusRequester.requestFocus()
                                }
                            }

                            CompositionLocalProvider(
                                LocalMinimumInteractiveComponentEnforcement provides false
                            ) {
                                Checkbox(
                                    modifier = Modifier.padding(6.dp),
                                    checked = step.checked ?: false,
                                    onCheckedChange = { checked ->
                                        onCheckedChange(CheckInfo(step, drawInfo.position, checked))
                                    },
                                    enabled = drawInfo.editable,
                                )
                            }

                            BasicTextField(
                                modifier = Modifier
                                    .weight(1F)
                                    .focusRequester(focusRequester)
                                    .onFocusChanged { focusState ->
                                        hasFocus = focusState.hasFocus
                                    }
                                    .callOnEmptyErase(inputText.selection) {
                                        onDeleteRequest(DeleteInfo(step, drawInfo.position))
                                    },
                                value = inputText,
                                onValueChange = { value: TextFieldValue ->
                                    if (!commandHandler.handleCommand(
                                            value.text,
                                            step,
                                            drawInfo.position
                                        )
                                    ) {
                                        inputText = value
                                        onTextEdit(value.text, drawInfo.position)
                                    }
                                },
                                textStyle = textStyle,
                                enabled = drawInfo.editable,
                                keyboardOptions = KeyboardOptions(
                                    capitalization = KeyboardCapitalization.Sentences
                                ),
                                cursorBrush = SolidColor(MaterialTheme.colorScheme.primary)
                            )
                        }
                    }
                }
            }
        }
    }
}
