package com.pranav.flipbook.pdf.metadata

import android.content.Context
import android.net.Uri
import com.tom_roush.pdfbox.android.PDFBoxResourceLoader
import com.tom_roush.pdfbox.pdmodel.PDDocument
import com.tom_roush.pdfbox.pdmodel.interactive.documentnavigation.outline.PDOutlineItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

data class PdfMetadata(
    val title: String? = null,
    val author: String? = null,
    val subject: String? = null,
    val creator: String? = null,
    val producer: String? = null,
    val pageCount: Int = 0
)

data class TocEntry(
    val title: String,
    val pageIndex: Int,
    val level: Int = 0,
    val children: List<TocEntry> = emptyList()
)

class PdfMetadataExtractor(private val context: Context) {

    init {
        PDFBoxResourceLoader.init(context)
    }

    suspend fun extractMetadata(uri: Uri): PdfMetadata? {
        return withContext(Dispatchers.IO) {
            try {
                context.contentResolver.openInputStream(uri)?.use { input ->
                    val document = PDDocument.load(input)
                    val info = document.documentInformation
                    val metadata = PdfMetadata(
                        title = info?.title?.takeIf { it.isNotBlank() },
                        author = info?.author?.takeIf { it.isNotBlank() },
                        subject = info?.subject?.takeIf { it.isNotBlank() },
                        creator = info?.creator?.takeIf { it.isNotBlank() },
                        producer = info?.producer?.takeIf { it.isNotBlank() },
                        pageCount = document.numberOfPages
                    )
                    document.close()
                    metadata
                }
            } catch (e: Exception) {
                e.printStackTrace()
                null
            }
        }
    }

    suspend fun extractTableOfContents(uri: Uri): List<TocEntry> {
        return withContext(Dispatchers.IO) {
            try {
                context.contentResolver.openInputStream(uri)?.use { input ->
                    val document = PDDocument.load(input)
                    val outline = document.documentCatalog?.documentOutline
                    val entries = mutableListOf<TocEntry>()

                    if (outline != null) {
                        var item = outline.firstChild
                        while (item != null) {
                            entries.add(extractOutlineItem(item, document, 0))
                            item = item.nextSibling
                        }
                    }
                    document.close()
                    entries
                } ?: emptyList()
            } catch (e: Exception) {
                e.printStackTrace()
                emptyList()
            }
        }
    }

    private fun extractOutlineItem(item: PDOutlineItem, document: PDDocument, level: Int): TocEntry {
        val pageIndex = try {
            val dest = item.destination as? com.tom_roush.pdfbox.pdmodel.interactive.documentnavigation.destination.PDPageDestination
                ?: (item.action as? com.tom_roush.pdfbox.pdmodel.interactive.action.PDActionGoTo)?.destination as? com.tom_roush.pdfbox.pdmodel.interactive.documentnavigation.destination.PDPageDestination
            val pNum = dest?.retrievePageNumber() ?: -1
            if (pNum >= 0) pNum else 0
        } catch (_: Exception) { 0 }

        val children = mutableListOf<TocEntry>()
        var child = item.firstChild
        while (child != null) {
            children.add(extractOutlineItem(child, document, level + 1))
            child = child.nextSibling
        }

        return TocEntry(
            title = item.title ?: "Untitled",
            pageIndex = pageIndex.coerceAtLeast(0),
            level = level,
            children = children
        )
    }
}
