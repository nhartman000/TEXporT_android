package com.americanmilestone.texport

import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.widget.CheckBox
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.Toast
import com.americanmilestone.texport.Ui.button
import com.americanmilestone.texport.Ui.card
import com.americanmilestone.texport.Ui.column
import com.americanmilestone.texport.Ui.dp
import com.americanmilestone.texport.Ui.row
import com.americanmilestone.texport.Ui.text

/** Collections list, first-run setup and settings. */
class MainActivity : Activity() {
    private lateinit var store: Store
    private val importRequest = 41

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        store = Store(this)
        if (!store.setupDone) showSetup()
    }

    override fun onResume() {
        super.onResume()
        render()
    }

    private fun render() {
        val root = column().apply { setBackgroundColor(Ui.BG) }
        root.addView(text("TExporT", 28f, Ui.INK, bold = true))
        root.addView(text("Mark what matters. The next AI can't quietly lose it.", 14f, Ui.AMBER))

        val how = card().apply {
            addView(text("How to mark", 15f, bold = true))
            addView(text("1. In any app (Claude, ChatGPT, a browser…), long-press and select text.\n" +
                    "2. In the menu that pops up, tap ⋮ (more) → Mark with TExporT.\n" +
                    "3. Add a note if you like, pick a collection, Save.\n\n" +
                    "Whole conversation? Use the app's Share button → TExporT.", 14f, Ui.MUTE))
        }
        root.addView(how); Ui.gap(how, dp(14))

        val cols = store.load()
        val head = text("Collections", 17f, bold = true); root.addView(head); Ui.gap(head, dp(18))
        if (cols.isEmpty()) root.addView(text("None yet. Your first marker creates one.", 14f, Ui.MUTE))
        for (c in cols.sortedByDescending { it.convos.maxOfOrNull { v -> v.capturedAt } ?: it.created }) {
            val size = Exporter.build(c).size
            val item = card().apply {
                addView(text(c.name, 16f, bold = true))
                addView(text("${c.markerCount} marker(s) · ${Ui.sizeLabel(size, store.unit)}", 13f, Ui.MUTE))
                isClickable = true
                setOnClickListener { startActivity(Intent(this@MainActivity, CollectionActivity::class.java).putExtra("id", c.id)) }
            }
            root.addView(item); Ui.gap(item, dp(8))
        }

        val actions = if (store.usesBrowserExtension)
            row(button("New collection") { newCollection() }, button("Import from browser") { pickImport() })
        else row(button("New collection") { newCollection() })
        root.addView(actions); Ui.gap(actions, dp(16))

        setContentView(ScrollView(this).apply { setBackgroundColor(Ui.BG); addView(root) })
    }

    private fun newCollection() {
        val input = EditText(this).apply { hint = "Collection name" }
        AlertDialog.Builder(this).setTitle("New collection").setView(input)
            .setPositiveButton("Create") { _, _ -> store.create(input.text.toString()); render() }
            .setNegativeButton("Cancel", null).show()
    }

    /** First-run setup: explains marking and asks about the browser extension. */
    private fun showSetup() {
        val box = column(20)
        box.addView(text("Welcome to TExporT", 18f, bold = true, color = 0xFF111111.toInt()))
        box.addView(text("Select text in any app, then choose “Mark with TExporT” from the pop-up menu (under ⋮ if it's not shown right away).", 14f, color = 0xFF333333.toInt()))
        val ext = CheckBox(this).apply {
            text = "I also use the TExporT browser extension (Firefox / Chrome)"
            isChecked = store.usesBrowserExtension
        }
        box.addView(ext); Ui.gap(ext, dp(12))
        box.addView(text("Turns on “Send to browser” and “Import from browser”, so collections move between phone and computer as .texport.json files.", 12f, color = 0xFF555555.toInt()))
        AlertDialog.Builder(this).setView(box).setCancelable(false)
            .setPositiveButton("Get started") { _, _ ->
                store.usesBrowserExtension = ext.isChecked
                store.setupDone = true
                render()
            }.show()
    }

    private fun showSettings() {
        val box = column(20)
        val ext = CheckBox(this).apply { text = "I also use the TExporT browser extension"; isChecked = store.usesBrowserExtension }
        box.addView(ext)
        box.addView(text("Measure markers in", 13f, color = 0xFF555555.toInt()).also { Ui.gap(it, dp(12)) })
        val units = arrayOf("tokens", "words", "characters")
        val unitPick = android.widget.Spinner(this).apply {
            adapter = android.widget.ArrayAdapter(this@MainActivity, android.R.layout.simple_spinner_dropdown_item, units)
            setSelection(units.indexOf(store.unit).coerceAtLeast(0))
        }
        box.addView(unitPick)
        AlertDialog.Builder(this).setTitle("Settings").setView(box)
            .setPositiveButton("Save") { _, _ ->
                store.usesBrowserExtension = ext.isChecked
                store.unit = units[unitPick.selectedItemPosition]
                render()
            }.setNegativeButton("Cancel", null).show()
    }

    private fun pickImport() {
        startActivityForResult(Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = "*/*"
        }, importRequest)
    }

    @Deprecated("Platform API; no AndroidX in this app")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        val uri = data?.data ?: return
        if (requestCode != importRequest || resultCode != RESULT_OK) return
        try {
            val txt = contentResolver.openInputStream(uri)!!.bufferedReader().use { it.readText() }
            val c = store.importCollection(txt)
            Toast.makeText(this, "Imported “${c.name}” (${c.markerCount} markers)", Toast.LENGTH_LONG).show()
            render()
        } catch (e: Exception) {
            AlertDialog.Builder(this).setTitle("Couldn't import").setMessage(e.message).setPositiveButton("OK", null).show()
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menu.add(0, 1, 0, "Settings")
        menu.add(0, 2, 1, "Setup guide")
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            1 -> showSettings()
            2 -> showSetup()
            else -> return super.onOptionsItemSelected(item)
        }
        return true
    }
}
