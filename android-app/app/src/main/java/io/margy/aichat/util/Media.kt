package io.margy.aichat.util

import android.content.ContentResolver
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.media.ExifInterface
import android.net.Uri
import android.provider.OpenableColumns
import android.util.Base64
import io.margy.aichat.data.Attachment
import java.io.ByteArrayOutputStream
import java.io.File

object Media {

    private val textExtensions = setOf(
        "txt", "md", "json", "kt", "java", "xml", "html", "css", "js", "ts", "py", "rs",
        "c", "cpp", "h", "gradle", "yml", "yaml", "ini", "cfg", "csv", "log", "sh", "sql"
    )

    fun fromUri(ctx: Context, uri: Uri): Attachment? = runCatching {
        val cr = ctx.contentResolver
        val mime = cr.getType(uri) ?: guessMime(uri)
        val name = queryName(cr, uri) ?: "file_${System.currentTimeMillis()}"
        if (mime.startsWith("image/")) {
            val bmp = decodeBitmap(cr, uri, 1200) ?: return@runCatching null
            val rotated = rotateByExif(cr, uri, bmp)
            val out = ByteArrayOutputStream()
            rotated.compress(Bitmap.CompressFormat.JPEG, 85, out)
            val bytes = out.toByteArray()
            val dir = File(ctx.filesDir, "media").apply { mkdirs() }
            val f = File(dir, "img_${System.currentTimeMillis()}.jpg")
            f.writeBytes(bytes)
            Attachment(
                name = name,
                mime = "image/jpeg",
                path = f.absolutePath,
                isImage = true,
                dataUrl = "data:image/jpeg;base64," + Base64.encodeToString(bytes, Base64.NO_WRAP),
                size = bytes.size.toLong()
            )
        } else {
            val bytes = cr.openInputStream(uri)?.use { it.readBytes() } ?: return@runCatching null
            val ext = name.substringAfterLast('.', "").lowercase()
            val isText = mime.startsWith("text/") || ext in textExtensions
            val text = if (isText && bytes.size < 2_000_000) String(bytes, Charsets.UTF_8) else null
            val dir = File(ctx.filesDir, "media").apply { mkdirs() }
            val f = File(dir, "file_${System.currentTimeMillis()}.bin")
            runCatching { f.writeBytes(bytes) }
            Attachment(
                name = name,
                mime = mime,
                path = f.absolutePath,
                isImage = false,
                text = text,
                size = bytes.size.toLong()
            )
        }
    }.getOrNull()

    fun decodeThumbnail(path: String?, max: Int = 220): Bitmap? = runCatching {
        if (path.isNullOrBlank()) return null
        val f = File(path)
        if (!f.exists()) return null
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(path, bounds)
        var sample = 1
        while (bounds.outWidth / (sample * 2) >= max && bounds.outHeight / (sample * 2) >= max) sample *= 2
        BitmapFactory.decodeFile(path, BitmapFactory.Options().apply { inSampleSize = sample })
    }.getOrNull()

    private fun decodeBitmap(cr: ContentResolver, uri: Uri, max: Int): Bitmap? {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        cr.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, bounds) } ?: return null
        if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return null
        var sample = 1
        while (bounds.outWidth / (sample * 2) >= max && bounds.outHeight / (sample * 2) >= max) sample *= 2
        val bmp = cr.openInputStream(uri)?.use {
            BitmapFactory.decodeStream(it, null, BitmapFactory.Options().apply { inSampleSize = sample })
        } ?: return null
        val scale = minOf(max.toFloat() / bmp.width, max.toFloat() / bmp.height, 1f)
        return if (scale < 1f) {
            Bitmap.createScaledBitmap(
                bmp,
                (bmp.width * scale).toInt().coerceAtLeast(1),
                (bmp.height * scale).toInt().coerceAtLeast(1),
                true
            )
        } else bmp
    }

    private fun rotateByExif(cr: ContentResolver, uri: Uri, bmp: Bitmap): Bitmap = runCatching {
        val orientation = cr.openInputStream(uri)?.use {
            ExifInterface(it).getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)
        } ?: ExifInterface.ORIENTATION_NORMAL
        val degrees = when (orientation) {
            ExifInterface.ORIENTATION_ROTATE_90 -> 90f
            ExifInterface.ORIENTATION_ROTATE_180 -> 180f
            ExifInterface.ORIENTATION_ROTATE_270 -> 270f
            else -> 0f
        }
        if (degrees == 0f) bmp
        else Bitmap.createBitmap(bmp, 0, 0, bmp.width, bmp.height, Matrix().apply { postRotate(degrees) }, true)
    }.getOrDefault(bmp)

    private fun queryName(cr: ContentResolver, uri: Uri): String? = runCatching {
        cr.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { c ->
            if (c.moveToFirst()) c.getString(0) else null
        }
    }.getOrNull()

    private fun guessMime(uri: Uri): String {
        val name = uri.lastPathSegment.orEmpty()
        val ext = name.substringAfterLast('.', "").lowercase()
        return when (ext) {
            "jpg", "jpeg" -> "image/jpeg"
            "png" -> "image/png"
            "webp" -> "image/webp"
            "gif" -> "image/gif"
            "pdf" -> "application/pdf"
            "txt", "md", "log" -> "text/plain"
            "json" -> "application/json"
            else -> "application/octet-stream"
        }
    }
}
