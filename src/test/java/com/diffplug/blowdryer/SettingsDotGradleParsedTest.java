/*
 * Copyright (C) 2023 DiffPlug
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

import org.assertj.core.api.Assertions;
import org.junit.Test;

public class SettingsDotGradleParsedTest {
	@Test
	public void somePlugins() {
		String input = "pluginManagement {\r\n" +
				"  repositories {\r\n" +
				"    mavenCentral()\r\n" +
				"    gradlePluginPortal()\r\n" +
				"  }\r\n" +
				"}\r\n" +
				"plugins {\r\n" +
				"  // https://plugins.gradle.org/plugin/com.gradle.plugin-publish\r\n" +
				"  id 'com.gradle.plugin-publish' version '0.20.0' apply false\r\n" +
				"  // https://github.com/equodev/equo-ide/blob/main/plugin-gradle/CHANGELOG.md\r\n" +
				"  id 'dev.equo.ide' version '0.12.1' apply false\r\n" +
				"  // https://github.com/gradle-nexus/publish-plugin/releases\r\n" +
				"  id 'io.github.gradle-nexus.publish-plugin' version '1.1.0' apply false\r\n" +
				"}\r\n" +
				"rootProject.name = 'blowdryer'\r\n";
		SettingsDotGradleParsed parsed = new SettingsDotGradleParsed(input);
		Assertions.assertThat(parsed.contentCorrectEndings()).isEqualTo(input);
		Assertions.assertThat(parsed.beforePlugins).isEqualTo("pluginManagement {\n" +
				"  repositories {\n" +
				"    mavenCentral()\n" +
				"    gradlePluginPortal()\n" +
				"  }\n" +
				"}");
		Assertions.assertThat(parsed.inPlugins).isEqualTo("  // https://plugins.gradle.org/plugin/com.gradle.plugin-publish\n" +
				"  id 'com.gradle.plugin-publish' version '0.20.0' apply false\n" +
				"  // https://github.com/equodev/equo-ide/blob/main/plugin-gradle/CHANGELOG.md\n" +
				"  id 'dev.equo.ide' version '0.12.1' apply false\n" +
				"  // https://github.com/gradle-nexus/publish-plugin/releases\n" +
				"  id 'io.github.gradle-nexus.publish-plugin' version '1.1.0' apply false");
		Assertions.assertThat(parsed.afterPlugins).isEqualTo("rootProject.name = 'blowdryer'\n");
	}
}
