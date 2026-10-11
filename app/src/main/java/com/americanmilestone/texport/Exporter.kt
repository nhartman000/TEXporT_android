package com.americanmilestone.texport

/*
 * The handoff document. A line-for-line port of buildDoc()/renderMarkdown() in the browser extension's
 * src/export.js, so a collection exports identically on the phone and in the browser.
 */

data class Size(val chars: Int, val words: Int, val tokens: Int)

object Measure {
    fun of(text: String?): Size {
        val t = (text ?: "").trim()
        val words = if (t.isEmpty()) 0 else t.split(Regex("\\s+")).size
        return Size(t.length, words, (t.length + 3) / 4)
    }

    fun short(n: Int): String = when {
        n >= 10000 -> "${Math.round(n / 1000.0)}k"
        n >= 1000 -> String.format(java.util.Locale.US, "%.1fk", n / 1000.0)
        else -> n.toString()
    }
}

data class ExportOptions(
    val placement: String = "both",   // both | top | end
    val content: String = "full",     // full | marked
    val counts: Boolean = true,
    val extra: String = "",
)

object Exporter {
    data class Part(val t: String, val n: Int = 0, val note: String = "")
    data class Block(val n: Int, val note: String)
    data class Msg(val role: String, val index: Int, val block: List<Block>, val parts: List<Part>) {
        val marked get() = block.isNotEmpty() || parts.any { it.n > 0 }
    }
    data class DocConvo(val src: Convo, val messages: List<Msg>)
    data class Reg(val n: Int, val ci: Int, val mi: Int, val role: String, val convoTitle: String,
                   val note: String, val text: String, val scope: String) {
        val size get() = Measure.of(text)
    }
    data class Doc(val name: String, val convos: List<DocConvo>, val register: List<Reg>, val total: Int) {
        val size: Size get() = register.fold(Size(0, 0, 0)) { a, r -> val m = r.size; Size(a.chars + m.chars, a.words + m.words, a.tokens + m.tokens) }
    }

    /** [start, end) of excerpt in text; exact first, then whitespace-tolerant. */
    fun locate(text: String, excerpt: String): IntArray? {
        if (excerpt.isEmpty()) return null
        val i = text.indexOf(excerpt)
        if (i >= 0) return intArrayOf(i, i + excerpt.length)
        val ne = excerpt.replace(Regex("\\s+"), " ").trim()
        if (ne.length < 4) return null
        val map = ArrayList<Int>()
        val sb = StringBuilder()
        for (k in text.indices) {
            val ch = if (text[k].isWhitespace()) ' ' else text[k]
            if (ch == ' ' && sb.endsWith(" ")) continue
            map.add(k); sb.append(ch)
        }
        val j = sb.indexOf(ne)
        if (j < 0) return null
        return intArrayOf(map[j], map[j + ne.length - 1] + 1)
    }

    fun build(col: Collection): Doc {
        var n = 0
        val reg = ArrayList<Reg>()
        val convos = col.convos.mapIndexed { ci, c ->
            val byMsg = HashMap<Int, MutableList<Marker>>()
            val orphans = ArrayList<Marker>()
            for (m in c.markers) if (m.idx >= 0 && m.idx < c.messages.size) byMsg.getOrPut(m.idx) { ArrayList() }.add(m) else orphans.add(m)

            val msgs = c.messages.mapIndexed { mi, msg ->
                val block = ArrayList<Marker>()
                val inline = ArrayList<Triple<Marker, Int, Int>>()
                for (m in byMsg[mi] ?: emptyList<Marker>()) {
                    val pos = locate(msg.text, m.excerpt)
                    if (pos != null) inline.add(Triple(m, pos[0], pos[1])) else block.add(m)
                }
                inline.sortBy { it.second }
                val kept = ArrayList<Triple<Marker, Int, Int>>()
                for (it in inline) if (kept.isNotEmpty() && it.second < kept.last().third) block.add(it.first) else kept.add(it)

                val blocks = block.map { m ->
                    n++
                    reg.add(Reg(n, ci, mi, msg.role, c.title, m.note, m.excerpt.ifEmpty { msg.text },
                        if (m.excerpt.isNotEmpty()) "${m.scope.ifEmpty { "selection" }} (not located inline)" else "whole message"))
                    Block(n, m.note)
                }
                val parts = ArrayList<Part>()
                var cursor = 0
                for ((m, s, e) in kept) {
                    n++
                    reg.add(Reg(n, ci, mi, msg.role, c.title, m.note, msg.text.substring(s, e), m.scope.ifEmpty { "selection" }))
                    if (s > cursor) parts.add(Part(msg.text.substring(cursor, s)))
                    parts.add(Part(msg.text.substring(s, e), n, m.note))
                    cursor = e
                }
                if (cursor < msg.text.length || parts.isEmpty()) parts.add(Part(msg.text.substring(cursor)))
                Msg(msg.role, mi, blocks, parts)
            }
            for (m in orphans) {
                n++
                reg.add(Reg(n, ci, -1, "?", c.title, m.note, m.excerpt.ifEmpty { "(marked message could not be located when captured)" }, "position not found"))
            }
            DocConvo(c, msgs)
        }
        reg.sortBy { it.n }
        return Doc(col.name, convos, reg, n)
    }

