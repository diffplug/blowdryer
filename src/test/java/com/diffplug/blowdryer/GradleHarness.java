/*
 * Copyright (C) 2018-2023 DiffPlug
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

	private static int jreVersion() {
		return Integer.parseInt(System.getProperty("java.vm.specification.version"));
	}
}
