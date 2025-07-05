package com.kylecorry.bell.infrastructure.parsers.selectors

import com.jayway.jsonpath.JsonPath
import com.kylecorry.andromeda.xml.XMLNode
import org.jsoup.nodes.Element

// Format:
// "tag" -> Selects all nodes with the tag
// "tag.class" -> Selects all nodes with the tag and class
// ".class" -> Selects all nodes with the class
// "#id" -> Selects all nodes with the id
// "tag #id" -> Selects all nodes with a parent of tag and the id
// "tag[attr=value]" -> Selects all nodes with the tag and the attribute value

private val attributeRegex = Regex("\\[(.+)=(.+)\\]")

data class Selector(
    val selector: String,
    val attribute: String? = null,
    val index: Int? = null,
    val valueOverride: String? = null,
    val delimiter: String = ", ",
    val raw: Boolean = false,
    val mapFn: (String?) -> String? = { it }
) {
    companion object {
        fun text(
            selector: String,
            index: Int = 0,
            raw: Boolean = false,
            mapFn: (String?) -> String? = { it }
        ): Selector {
            return Selector(selector, index = index, mapFn = mapFn, raw = raw)
        }

        fun attr(
            selector: String,
            attribute: String,
            index: Int = 0,
            mapFn: (String?) -> String? = { it }
        ): Selector {
            return Selector(selector, attribute = attribute, index = index, mapFn = mapFn)
        }

        fun value(
            value: String,
            mapFn: (String?) -> String? = { it }
        ): Selector {
            return Selector("", valueOverride = value, mapFn = mapFn)
        }

        fun allText(
            selector: String,
            delimiter: String = ", ",
            mapFn: (String?) -> String? = { it }
        ): Selector {
            return Selector(selector, delimiter = delimiter, mapFn = mapFn)
        }

        fun allAttr(
            selector: String,
            attribute: String,
            delimiter: String = ", ",
            mapFn: (String?) -> String? = { it }
        ): Selector {
            return Selector(selector, attribute = attribute, delimiter = delimiter, mapFn = mapFn)
        }
    }
}

// ======================== Combined ========================
fun <T> select(element: T, selector: String): List<T> {
    @Suppress("UNCHECKED_CAST")
    return when (element) {
        is Element -> selectHtml(element, selector)
        is XMLNode -> selectXml(element, selector)
        else -> selectJson(element as Any, selector)
    } as List<T>
}

fun <T> select(element: T, selector: Selector): String? {
    if (selector.valueOverride != null) {
        return selector.mapFn(selector.valueOverride)
    }
    return when (element) {
        is Element -> selectHtml(element, selector)
        is XMLNode -> selectXml(element, selector)
        else -> selectJson(element as Any, selector)
    }
}

// ======================== HTML ========================

private fun selectHtml(element: Element, selector: String): List<Element> {
    return element.select(selector)
}

private fun selectHtml(element: Element, selector: Selector): String? {
    val elements = selectHtml(element, selector.selector)

    val selectedElement = if (elements.isEmpty()) {
        return null
    } else if (selector.index == null) {
        return selector.mapFn(elements.mapNotNull {
            if (selector.attribute != null) {
                it.attr(selector.attribute)
            } else if (selector.raw) {
                it.html()
            } else {
                it.wholeText()
            }
        }.joinToString(selector.delimiter))
    } else {
        elements.getOrNull(selector.index)
    } ?: return null

    return selector.mapFn(
        if (selector.attribute != null) {
            selectedElement.attr(selector.attribute)
        } else if (selector.raw) {
            selectedElement.html()
        } else {
            selectedElement.wholeText()
        }
    )
}

// ======================== XML ========================

private fun selectXml(node: XMLNode, selector: String): List<XMLNode> {
    // ID and class selectors are not supported for XML
    val selections = selector.split(" ")
    var current = listOf(node)
    selections.forEach {
        val attributes = attributeRegex.findAll(it)
        val tag = if (it.startsWith("[")) {
            null
        } else {
            it.substringBefore("[")
        }

        val attributeMap = attributes.associate {
            it.groupValues[1] to it.groupValues[2]
        }

        current = current.flatMap { node ->
            selectNodes(node, tag, attributeMap)
        }
    }

    return current
}

private fun selectXml(node: XMLNode, selector: Selector): String? {
    val elements = selectXml(node, selector.selector)
    val selectedElement = if (elements.isEmpty()) {
        return null
    } else if (selector.index == null) {
        return selector.mapFn(elements.mapNotNull {
            if (selector.attribute != null) {
                it.attributes[selector.attribute]
            } else {
                it.text
            }
        }.joinToString(selector.delimiter))
    } else {
        elements.getOrNull(selector.index)
    } ?: return null

    return selector.mapFn(
        if (selector.attribute != null) {
            selectedElement.attributes[selector.attribute]
        } else {
            selectedElement.text
        }
    )
}

private fun containsAll(map1: Map<String, String>, map2: Map<String, String>): Boolean {
    return map2.all { (key, value) -> map1[key] == value }
}

private fun selectNodes(
    node: XMLNode,
    tag: String?,
    attributes: Map<String, String>
): List<XMLNode> {
    val matches = mutableListOf<XMLNode>()
    if ((tag == null || node.tag == tag) && containsAll(node.attributes, attributes)) {
        matches.add(node)
    }

    matches.addAll(node.children.flatMap {
        selectNodes(it, tag, attributes)
    })

    return matches
}

// ======================== JSON ========================

private fun selectJson(node: Any, selector: String): List<Any> {
    val fullSelector = if (selector.startsWith("$")) {
        selector
    } else {
        "$.$selector"
    }.trimEnd('.').replace(" ", ".")
    return JsonPath.read(node as String, fullSelector)
}

private fun selectJson(node: Any, selector: Selector): String? {
    val fullSelector = (if (selector.selector.startsWith("$")) {
        selector.selector
    } else {
        "$.${selector.selector}"
    } + if (selector.attribute != null) {
        ".${selector.attribute}"
    } else {
        ""
    }).trimEnd('.').replace(" ", ".")

    val elements = try {
        if (node is String) {
            JsonPath.read(node.toString(), fullSelector)
        } else {
            JsonPath.read<Any>(node, fullSelector)
        }
    } catch (e: Exception) {
        e.printStackTrace()
        return null
    }
    val selectedElement = if (elements is List<*> && elements.isEmpty()) {
        null
    } else if (elements is List<*> && selector.index == null) {
        elements.joinToString(selector.delimiter)
    } else if (elements is List<*>) {
        elements.getOrNull(selector.index ?: 0)
    } else {
        elements
    } ?: return null

    return selector.mapFn(selectedElement.toString())
}