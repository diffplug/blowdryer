/*
 * Copyright 2020 DiffPlug
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


import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.util.GradleVersion;

/** Optional gradle plugin which can only be applied to the root project, and will create the 干 extension on every project. */
public class BlowdryerPlugin implements Plugin<Project> {
	static final String PLUGIN_ID = "com.diffplug.blowdryer";
	static final String WIPE_CACHE_TASK = "blowdryerWipeEntireCache";

	@Override
	public void apply(Project root) {
		if (root != root.getRootProject()) {
			throw new IllegalArgumentException("You must apply this plugin only to the root project.");
		}
		root.allprojects(p -> {
			Blowdryer.WithProject withProject = new Blowdryer.WithProject(p);
			p.getExtensions().add("干", withProject);
			p.getExtensions().add("Blowdryer", withProject);
		});

		if (GradleVersion.current().compareTo(BlowdryerPluginLegacy.CONFIG_AVOIDANCE_INTRODUCED) >= 0) {
			BlowdryerPluginConfigAvoidance.wipeCacheTask(root);
		} else {
			BlowdryerPluginLegacy.wipeCacheTask(root);
		}
	}
}
