/*
 * paper-loader-gen
 * Copyright (c) 2026 Harrison Boyd
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package dev.hboyd.paperloadergen.artifact

import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import java.net.URI

/**
 * A Maven repository.
 */
internal abstract class SerializableMavenArtifactRepository {
    @get:Input
    abstract val name: Property<String>

    @get:Input
    abstract val uri: Property<URI>

    @get:Input
    abstract val hasPasswordCredentials: Property<Boolean>
}
