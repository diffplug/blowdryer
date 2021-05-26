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
import org.assertj.core.api.Assertions;
import org.junit.Test;

public class BlowdryerPluginTest extends GradleHarness {

	private static final String SETTINGS_GRADLE = "settings.gradle";
	private static final String BUILD_GRADLE = "build.gradle";

	private void settingsGithub(String tag, String... extra) throws IOException {
		write(SETTINGS_GRADLE,
				"plugins { id 'com.diffplug.blowdryerSetup' }",
				"blowdryerSetup { github('diffplug/blowdryer', 'tag', '" + tag + "') }",
				Arrays.stream(extra).collect(Collectors.joining("\n")));
	}

	private void settingsGitlab(String tag, String... extra) throws IOException {
		write(SETTINGS_GRADLE,
				"plugins { id 'com.diffplug.blowdryerSetup' }",
				"blowdryerSetup { gitlab('diffplug/blowdryer', 'tag', '" + tag + "') }",
				Arrays.stream(extra).collect(Collectors.joining("\n")));
	}

	private void settingsCustomGitlab(String tag, String... extra) throws IOException {
		write(SETTINGS_GRADLE,
				"plugins { id 'com.diffplug.blowdryerSetup' }",
				"blowdryerSetup { gitlab('diffplug/blowdryer', 'tag', '" + tag + "').customDomainHttps('gitlab.com') }",
				Arrays.stream(extra).collect(Collectors.joining("\n")));
	}

	private void settingsGitlabRootFolder(String tag, String... extra) throws IOException {
		write(SETTINGS_GRADLE,
				"plugins { id 'com.diffplug.blowdryerSetup' }",
				"blowdryerSetup { repoSubfolder(''); gitlab('diffplug/blowdryer', 'tag', '" + tag + "') }",
				Arrays.stream(extra).collect(Collectors.joining("\n")));
	}

