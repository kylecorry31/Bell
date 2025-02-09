package com.kylecorry.bell.infrastructure.utils

import org.jsoup.Jsoup

object HtmlTextFormatter {

    fun getText(document: String, selector: String?): String {
        val html = Jsoup.parse(document)
        val updatedHtml = if (selector != null) {
            html.select(selector).html()
        } else {
            html.html()
        }
        return Jsoup.parse(updatedHtml).wholeText()
            .replace(" ", "").split("\n").filter { it.trim().isNotBlank() }
            .joinToString("\n\n") { it.trim() }
    }

}