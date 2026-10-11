package com.americanmilestone.texport

import org.json.JSONArray
import org.json.JSONObject

/*
 * Data model. Identical in shape to the TExporT Markers browser extension's storage, so collections
 * move between the two as .texport.json files (see docs/FORMAT.md).
 *
 * On the phone, every snippet you mark is one message in a "conversation" that groups snippets from
 * the same source app, plus one marker covering that whole message (idx = message index, excerpt = "").
 */

data class Message(val role: String, val text: String)

data class Marker(
    val id: String,
    val idx: Int,
    val excerpt: String = "",
    var note: String = "",
    val scope: String = "selection",
)

data class Convo(
    val key: String,
    var title: String,
    val site: String,
    val url: String = "",
    var capturedAt: String,
    val messages: MutableList<Message> = mutableListOf(),
    val markers: MutableList<Marker> = mutableListOf(),
)

data class Collection(
    val id: String,
    var name: String,
    val created: String,
    val convos: MutableList<Convo> = mutableListOf(),
) {
    val markerCount: Int get() = convos.sumOf { it.markers.size }
}

object Json {
    const val FORMAT = "texport.collection"

    fun convoToJson(c: Convo) = JSONObject()
        .put("key", c.key).put("url", c.url).put("title", c.title).put("site", c.site).put("capturedAt", c.capturedAt)
        .put("messages", JSONArray().also { a -> c.messages.forEach { a.put(JSONObject().put("role", it.role).put("text", it.text)) } })
        .put("markers", JSONArray().also { a ->
            c.markers.forEach {
                a.put(JSONObject().put("id", it.id).put("idx", it.idx).put("excerpt", it.excerpt).put("note", it.note).put("scope", it.scope))
            }
        })

    fun collectionToJson(col: Collection) = JSONObject()
        .put("name", col.name).put("created", col.created)
        .put("convos", JSONArray().also { a -> col.convos.forEach { a.put(convoToJson(it)) } })

    private fun JSONObject.str(k: String, d: String = "") = if (has(k) && get(k) is String) getString(k) else d

    fun convoFromJson(o: JSONObject, i: Int, now: String): Convo {
        val msgs = o.optJSONArray("messages") ?: JSONArray()
        val mks = o.optJSONArray("markers") ?: JSONArray()
        return Convo(
            key = o.str("key", "import:$i"),
            title = o.str("title", "Conversation ${i + 1}"),
            site = o.str("site", "Imported"),
            url = o.str("url"),
            capturedAt = o.str("capturedAt", now),
            messages = (0 until msgs.length()).map { k ->
                val m = msgs.optJSONObject(k) ?: JSONObject()
                Message(m.str("role", "text"), m.str("text"))
            }.toMutableList(),
            markers = (0 until mks.length()).map { k ->
                val m = mks.optJSONObject(k) ?: JSONObject()
                val idx = m.opt("idx")
                Marker(m.str("id", "m$k"), if (idx is Int) idx else -1, m.str("excerpt"), m.str("note"), m.str("scope", "selection"))
            }.toMutableList(),
        )
    }

    fun collectionFromJson(id: String, o: JSONObject, now: String): Collection {
        val cs = o.optJSONArray("convos") ?: throw IllegalArgumentException("Collection has no conversations.")
        return Collection(
            id = id,
            name = o.str("name", "Imported collection").take(200).ifBlank { "Imported collection" },
            created = o.str("created", now),
            convos = (0 until cs.length()).map { convoFromJson(cs.optJSONObject(it) ?: JSONObject(), it, now) }.toMutableList(),
        )
    }

    /** A .texport.json file: the same envelope the browser extension writes. */
    fun serializeFile(col: Collection, app: String, now: String): String = JSONObject()
        .put("format", FORMAT).put("version", 1).put("app", app).put("exportedAt", now)
        .put("collection", collectionToJson(col)).toString(2)

    fun parseFile(text: String, newId: String, now: String): Collection {
        val o = try { JSONObject(text) } catch (e: Exception) { throw IllegalArgumentException("Not a TExporT collection file (not valid JSON).") }
        if (o.optString("format") != FORMAT || !o.has("collection")) throw IllegalArgumentException("Not a TExporT collection file.")
        if (o.optInt("version", 1) > 1) throw IllegalArgumentException("This file is a newer format version; update TExporT to open it.")
        return collectionFromJson(newId, o.getJSONObject("collection"), now)
    }
}
