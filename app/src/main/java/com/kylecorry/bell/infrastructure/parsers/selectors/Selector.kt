package com.kylecorry.bell.infrastructure.parsers.selectors

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
    val mapFn: (String?) -> String? = { it }
) {
    companion object {
        fun text(
            selector: String,
            index: Int? = null,
            mapFn: (String?) -> String? = { it }
        ): Selector {
            return Selector(selector, index = index, mapFn = mapFn)
        }

        fun attr(
            selector: String,
            attribute: String,
            index: Int? = null,
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
    }
}

// ======================== Combined ========================
fun <T> select(element: T, selector: String): List<T> {
    return when (element) {
        is Element -> select(element, selector) as List<T>
        is XMLNode -> select(element, selector) as List<T>
        else -> throw IllegalArgumentException("Unsupported type")
    }
}

fun <T> select(element: T, selector: Selector): String? {
    if (selector.valueOverride != null) {
        return selector.mapFn(selector.valueOverride)
    }
    return when (element) {
        is Element -> select(element, selector)
        is XMLNode -> select(element, selector)
        else -> throw IllegalArgumentException("Unsupported type")
    }
}

// ======================== HTML ========================

private fun select(element: Element, selector: String): List<Element> {
    return element.select(selector)
}

private fun select(element: Element, selector: Selector): String? {
    val elements = select(element, selector.selector)
    val selectedElement = elements.getOrNull(selector.index ?: 0) ?: return null

    return selector.mapFn(
        if (selector.attribute != null) {
            selectedElement.attr(selector.attribute)
        } else {
            selectedElement.wholeText()
        }
    )
}

// ======================== XML ========================

private fun select(node: XMLNode, selector: String): List<XMLNode> {
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

private fun select(node: XMLNode, selector: Selector): String? {
    val elements = select(node, selector.selector)
    val selectedElement = elements.getOrNull(selector.index ?: 0) ?: return null

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