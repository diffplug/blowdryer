/*
 * Copyright (C) 2021 DiffPlug
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
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.stream.Collectors;
import org.junit.Ignore;
import org.junit.Test;

@Ignore("has to be filled with private tokens and repos")
public class BlowdryerPluginAuthTest extends GradleHarness {

	private static final String BITBUCKET_REPO_ORG = "bHack/blowdryer-private";
	private static final String BITBUCKET_REPO_USER = "bHack";
	private static final String BITBUCKET_REPO_APP_PW = "replace-with-app-pw";

	private static final String BITBUCKET_REPO_PAT_REPO_ORG = "MNT/mnt-centralised-cfg";
	private static final String BITBUCKET_REPO_PAT = "replace-with-pat";
	private static final String BITBUCKET_PRIVATE_SERVER_HOST = "replace.with.private.host";

	private void settingsGitlabAuth(String tag, String... extra) throws IOException {
		write("settings.gradle",
				"plugins { id 'com.diffplug.blowdryerSetup' }",
				"blowdryerSetup { repoSubfolder(''); "
						+ "gitlab('private/repo', 'tag', '" + tag + "').authToken('foobar');"
						+ " }",
				Arrays.stream(extra).collect(Collectors.joining("\n")));
	}

	private void settingsGithubAuth(String tag, String... extra) throws IOException {
		write("settings.gradle",
				"plugins { id 'com.diffplug.blowdryerSetup' }",
				"blowdryerSetup { github('private/repo', 'tag', '" + tag + "').authToken('foobar');"
						+ " }",
				Arrays.stream(extra).collect(Collectors.joining("\n")));
	}

	private void settingsBitbucketBasicAuth(String tag, String... extra) throws IOException {
		write("settings.gradle",
			"plugins { id 'com.diffplug.blowdryerSetup' }",
			String.format("blowdryerSetup { bitbucket('%s', 'tree', '%s').cloudAuth('%s:%s');",
				BITBUCKET_REPO_ORG, tag, BITBUCKET_REPO_USER, BITBUCKET_REPO_APP_PW)
				+ " }",
			Arrays.stream(extra).collect(Collectors.joining("\n")));
	}

	private void settingsBitbucketPersonalAccessTokenAuth(String tag, String... extra) throws IOException {
		write("settings.gradle",
			"plugins { id 'com.diffplug.blowdryerSetup' }",
			String.format("blowdryerSetup { bitbucket('%s', 'tree', '%s').customDomainHttps('%s')"
				+ ".serverAuth('%s');", BITBUCKET_REPO_PAT_REPO_ORG, tag, BITBUCKET_PRIVATE_SERVER_HOST, BITBUCKET_REPO_PAT)
				+ " }",
			Arrays.stream(extra).collect(Collectors.joining("\n")));
	}

	@Test
	public void githubAuthTag() throws IOException {
		settingsGithubAuth("master");
		write("build.gradle",
				"apply plugin: 'com.diffplug.blowdryer'",
				"assert 干.file('sample').text == 'a'");
		gradleRunner().build();
	}

	@Test
	public void gitlabAuthTag() throws IOException {
		settingsGitlabAuth("init-test-for-auth");
		write("build.gradle",
				"apply plugin: 'com.diffplug.blowdryer'",
				"assert 干.file('sample').text == 'a'");
		gradleRunner().build();
	}

	@Test
	public void bitbucketCloudAuth() throws IOException {
		settingsBitbucketBasicAuth("master");
		write("build.gradle",
			"apply plugin: 'com.diffplug.blowdryer'",
			"assert 干.file('sample').text == 'a'");
		gradleRunner().build();
	}

	@Test
	public void bitbucketServerAuth() throws IOException {
		settingsBitbucketPersonalAccessTokenAuth("master");
		write("build.gradle",
			"apply plugin: 'com.diffplug.blowdryer'",
			"assert 干.file('checkstyle/spotless.gradle').text == 'a'");
		gradleRunner().build();
	}

	/** Writes the given content to the given path. */
	protected File write(String path, String... lines) throws IOException {
		File file = file(path);
		file.getParentFile().mkdirs();
		Files.write(file.toPath(), Arrays.asList(lines), StandardCharsets.UTF_8);
		return file;
	}
}
