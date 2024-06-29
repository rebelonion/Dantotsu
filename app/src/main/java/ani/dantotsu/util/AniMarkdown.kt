package ani.dantotsu.util

import ani.dantotsu.getYoutubeId
import ani.dantotsu.util.ColorEditor.Companion.toCssColor

class AniMarkdown { //istg anilist has the worst api
    companion object {
        private fun String.convertNestedImageToHtml(): String {
            val regex = """\[!\[(.*?)]\((.*?)\)]\((.*?)\)""".toRegex()
            return regex.replace(this) { matchResult ->
                val altText = matchResult.groupValues[1]
                val imageUrl = matchResult.groupValues[2]
                val linkUrl = matchResult.groupValues[3]
                """<a href="$linkUrl"><img src="$imageUrl" alt="$altText"></a>"""
            }
        }

        private fun String.convertImageToHtml(): String {
            val regex = """!\[(.*?)]\((.*?)\)""".toRegex()
            val anilistRegex = """img\(.*?\)""".toRegex()
            val markdownImage = regex.replace(this) { matchResult ->
                val altText = matchResult.groupValues[1]
                val imageUrl = matchResult.groupValues[2]
                """<img src="$imageUrl" alt="$altText">"""
            }
            return anilistRegex.replace(markdownImage) { matchResult ->
                val imageUrl = matchResult.groupValues[1]
                """<img src="$imageUrl" alt="Image">"""
            }
        }

        private fun String.convertLinkToHtml(): String {
            val regex = """\[(.*?)]\((.*?)\)""".toRegex()
            return regex.replace(this) { matchResult ->
                val linkText = matchResult.groupValues[1]
                val linkUrl = matchResult.groupValues[2]
                """<a href="$linkUrl">$linkText</a>"""
            }
        }

        private fun String.convertYoutubeToHtml(): String {
            val regex = """<div class='youtube' id='(.*?)'></div>""".toRegex()
            return regex.replace(this) { matchResult ->
                val url = matchResult.groupValues[1]
                val id = getYoutubeId(url)
                if (id.isNotEmpty()) {
                    """<div>
                    <a href="https://www.youtube.com/watch?v=$id"><img src="https://i3.ytimg.com/vi/$id/maxresdefault.jpg" alt="$url"></a>
                    <align center>
                    <a href="https://www.youtube.com/watch?v=$id">Youtube Link</a>
                    </align>
                    </div>""".trimIndent()
                } else {
                    """<a href="$url">Youtube Video</a>"""
                }
            }
        }

        private fun String.replaceLeftovers(): String {
            return this.replace("&nbsp;", " ")
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

        private fun String.underlineToHtml(): String {
            return this.replace("(?s)___(.*?)___".toRegex(), "<br><em><strong>$1</strong></em><br>")
                .replace("(?s)__(.*?)__".toRegex(), "<br><strong>$1</strong><br>")
                .replace("(?s)\\s+_([^_]+)_\\s+".toRegex(), "<em>$1</em>")
        }

        private fun String.convertCenterToHtml(): String {
            val regex = """~~~(.*?)~~~""".toRegex()
            return regex.replace(this) { matchResult ->
                val centerText = matchResult.groupValues[1]
                """<align center>$centerText</align>"""
            }
        }

        fun getBasicAniHTML(html: String): String {
            return html
                .convertNestedImageToHtml()
                .convertImageToHtml()
                .convertLinkToHtml()
                .convertYoutubeToHtml()
                .convertCenterToHtml()
                .replaceLeftovers()
                .underlineToHtml()
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
<body>$basicHtml</body>
</html>
    """.trimIndent()
            return returnHtml
        }
    }
}