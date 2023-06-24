package com.github.leandroborgesferreira.storyteller.drawer.content

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyItemScope
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import com.github.leandroborgesferreira.storyteller.R
import com.github.leandroborgesferreira.storyteller.drawer.DrawInfo
import com.github.leandroborgesferreira.storyteller.drawer.StoryUnitDrawer
import com.github.leandroborgesferreira.storyteller.model.change.LineBreakInfo
import com.github.leandroborgesferreira.storyteller.model.story.StoryStep
import com.github.leandroborgesferreira.storyteller.text.edition.TextCommandHandler

/**
 * Draw a text that can be edited. The edition of the text is both reflect in this Composable and
 * also notified by onTextEdit. It is necessary to reflect here to avoid losing the focus on the
 * TextField.
 */
@OptIn(ExperimentalMaterial3Api::class)
class TitleDrawer(
    private val containerModifier: Modifier = Modifier,
    private val innerContainerModifier: Modifier = Modifier,
    private val onTextEdit: (String, Int) -> Unit,
    private val onLineBreak: (LineBreakInfo) -> Unit,
) : StoryUnitDrawer {

    @Composable
    override fun Step(step: StoryStep, drawInfo: DrawInfo) {
        val focusRequester = remember { FocusRequester() }

        Column(modifier = containerModifier.clickable {
            focusRequester.requestFocus()
        }) {
            if (drawInfo.editable) {
                var inputText by remember {
                    val text = step.text ?: ""
                    mutableStateOf(TextFieldValue(text, TextRange(text.length)))
                }

                LaunchedEffect(drawInfo.focus?.id) {
                    if (drawInfo.focus?.id == step.id) {
                        focusRequester.requestFocus()
                    }
                }

                TextField(
                    modifier = innerContainerModifier
                        .focusRequester(focusRequester)
                        .fillMaxWidth(),
                    value = inputText,
                    onValueChange = { value ->
                        if (value.text.contains("\n")) {
                            onLineBreak(LineBreakInfo(step, drawInfo.position))
                        } else {
                            inputText = value
                            onTextEdit(value.text, drawInfo.position)
                        }
                    },
                    keyboardOptions = KeyboardOptions(
                        capitalization = KeyboardCapitalization.Sentences
                    ),
                    placeholder = {
                        Text(
                            stringResource(R.string.title),
                            style = MaterialTheme.typography.titleLarge.copy(
                                color = Color.Gray,
                                fontWeight = FontWeight.Bold,
                            ),
                        )
                    },
                    textStyle = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.Bold
                    ),
                    colors = TextFieldDefaults.textFieldColors(
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent,
                        disabledIndicatorColor = Color.Transparent,
                        containerColor = Color.Transparent,
                        cursorColor = MaterialTheme.colorScheme.primary
                    )
                )
            } else {
                Text(
                    text = step.text ?: "",
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                    style = MaterialTheme.typography.titleLarge,
                )
            }
        }
    }
}
