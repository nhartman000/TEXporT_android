package com.americanmilestone.texport

import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import android.widget.EditText
import android.widget.ScrollView
import android.widget.Toast
import com.americanmilestone.texport.Ui.button
import com.americanmilestone.texport.Ui.card
import com.americanmilestone.texport.Ui.column
import com.americanmilestone.texport.Ui.dp
import com.americanmilestone.texport.Ui.row
import com.americanmilestone.texport.Ui.text

/** One collection: its markers, notes, and exports (Markdown handoff, .texport.json for the browser). */
class CollectionActivity : Activity() {
    private lateinit var store: Store
    private lateinit var id: String
    private var pendingSave: String = ""
    private val saveRequest = 51

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        store = Store(this)
        id = intent.getStringExtra("id") ?: run { finish(); return }
    }

    override fun onResume() {
        super.onResume()
        render()
    }

    private fun render() {
        val col = store.get(id) ?: run { finish(); return }
        title = col.name
        val doc = Exporter.build(col)
        val root = column().apply { setBackgroundColor(Ui.BG) }
        root.addView(text(col.name, 22f, bold = true))
        root.addView(text("${doc.total} marker(s) · ${Ui.sizeLabel(doc.size, store.unit)} · ${col.convos.size} source(s)", 13f, Ui.MUTE))

        val ex1 = row(button("Share to an AI app", primary = true) { shareMarkdown(col) }, button("Save .md") { saveFile(col, "md") })
        root.addView(ex1); Ui.gap(ex1, dp(12))
        val ex2 = if (store.usesBrowserExtension)
            row(button("Send to browser (.texport.json)") { saveFile(col, "json") })
        else null
        ex2?.let { root.addView(it); Ui.gap(it, dp(8)) }

        if (doc.total == 0) root.addView(text("\nNo markers yet. Select text in any app → Mark with TExporT.", 14f, Ui.MUTE))
        for (r in doc.register) {
            val c = col.convos[r.ci]
            val item = card().apply {
                addView(text("M${r.n} · ${c.title.removePrefix("From ")} · ${Ui.sizeLabel(r.size, store.unit)}", 12f, Ui.AMBER, bold = true))
                addView(text(if (r.text.length > 600) r.text.take(600) + "…" else r.text, 14f))
                if (r.note.isNotEmpty()) addView(text("Note: ${r.note}", 13f, 0xFFFCD34D.toInt()))
                isClickable = true
                setOnClickListener { markerMenu(col, r) }
            }
            root.addView(item); Ui.gap(item, dp(8))
        }

        val manage = row(button("Rename") { rename(col) }, button("Delete collection") { deleteCollection(col) })
        root.addView(manage); Ui.gap(manage, dp(18))
        setContentView(ScrollView(this).apply { setBackgroundColor(Ui.BG); addView(root) })
    }

    /** Find the stored marker behind a register entry (same order as Exporter.build numbers them). */
    private fun markerFor(col: Collection, r: Exporter.Reg): Marker? {
        val c = col.convos[r.ci]
        return if (r.mi >= 0) c.markers.firstOrNull { it.idx == r.mi && (it.excerpt.isEmpty() || r.text.contains(it.excerpt.trim().take(40))) }
               else c.markers.firstOrNull { it.idx < 0 || it.idx >= c.messages.size }
    }

    private fun markerMenu(col: Collection, r: Exporter.Reg) {
        AlertDialog.Builder(this).setTitle("M${r.n}")
            .setItems(arrayOf("Edit note", "Delete marker", "Copy text")) { _, which ->
                when (which) {
                    0 -> editNote(col, r)
                    1 -> AlertDialog.Builder(this).setMessage("Delete M${r.n}?")
                        .setPositiveButton("Delete") { _, _ ->
                            store.update(col.id) { cc ->
                                val c = cc.convos[r.ci]
                                val m = markerFor(cc, r) ?: return@update
                                c.markers.remove(m)
                                // a phone snippet is only its marker: drop the message too, renumbering later markers
                                if (m.excerpt.isEmpty() && m.idx >= 0 && c.key.startsWith("android:")) {
                                    c.messages.removeAt(m.idx)
                                    val kept = c.markers.map { if (it.idx > m.idx) it.copy(idx = it.idx - 1) else it }
                                    c.markers.clear(); c.markers.addAll(kept)
                                }
                                if (c.messages.isEmpty()) cc.convos.remove(c)
                            }
                            render()
                        }.setNegativeButton("Cancel", null).show()
                    2 -> {
                        val cm = getSystemService(CLIPBOARD_SERVICE) as android.content.ClipboardManager
                        cm.setPrimaryClip(android.content.ClipData.newPlainText("TExporT marker", r.text))
                        Toast.makeText(this, "Copied", Toast.LENGTH_SHORT).show()
                    }
                }
            }.show()
    }

    private fun editNote(col: Collection, r: Exporter.Reg) {
        val input = EditText(this).apply { setText(r.note) }
        AlertDialog.Builder(this).setTitle("Note for M${r.n}").setView(input)
            .setPositiveButton("Save") { _, _ ->
                store.update(col.id) { cc -> markerFor(cc, r)?.note = input.text.toString().trim() }
                render()
            }.setNegativeButton("Cancel", null).show()
    }

    private fun markdown(col: Collection) = Exporter.markdown(Exporter.build(col), ExportOptions(), Store.today())

    private fun shareMarkdown(col: Collection) {
        val send = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_SUBJECT, col.name)
            putExtra(Intent.EXTRA_TEXT, markdown(col))
        }
        startActivity(Intent.createChooser(send, "Send “${col.name}” to…"))
    }

    private fun saveFile(col: Collection, kind: String) {
        val safe = col.name.replace(Regex("[^\\w\\- ]+"), "").trim().replace(Regex("\\s+"), "_").ifEmpty { "collection" }
        pendingSave = if (kind == "md") markdown(col) else Json.serializeFile(col, "TExporT for Android", Store.now())
        startActivityForResult(Intent(Intent.ACTION_CREATE_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = if (kind == "md") "text/markdown" else "application/json"
            putExtra(Intent.EXTRA_TITLE, if (kind == "md") "$safe.md" else "$safe.texport.json")
        }, saveRequest)
    }

    @Deprecated("Platform API; no AndroidX in this app")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        val uri = data?.data ?: return
        if (requestCode != saveRequest || resultCode != RESULT_OK) return
        try {
            contentResolver.openOutputStream(uri, "wt")!!.bufferedWriter().use { it.write(pendingSave) }
            Toast.makeText(this, "Saved", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            Toast.makeText(this, "Couldn't save: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    private fun rename(col: Collection) {
        val input = EditText(this).apply { setText(col.name) }
        AlertDialog.Builder(this).setTitle("Rename").setView(input)
            .setPositiveButton("Save") { _, _ -> store.update(col.id) { it.name = input.text.toString().trim().ifEmpty { it.name } }; render() }
            .setNegativeButton("Cancel", null).show()
    }

    private fun deleteCollection(col: Collection) {
        AlertDialog.Builder(this).setMessage("Delete “${col.name}” and its ${col.markerCount} marker(s)?")
            .setPositiveButton("Delete") { _, _ -> store.delete(col.id); finish() }
            .setNegativeButton("Cancel", null).show()
    }
}
