package com.github.leandroborgesferreira.storyteller.manager

import com.github.leandroborgesferreira.storyteller.model.document.Document
import com.github.leandroborgesferreira.storyteller.model.story.StoryStep

/**
 * DocumentRepository is the repository for using simple CRUD operations in [Document].
 * The implementations of this interface shouldn't control order (sorting) or oder configurations,
 * those need to be passed as parameters.
 */
interface DocumentRepository {

    suspend fun loadDocuments(orderBy: String): List<Document>

    suspend fun loadDocumentBy(id: String): Document?

    suspend fun saveDocument(document: Document)

    suspend fun save(documentId: String, content: Map<Int, StoryStep>)

    suspend fun deleteDocument(document: Document)
}
