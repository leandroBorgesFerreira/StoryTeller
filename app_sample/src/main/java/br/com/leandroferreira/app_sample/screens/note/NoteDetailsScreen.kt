package br.com.leandroferreira.app_sample.screens.note

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Scaffold
import androidx.compose.material.Text
import androidx.compose.material.TextButton
import androidx.compose.material.TopAppBar
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import br.com.leandroferreira.app_sample.theme.ApplicationComposeTheme
import br.com.leandroferreira.storyteller.StoryTellerTimeline
import br.com.leandroferreira.storyteller.drawer.DefaultDrawers

@Composable
fun NoteDetailsScreen(documentId: String, noteDetailsViewModel: NoteDetailsViewModel) {
    noteDetailsViewModel.requestDocumentContent(documentId)

    ApplicationComposeTheme {
        Scaffold(
            topBar = { TopBar(noteDetailsViewModel = noteDetailsViewModel) },
        ) { paddingValues ->
            Box(modifier = Modifier.padding(top = paddingValues.calculateTopPadding())) {
                Body(noteDetailsViewModel)
            }
        }
    }
}

@Composable
private fun TopBar(noteDetailsViewModel: NoteDetailsViewModel) {
    TopAppBar(
        title = { Text(text = "Note") },
        actions = {
            TextButton(onClick = noteDetailsViewModel::saveNote) {
                Text(
                    text = "Save",
                    style = MaterialTheme.typography.h6.copy(
                        fontSize = 19.sp,
                        color = MaterialTheme.colors.onPrimary
                    )
                )
            }
        }
    )
}

@Composable
fun Body(noteDetailsViewModel: NoteDetailsViewModel) {
    val storyState by noteDetailsViewModel.story.collectAsStateWithLifecycle()
    val editable by noteDetailsViewModel.editModeState.collectAsStateWithLifecycle()

    StoryTellerTimeline(
        modifier = Modifier.fillMaxSize(),
        storyState = storyState,
        contentPadding = PaddingValues(top = 4.dp, bottom = 60.dp),
        editable = editable,
//            listState = listState,
        drawers = DefaultDrawers.create(editable, noteDetailsViewModel.storyTellerManager)
    )
}

