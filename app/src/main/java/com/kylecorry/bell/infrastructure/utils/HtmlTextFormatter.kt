package com.kylecorry.bell.infrastructure.utils

import org.jsoup.Jsoup
import org.jsoup.nodes.Element

object HtmlTextFormatter {

    fun getText(document: String, selector: String? = null): String {
        val html = Jsoup.parse(document)
        val updatedHtml = if (selector != null) {
            html.select(selector).html()
        } else {
            html.html()
        }
        return getText(Jsoup.parse(updatedHtml))
    }

    fun getText(element: Element): String {
        return element.wholeText()
            .replace(" ", "").split("\n").filter { it.trim().isNotBlank() }
            .joinToString("\n\n") { it.trim() }
    }

}