package com.github.leandroborgesferreira.storyteller.drawer

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.github.leandroborgesferreira.storyteller.drawer.commands.CommandsDecoratorDrawer
import com.github.leandroborgesferreira.storyteller.drawer.content.AddButtonDrawer
import com.github.leandroborgesferreira.storyteller.drawer.content.CheckItemDrawer
import com.github.leandroborgesferreira.storyteller.drawer.content.ImageGroupDrawer
import com.github.leandroborgesferreira.storyteller.drawer.content.ImageStepDrawer
import com.github.leandroborgesferreira.storyteller.drawer.content.ImageStepDrawer.Companion.defaultModifier
import com.github.leandroborgesferreira.storyteller.drawer.content.LargeEmptySpace
import com.github.leandroborgesferreira.storyteller.drawer.content.MessageStepDrawer
import com.github.leandroborgesferreira.storyteller.drawer.content.VideoStepDrawer
import com.github.leandroborgesferreira.storyteller.drawer.content.SpaceDrawer
import com.github.leandroborgesferreira.storyteller.manager.StoryTellerManager
import com.github.leandroborgesferreira.storyteller.model.story.StoryType
import com.github.leandroborgesferreira.storyteller.model.change.CheckInfo
import com.github.leandroborgesferreira.storyteller.model.change.DeleteInfo
import com.github.leandroborgesferreira.storyteller.model.change.LineBreakInfo
import com.github.leandroborgesferreira.storyteller.model.change.MergeInfo
import com.github.leandroborgesferreira.storyteller.model.change.MoveInfo
import com.github.leandroborgesferreira.storyteller.text.edition.TextCommandHandler

object DefaultDrawers {

    fun create(
        editable: Boolean = false,
        manager: StoryTellerManager,
        groupsBackgroundColor: Color = Color.Transparent
    ): Map<String, StoryUnitDrawer> =
        create(
            editable = editable,
            onTextEdit = manager::onTextEdit,
            onLineBreak = manager::onLineBreak,
            mergeRequest = manager::mergeRequest,
            moveRequest = manager::moveRequest,
            checkRequest = manager::checkRequest,
            onDeleteRequest = manager::onDelete,
            createCheckItem = manager::createCheckItem,
            clickAtTheEnd = manager::clickAtTheEnd,
            groupsBackgroundColor = groupsBackgroundColor
        )

    fun create(
        editable: Boolean = false,
        onTextEdit: (String, List<Int>) -> Unit,
        onLineBreak: (LineBreakInfo) -> Unit,
        mergeRequest: (MergeInfo) -> Unit = { },
        moveRequest: (MoveInfo) -> Unit = { },
        checkRequest: (CheckInfo) -> Unit = { },
        onDeleteRequest: (DeleteInfo) -> Unit,
        createCheckItem: (List<Int>) -> Unit,
        clickAtTheEnd: () -> Unit,
        groupsBackgroundColor: Color = Color.Transparent
    ): Map<String, StoryUnitDrawer> =
        buildMap {
            val textCommandHandlerMessage = TextCommandHandler(
                mapOf(
                    "\n" to { storyStep, position ->
                        onLineBreak(LineBreakInfo(storyStep, position))
                    },
                    "-[]" to { _, position ->
                        createCheckItem(position)
                    }
                )
            )

            val textCommandHandlerCheckItem = TextCommandHandler(
                mapOf(
                    "\n" to { storyStep, position ->
                        onLineBreak(LineBreakInfo(storyStep, position))
                    },
                )
            )

            val commandsComposite: (StoryUnitDrawer) -> StoryUnitDrawer = { stepDrawer ->
                CommandsDecoratorDrawer(
                    stepDrawer,
                    onDelete = onDeleteRequest,
                )
            }

            val imageDrawer = ImageStepDrawer(
                containerModifier = { inBound -> Modifier.defaultModifier(inBound) },
                mergeRequest = mergeRequest
            )

            val imageDrawerInGroup = ImageStepDrawer(
                containerModifier = { inBound -> Modifier.defaultModifier(inBound) },
                mergeRequest = mergeRequest
            )

            val messageBoxDrawer = MessageStepDrawer(
                containerModifier = Modifier
                    .padding(horizontal = 8.dp)
                    .clip(shape = RoundedCornerShape(size = 12.dp))
                    .background(groupsBackgroundColor),
                innerContainerModifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
                onTextEdit = onTextEdit,
                onDeleteRequest = onDeleteRequest,
                commandHandler = textCommandHandlerMessage
            )

            val messageDrawer = MessageStepDrawer(
                containerModifier = Modifier,
                innerContainerModifier = Modifier.padding(horizontal = 8.dp, vertical = 0.dp),
                onTextEdit = onTextEdit,
                onDeleteRequest = onDeleteRequest,
                commandHandler = textCommandHandlerMessage
            )

            val checkItemDrawer = CheckItemDrawer(
                onCheckedChange = checkRequest,
                onTextEdit = onTextEdit,
                onDeleteRequest = onDeleteRequest,
                commandHandler = textCommandHandlerCheckItem
            )

            put(StoryType.MESSAGE_BOX.type, messageBoxDrawer)
            put(StoryType.MESSAGE.type, messageDrawer)
            put(StoryType.ADD_BUTTON.type, AddButtonDrawer())
            put(StoryType.IMAGE.type, if (editable) commandsComposite(imageDrawer) else imageDrawer)
            put(
                StoryType.GROUP_IMAGE.type,
                if (editable) {
                    commandsComposite(ImageGroupDrawer(commandsComposite(imageDrawerInGroup)))
                } else {
                    ImageGroupDrawer(imageDrawerInGroup)
                }
            )
            put(
                StoryType.VIDEO.type,
                if (editable) commandsComposite(VideoStepDrawer()) else VideoStepDrawer()
            )

            put(StoryType.SPACE.type, SpaceDrawer(moveRequest))
            put(StoryType.LARGE_SPACE.type, LargeEmptySpace(moveRequest, clickAtTheEnd))
            put(StoryType.CHECK_ITEM.type, checkItemDrawer)
        }
}
