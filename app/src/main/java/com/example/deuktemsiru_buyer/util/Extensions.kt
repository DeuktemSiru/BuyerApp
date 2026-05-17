package com.example.deuktemsiru_buyer.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color
import android.view.View
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.example.deuktemsiru_buyer.R
import com.google.android.material.color.MaterialColors
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.MultiFormatWriter
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

// S14 – Price formatting
fun Int.formatPrice(): String = "%,d원".format(this)

// S18 – 24h → 12h display conversion
fun String.toDisplayHour(): String {
    val hour = substringBefore(":").toIntOrNull() ?: return this
    val minute = substringAfter(":", "00")
    val displayHour = when {
        hour == 0 -> 12
        hour > 12 -> hour - 12
        else -> hour
    }
    return "$displayHour:${minute.padStart(2, '0')}"
}

// S16 – Shared countdown timer starter for Fragments
fun Fragment.startTimerInto(
    totalSeconds: Long,
    currentJob: Job?,
    onTick: (Long) -> Unit,
): Job {
    currentJob?.cancel()
    return viewLifecycleOwner.lifecycleScope.launch {
        repeatOnLifecycle(Lifecycle.State.STARTED) {
            countdownFlow(totalSeconds).collect { remaining ->
                onTick(remaining)
            }
        }
    }
}

// S17 – Shared QR bitmap generation
fun generateQrBitmap(content: String, size: Int = 512): Bitmap? = runCatching {
    val hints = mapOf(EncodeHintType.MARGIN to 1)
    val matrix = MultiFormatWriter().encode(content, BarcodeFormat.QR_CODE, size, size, hints)
    val pixels = IntArray(size * size) { i ->
        if (matrix[i % size, i / size]) Color.BLACK else Color.WHITE
    }
    Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        .also { it.setPixels(pixels, 0, size, 0, 0, size, size) }
}.getOrNull()

// S20 – Shared chip selection styling
fun Map<TextView, String>.updateChipSelection(
    selected: String,
    context: Context,
    selectedTextAttr: Int = com.google.android.material.R.attr.colorOnPrimary,
    unselectedTextColorRes: Int = R.color.color_text,
) {
    forEach { (chip, category) ->
        val isSelected = category == selected
        chip.setBackgroundResource(
            if (isSelected) R.drawable.bg_chip_selected else R.drawable.bg_chip_unselected
        )
        chip.setTextColor(
            if (isSelected)
                MaterialColors.getColor(chip, selectedTextAttr)
            else
                context.getColor(unselectedTextColorRes)
        )
    }
}

// S21 – Shared store filtering by category and query
fun <T> List<T>.filterByCategory(
    category: String,
    query: String,
    getCategoryApi: (T) -> String?,
    getName: (T) -> String,
    getCategory: (T) -> String,
    getAddress: (T) -> String,
    getMenuNames: (T) -> List<String>,
): List<T> {
    val apiCategory = if (category == "전체") null else {
        com.example.deuktemsiru_buyer.data.categoryToApi(category)
    }
    return filter { store ->
        val matchesCategory = apiCategory == null || getCategoryApi(store) == apiCategory
        val matchesQuery = query.isBlank() ||
            getName(store).contains(query, ignoreCase = true) ||
            getCategory(store).contains(query, ignoreCase = true) ||
            getAddress(store).contains(query, ignoreCase = true) ||
            getMenuNames(store).any { it.contains(query, ignoreCase = true) }
        matchesCategory && matchesQuery
    }
}
