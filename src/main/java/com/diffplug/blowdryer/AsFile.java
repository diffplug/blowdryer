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


import com.diffplug.common.base.Errors;
import com.diffplug.common.base.StandardSystemProperty;
import com.diffplug.common.hash.Hashing;
import com.diffplug.common.io.Files;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Properties;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;
import okio.BufferedSink;
import okio.Okio;

public class AsFile {
	static {
		File tmpDir = new File(StandardSystemProperty.JAVA_IO_TMPDIR.value());
		cacheDir = new File(tmpDir, "blowdryer-cache");
	}

	private static final File cacheDir;
	private static final Map<String, File> urlToContent = new HashMap<>();

	/**
	 * Downloads the given url to a local file in the system temporary directory.
	 * It will only be downloaded once, system-wide, and it will not be checked for updates.
	 * This is appropriate only for immutable URLs, such as specific hashes from Git.
	 */
	public static File immutableUrl(String url) {
		synchronized (AsFile.class) {
			File result = urlToContent.get(url);
			if (result != null) {
				return result;
			}

			String safe = filenameSafe(url);
			File metaFile = new File(cacheDir, safe + ".properties");
			File dataFile = new File(cacheDir, safe);

			try {
				if (metaFile.exists() && dataFile.exists()) {
					Properties props = new Properties();
					try (InputStream input = Files.asByteSource(metaFile).openBufferedStream()) {
						props.load(input);
					}
					String propUrl = props.getProperty(PROP_URL);
					if (propUrl.equals(url)) {
						urlToContent.put(url, dataFile);
						return dataFile;
					} else {
						throw new IllegalStateException("Expected url " + url + " but was " + propUrl + " for " + metaFile.getAbsolutePath());
					}
				} else {
					Files.createParentDirs(dataFile);
					download(url, dataFile);
					Properties props = new Properties();
					props.setProperty("version", "1");
					props.setProperty(PROP_URL, url);
					props.setProperty("downloadedAt", new Date().toString());
					try (OutputStream output = Files.asByteSink(metaFile).openBufferedStream()) {
						props.store(output, "");
					}
					urlToContent.put(url, dataFile);
					return dataFile;
				}
			} catch (IOException e) {
				throw Errors.asRuntime(e);
			}
		}
	}

	private static final String PROP_URL = "url";

	private static void download(String url, File dst) throws IOException {
		OkHttpClient client = new OkHttpClient.Builder().build();
		Request req = new Request.Builder().url(url).build();
		try (Response response = client.newCall(req).execute();
				BufferedSink sink = Okio.buffer(Okio.sink(dst))) {
			try (ResponseBody body = response.body()) {
				if (body == null) {
					throw new IllegalArgumentException("Body was expected to be non-null");
				}
				sink.writeAll(body.source());
			}
		}
	}

	private static final int MAX_FILE_LENGTH = 92;
	private static final int ABBREVIATED = 40;

	/** Returns either the filename safe URL, or (first40)--(Base64 filenamesafe)(last40). */
	static String filenameSafe(String url) {
		String allSafeCharacters = url.replaceAll("[^a-zA-Z0-9-+_\\.]", "-");
		String noDuplicateDash = allSafeCharacters.replaceAll("-+", "-");
		if (noDuplicateDash.length() <= MAX_FILE_LENGTH) {
			return noDuplicateDash;
		} else {
			int secondPoint = noDuplicateDash.length() - ABBREVIATED;
			String first = noDuplicateDash.substring(0, ABBREVIATED);
			String middle = noDuplicateDash.substring(ABBREVIATED, secondPoint);
			String end = noDuplicateDash.substring(secondPoint);
			byte[] hash = Hashing.murmur3_32()
					.hashString(middle, StandardCharsets.UTF_8)
					.asBytes();
			String hashed = Base64.getEncoder().encodeToString(hash)
					.replace('/', '-').replace('=', '-');
			return first + "--" + hashed + end;
		}
	}

	//////////////////////
	// plugin interface //
	//////////////////////
	static interface ResourcePlugin {
		String toImmutableUrl(String resourcePath);
	}

	private static ResourcePlugin plugin;

	static void assertPluginNotSet(String errorMessage) {
		if (AsFile.plugin != null) {
			throw new IllegalStateException(errorMessage);
		}
	}

	static void setResourcePlugin(ResourcePlugin plugin) {
		assertPluginNotSet("You already initialized the `blowdryer` plugin, you can't do this twice.");
		AsFile.plugin = plugin;
	}

	public static File resource(String resourcePath) {
		if (plugin == null) {
			throw new IllegalStateException("You needed to initialize the `blowdryer` plugin in the root build.gradle first.");
		}
		if (plugin instanceof DevPlugin) {
			return new File(((DevPlugin) plugin).root, resourcePath);
		} else {
			return immutableUrl(plugin.toImmutableUrl(resourcePath));
		}
	}

	static final class DevPlugin implements ResourcePlugin {
		File root;

		public DevPlugin(File root) {
			this.root = Objects.requireNonNull(root);
		}

		@Override
		@Deprecated
		public final String toImmutableUrl(String resourcePath) {
			throw new UnsupportedOperationException();
		}
	}
}
