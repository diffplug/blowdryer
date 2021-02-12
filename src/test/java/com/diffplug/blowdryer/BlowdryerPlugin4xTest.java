/*
 * Copyright (C) 2019-2020 DiffPlug
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
import org.assertj.core.api.Assertions;
import org.gradle.testkit.runner.GradleRunner;
import org.junit.Ignore;
import org.junit.Test;

/**
 * Blowdryer fails on Gradle 4.10 because the kotlin-std-lib is too old, and
 * I can't figure out how to upgrade it.
 * 
 * But if you want to work more on it, this is a good starting point.
 * 
 * ```
 * Caused by: java.lang.NoSuchMethodError: kotlin.collections.ArraysKt.copyInto([B[BIII)[B
 *   at okio.Segment.writeTo(Segment.kt:164)
 *   at okio.Segment.compact(Segment.kt:147)
 *   at okio.Buffer.write(Buffer.kt:1849)
 *   at okio.Buffer.read(Buffer.kt:1860)
 *   at okio.Buffer.writeAll(Buffer.kt:1650)
 *   at okio.Options$Companion.buildTrieRecursive(Options.kt:187)
 *   at okio.Options$Companion.buildTrieRecursive(Options.kt:174)
 *   at okio.Options$Companion.buildTrieRecursive$default(Options.kt:113)
 *   at okio.Options$Companion.of(Options.kt:72)
 *   at okhttp3.internal.Util.<clinit>(Util.kt:69)
 *   at okhttp3.internal.connection.RealConnectionPool.<clinit>(RealConnectionPool.kt:263)
 *   at okhttp3.ConnectionPool.<init>(ConnectionPool.kt:37)
 *   at okhttp3.ConnectionPool.<init>(ConnectionPool.kt:39)
 *   at okhttp3.OkHttpClient$Builder.<init>(OkHttpClient.kt:438)
 *   at com.diffplug.blowdryer.Blowdryer.download(Blowdryer.java:134)
 *   at com.diffplug.blowdryer.Blowdryer.immutableUrl(Blowdryer.java:102)
 *   at com.diffplug.blowdryer.Blowdryer.file(Blowdryer.java:214)
 * ```
 */
@Ignore
public class BlowdryerPlugin4xTest extends GradleHarness {
	@Override
	protected GradleRunner gradleRunner() throws IOException {
		return super.gradleRunner().withArguments("--stacktrace").withGradleVersion("4.10");
	}

	private void settingsGithub(String tag, String... extra) throws IOException {
		write("buildSrc/build.gradle",
				"apply plugin: 'java'",
				"repositories {",
				"  jcenter()",
				"}",
				"dependencies {",
				"  compile 'com.diffplug:blowdryer:0.2.0'",
				"}");
		write("settings.gradle",
				"import com.diffplug.blowdryer.BlowdryerSetup",
				"import com.diffplug.blowdryer.BlowdryerSetup.GitAnchorType",
				"BlowdryerSetup setup = new BlowdryerSetup(rootDir)",
				"setup.github('diffplug/blowdryer', GitAnchorType.TAG, '" + tag + "')",
				Arrays.stream(extra).collect(Collectors.joining("\n")));
	}

	@Test
	public void githubTag() throws IOException {
		settingsGithub("test/2/a");
		write("build.gradle",
				"apply plugin: 'com.diffplug.blowdryer'",
				"assert 干.file('sample').text == 'a'",
				"assert 干.prop('sample', 'name') == 'test'",
				"assert 干.prop('sample', 'ver_spotless') == '1.2.0'");
		gradleRunner().build();

		settingsGithub("test/2/b");
		write("build.gradle",
				"apply plugin: 'com.diffplug.blowdryer'",
				"assert 干.file('sample').text == 'b'",
				"assert 干.prop('sample', 'name') == 'testB'",
				"assert 干.prop('sample', 'group') == 'com.diffplug.gradleB'");
		gradleRunner().build();

		// double-check that failures do fail
		settingsGithub("test/2/b");
		write("build.gradle",
				"plugins { id 'com.diffplug.blowdryer' }",
				"assert Blowdryer.file('sample').text == 'a'");
		gradleRunner().buildAndFail();
	}

	@Test
	public void multiproject() throws IOException {
		settingsGithub("test/2/a",
				"include 'subproject'");
		write("build.gradle",
				"apply plugin: 'com.diffplug.blowdryer'",
				"assert 干.file('sample').text == 'a'",
				"assert 干.prop('sample', 'name') == 'test'",
				"assert 干.prop('sample', 'group') == 'com.diffplug.gradle'");
		write("subproject/build.gradle",
				"assert 干.file('sample').text == 'a'",
				"assert 干.prop('sample', 'name') == 'test'",
				"assert 干.prop('sample', 'group') == 'com.diffplug.gradle'");
		gradleRunner().build();

		// double-check that failures do fail
		write("subproject/build.gradle",
				"import com.diffplug.blowdryer.Blowdryer",
				"",
				"assert Blowdryer.file('sample').text == 'b'");
		gradleRunner().buildAndFail();
	}

	@Test
	public void missingResourceThrowsError() throws IOException {
		settingsGithub("test/2/a");
		write("build.gradle",
				"apply plugin: 'com.diffplug.blowdryer'",
				"干.file('notPresent')");
		Assertions.assertThat(gradleRunner().buildAndFail().getOutput().replace("\r\n", "\n")).contains(
				"https://raw.githubusercontent.com/diffplug/blowdryer/test/2/a/src/main/resources/notPresent\n" +
						"  received http code 404\n" +
						"  404: Not Found");
	}
}
