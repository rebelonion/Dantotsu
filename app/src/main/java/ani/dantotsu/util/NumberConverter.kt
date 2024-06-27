package ani.dantotsu.util

import java.util.Locale

class NumberConverter {
    companion object {
        fun Number.toBinary(): String {
            return when (this) {
                is Int -> Integer.toBinaryString(this)
                is Long -> java.lang.Long.toBinaryString(this)
                is Short -> Integer.toBinaryString(this.toInt())
                is Byte -> Integer.toBinaryString(this.toInt())
                is Double -> doubleToBinary(this)
                is Float -> floatToBinary(this)
                else -> throw IllegalArgumentException("Unsupported number type")
            }
        }

        fun Number.toHex(): String {
            return when (this) {
                is Int -> Integer.toHexString(this)
                is Long -> java.lang.Long.toHexString(this)
                is Short -> Integer.toHexString(this.toInt())
                is Byte -> Integer.toHexString(this.toInt())
                is Double -> doubleToHex(this)
                is Float -> floatToHex(this)
                else -> throw IllegalArgumentException("Unsupported number type")
            }
        }

        private fun doubleToHex(number: Double): String {
            val longBits = java.lang.Double.doubleToLongBits(number)
            return "0x" + java.lang.Long.toHexString(longBits).uppercase(Locale.ROOT)
        }

        private fun floatToHex(number: Float): String {
            val intBits = java.lang.Float.floatToIntBits(number)
            return "0x" + Integer.toHexString(intBits).uppercase(Locale.ROOT)
        }

        private fun doubleToBinary(number: Double): String {
            val longBits = java.lang.Double.doubleToLongBits(number)
            return java.lang.Long.toBinaryString(longBits)
        }

        private fun floatToBinary(number: Float): String {
            val intBits = java.lang.Float.floatToIntBits(number)
            return Integer.toBinaryString(intBits)
        }

        fun Int.ofLength(length: Int): String {
            return this.toString().padStart(length, '0')
        }
    }
}