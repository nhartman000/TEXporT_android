package com.americanmilestone.texport

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import java.util.UUID

/** Collections live in one JSON file in the app's private storage. Nothing leaves the phone unless you export it. */
class Store(private val ctx: Context) {
    private val file = File(ctx.filesDir, "collections.json")
    private val prefs = ctx.getSharedPreferences("texport", Context.MODE_PRIVATE)

    // ---- settings
    var usesBrowserExtension: Boolean
        get() = prefs.getBoolean("usesBrowserExtension", false)
        set(v) = prefs.edit().putBoolean("usesBrowserExtension", v).apply()
    var setupDone: Boolean
        get() = prefs.getBoolean("setupDone", false)
        set(v) = prefs.edit().putBoolean("setupDone", v).apply()
    var lastCollectionId: String?
        get() = prefs.getString("lastCollection", null)
        set(v) = prefs.edit().putString("lastCollection", v).apply()
    var unit: String
        get() = prefs.getString("unit", "tokens") ?: "tokens"
        set(v) = prefs.edit().putString("unit", v).apply()

    // ---- collections
    fun load(): MutableList<Collection> {
        if (!file.exists()) return mutableListOf()
        return try {
            val root = JSONObject(file.readText())
            val arr = root.optJSONArray("collections") ?: JSONArray()
            (0 until arr.length()).map {
                val o = arr.getJSONObject(it)
                Json.collectionFromJson(o.optString("id", newId()), o, now())
            }.toMutableList()
        } catch (e: Exception) {
            file.copyTo(File(ctx.filesDir, "collections.broken-${System.currentTimeMillis()}.json"), overwrite = true)
            mutableListOf()
        }
    }

    fun save(cols: List<Collection>) {
        val arr = JSONArray()
        cols.forEach { arr.put(Json.collectionToJson(it).put("id", it.id)) }
        val tmp = File(ctx.filesDir, "collections.json.tmp")
        tmp.writeText(JSONObject().put("version", 1).put("collections", arr).toString())
        tmp.renameTo(file)
    }

    fun get(id: String): Collection? = load().firstOrNull { it.id == id }

    fun update(id: String, change: (Collection) -> Unit) {
        val cols = load()
        cols.firstOrNull { it.id == id }?.let { change(it); save(cols) }
    }

    fun create(name: String): Collection {
        val cols = load()
        val c = Collection(newId(), name.trim().ifEmpty { "My collection" }, now())
        cols.add(c)
        save(cols)
        return c
    }

    fun delete(id: String) = save(load().filterNot { it.id == id })

    /**
     * Save a snippet. Snippets from the same app are grouped into one "conversation" per collection,
     * each snippet one message, marked whole (unless [marked] is false, e.g. shared context).
     */
    fun addSnippet(collectionId: String, text: String, note: String, sourceLabel: String, sourcePkg: String, marked: Boolean) {
        update(collectionId) { col ->
            val key = "android:$sourcePkg"
            val convo = col.convos.firstOrNull { it.key == key }
                ?: Convo(key, "From $sourceLabel", "Android · $sourceLabel", "", now()).also { col.convos.add(it) }
            convo.messages.add(Message(if (marked) "excerpt" else "context", text))
            if (marked) convo.markers.add(Marker(newId(), convo.messages.size - 1, "", note.trim(), "selection"))
            convo.capturedAt = now()
        }
        lastCollectionId = collectionId
    }

    fun importCollection(text: String): Collection {
        val cols = load()
        val c = Json.parseFile(text, newId(), now())
        if (cols.any { it.name == c.name }) c.name = c.name + " (imported)"
        cols.add(c)
        save(cols)
        return c
    }

    companion object {
        fun newId(): String = UUID.randomUUID().toString().replace("-", "").take(12)
        fun now(): String = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US).apply { timeZone = TimeZone.getTimeZone("UTC") }.format(Date())
        fun today(): String = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
    }
}
