package com.diffplug.blowdryer;

import org.assertj.core.api.Assertions;
import org.junit.Test;

import com.diffplug.blowdryer.AsFile;

public class AsLocalTest {
	@Test
	public void filenameSafe() {
		filenameSafe("http://shortName.com/a+b-0-9~Z", "http-shortName.com-a+b-0-9-Z");
		filenameSafe("https://raw.githubusercontent.com/diffplug/durian-build/07f588e52eb0f31e596eab0228a5df7233a98a14/gradle/spotless/spotless.license.java",
				"https-raw.githubusercontent.com-diffplug--3vpUTw--14-gradle-spotless-spotless.license.java");
	}

	private void filenameSafe(String url, String safe) {
		Assertions.assertThat(AsFile.filenameSafe(url)).isEqualTo(safe);
	}
}
