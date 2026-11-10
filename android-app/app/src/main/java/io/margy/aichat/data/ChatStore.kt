package io.margy.aichat.data

import android.content.Context
import java.io.File

class ChatStore(context: Context) {
    private val filesDir = context.filesDir

    private fun fileFor(user: String): File {
        return if (user.isNotBlank()) File(filesDir, "chats_${user}.json") else File(filesDir, "chats.json")
    }

    @Synchronized
    fun load(accountUser: String): List<Chat> = runCatching {
        val f = fileFor(accountUser)
        if (!f.exists()) emptyList() else ChatJson.decode(f.readText())
    }.getOrDefault(emptyList())

    @Synchronized
    fun save(chats: List<Chat>, accountUser: String) {
        runCatching {
            val f = fileFor(accountUser)
            val tmp = File(filesDir, "chats_${accountUser}.tmp")
            tmp.writeText(ChatJson.encode(chats))
            if (f.exists()) f.delete()
            tmp.renameTo(f)
        }
    }
}
