package io.margy.aichat.data

import android.content.Context
import java.io.File

class ChatStore(context: Context) {
    private val file = File(context.filesDir, "chats.json")

    @Synchronized
    fun load(): List<Chat> = runCatching {
        if (!file.exists()) emptyList() else ChatJson.decode(file.readText())
    }.getOrDefault(emptyList())

    @Synchronized
    fun save(chats: List<Chat>) {
        runCatching {
            val tmp = File(file.parentFile, "chats.tmp")
            tmp.writeText(ChatJson.encode(chats))
            if (file.exists()) file.delete()
            tmp.renameTo(file)
        }
    }
}