    private fun tag(n: Any, which: String) = "[[MARK M$n $which]]"

    fun instructions(doc: Doc, o: ExportOptions): String {
        val where = when (o.placement) {
            "both" -> "at the start and again at the end of this document"
            "end" -> "at the end of this document"
            else -> "directly below"
        }
        val scope = if (o.content == "marked") "\n• Only messages containing markers are included; unmarked messages were omitted by the compiler." else ""
        val extra = if (o.extra.isNotBlank()) "\n\nAdditional instructions from the compiler:\n${o.extra.trim()}" else ""
        return """READER INSTRUCTIONS — for any AI model processing this document

This document compiles ${doc.convos.size} AI conversation(s). The person who compiled it marked ${doc.total} passage(s) as critical.

• Every marked passage is wrapped inline between ${tag("n", "START")} and ${tag("n", "END")} (n = its ID).
• Every marked passage is also reproduced in full in the MARKED PASSAGES REGISTER $where.$scope

Requirements:
1. Read the entire document, not just the register.
2. Treat every marked passage as high priority. Do not drop, compress, or paraphrase away its specifics (numbers, names, definitions, decisions, code, wording).
3. When summarizing or answering, keep marked content verbatim where precision matters and cite it by ID (e.g. M3).
4. If a marked passage conflicts with other content, flag the conflict instead of silently resolving it.
5. Notes attached to a marker are the compiler's explanation of why it matters — follow them.
6. Before finishing any summary, confirm every ID M1–M${maxOf(doc.total, 1)} is accounted for.$extra"""
    }

    private fun regHead(r: Reg, o: ExportOptions): String {
        val pos = if (r.mi >= 0) "message ${r.mi + 1}, ${r.role}" else "position unknown"
        val size = if (o.counts) " — ${r.size.words} words, ≈${r.size.tokens} tokens" else ""
        return "M${r.n} — Conversation ${r.ci + 1} (\"${r.convoTitle}\"), $pos — ${r.scope}$size"
    }

    fun markdown(doc: Doc, o: ExportOptions = ExportOptions(), date: String): String {
        val L = ArrayList<String>()
        val sz = doc.size
        L += listOf("# ${doc.name}", "",
            "Compiled $date · ${doc.convos.size} conversation(s) · ${doc.total} marker(s) · marked text: ${sz.words} words, ≈${sz.tokens} tokens", "")
        L += listOf(instructions(doc, o), "")
        fun register(label: String) {
            L += listOf("---", "", "## $label", "")
            if (doc.register.isEmpty()) L += listOf("(no markers)", "")
            for (r in doc.register) {
                L += "### ${regHead(r, o)}"
                if (r.note.isNotEmpty()) L += "Note: ${r.note}"
                L += listOf(tag(r.n, "START"), r.text, tag(r.n, "END"), "")
            }
        }
        if (o.placement != "end") register("MARKED PASSAGES REGISTER")
        doc.convos.forEachIndexed { ci, dc ->
            val c = dc.src
            L += listOf("---", "", "## CONVERSATION ${ci + 1}: ${c.title}",
                "Source: ${c.site} · ${c.url} · captured ${c.capturedAt.take(16).replace("T", " ")}", "")
            var skipped = 0
            fun flush() { if (skipped > 0) { L += listOf("[… $skipped unmarked message(s) omitted …]", ""); skipped = 0 } }
            for (m in dc.messages) {
                if (o.content == "marked" && !m.marked) { skipped++; continue }
                flush()
                L += "### ${m.role.uppercase()} (message ${m.index + 1})${if (m.marked) " ★ MARKED" else ""}"
                for (b in m.block) L += tag(b.n, "START") + (if (b.note.isNotEmpty()) " Note: ${b.note}" else "")
                L += m.parts.joinToString("") { p ->
                    if (p.n > 0) "${tag(p.n, "START")}${if (p.note.isNotEmpty()) " (Note: ${p.note}) " else ""}${p.t}${tag(p.n, "END")}" else p.t
                }
                for (b in m.block.reversed()) L += tag(b.n, "END")
                L += ""
            }
            flush()
        }
        if (o.placement != "top") register(if (o.placement == "both") "MARKED PASSAGES REGISTER (repeated — verify every ID before finishing)" else "MARKED PASSAGES REGISTER")
        return L.joinToString("\n")
    }
}
