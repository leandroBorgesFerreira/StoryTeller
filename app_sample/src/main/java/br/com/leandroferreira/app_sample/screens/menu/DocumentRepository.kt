package br.com.leandroferreira.app_sample.screens.menu

import android.content.Context
import br.com.leandroferreira.app_sample.data.supermarketList
import br.com.leandroferreira.app_sample.data.syncHistory
import br.com.leandroferreira.app_sample.parse.toEntity
import br.com.leandroferreira.storyteller.model.story.GroupStep
import br.com.leandroferreira.storyteller.model.story.StoryStep
import br.com.leandroferreira.storyteller.model.story.StoryUnit
import br.com.leandroferreira.storyteller.persistence.dao.DocumentDao
import br.com.leandroferreira.storyteller.persistence.dao.StoryUnitDao
import br.com.leandroferreira.storyteller.persistence.entity.document.DocumentEntity
import br.com.leandroferreira.storyteller.persistence.entity.story.StoryUnitEntity
import java.util.UUID

class DocumentRepository(
    private val documentDao: DocumentDao,
    private val storyUnitDao: StoryUnitDao,
) {

    suspend fun loadDocuments(): List<DocumentEntity> = documentDao.loadAllDocuments()

    suspend fun mockData(context: Context) {
        val travelNoteId = UUID.randomUUID().toString()
        val superMarketNoteId = UUID.randomUUID().toString()

        documentDao.insertDocuments(
            DocumentEntity(travelNoteId, "Travel Note"),
            DocumentEntity(superMarketNoteId, "Supermarket List")
        )

        val travelContent = syncHistory(context).toEntity(travelNoteId)
        val supermarketContent = supermarketList().toEntity(superMarketNoteId)

        storyUnitDao.insertDocuments(*travelContent.toTypedArray())
        storyUnitDao.insertDocuments(*supermarketContent.toTypedArray())
    }
}

fun Map<Int, StoryUnit>.toEntity(id: String): List<StoryUnitEntity> =
    map { (position, storyUnit) ->
        if (storyUnit is GroupStep) {
            storyUnit.toEntity(position, id)
        } else {
            (storyUnit as StoryStep).toEntity(position, id)
        }
    }
