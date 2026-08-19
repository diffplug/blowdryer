/*
 * Copyright (C) 2018-2026 DiffPlug
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
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;
import org.gradle.testkit.runner.GradleRunner;

public class GradleHarness extends ResourceHarness {
	/** A gradleRunner(). */
	protected GradleRunner gradleRunner() throws IOException {
		GradleRunner runner = GradleRunner.create()
				.withProjectDir(rootFolder())
				.withPluginClasspath();
		if (jreVersion() < 16) {
			runner.withGradleVersion(BlowdryerSetupPlugin.MINIMUM_GRADLE);
		}
		return runner;
	}

	/** A gradleRunner() whose plugin classpath also includes the given jars. */
	protected GradleRunner gradleRunnerWithExtraClasspath(File... extraJars) throws IOException {
		List<File> classpath = new ArrayList<>(pluginClasspath());
		classpath.addAll(Arrays.asList(extraJars));
		GradleRunner runner = GradleRunner.create()
				.withProjectDir(rootFolder())
				.withPluginClasspath(classpath);
		if (jreVersion() < 16) {
			runner.withGradleVersion(BlowdryerSetupPlugin.MINIMUM_GRADLE);
		}
		return runner;
	}

	/** Reads the same plugin-under-test classpath that {@code withPluginClasspath()} uses. */
	private static List<File> pluginClasspath() throws IOException {
		try (InputStream input = GradleHarness.class.getClassLoader().getResourceAsStream("plugin-under-test-metadata.properties")) {
			if (input == null) {
				throw new IllegalStateException("Could not find plugin-under-test-metadata.properties on the test classpath.");
			}
			Properties props = new Properties();
			props.load(input);
			String classpath = props.getProperty("implementation-classpath");
			List<File> files = new ArrayList<>();
			for (String path : classpath.split(File.pathSeparator)) {
				files.add(new File(path));
			}
			return files;
		}
	}

	private static int jreVersion() {
		return Integer.parseInt(System.getProperty("java.vm.specification.version"));
	}
}
