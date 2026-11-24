package com.focuslock.app.domain

/** Pure short-form feed marker matching. */
object MarkerMatcher {
    fun matches(texts: List<String?>, markers: List<String>): Boolean {
        if (markers.isEmpty()) return false
        return texts.any { text ->
            !text.isNullOrEmpty() && markers.any { marker ->
                marker.isNotBlank() && text.contains(marker, ignoreCase = true)
            }
        }
    }
}
