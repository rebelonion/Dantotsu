package ani.dantotsu.util

import ani.dantotsu.util.ColorEditor.Companion.toCssColor

class AniMarkdown { //istg anilist has the worst api
    companion object {
        private fun convertNestedImageToHtml(markdown: String): String {
            val regex = """\[!\[(.*?)]\((.*?)\)]\((.*?)\)""".toRegex()
            return regex.replace(markdown) { matchResult ->
                val altText = matchResult.groupValues[1]
                val imageUrl = matchResult.groupValues[2]
                val linkUrl = matchResult.groupValues[3]
                """<a href="$linkUrl"><img src="$imageUrl" alt="$altText"></a>"""
            }
        }

        private fun convertImageToHtml(markdown: String): String {
            val regex = """!\[(.*?)]\((.*?)\)""".toRegex()
            return regex.replace(markdown) { matchResult ->
                val altText = matchResult.groupValues[1]
                val imageUrl = matchResult.groupValues[2]
                """<img src="$imageUrl" alt="$altText">"""
            }
        }

        private fun convertLinkToHtml(markdown: String): String {
            val regex = """\[(.*?)]\((.*?)\)""".toRegex()
            return regex.replace(markdown) { matchResult ->
                val linkText = matchResult.groupValues[1]
                val linkUrl = matchResult.groupValues[2]
                """<a href="$linkUrl">$linkText</a>"""
            }
        }

        private fun replaceLeftovers(html: String): String {
            return html.replace("&nbsp;", " ")
                .replace("&amp;", "&")
                .replace("&lt;", "<")
                .replace("&gt;", ">")
                .replace("&quot;", "\"")
                .replace("&apos;", "'")
                .replace("<pre>", "")
                .replace("`", "")
                .replace("~", "")
                .replace(">\n<", "><")
                .replace("\n", "<br>")
        }

        private fun underlineToHtml(html: String): String {
            return html.replace("(?s)___(.*?)___".toRegex(), "<br><em><strong>$1</strong></em><br>")
                .replace("(?s)__(.*?)__".toRegex(), "<br><strong>$1</strong><br>")
                .replace("(?s)\\s+_([^_]+)_\\s+".toRegex(), "<em>$1</em>")
        }

        fun getBasicAniHTML(html: String): String {
            val step0 = convertNestedImageToHtml(html)
            val step1 = convertImageToHtml(step0)
            val step2 = convertLinkToHtml(step1)
            val step3 = replaceLeftovers(step2)
            return underlineToHtml(step3)
        }

        fun getFullAniHTML(html: String, textColor: Int): String {
            val basicHtml = getBasicAniHTML(html)


            val returnHtml = """
            <html>
<head>
    <meta name="viewport" content="width=device-width, initial-scale=1.0, charset=UTF-8">
        <style>
            body {
                color: ${textColor.toCssColor()};
                margin: 0;
                padding: 0;
                max-width: 100%;
                overflow-x: hidden; /* Prevent horizontal scrolling */
            }
            img {
                max-width: 100%;
                height: auto; /* Maintain aspect ratio */
            }
            video {
                max-width: 100%;
                height: auto; /* Maintain aspect ratio */
            }
            a {
                color: ${textColor.toCssColor()};
            }
            /* Add responsive design elements for other content as needed */
        </style>
</head>
<body>
    $basicHtml
</body>

    """.trimIndent()
            return returnHtml
        }
    }
}