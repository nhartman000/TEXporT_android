package com.americanmilestone.texport

import android.app.Activity
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView

/** Small helpers so screens can be built in code (no AndroidX, no XML layouts). */
object Ui {
    const val BG = 0xFF0B1020.toInt()
    const val PANEL = 0xFF121A2E.toInt()
    const val LINE = 0xFF24304D.toInt()
    const val INK = 0xFFF3F4F6.toInt()
    const val MUTE = 0xFF9AA4B8.toInt()
    const val AMBER = 0xFFF59E0B.toInt()

    fun Activity.dp(v: Int): Int = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, v.toFloat(), resources.displayMetrics).toInt()

    fun Activity.column(pad: Int = 16): LinearLayout = LinearLayout(this).apply {
        orientation = LinearLayout.VERTICAL
        setPadding(dp(pad), dp(pad), dp(pad), dp(pad))
    }

    fun Activity.text(s: CharSequence, size: Float = 15f, color: Int = INK, bold: Boolean = false): TextView =
        TextView(this).apply {
            text = s
            setTextSize(TypedValue.COMPLEX_UNIT_SP, size)
            setTextColor(color)
            if (bold) typeface = Typeface.DEFAULT_BOLD
            setLineSpacing(0f, 1.15f)
        }

    fun Activity.card(): LinearLayout = LinearLayout(this).apply {
        orientation = LinearLayout.VERTICAL
        setPadding(dp(14), dp(12), dp(14), dp(12))
        background = GradientDrawable().apply { setColor(PANEL); cornerRadius = dp(12).toFloat(); setStroke(dp(1), LINE) }
    }

    fun Activity.button(label: String, primary: Boolean = false, onClick: () -> Unit): Button = Button(this).apply {
        text = label
        isAllCaps = false
        setTextColor(if (primary) Color.BLACK else INK)
        background = GradientDrawable().apply {
            setColor(if (primary) AMBER else PANEL); cornerRadius = dp(10).toFloat(); if (!primary) setStroke(dp(1), LINE)
        }
        setPadding(dp(14), dp(8), dp(14), dp(8))
        setOnClickListener { onClick() }
    }

    fun gap(v: View, top: Int) {
        val lp = (v.layoutParams as? ViewGroup.MarginLayoutParams)
            ?: LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        lp.topMargin = top
        v.layoutParams = lp
    }

    fun Activity.row(vararg views: View): LinearLayout = LinearLayout(this).apply {
        orientation = LinearLayout.HORIZONTAL
        gravity = Gravity.CENTER_VERTICAL
        views.forEachIndexed { i, v ->
            addView(v, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f).apply { if (i > 0) marginStart = dp(8) })
        }
    }

    fun sizeLabel(s: Size, unit: String): String = when (unit) {
        "words" -> "${Measure.short(s.words)} words"
        "characters" -> "${Measure.short(s.chars)} chars"
        else -> "${s.words} words · ≈${Measure.short(s.tokens)} tokens"
    }
}
