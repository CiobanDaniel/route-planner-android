package com.danielcioban.routeplanner.util

object LibraryTags {
    fun parse(raw: String): List<String> =
        raw.split(',', ';')
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .distinctBy { it.lowercase() }

    fun join(tags: List<String>): String = parse(tags.joinToString(",")).joinToString(", ")

    fun join(raw: String): String = join(parse(raw))

    fun matches(raw: String, needle: String): Boolean {
        val q = needle.trim()
        if (q.isEmpty()) return true
        return parse(raw).any { it.contains(q, ignoreCase = true) }
    }

    fun merge(a: String, b: String): String = join(parse(a) + parse(b))
}
