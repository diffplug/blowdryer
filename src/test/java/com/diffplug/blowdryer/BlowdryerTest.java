/*
 * Copyright (C) 2019-2023 DiffPlug
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

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.spy;

import com.diffplug.blowdryer.Blowdryer.AuthPlugin;
import com.diffplug.blowdryer.Blowdryer.ResourcePlugin;
import com.diffplug.blowdryer.BlowdryerSetup.Bitbucket;
import com.diffplug.blowdryer.BlowdryerSetup.GitAnchorType;
import com.diffplug.common.base.StandardSystemProperty;
import java.lang.reflect.Field;
import java.util.Base64;
import java.util.Locale;
import java.util.UUID;
import okhttp3.Request;
import okhttp3.Request.Builder;
import org.assertj.core.api.Assertions;
import org.junit.BeforeClass;
import org.junit.Test;

public class BlowdryerTest {
	private static final String JAR_FILE_RESOURCE_SEPARATOR = "!/";
	private static final String FILE_PROTOCOL = "file:///";

	@BeforeClass
	public static void setup() {
		Blowdryer.initTempDir(StandardSystemProperty.JAVA_IO_TMPDIR.value());
	}

	@Test
	public void filenameSafe() {
		filenameSafe("https://foo.org/?file=blah.foo&rev=7", "https-foo.org-file-blah.foo-rev-7");
		filenameSafe("http://shortName.com/a+b-0-9~Z", "http-shortName.com-a+b-0-9-Z");
		filenameSafe("https://raw.githubusercontent.com/diffplug/durian-build/07f588e52eb0f31e596eab0228a5df7233a98a14/gradle/spotless/spotless.license.java",
				"https-raw.githubusercontent.com-diffplug--3vpUTw--14-gradle-spotless-spotless.license.java");
	}

	private void filenameSafe(String url, String safe) {
		assertThat(Blowdryer.filenameSafe(url)).isEqualTo(safe);
	}

	@Test
	public void cachedFileDeleted_issue_11() {
		String test = "https://raw.githubusercontent.com/diffplug/blowdryer/test/2/b/src/main/resources/sample";
		assertThat(Blowdryer.immutableUrl(test)).hasContent("b");
		Blowdryer.immutableUrl(test).delete();
		assertThat(Blowdryer.immutableUrl(test)).hasContent("b");
	}

	@Test
	public void immutableUrlOfLocalJar() {
		String jarFile = BlowdryerPluginTest.class.getResource("test.jar").getFile();
		if (System.getProperty("os.name").toLowerCase(Locale.ROOT).contains("win")) {
			Assertions.assertThat(jarFile).startsWith("/");
			jarFile = jarFile.substring(1).replace('\\', '/');
		}
		assertThat(Blowdryer.immutableUrl(FILE_PROTOCOL + jarFile + JAR_FILE_RESOURCE_SEPARATOR + "sample")).exists();
	}

	@Test
	public void requiredSuffix() {
		String jarFile = BlowdryerPluginTest.class.getResource("test.jar").getFile();
		assertThat(Blowdryer.immutableUrl(FILE_PROTOCOL + jarFile + JAR_FILE_RESOURCE_SEPARATOR + "sample", ".suffix").getName()).endsWith(".suffix");
		assertThat(Blowdryer.immutableUrl(FILE_PROTOCOL + jarFile + JAR_FILE_RESOURCE_SEPARATOR + "sample", ".suffix2").getName()).endsWith(".suffix2");
	}

	@Test
	public void bitbucketCloud_tagAnchorType() throws Exception {
		final String hashRequestUrl = "https://api.bitbucket.org/2.0/repositories/testOrg/testRepo/refs/tags/testAnchor";
		final String hash = UUID.randomUUID().toString();
		final String expected = "https://api.bitbucket.org/2.0/repositories/testOrg/testRepo/src/" + hash + "/src/main/resources/test.properties";

		Bitbucket spy = spy(setupBitbucketTestTarget(GitAnchorType.TAG)).authToken("un:pw");
		doReturn(hash).when(spy).getCommitHashFromBitbucket(hashRequestUrl);
		final ResourcePlugin target = getResourcePlugin();

		assertThat(target.toImmutableUrl("test.properties")).isEqualTo(expected);
	}

	@Test
	public void bitbucketCloud_commitAnchorType() throws Exception {
		final String expected = "https://api.bitbucket.org/2.0/repositories/testOrg/testRepo/src/testAnchor/src/main/resources/test.properties";
		setupBitbucketTestTarget(GitAnchorType.COMMIT);
		final ResourcePlugin target = getResourcePlugin();

		assertThat(target.toImmutableUrl("test.properties")).isEqualTo(expected);
	}

	@Test
	public void bitbucketCloud_treeAnchorType() throws Exception {
		setupBitbucketTestTarget(GitAnchorType.TREE);
		final ResourcePlugin target = getResourcePlugin();

		assertThatThrownBy(() -> target.toImmutableUrl("test.properties"))
				.isInstanceOf(UnsupportedOperationException.class)
				.hasMessage("TREE not supported for Bitbucket");
	}

	@Test
	public void bitbucketServer_tagAnchorType() throws Exception {
		final String expected = "https://my.bitbucket.com/projects/testOrg/repos/testRepo/raw/src/main/resources/test.properties?at=refs%2Ftags%2FtestAnchor";
		setupBitbucketTestTarget(GitAnchorType.TAG).customDomainHttps("my.bitbucket.com");
		final ResourcePlugin target = getResourcePlugin();

		assertThat(target.toImmutableUrl("test.properties")).isEqualTo(expected);
	}

	@Test
	public void bitbucketServer_commitAnchorType() throws Exception {
		final String expected = "https://my.bitbucket.com/projects/testOrg/repos/testRepo/raw/src/main/resources/test.properties?at=testAnchor";
		setupBitbucketTestTarget(GitAnchorType.COMMIT).customDomainHttps("my.bitbucket.com");
		final ResourcePlugin target = getResourcePlugin();

		assertThat(target.toImmutableUrl("test.properties")).isEqualTo(expected);
	}

	@Test
	public void bitbucketServer_treeAnchorType() throws Exception {
		setupBitbucketTestTarget(GitAnchorType.TREE).customDomainHttps("my.bitbucket.com");
		final ResourcePlugin target = getResourcePlugin();

		assertThatThrownBy(() -> target.toImmutableUrl("test.properties"))
				.isInstanceOf(UnsupportedOperationException.class)
				.hasMessage("TREE not supported for Bitbucket");
	}

	@Test
	public void bitbucketCloudAuth() throws Exception {
		final String expected = "https://api.bitbucket.org/2.0/repositories/testOrg/testRepo/src/testAnchor/src/main/resources/test.properties";
		final String usernameAndAppPassword = String.format("%s:%s", randomUUID(), randomUUID());
		setupBitbucketTestTarget(GitAnchorType.COMMIT).authToken(usernameAndAppPassword);

		final ResourcePlugin target = getResourcePlugin();
		final AuthPlugin otherTarget = getAuthPlugin();
		final Builder requestBuilder = new Builder().url(expected);
		otherTarget.addAuthToken(expected, requestBuilder);
		final Request request = requestBuilder.build();

		assertThat(target.toImmutableUrl("test.properties")).isEqualTo(expected);
		final String encoded = Base64.getEncoder().encodeToString((usernameAndAppPassword)
				.getBytes(UTF_8));
		assertThat(request.header("Authorization")).isEqualTo(String.format("Basic %s", encoded));
	}

	private Bitbucket setupBitbucketTestTarget(final GitAnchorType anchorType) {
		final String repoOrg = "testOrg/testRepo";
		final String anchor = "testAnchor";
		return new BlowdryerSetup(null).bitbucket(repoOrg, anchorType, anchor);
	}

	private ResourcePlugin getResourcePlugin() throws Exception {
		final Field field = Blowdryer.class.getDeclaredField("plugin");
		field.setAccessible(true);
		return (ResourcePlugin) field.get(null);
	}

	private AuthPlugin getAuthPlugin() throws Exception {
		final Field field = Blowdryer.class.getDeclaredField("authPlugin");
		field.setAccessible(true);
		return (AuthPlugin) field.get(null);
	}

	private String randomUUID() {
		return UUID.randomUUID().toString();
	}

}
