package com.example.datepicker

import android.app.Dialog
import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import com.example.datepicker.databinding.BottomSheetBinding
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import java.util.*

class CustomBottomSheet( themeI: Int): BottomSheetDialogFragment() {
    private var binding: BottomSheetBinding? = null
    private var bottomSheetTheme = themeI



    override fun getTheme(): Int = bottomSheetTheme


    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog = BottomSheetDialog(requireContext(), theme)

    companion object {
        const val TAG = "CustomBottomSheetD"
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        binding = BottomSheetBinding.inflate(inflater, container, false)
        return binding!!.root
    }




    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        stylingPicker()
        setOnCLick()

    }

    private fun setOnCLick(){
        binding!!.selectBtn.setOnClickListener {
            var day = binding!!.picker.dayOfMonth
            var month = binding!!.picker.month
            var year = binding!!.picker.year
            Toast.makeText(requireContext(),"$day/$month/$year",Toast.LENGTH_SHORT).show()

        }
    }

    private fun stylingPicker () {
        val today = Calendar.getInstance()
        today[2000, 1] = 1
        binding!!.picker.setMinDate(today.timeInMillis)
        today[2100, 1] = 1
        binding!!.picker.setMaxDate(today.timeInMillis)
        binding!!.picker.setDayViewShown(true)
        binding!!.picker.setFormatter("%02d", "%02d", "%02d")
        binding!!.picker.setDividerColorResource(R.color.yellow)
        binding!!.picker.setSelectedTextColor(Color.BLACK)
        binding!!.picker.setDividerThickness(7)
    }
}