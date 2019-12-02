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


import java.io.IOException;
import java.util.Arrays;
import java.util.stream.Collectors;
import org.assertj.core.api.Assertions;
import org.junit.Test;

public class BlowdryerPluginTest extends GradleHarness {
	private void writePathTagExtra(String path, String tag, String... extra) throws IOException {
		write(path,
				"plugins {",
				"  id 'com.diffplug.blowdryer'",
				"}",
				"blowdryer {",
				"  github('diffplug/blowdryer', 'tag', '" + tag + "');",
				"}",
				"import com.diffplug.blowdryer.AsFile",
				"",
				Arrays.stream(extra).collect(Collectors.joining("\n")));
	}

	@Test
	public void githubTag() throws IOException {
		writePathTagExtra("build.gradle", "test/a",
				"assert AsFile.resource('sample').text == 'a'");
		gradleRunner().build();

		writePathTagExtra("build.gradle", "test/b",
				"assert AsFile.resource('sample').text == 'b'");
		gradleRunner().build();

		// double-check that failures do fail
		writePathTagExtra("build.gradle", "test/b",
				"assert AsFile.resource('sample').text == 'a'");
		gradleRunner().buildAndFail();
	}

	@Test
	public void multiproject() throws IOException {
		write("settings.gradle",
				"include 'subproject'");
		writePathTagExtra("build.gradle", "test/a",
				"assert AsFile.resource('sample').text == 'a'");
		write("subproject/build.gradle",
				"import com.diffplug.blowdryer.AsFile",
				"",
				"assert AsFile.resource('sample').text == 'a'");
		gradleRunner().build();

		// double-check that failures do fail
		write("subproject/build.gradle",
				"import com.diffplug.blowdryer.AsFile",
				"",
				"assert AsFile.resource('sample').text == 'b'");
		gradleRunner().buildAndFail();
	}

	@Test
	public void missingResourceThrowsError() throws IOException {
		writePathTagExtra("build.gradle", "test/a",
				"AsFile.resource('notPresent')");
		Assertions.assertThat(gradleRunner().buildAndFail().getOutput().replace("\r\n", "\n")).contains(
				"https://raw.githubusercontent.com/diffplug/blowdryer/test/a/src/main/resources/notPresent\n" +
						"  received http code 404\n" +
						"  404: Not Found");
	}

	@Test
	public void applyOnNonRootThrowsError() throws IOException {
		write("settings.gradle",
				"include 'subproject'");
		writePathTagExtra("subproject/build.gradle", "test/a");
		Assertions.assertThat(gradleRunner().buildAndFail().getOutput().replace("\r\n", "\n")).contains(
				"An exception occurred applying plugin request [id: 'com.diffplug.blowdryer']\n" +
						"> Failed to apply plugin [id 'com.diffplug.blowdryer']\n" +
						"   > You must apply com.diffplug.blowdryer only on the root project, not :subproject");
	}
}
