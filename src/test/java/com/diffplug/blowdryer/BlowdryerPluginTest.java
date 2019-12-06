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
	private void settingsGithub(String tag, String... extra) throws IOException {
		write("settings.gradle",
				"plugins { id 'com.diffplug.blowdryerSetup' }",
				"blowdryerSetup { github('diffplug/blowdryer', 'tag', '" + tag + "') }",
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
	public void devLocal() throws IOException {
		write("../blowdryer-script/src/main/resources/sample", "c");
		write("../blowdryer-script/src/main/resources/sample.properties",
				"name=test",
				"group=com.diffplug.gradle");
		write("settings.gradle",
				"plugins { id 'com.diffplug.blowdryerSetup' }",
				"blowdryerSetup { devLocal('../blowdryer-script') }");
		write("build.gradle",
				"apply plugin: 'com.diffplug.blowdryer'",
				"assert 干.file('sample').text == 'c\\n'",
				"assert 干.prop('sample', 'name') == 'test'",
				"assert 干.prop('sample', 'group') == 'com.diffplug.gradle'");
		gradleRunner().build();
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
				"apply plugin: 'com.diffplug.blowdryer'",
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
				"plugins { id 'com.diffplug.blowdryer' }",
				"干.file('notPresent')");
		Assertions.assertThat(gradleRunner().buildAndFail().getOutput().replace("\r\n", "\n")).contains(
				"https://raw.githubusercontent.com/diffplug/blowdryer/test/2/a/src/main/resources/notPresent\n" +
						"  received http code 404\n" +
						"  404: Not Found");
	}

	@Test
	public void projTestGroovy() throws IOException {
		write("../blowdryer-script/src/main/resources/sample.properties",
				"name=test",
				"group=com.diffplug.gradle");
		write("settings.gradle",
				"plugins { id 'com.diffplug.blowdryerSetup' }",
				"blowdryerSetup { devLocal('../blowdryer-script') }");
		write("../blowdryer-script/src/main/resources/script.gradle",
				"import com.diffplug.blowdryer.Blowdryer",
				"apply plugin: 'com.diffplug.blowdryer'",
				"println 干.proj('pluginPass', 'password for the keyFile')",
				"println 干.proj(File.class, 'keyFile', 'location of the keyFile')",
				"println 干.prop('sample', 'group')",
				"");
		write("build.gradle",
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
	public void projTestKotlin() throws IOException {
		write("../blowdryer-script/src/main/resources/sample.properties",
				"name=test",
				"group=com.diffplug.gradle");
		write("settings.gradle.kts",
				"plugins { id(\"com.diffplug.blowdryerSetup\") }",
				"import com.diffplug.blowdryer.BlowdryerSetupExtension",
				"configure<BlowdryerSetupExtension> {",
				"  devLocal(\"../blowdryer-script\")",
				"}");
		write("../blowdryer-script/src/main/resources/script.gradle.kts",
				"import com.diffplug.blowdryer.BlowdryerKotlinExtension",
				"apply(plugin = \"com.diffplug.blowdryer\")",
				"configure<BlowdryerKotlinExtension> {",
				"  println(干.proj(\"pluginPass\", \"password for the keyFile\"))",
				"  println(干.proj(File::class.java, \"keyFile\", \"location of the keyFile\"))",
				"  println(干.prop(\"sample\", \"group\"))",
				"}");
		write("build.gradle.kts",
				"import com.diffplug.blowdryer.BlowdryerKotlinExtension",
				"apply(plugin = \"com.diffplug.blowdryer\")",
				"val pluginPass by extra(\"supersecret\")",
				"val keyFile by extra(File(\"keyFile.txt\"))",
				"configure<BlowdryerKotlinExtension> {",
				"  apply(from = 干.file(\"script.gradle.kts\"))",
				"}");
		Assertions.assertThat(gradleRunner().build().getOutput().replace("\r\n", "\n")).contains(
				"> Configure project :\n" +
						"supersecret\n" +
						"keyFile.txt\n" +
						"com.diffplug.gradle\n" +
						"\n");
	}
}
