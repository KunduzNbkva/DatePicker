package com.example.datepicker


import android.annotation.SuppressLint
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.datepicker.databinding.ActivityMainBinding


class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding


    @SuppressLint("ResourceAsColor")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setClick()
    }

    @SuppressLint("ResourceAsColor")
    private fun setClick() {
        binding.button.setOnClickListener {
            CustomBottomSheet(R.style.AppBottomSheetDialogTheme).apply {
                show(supportFragmentManager, CustomBottomSheet.TAG)
            }
        }
    }
}