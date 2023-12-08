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

import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.gradle.api.GradleException;
import org.gradle.api.Plugin;
import org.gradle.api.initialization.Settings;

/** Gradle settings plugin which configures the source URL and version. */
public class BlowdryerSetupPlugin implements Plugin<Settings> {
	static final String MINIMUM_GRADLE = "6.8";

	private static final Pattern BAD_SEMVER = Pattern.compile("(\\d+)\\.(\\d+)");

	@Override
	public void apply(Settings settings) {
		if (badSemver(settings.getGradle().getGradleVersion()) < badSemver(MINIMUM_GRADLE)) {
			throw new GradleException("Blowdryer requires Gradle " + MINIMUM_GRADLE + " or newer, this was " + settings.getGradle().getGradleVersion());
		}
		Blowdryer.initTempDir(settings.getProviders().systemProperty("java.io.tmpdir").get());
		settings.getExtensions().create(BlowdryerSetup.NAME, BlowdryerSetup.class, settings.getRootDir());
	}

	private static int badSemver(String input) {
		Matcher matcher = BAD_SEMVER.matcher(input);
		if (!matcher.find() || matcher.start() != 0) {
			throw new IllegalArgumentException("Version must start with " + BAD_SEMVER.pattern());
		}
		String major = matcher.group(1);
		String minor = matcher.group(2);
		return badSemver(Integer.parseInt(major), Integer.parseInt(minor));
	}

	/** Ambiguous after 2147.483647.blah-blah */
	private static int badSemver(int major, int minor) {
		return major * 1_000_000 + minor;
	}
}
