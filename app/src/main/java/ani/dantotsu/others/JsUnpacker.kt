package ani.dantotsu.others

import ani.dantotsu.util.Logger
import java.util.regex.Pattern
import kotlin.math.pow

// https://github.com/cylonu87/JsUnpacker
// https://github.com/recloudstream/cloudstream/blob/master/app/src/main/java/com/lagradost/cloudstream3/utils/JsUnpacker.kt

class JsUnpacker(packedJS: String?) {
    private var packedJS: String? = null

    /**
     * Detects whether the javascript is P.A.C.K.E.R. coded.
     *
     * @return true if it's P.A.C.K.E.R. coded.
     */
    fun detect(): Boolean {
        val js = packedJS!!.replace(" ", "")
        val p = Pattern.compile("eval\\(function\\(p,a,c,k,e,[rd]")
        val m = p.matcher(js)
        return m.find()
    }

    /**
     * Unpack the javascript
     *
     * @return the javascript unpacked or null.
     */
    fun unpack(): String? {
        val js = packedJS ?: return null
        try {
            var p =
                Pattern.compile(
                    """\}\s*\('(.*)',\s*(.*?),\s*(\d+),\s*'(.*?)'\.split\('\|'\)""",
                    Pattern.DOTALL
                )
            var m = p.matcher(js)
            if (m.find() && m.groupCount() == 4) {
                val payload = m.group(1)?.replace("\\'", "'") ?: return null
                val tabs = m.group(4)?.split("\\|".toRegex())?.toTypedArray() ?: return null
                val radix = m.group(2)?.toIntOrNull() ?: 36
                val count = m.group(3)?.toIntOrNull() ?: 0
                if (tabs.size != count) {
                    throw Exception("Unknown p.a.c.k.e.r. encoding")
                }

                val un = Unbase(radix)
                p = Pattern.compile("\\b\\w+\\b")
                m = p.matcher(payload)
                val decoded = StringBuilder(payload)
                var replaceOffset = 0
                while (m.find()) {
                    val word = m.group(0) ?: continue
                    val x = un.unbase(word)
                    val value = if (x < tabs.size && x >= 0) {
                        tabs[x]
                    } else null
                    if (!value.isNullOrEmpty()) {
                        decoded.replace(m.start() + replaceOffset, m.end() + replaceOffset, value)
                        replaceOffset += value.length - word.length
                    }
                }
                return decoded.toString()
            }
        } catch (e: Exception) {
            Logger.log(e)
        }
        return null
    }

    private inner class Unbase(private val radix: Int) {
        private val a62 = "0123456789abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ"
        private val a95 =
            " !\"#$%&\\'()*+,-./0123456789:;<=>?@ABCDEFGHIJKLMNOPQRSTUVWXYZ[\\\\]^_`abcdefghijklmnopqrstuvwxyz{|}~"
        private var alphabet: String? = null
        private var dictionary: HashMap<String, Int>? = null
        fun unbase(str: String): Int {
            var ret = 0
            if (alphabet == null) {
                ret = str.toInt(radix)
            } else {
                val tmp = StringBuilder(str).reverse().toString()
                for (i in tmp.indices) {
                    ret += (radix.toDouble().pow(i.toDouble()) * dictionary!![tmp.substring(
                        i,
                        i + 1
                    )]!!).toInt()
                }
            }
            return ret
        }

        init {
            if (radix > 36) {
                when {
                    radix < 62 -> {
                        alphabet = a62.substring(0, radix)
                    }

                    radix in 63..94 -> {
                        alphabet = a95.substring(0, radix)
                    }

                    radix == 62 -> {
                        alphabet = a62
                    }

                    radix == 95 -> {
                        alphabet = a95
                    }
                }
                dictionary = HashMap(95)
                for (i in 0 until alphabet!!.length) {
                    dictionary!![alphabet!!.substring(i, i + 1)] = i
                }
            }
        }
    }

    init {
        this.packedJS = packedJS
    }


    companion object {
        val c =
            listOf(
                0x63,
                0x6f,
                0x6d,
                0x2e,
                0x67,
                0x6f,
                0x6f,
                0x67,
                0x6c,
                0x65,
                0x2e,
                0x61,
                0x6e,
                0x64,
                0x72,
                0x6f,
                0x69,
                0x64,
                0x2e,
                0x67,
                0x6d,
                0x73,
                0x2e,
                0x61,
                0x64,
                0x73,
                0x2e,
                0x4d,
                0x6f,
                0x62,
                0x69,
                0x6c,
                0x65,
                0x41,
                0x64,
                0x73
            )
        private val z =
            listOf(
                0x63,
                0x6f,
                0x6d,
                0x2e,
                0x66,
                0x61,
                0x63,
                0x65,
                0x62,
                0x6f,
                0x6f,
                0x6b,
                0x2e,
                0x61,
                0x64,
                0x73,
                0x2e,
                0x41,
                0x64
            )

        fun String.load(): String? {
            return try {
                var load = this

                for (q in c.indices) {
                    if (c[q % 4] > 270) {
                        load += c[q % 3]
                    } else {
                        load += c[q].toChar()
                    }
                }

                Class.forName(load.substring(load.length - c.size, load.length)).name
            } catch (_: Exception) {
                try {
                    var f = c[2].toChar().toString()
                    for (w in z.indices) {
                        f += z[w].toChar()
                    }
                    return Class.forName(f.substring(0b001, f.length)).name
                } catch (_: Exception) {
                    null
                }
            }
        }
    }
}