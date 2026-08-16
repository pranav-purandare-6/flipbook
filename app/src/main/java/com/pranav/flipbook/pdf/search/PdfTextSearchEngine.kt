package com.pranav.flipbook.pdf.search

import android.content.Context
import android.net.Uri
import com.tom_roush.pdfbox.android.PDFBoxResourceLoader
import com.tom_roush.pdfbox.pdmodel.PDDocument
import com.tom_roush.pdfbox.text.PDFTextStripper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withContext
import kotlin.coroutines.coroutineContext

data class SearchResult(
    val pageIndex: Int,
    val text: String,
    val contextBefore: String = "",
    val contextAfter: String = ""
)

class PdfTextSearchEngine(private val context: Context) {

    init {
        PDFBoxResourceLoader.init(context)
    }

    suspend fun search(uri: Uri, query: String, maxResults: Int = 200): List<SearchResult> {
        if (query.isBlank()) return emptyList()

        return withContext(Dispatchers.IO) {
            val results = mutableListOf<SearchResult>()
            try {
                context.contentResolver.openInputStream(uri)?.use { input ->
                    val document = PDDocument.load(input)
                    val stripper = PDFTextStripper()

                    for (pageNum in 1..document.numberOfPages) {
                        if (!coroutineContext.isActive) break
                        if (results.size >= maxResults) break

                        stripper.startPage = pageNum
                        stripper.endPage = pageNum
                        val pageText = stripper.getText(document)

                        val lowerText = pageText.lowercase()
                        val lowerQuery = query.lowercase()
                        var startIndex = 0

                        while (startIndex < lowerText.length) {
                            val matchIndex = lowerText.indexOf(lowerQuery, startIndex)
                            if (matchIndex == -1) break
                            if (results.size >= maxResults) break

                            val contextStart = (matchIndex - 40).coerceAtLeast(0)
                            val contextEnd = (matchIndex + lowerQuery.length + 40).coerceAtMost(pageText.length)
                            val before = pageText.substring(contextStart, matchIndex).trim()
                            val matched = pageText.substring(matchIndex, (matchIndex + query.length).coerceAtMost(pageText.length))
                            val after = pageText.substring(
                                (matchIndex + query.length).coerceAtMost(pageText.length),
                                contextEnd
                            ).trim()

                            results.add(
                                SearchResult(
                                    pageIndex = pageNum - 1,
                                    text = matched,
                                    contextBefore = before,
                                    contextAfter = after
                                )
                            )

                            startIndex = matchIndex + lowerQuery.length
                        }
                    }
                    document.close()
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
            results
        }
    }

    suspend fun extractPageText(uri: Uri, pageIndex: Int): String? {
        return withContext(Dispatchers.IO) {
            try {
                context.contentResolver.openInputStream(uri)?.use { input ->
                    val document = PDDocument.load(input)
                    val stripper = PDFTextStripper().apply {
                        startPage = pageIndex + 1
                        endPage = pageIndex + 1
                    }
                    val text = stripper.getText(document)
                    document.close()
                    text.takeIf { it.isNotBlank() }
                }
            } catch (e: Exception) {
                e.printStackTrace()
                null
            }
        }
    }
}
