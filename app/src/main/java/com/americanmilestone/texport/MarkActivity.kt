package com.americanmilestone.texport

import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.text.InputType
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.CheckBox
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.Spinner
import android.widget.Toast
import com.americanmilestone.texport.Ui.button
import com.americanmilestone.texport.Ui.card
import com.americanmilestone.texport.Ui.column
import com.americanmilestone.texport.Ui.dp
import com.americanmilestone.texport.Ui.row
import com.americanmilestone.texport.Ui.text

/**
 * "Mark with TExporT": appears in the text-selection menu of every app (ACTION_PROCESS_TEXT),
 * and in the Share sheet (ACTION_SEND). Saves the selected text to a collection, with an optional note.
 */
class MarkActivity : Activity() {
    private lateinit var store: Store

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        store = Store(this)
        setResult(RESULT_CANCELED)    // never change the text in the app you came from

        val shared = intent.action == Intent.ACTION_SEND
        val raw = if (shared) intent.getStringExtra(Intent.EXTRA_TEXT)
                  else intent.getCharSequenceExtra(Intent.EXTRA_PROCESS_TEXT)?.toString()
        val selected = raw?.trim().orEmpty()
        if (selected.isEmpty()) {
            Toast.makeText(this, "Nothing selected to mark.", Toast.LENGTH_SHORT).show()
            finish(); return
        }
        val (srcLabel, srcPkg) = source()

        var cols = store.load()
        if (cols.isEmpty()) cols = mutableListOf(store.create("My collection"))
        val names = cols.map { it.name } + "＋ New collection…"
        val lastIdx = cols.indexOfFirst { it.id == store.lastCollectionId }.coerceAtLeast(0)

        val root = column(18).apply { setBackgroundColor(Ui.BG) }
        root.addView(text("Mark with TExporT", 19f, Ui.AMBER, bold = true))
        root.addView(text("From $srcLabel · ${Ui.sizeLabel(Measure.of(selected), store.unit)}", 13f, Ui.MUTE))

        val preview = card().apply { addView(text(selected, 15f)) }
        val previewScroll = ScrollView(this).apply { addView(preview) }
        root.addView(previewScroll, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f).apply { topMargin = dp(10) })

        val note = EditText(this).apply {
            hint = "Note: why it matters (optional)"
            setTextColor(Ui.INK); setHintTextColor(Ui.MUTE)
            inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_CAP_SENTENCES or InputType.TYPE_TEXT_FLAG_MULTI_LINE
            maxLines = 4
        }
        root.addView(note)

        root.addView(text("Collection", 13f, Ui.MUTE).also { Ui.gap(it, dp(8)) })
        val spinner = Spinner(this).apply {
            adapter = ArrayAdapter(this@MarkActivity, android.R.layout.simple_spinner_dropdown_item, names)
            setSelection(lastIdx)
        }
        root.addView(spinner)

        val markIt = CheckBox(this).apply {
            this.text = "Mark it (unticked = save as context only)"
            setTextColor(Ui.INK)
            isChecked = true
        }
        if (shared) root.addView(markIt)

        val save = button("★ Save marker", primary = true) {
            val pick = spinner.selectedItemPosition
            val finishSave = { colId: String ->
                store.addSnippet(colId, selected, note.text.toString(), srcLabel, srcPkg, markIt.isChecked)
                val colName = store.get(colId)?.name ?: ""
                Toast.makeText(this, if (markIt.isChecked) "Marked in “$colName”" else "Saved to “$colName”", Toast.LENGTH_SHORT).show()
                finish()
            }
            if (pick < cols.size) finishSave(cols[pick].id)
            else askName { finishSave(store.create(it).id) }
        }
        val cancel = button("Cancel") { finish() }
        root.addView(row(cancel, save).also { Ui.gap(it, dp(12)) })
        setContentView(root)
        window.setLayout((resources.displayMetrics.widthPixels * 0.94).toInt(), (resources.displayMetrics.heightPixels * 0.75).toInt())
    }

    private fun askName(done: (String) -> Unit) {
        val input = EditText(this).apply { hint = "Collection name" }
        AlertDialog.Builder(this)
            .setTitle("New collection")
            .setView(input)
            .setPositiveButton("Create") { _, _ -> done(input.text.toString()) }
            .setNegativeButton("Cancel", null)
            .show()
    }

    /** The app the text came from, e.g. "Claude", "Firefox Nightly". */
    private fun source(): Pair<String, String> {
        val pkg = callingPackage
            ?: (if (Build.VERSION.SDK_INT >= 22) referrer?.host else null)
            ?: return "another app" to "unknown"
        return try {
            val info = packageManager.getApplicationInfo(pkg, 0)
            packageManager.getApplicationLabel(info).toString() to pkg
        } catch (e: PackageManager.NameNotFoundException) {
            pkg to pkg
        }
    }
}
