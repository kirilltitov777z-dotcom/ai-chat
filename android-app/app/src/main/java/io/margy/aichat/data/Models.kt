package io.margy.aichat.data

import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

enum class Role(val wire: String) {
    USER("user"),
    ASSISTANT("assistant")
}

data class Attachment(
    val name: String = "",
    val mime: String = "application/octet-stream",
    val path: String? = null,
    val isImage: Boolean = false,
    val dataUrl: String? = null,
    val text: String? = null,
    val size: Long = 0L
) {
    fun toJson(): JSONObject = JSONObject().apply {
        put("name", name)
        put("mime", mime)
        put("path", path ?: JSONObject.NULL)
        put("isImage", isImage)
        put("dataUrl", dataUrl ?: JSONObject.NULL)
        put("text", text ?: JSONObject.NULL)
        put("size", size)
    }

    companion object {
        fun fromJson(o: JSONObject): Attachment = Attachment(
            name = o.optString("name"),
            mime = o.optString("mime", "application/octet-stream"),
            path = if (o.isNull("path")) null else o.optString("path"),
            isImage = o.optBoolean("isImage"),
            dataUrl = if (o.isNull("dataUrl")) null else o.optString("dataUrl"),
            text = if (o.isNull("text")) null else o.optString("text"),
            size = o.optLong("size")
        )
    }
}

data class Message(
    val id: String = UUID.randomUUID().toString(),
    val role: Role = Role.USER,
    val text: String = "",
    val reasoning: String = "",
    val attachments: List<Attachment> = emptyList(),
    val time: Long = System.currentTimeMillis(),
    val error: Boolean = false,
    // Поля только для UI, в файл не сохраняются
    val streaming: Boolean = false,
    val status: String? = null
) {
    fun toJson(): JSONObject = JSONObject().apply {
        put("id", id)
        put("role", role.wire)
        put("text", text)
        put("reasoning", reasoning)
        put("time", time)
        put("error", error)
        put("attachments", JSONArray().apply { attachments.forEach { put(it.toJson()) } })
    }

    companion object {
        fun fromJson(o: JSONObject): Message = Message(
            id = o.optString("id", UUID.randomUUID().toString()),
            role = if (o.optString("role") == "assistant") Role.ASSISTANT else Role.USER,
            text = o.optString("text"),
            reasoning = o.optString("reasoning"),
            time = o.optLong("time", System.currentTimeMillis()),
            error = o.optBoolean("error"),
            attachments = run {
                val arr = o.optJSONArray("attachments") ?: return@run emptyList()
                (0 until arr.length()).mapNotNull { i ->
                    arr.optJSONObject(i)?.let { Attachment.fromJson(it) }
                }
            }
        )
    }
}

data class Chat(
    val id: String = UUID.randomUUID().toString(),
    val title: String = "Новый чат",
    val messages: List<Message> = emptyList(),
    val created: Long = System.currentTimeMillis(),
    val updated: Long = System.currentTimeMillis()
) {
    fun preview(): String {
        val last = messages.lastOrNull { it.text.isNotBlank() } ?: return "Пусто"
        return last.text.replace('\n', ' ').take(60)
    }
}

object ChatJson {
    fun encode(chats: List<Chat>): String {
        val arr = JSONArray()
        chats.forEach { c ->
            arr.put(JSONObject().apply {
                put("id", c.id)
                put("title", c.title)
                put("created", c.created)
                put("updated", c.updated)
                put("messages", JSONArray().apply { c.messages.forEach { put(it.toJson()) } })
            })
        }
        return JSONObject().put("version", 2).put("chats", arr).toString()
    }

    fun decode(raw: String): List<Chat> {
        if (raw.isBlank()) return emptyList()
        val root = JSONObject(raw)
        val arr = root.optJSONArray("chats") ?: return emptyList()
        val out = ArrayList<Chat>(arr.length())
        for (i in 0 until arr.length()) {
            val o = arr.optJSONObject(i) ?: continue
            val msgs = o.optJSONArray("messages") ?: JSONArray()
            val list = ArrayList<Message>(msgs.length())
            for (j in 0 until msgs.length()) {
                msgs.optJSONObject(j)?.let { list.add(Message.fromJson(it)) }
            }
            out.add(
                Chat(
                    id = o.optString("id", UUID.randomUUID().toString()),
                    title = o.optString("title", "Чат"),
                    messages = list,
                    created = o.optLong("created", System.currentTimeMillis()),
                    updated = o.optLong("updated", System.currentTimeMillis())
                )
            )
        }
        return out
    }
}
