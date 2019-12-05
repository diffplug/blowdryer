/*
 * Copyright 2019 DiffPlug
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
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
import org.gradle.api.Plugin;
import org.gradle.api.Project;

/** Alias for {@link Blowdryer}. */
public class 干 implements Plugin<Project> {
	@Override
	public void apply(Project proj) {
		proj.getExtensions().create("干", Extension.class, proj);
	}

	public static class Extension {
		private final Project project;

		public Extension(Project project) {
			this.project = project;
		}

		/** Alias for {@link Blowdryer#immutableUrl(String)}. */
		public File immutableUrl(String url) {
			return Blowdryer.immutableUrl(url);
		}

		/** Alias for {@link Blowdryer#file(String)}. */
		public File file(String resource) {
			return Blowdryer.file(resource);
		}

		/** Alias for {@link Blowdryer#prop(String, String)}. */
		public String prop(String propFile, String key) throws IOException {
			return Blowdryer.prop(propFile, key);
		}

		/** Alias for {@link Blowdryer#cfg(Project, String, String)}. */
		public String cfg(String key, String descForError) {
			return Blowdryer.cfg(project, key, descForError);
		}

		/** Alias for {@link Blowdryer#cfg(Project, Class, String, String)}. */
		public <T> T cfg(Class<T> clazz, String key, String descForError) {
			return Blowdryer.cfg(project, clazz, key, descForError);
		}
	}
}
