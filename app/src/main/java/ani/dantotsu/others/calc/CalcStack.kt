package ani.dantotsu.others.calc

import java.util.Stack

class CalcStack {
    private var expression: String = ""
    private val maxExpressionLength = 256

    fun evaluate(): Double {
        val ops = Stack<Char>()
        val values = Stack<Double>()

        var i = 0
        while (i < expression.length) {
            when {
                expression[i] == ' ' -> i++
                expression[i].isDigit() || expression[i] == '.' -> {
                    var value = 0.0
                    var isDecimal = false
                    var decimalFactor = 0.1
                    while (i < expression.length && (expression[i].isDigit() || expression[i] == '.' && !isDecimal)) {
                        if (expression[i] == '.') {
                            isDecimal = true
                        } else if (!isDecimal) {
                            value = value * 10 + (expression[i] - '0')
                        } else {
                            value += (expression[i] - '0') * decimalFactor
                            decimalFactor *= 0.1
                        }
                        i++
                    }
                    values.push(value)
                    i-- // to compensate the additional i++ in the loop
                }

                else -> {
                    while (!ops.isEmpty() && precedence(ops.peek()) >= precedence(expression[i])) {
                        val val2 = values.pop()
                        val val1 = values.pop()
                        val op = ops.pop()
                        values.push(applyOp(val1, val2, op))
                    }
                    ops.push(expression[i])
                }
            }
            i++
        }

        while (!ops.isEmpty()) {
            val val2 = values.pop()
            val val1 = values.pop()
            val op = ops.pop()
            values.push(applyOp(val1, val2, op))
        }


        val ans = values.pop()
        expression = ans.toString()
        return ans
    }

    fun add(c: Char) {
        if (expression.length >= maxExpressionLength) return
        expression += c
    }

    fun clear() {
        expression = ""
    }

    fun remove() {
        if (expression.isNotEmpty()) {
            expression = expression.substring(0, expression.length - 1)
        }
    }

    fun getExpression(): String {
        return expression
    }


    private fun precedence(op: Char): Int {
        return when (op) {
            '+', '-' -> 1
            '*', '/' -> 2
            else -> -1
        }
    }

    private fun applyOp(a: Double, b: Double, op: Char): Double {
        return when (op) {
            '+' -> a + b
            '-' -> a - b
            '*' -> a * b
            '/' -> {
                if (b == 0.0) throw UnsupportedOperationException("Cannot divide by zero.")
                a / b
            }

            else -> 0.0
        }
    }
}