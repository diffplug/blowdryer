/*
 * Copyright (C) 2019-2021 DiffPlug
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


import java.io.IOException;
import java.util.Arrays;
import java.util.stream.Collectors;
import org.junit.Test;

public class BlowdryerPluginConfigurationCacheTest extends GradleHarness {
	private static final String GRADLE_PROPERTIES = "gradle.properties";
	private static final String SETTINGS_GRADLE = "settings.gradle";
	private static final String BUILD_GRADLE = "build.gradle";

	private void settingsGithub(String tag, String... extra) throws IOException {
		write(GRADLE_PROPERTIES, "org.gradle.unsafe.configuration-cache=true");
		write(SETTINGS_GRADLE,
				"plugins { id 'com.diffplug.blowdryerSetup' }",
				"blowdryerSetup { github('diffplug/blowdryer', 'tag', '" + tag + "') }",
				Arrays.stream(extra).collect(Collectors.joining("\n")));
	}

	@Test
	public void githubTag() throws IOException {
		settingsGithub("test/2/a");
		write(BUILD_GRADLE,
				"apply plugin: 'com.diffplug.blowdryer'",
				"assert 干.file('sample').text == 'a'",
				"assert 干.prop('sample', 'name') == 'test'",
				"assert 干.prop('sample', 'ver_spotless') == '1.2.0'");
		gradleRunner().build();

		settingsGithub("test/2/b");
		write(BUILD_GRADLE,
				"apply plugin: 'com.diffplug.blowdryer'",
				"assert 干.file('sample').text == 'b'",
				"assert 干.prop('sample', 'name') == 'testB'",
				"assert 干.prop('sample', 'group') == 'com.diffplug.gradleB'");
		gradleRunner().build();

		// double-check that failures do fail
		settingsGithub("test/2/b");
		write(BUILD_GRADLE,
				"plugins { id 'com.diffplug.blowdryer' }",
				"assert Blowdryer.file('sample').text == 'a'");
		gradleRunner().buildAndFail();
	}
}
