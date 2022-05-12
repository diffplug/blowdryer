/*
 * Copyright (C) 2019-2022 DiffPlug
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

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.client.WireMock.verify;
import static com.github.tomakehurst.wiremock.stubbing.Scenario.STARTED;
import static org.assertj.core.api.Assertions.assertThat;

import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import com.github.tomakehurst.wiremock.junit.WireMockRule;
import java.io.File;
import java.io.IOException;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

public class BlowdryerRetryTest extends GradleHarness {

	private static final String RETRY_SCENARIO = "Retry Scenario";
	private static final String CAUSE_LIMIT_FAILED = "Cause limitFailed";

	@Rule
	public TemporaryFolder tempDir = new TemporaryFolder();

	@Rule
	public WireMockRule wireMockRule = new WireMockRule(WireMockConfiguration.wireMockConfig().dynamicPort().dynamicHttpsPort());

	@Before
	public void setup() throws IOException {
		Blowdryer.setResourcePluginNull();
		Blowdryer.setTempDirNull();
		Blowdryer.initTempDir(tempDir.newFolder("temp-cache-dir").getAbsolutePath());
	}

	@Test
	public void gitlabTriggersRateLimit() throws IOException {
		BlowdryerSetup blowdryerSetup = new BlowdryerSetup(new File("."));
		blowdryerSetup.gitlab("foo/bar", BlowdryerSetup.GitAnchorType.TAG, "1.0");
		String fileContent = "foobar";

		wireMockRule.stubFor(WireMock.get(WireMock.urlEqualTo("/bar"))
				.inScenario(RETRY_SCENARIO)
				.whenScenarioStateIs(STARTED)
				.willReturn(aResponse()
						.withHeader("Retry-After", "1")
						.withStatus(429))
				.willSetStateTo(CAUSE_LIMIT_FAILED));

		wireMockRule.stubFor(WireMock.get(WireMock.urlEqualTo("/bar"))
				.inScenario(RETRY_SCENARIO)
				.whenScenarioStateIs(CAUSE_LIMIT_FAILED)
				.willReturn(aResponse()
						.withStatus(200)
						.withBody(fileContent)));

		File downloadedFile = Blowdryer.immutableUrl("http://localhost:" + wireMockRule.port() + "/bar");

		verify(2, getRequestedFor(urlEqualTo("/bar")));
		assertThat(downloadedFile).hasContent(fileContent);
	}

	@Test
	public void gitlabTriggersRateLimitNoHeader() throws IOException {
		BlowdryerSetup blowdryerSetup = new BlowdryerSetup(new File("."));
		blowdryerSetup.gitlab("foo/bar", BlowdryerSetup.GitAnchorType.TAG, "1.0");
		String fileContent = "foobar";

		wireMockRule.stubFor(WireMock.get(WireMock.urlEqualTo("/bar"))
				.inScenario(RETRY_SCENARIO)
				.whenScenarioStateIs(STARTED)
				.willReturn(aResponse()
						.withStatus(429))
				.willSetStateTo(CAUSE_LIMIT_FAILED));

		wireMockRule.stubFor(WireMock.get(WireMock.urlEqualTo("/bar"))
				.inScenario(RETRY_SCENARIO)
				.whenScenarioStateIs(CAUSE_LIMIT_FAILED)
				.willReturn(aResponse()
						.withStatus(200)
						.withBody(fileContent)));

		File downloadedFile = Blowdryer.immutableUrl("http://localhost:" + wireMockRule.port() + "/bar");

		verify(2, getRequestedFor(urlEqualTo("/bar")));
		assertThat(downloadedFile).hasContent(fileContent);
	}

	@Test
	public void gitlabRequestWithoutLimit() throws IOException {
		BlowdryerSetup blowdryerSetup = new BlowdryerSetup(new File("."));
		blowdryerSetup.gitlab("foo/bar", BlowdryerSetup.GitAnchorType.TAG, "1.0");
		String fileContent = "foobar";

		wireMockRule.stubFor(WireMock.get(WireMock.urlEqualTo("/bar"))
				.willReturn(aResponse()
						.withStatus(200)
						.withBody(fileContent)));

		File downloadedFile = Blowdryer.immutableUrl("http://localhost:" + wireMockRule.port() + "/bar");

		verify(1, getRequestedFor(urlEqualTo("/bar")));
		assertThat(downloadedFile).hasContent(fileContent);
	}

	@Test
	public void githubRequestWithoutLimit() throws IOException {
		BlowdryerSetup blowdryerSetup = new BlowdryerSetup(new File("."));
		blowdryerSetup.github("foo/bar", BlowdryerSetup.GitAnchorType.TAG, "1.0");
		String fileContent = "foobar";

		wireMockRule.stubFor(WireMock.get(WireMock.urlEqualTo("/bar"))
				.willReturn(aResponse()
						.withStatus(200)
						.withBody(fileContent)));

		File downloadedFile = Blowdryer.immutableUrl("http://localhost:" + wireMockRule.port() + "/bar");

		verify(1, getRequestedFor(urlEqualTo("/bar")));
		assertThat(downloadedFile).hasContent(fileContent);
	}

}
