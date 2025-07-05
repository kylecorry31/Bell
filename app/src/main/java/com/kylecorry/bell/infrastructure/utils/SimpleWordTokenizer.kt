package com.kylecorry.bell.infrastructure.utils

class SimpleWordTokenizer(private val preservedWords: Set<String> = emptySet()) {

    // Letters, numbers, and apostrophes followed by letters
    private val wordRegex = Regex("[a-zA-Z0-9]+(?:'[a-zA-Z]+)?")

    fun tokenize(text: String): List<String> {

        val preservedWordRegex = Regex(preservedWords.joinToString("|") { Regex.escape(it) })
        val fullWordRegex = if (preservedWords.any()) {
            Regex(
                "\\b(?:${preservedWordRegex.pattern}|${wordRegex.pattern})\\b",
                RegexOption.IGNORE_CASE
            )
        } else {
            wordRegex
        }

        return fullWordRegex.findAll(text).map { it.value }.toList()
    }

}