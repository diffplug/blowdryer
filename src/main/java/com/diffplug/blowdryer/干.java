/*
 * Copyright (C) 2019-2022 DiffPlug
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.diffplug.blowdryer;


import java.io.File;
import java.io.IOException;
import javax.annotation.Nullable;
import org.gradle.api.Project;

/** Alias for {@link Blowdryer}. */
public class 干 {
	private 干() {}

	/** Alias for {@link Blowdryer#immutableUrl(String)}. */
	public static File immutableUrl(String url) {
		return Blowdryer.immutableUrl(url);
	}

	/** Alias for {@link Blowdryer#immutableUrl(String, String)}. */
	public File immutableUrl(String url, @Nullable String requiredSuffix) {
		return Blowdryer.immutableUrl(url, requiredSuffix);
	}

	/** Alias for {@link Blowdryer#file(String)}. */
	public static File file(String resource) {
		return Blowdryer.file(resource);
	}

	/** Alias for {@link Blowdryer#prop(String, String)}. */
	public static String prop(String propFile, String key) throws IOException {
		return Blowdryer.prop(propFile, key);
	}

	/** Alias for {@link Blowdryer#proj(Project, String, String)}. */
	public static String proj(Project project, String key, String descForError) {
		return Blowdryer.proj(project, key, descForError);
	}

	/** Alias for {@link Blowdryer#proj(Project, Class, String, String)}. */
	public static <T> T proj(Project project, Class<T> clazz, String key, String descForError) {
		return Blowdryer.proj(project, clazz, key, descForError);
	}

	/** Alias for {@link Blowdryer#projOptional(Project, Class, String, String)} with {@code String.class}. */
	public static @Nullable String projOptional(Project project, String key, String descForError) {
		return Blowdryer.projOptional(project, key, descForError);
	}

	/** Alias for {@link Blowdryer#projOptional(Project, Class, String, String)}. */
	public static @Nullable <T> T projOptional(Project project, Class<T> clazz, String key, String descForError) {
		return Blowdryer.projOptional(project, clazz, key, descForError);
	}
}
