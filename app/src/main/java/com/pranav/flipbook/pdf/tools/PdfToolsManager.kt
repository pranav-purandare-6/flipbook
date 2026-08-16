package com.pranav.flipbook.pdf.tools

import android.content.Context
import android.net.Uri
import com.tom_roush.pdfbox.android.PDFBoxResourceLoader
import com.tom_roush.pdfbox.pdmodel.PDDocument
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

class PdfToolsManager(private val context: Context) {

    init {
        PDFBoxResourceLoader.init(context)
    }

    suspend fun duplicatePdf(sourceUri: Uri, bookTitle: String): String? = withContext(Dispatchers.IO) {
        try {
            val booksDir = File(context.filesDir, "books").apply { mkdirs() }
            val destFile = File(booksDir, "book_${System.currentTimeMillis()}_copy.pdf")

            context.contentResolver.openInputStream(sourceUri)?.use { input ->
                FileOutputStream(destFile).use { output ->
                    input.copyTo(output)
                }
            } ?: return@withContext null

            Uri.fromFile(destFile).toString()
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    suspend fun extractPages(sourceUri: Uri, startPage: Int, endPage: Int, outputName: String): String? = withContext(Dispatchers.IO) {
        try {
            context.contentResolver.openInputStream(sourceUri)?.use { input ->
                val document = PDDocument.load(input)
                val newDoc = PDDocument()

                val total = document.numberOfPages
                val from = (startPage - 1).coerceIn(0, total - 1)
                val to = (endPage - 1).coerceIn(from, total - 1)

                for (i in from..to) {
                    newDoc.addPage(document.getPage(i))
                }

                val booksDir = File(context.filesDir, "books").apply { mkdirs() }
                val destFile = File(booksDir, "extracted_${System.currentTimeMillis()}.pdf")
                newDoc.save(destFile)
                newDoc.close()
                document.close()

                Uri.fromFile(destFile).toString()
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}
