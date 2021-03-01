package com.example.datepicker

import android.content.Context
import android.util.AttributeSet
import android.widget.FrameLayout
import androidx.annotation.ColorInt

abstract class BasePicker : FrameLayout {
    /**
     * Constant for unspecified size.
     */
    protected val SIZE_UNSPECIFIED = -1

    constructor(context: Context?) : super(context!!) {}
    constructor(context: Context?, attrs: AttributeSet?) : super(context!!, attrs) {}
    constructor(context: Context?, attrs: AttributeSet?, defStyleAttr: Int) : super(context!!, attrs, defStyleAttr) {}

    protected fun setDividerColor(@ColorInt color: Int, vararg pickers: NumberPicker) {
        for (picker in pickers) {
            picker.setDividerColor(color)
        }
    }

    protected fun setDividerThickness(thickness: Int, vararg pickers: NumberPicker) {
        for (picker in pickers) {
            picker.setDividerThickness(thickness)
        }
    }

    protected fun setSelectedTextColor(@ColorInt color: Int, vararg pickers: NumberPicker) {
        for (picker in pickers) {
            picker.setSelectedTextColor(color)
        }
    }
}