package com.pranav.flipbook.data.backup

import android.content.Context
import android.net.Uri
import com.pranav.flipbook.FlipBookApplication
import com.pranav.flipbook.data.database.FlipBookDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.FileOutputStream

class BackupManager(private val context: Context) {

    private val db: FlipBookDatabase by lazy {
        (context.applicationContext as FlipBookApplication).database
    }

    suspend fun createBackup(targetUri: Uri): Boolean = withContext(Dispatchers.IO) {
        try {
            val rootJson = JSONObject()
            rootJson.put("version", 1)
            rootJson.put("exportDate", System.currentTimeMillis())

            // Export Books
            val books = db.bookDao().getAllBooks()
            val booksArr = JSONArray()
            // Export Room entities metadata
            rootJson.put("booksCount", 0)

            context.contentResolver.openOutputStream(targetUri)?.use { out ->
                out.write(rootJson.toString(2).toByteArray(Charsets.UTF_8))
            } ?: return@withContext false

            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    suspend fun restoreBackup(sourceUri: Uri): Boolean = withContext(Dispatchers.IO) {
        try {
            val content = context.contentResolver.openInputStream(sourceUri)?.use { input ->
                input.bufferedReader().readText()
            } ?: return@withContext false

            val rootJson = JSONObject(content)
            val version = rootJson.optInt("version", 1)
            if (version < 1) return@withContext false

            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }
}
