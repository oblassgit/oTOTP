package org.olidev.ototp.settings

import com.mikepenz.aboutlibraries.Libs
import kotlinx.collections.immutable.toImmutableList

internal fun Libs.runtimeLibraries(coordinates: Set<String>): Libs = copy(
    libraries = libraries
        .filter { it.uniqueId in coordinates }
        .distinctBy { it.uniqueId }
        .toImmutableList()
)
