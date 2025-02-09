package com.kylecorry.preparedness_feed.infrastructure.utils

import com.kylecorry.andromeda.xml.XMLNode

object XmlUtils {
    fun findAll(xml: XMLNode, tag: String): List<XMLNode> {
        val matches = mutableListOf<XMLNode>()
        if (xml.tag.lowercase() == tag.lowercase()) {
            matches.add(xml)
        }
        for (child in xml.children) {
            matches.addAll(findAll(child, tag))
        }
        return matches
    }

    fun find(xml: XMLNode, tag: String): XMLNode? {
        if (xml.tag.lowercase() == tag.lowercase()) {
            return xml
        }
        for (child in xml.children) {
            val match = find(child, tag)
            if (match != null) {
                return match
            }
        }
        return null
    }

    fun getTextBySelector(xml: XMLNode, selector: String): String? {
        if (selector.contains(" + ")) {
            val parts = selector.split(" + ").mapNotNull { getTextBySelector(xml, it) }
            return parts.joinToString("\n\n")
        }

        val tag = selector.substringBefore("[")
        var matches = findAll(xml, tag)

        var remainingSelector = if (selector == tag) {
            ""
        } else {
            selector
        }
        while (matches.isNotEmpty() && remainingSelector.isNotEmpty()) {
            val attribute = remainingSelector.substringAfter("[").substringBefore("]")
            if (attribute.contains("=")) {
                val parts = attribute.split("=")
                val key = parts[0]
                val value = parts[1]
                matches = matches.filter { it.attributes[key] == value }
                remainingSelector = remainingSelector.substringAfter("]")
            } else {
                // This is a stop condition
                return matches.firstOrNull()?.attributes?.get(attribute)
            }
        }

        return matches.firstOrNull()?.text

    }
}