	private void settingsLocalJar(String dependency) throws IOException {
		write(SETTINGS_GRADLE,
				"plugins { id 'com.diffplug.blowdryerSetup' }",
				"blowdryerSetup { localJar('" + dependency + "') }");
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

	@Test
	public void gitlabTag() throws IOException {
		settingsGitlab("test/2/a");
		write(BUILD_GRADLE,
				"apply plugin: 'com.diffplug.blowdryer'",
				"assert 干.file('sample').text == 'a'",
				"assert 干.prop('sample', 'name') == 'test'",
				"assert 干.prop('sample', 'ver_spotless') == '1.2.0'");
		gradleRunner().build();

		settingsGitlab("test/2/b");
		write(BUILD_GRADLE,
				"apply plugin: 'com.diffplug.blowdryer'",
				"assert 干.file('sample').text == 'b'",
				"assert 干.prop('sample', 'name') == 'testB'",
				"assert 干.prop('sample', 'group') == 'com.diffplug.gradleB'");
		gradleRunner().build();

		// double-check that failures do fail
		settingsGitlab("test/2/b");
		write(BUILD_GRADLE,
				"plugins { id 'com.diffplug.blowdryer' }",
				"assert Blowdryer.file('sample').text == 'a'");
		gradleRunner().buildAndFail();
	}

	@Test
	public void customGitlabTag() throws IOException {
		settingsCustomGitlab("test/2/a");
		write(BUILD_GRADLE,
				"apply plugin: 'com.diffplug.blowdryer'",
				"assert 干.file('sample').text == 'a'",
				"assert 干.prop('sample', 'name') == 'test'",
				"assert 干.prop('sample', 'ver_spotless') == '1.2.0'");
		gradleRunner().build();

		settingsCustomGitlab("test/2/b");
		write(BUILD_GRADLE,
				"apply plugin: 'com.diffplug.blowdryer'",
				"assert 干.file('sample').text == 'b'",
				"assert 干.prop('sample', 'name') == 'testB'",
				"assert 干.prop('sample', 'group') == 'com.diffplug.gradleB'");
		gradleRunner().build();

		// double-check that failures do fail
		settingsCustomGitlab("test/2/b");
		write(BUILD_GRADLE,
				"plugins { id 'com.diffplug.blowdryer' }",
				"assert Blowdryer.file('sample').text == 'a'");
		gradleRunner().buildAndFail();
	}

	@Test
	public void rootfolderGitlabTag() throws IOException {
		settingsGitlabRootFolder("test/2/a");
		write(BUILD_GRADLE,
				"apply plugin: 'com.diffplug.blowdryer'",
				"assert 干.file('src/main/resources/sample').text == 'a'",
				"assert 干.prop('src/main/resources/sample', 'name') == 'test'",
				"assert 干.prop('src/main/resources/sample', 'ver_spotless') == '1.2.0'");
		gradleRunner().build();

		settingsGitlabRootFolder("test/2/b");
		write(BUILD_GRADLE,
				"apply plugin: 'com.diffplug.blowdryer'",
				"assert 干.file('src/main/resources/sample').text == 'b'",
				"assert 干.prop('src/main/resources/sample', 'name') == 'testB'",
				"assert 干.prop('src/main/resources/sample', 'group') == 'com.diffplug.gradleB'");
		gradleRunner().build();

		// double-check that failures do fail
		settingsGitlabRootFolder("test/2/b");
		write(BUILD_GRADLE,
				"plugins { id 'com.diffplug.blowdryer' }",
				"assert Blowdryer.file('src/main/resources/sample').text == 'a'");
		gradleRunner().buildAndFail();
	}

	@Test
	public void devLocal() throws IOException {
		write("../blowdryer-script/src/main/resources/sample", "c");
		write("../blowdryer-script/src/main/resources/sample.properties",
				"name=test",
				"group=com.diffplug.gradle");
		write(SETTINGS_GRADLE,
				"plugins { id 'com.diffplug.blowdryerSetup' }",
				"blowdryerSetup { devLocal('../blowdryer-script') }");
		write(BUILD_GRADLE,
				"apply plugin: 'com.diffplug.blowdryer'",
				// .replace('\\r', '') fixes test on windows
				"assert 干.file('sample').text.replace('\\r', '') == 'c\\n'",
				"assert 干.prop('sample', 'name') == 'test'",
				"assert 干.prop('sample', 'group') == 'com.diffplug.gradle'");
		gradleRunner().build();
	}

	@Test
	public void multiproject() throws IOException {
		settingsGithub("test/2/a",
				"include 'subproject'");
		write(BUILD_GRADLE,
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
		write(BUILD_GRADLE,
				"plugins { id 'com.diffplug.blowdryer' }",
				"干.file('notPresent')");
		Assertions.assertThat(gradleRunner().buildAndFail().getOutput().replace("\r\n", "\n")).contains(
				"https://raw.githubusercontent.com/diffplug/blowdryer/test/2/a/src/main/resources/notPresent\n" +
						"  received http code 404\n" +
						"  404: Not Found");
	}

	@Test
	public void cfgTestGroovy() throws IOException {
		write("../blowdryer-script/src/main/resources/sample.properties",
				"name=test",
				"group=com.diffplug.gradle");
		write(SETTINGS_GRADLE,
				"plugins { id 'com.diffplug.blowdryerSetup' }",
				"blowdryerSetup { devLocal('../blowdryer-script') }");
		write("../blowdryer-script/src/main/resources/script.gradle",
				"import com.diffplug.blowdryer.Blowdryer",
				"apply plugin: 'com.diffplug.blowdryer'",
				"println 干.proj('pluginPass', 'password for the keyFile')",
				"println 干.proj(File.class, 'keyFile', 'location of the keyFile')",
				"println 干.prop('sample', 'group')",
				"");
		write(BUILD_GRADLE,
				"apply plugin: 'com.diffplug.blowdryer'",
				"ext.pluginPass = 'supersecret'",
				"ext.keyFile = new File('keyFile.txt')",
				"apply from: 干.file('script.gradle')");
		Assertions.assertThat(gradleRunner().build().getOutput().replace("\r\n", "\n")).contains(
				"> Configure project :\n" +
						"supersecret\n" +
						"keyFile.txt\n" +
						"com.diffplug.gradle\n" +
						"\n");
	}

	@Test
	public void cfgTestKotlin() throws IOException {
		write("../blowdryer-script/src/main/resources/sample.properties",
				"name=test",
				"group=com.diffplug.gradle");
		write("settings.gradle.kts",
				"plugins { id(\"com.diffplug.blowdryerSetup\") }",
				"import com.diffplug.blowdryer.BlowdryerSetup",
				"configure<BlowdryerSetup> {",
				"  devLocal(\"../blowdryer-script\")",
				"}");
		write("../blowdryer-script/src/main/resources/script.gradle.kts",
				"import com.diffplug.blowdryer.干",
				"println(干.proj(project, \"pluginPass\", \"password for the keyFile\"))",
				"println(干.proj(project, File::class.java, \"keyFile\", \"location of the keyFile\"))",
				"println(干.prop(\"sample\", \"group\"))");
		write("build.gradle.kts",
				"import com.diffplug.blowdryer.干",
				"val pluginPass by extra(\"supersecret\")",
				"val keyFile by extra(File(\"keyFile.txt\"))",
				"apply(from = 干.file(\"script.gradle.kts\"))");
		Assertions.assertThat(gradleRunner().build().getOutput().replace("\r\n", "\n")).contains(
				"> Configure project :\n" +
						"supersecret\n" +
						"keyFile.txt\n" +
						"com.diffplug.gradle\n" +
						"\n");
	}

	@Test
	public void settingsTest() throws IOException {
		write(SETTINGS_GRADLE,
				"plugins { id 'com.diffplug.blowdryerSetup' }",
				"blowdryerSetup { github('diffplug/blowdryer', 'tag', 'test/2/a') }",
				"import com.diffplug.blowdryer.干",
				"assert 干.file('sample').text == 'a'",
				"assert 干.prop('sample', 'name') == 'test'",
				"assert 干.prop('sample', 'ver_spotless') == '1.2.0'",
				"println 'test was success'");
		Assertions.assertThat(gradleRunner().build().getOutput().replace("\r\n", "\n"));
	}

	@Test
	public void localJarFileDownloadExists() throws IOException {
		String jarFile = BlowdryerPluginTest.class.getResource("test-dependency.jar").getFile();
		settingsLocalJar(jarFile);

		write(BUILD_GRADLE,
				"apply plugin: 'com.diffplug.blowdryer'",
				"assert 干.file('spotless/license-header.java').exists()");

		gradleRunner().build();
	}
}
