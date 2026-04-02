package io.github.mkyrii.lab1

import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.lifecycle.ViewModel
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import io.github.mkyrii.lab1.databinding.ActivityMainBinding


class CalcViewModel : androidx.lifecycle.ViewModel() {
    var firstNumber: String = "0"
    var secondNumber: String = ""
    var operation: String = "null"
    var displayValue: String = "0"
}

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private val viewModel: CalcViewModel by viewModels()

    fun updateDisplay() {
        binding.mainText.text = viewModel.displayValue
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.btn0.setOnClickListener { clickOnDigit("0") }
        binding.btn1.setOnClickListener { clickOnDigit("1") }
        binding.btn2.setOnClickListener { clickOnDigit("2") }
        binding.btn3.setOnClickListener { clickOnDigit("3") }
        binding.btn4.setOnClickListener { clickOnDigit("4") }
        binding.btn5.setOnClickListener { clickOnDigit("5") }
        binding.btn6.setOnClickListener { clickOnDigit("6") }
        binding.btn7.setOnClickListener { clickOnDigit("7") }
        binding.btn8.setOnClickListener { clickOnDigit("8") }
        binding.btn9.setOnClickListener { clickOnDigit("9") }

        binding.btnPlus.setOnClickListener { clickOnOperation("plus") }
        binding.btnMinus.setOnClickListener { clickOnOperation("minus") }
        binding.btnMultiply.setOnClickListener { clickOnOperation("multiply") }
        binding.btnDivide.setOnClickListener { clickOnOperation("divide") }

        binding.btnDot.setOnClickListener { clickOnDot() }

        binding.solveBtn.setOnClickListener { solve() }
        binding.clearBtn.setOnClickListener { clear() }

        updateDisplay()

    }

    private fun clickOnDigit(digit: String){
        val currentDisplay = viewModel.displayValue

        if (currentDisplay.startsWith("error")){
            clear()
        }

        if (currentDisplay == "0"){
            viewModel.displayValue = digit
            if (viewModel.operation == "null") {
                viewModel.firstNumber = digit
            }
            else {
                viewModel.secondNumber = digit
            }
        }
        else {
            viewModel.displayValue += digit
            if (viewModel.operation == "null") {
                viewModel.firstNumber += digit
            }
            else {
                viewModel.secondNumber += digit
            }
        }


        updateDisplay()
    }

    private fun clickOnOperation(operation: String){

        if (viewModel.displayValue.startsWith("error")){
            clear()
        }

        if (viewModel.operation == "null") {
            viewModel.operation = operation

            if (operation == "plus") {
                viewModel.displayValue += " + "
            } else if (operation == "minus") {
                viewModel.displayValue += " - "
            } else if (operation == "multiply") {
                viewModel.displayValue += " × "
            } else {
                viewModel.displayValue += " ÷ "
            }

            updateDisplay()
        }
    }

    private fun clickOnDot(){

        if (viewModel.displayValue.startsWith("error")){
            clear()
        }


        if (viewModel.operation == "null") {
            if (!viewModel.firstNumber.contains('.')) {
                viewModel.firstNumber += "."
                viewModel.displayValue += "."
            }
        }
        else {
            if (!viewModel.secondNumber.contains('.')) {
                viewModel.secondNumber += "."
                viewModel.displayValue += "."
            }
        }

        updateDisplay()
    }

    private fun clear() {
        viewModel.displayValue = "0"
        viewModel.firstNumber = "0"
        viewModel.secondNumber = ""
        viewModel.operation = "null"

        updateDisplay()
    }

    private fun formatResult(value: Double): String {
        val rounded = "%.6f".format(java.util.Locale.US, value)
        return rounded.removeSuffix(".0")
    }

    private fun solve() {
        val operation = viewModel.operation

        if (operation == "null") {
            return
        }

        val num1: Double = viewModel.firstNumber.toDouble()
        val num2: Double = viewModel.secondNumber.toDouble()

        if (operation == "plus") {
            val result = num1 + num2
            viewModel.displayValue = formatResult(result)
            viewModel.firstNumber = formatResult(result)
        }
        else if (operation == "minus") {
            val result = num1 - num2
            viewModel.displayValue = formatResult(result)
            viewModel.firstNumber = formatResult(result)
        }
        else if (operation == "multiply") {
            val result = num1 * num2
            viewModel.displayValue = formatResult(result)
            viewModel.firstNumber = formatResult(result)
        }
        else {
            if (num2 == 0.0) {
                viewModel.displayValue = "error: can't divide by 0"
                viewModel.firstNumber = "0"
            }
            else {
                val result = num1 / num2
                viewModel.displayValue = formatResult(result)
                viewModel.firstNumber = formatResult(result)
            }
        }
        viewModel.secondNumber = ""
        viewModel.operation = "null"

        updateDisplay()

    }
}