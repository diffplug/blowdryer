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


import com.diffplug.common.base.Errors;
import com.diffplug.common.base.Preconditions;
import com.diffplug.common.hash.Hashing;
import com.diffplug.common.io.Files;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.Base64;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Properties;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import javax.annotation.Nullable;
import okhttp3.Interceptor;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;
import okio.BufferedSink;
import okio.Okio;
import org.gradle.api.Project;

/**
 * Public static methods which retrieve resources as
 * determined by {@link BlowdryerSetup}.
 */
public class Blowdryer {

	private static final String FILE_PROTOCOL = "file:///";
	private static final String JAR_FILE_RESOURCE_SEPARATOR = "!/";

	private Blowdryer() {}

	static void setTempDirNull() {
		cacheTempDir = null;
	}

	static void initTempDir(String tempDirPath) {
		File tempDir = new File(tempDirPath);
		if (cacheTempDir == null) {
			cacheTempDir = tempDir;
		} else {
			Preconditions.checkArgument(cacheTempDir.equals(tempDir));
		}
	}

	static File cacheDir() {
		Preconditions.checkArgument(cacheTempDir != null, "Call initTempDir first");
		return new File(cacheTempDir, "blowdryer-cache");
	}

	private static File cacheTempDir;
	private static final Map<String, File> urlToContent = new HashMap<>();
	private static final Map<File, Map<String, String>> fileToProps = new HashMap<>();

	static void wipeEntireCache() {
		synchronized (Blowdryer.class) {
			try {
				urlToContent.clear();
				fileToProps.clear();
				java.nio.file.Files.walk(cacheDir().toPath())
						.sorted(Comparator.reverseOrder())
						.forEach(Errors.rethrow().wrap((Path path) -> java.nio.file.Files.delete(path)));
			} catch (IOException e) {
				throw Errors.asRuntime(e);
			}
		}
	}

	/**
	 * Downloads the given url to a local file in the system temporary directory.
	 * It will only be downloaded once, system-wide, and it will not be checked for updates.
	 * This is appropriate only for immutable URLs, such as specific hashes from Git.
	 */
	public static File immutableUrl(String url) {
		return immutableUrl(url, null);
	}

	/**
	 * Downloads the given url to a local file in the system temporary directory.
	 * It will only be downloaded once, system-wide, and it will not be checked for updates.
	 * This is appropriate only for immutable URLs, such as specific hashes from Git.
	 *
	 * If requiredSuffix is non-null, it is guaranteed that the returned filename will end
	 * with that string.
	 */
	public static File immutableUrl(String url, @Nullable String requiredSuffix) {
		synchronized (Blowdryer.class) {
			String cacheKey = requiredSuffix == null ? url : url + "|" + requiredSuffix; // | is illegal in URLs
			File result = urlToContent.get(cacheKey);
			if (result != null && result.isFile()) {
				return result;
			}

			String safe = filenameSafe(url);
			if (requiredSuffix != null && !safe.endsWith(requiredSuffix)) {
				safe = safe + requiredSuffix;
			}
			File metaFile = new File(cacheDir(), "meta_" + safe + ".properties");
			File dataFile = new File(cacheDir(), safe);

			try {
				if (metaFile.exists() && dataFile.exists()) {
					Map<String, String> props = loadPropertyFile(metaFile);
					String propUrl = props.get(PROP_URL);
					if (propUrl == null) {
						throw new IllegalArgumentException("Unexpected content, recommend deleting file at " + metaFile);
					}
					if (propUrl.equals(url)) {
						urlToContent.put(cacheKey, dataFile);
						return dataFile;
					} else {
						throw new IllegalStateException("Expected url " + url + " but was " + propUrl + ", recommend deleting file at " + metaFile.getAbsolutePath());
					}
				} else {
					if (metaFile.exists()) {
						metaFile.delete();
					}
					if (dataFile.exists()) {
						dataFile.delete();
					}
					Files.createParentDirs(dataFile);
					download(url, dataFile);
					Properties props = new Properties();
					props.setProperty("version", "1");
					props.setProperty(PROP_URL, url);
					props.setProperty("downloadedAt", new Date().toString());
					try (OutputStream output = Files.asByteSink(metaFile).openBufferedStream()) {
						props.store(output, "");
					}
					urlToContent.put(cacheKey, dataFile);
					return dataFile;
				}
			} catch (IOException | URISyntaxException e) {
				throw Errors.asRuntime(e);
			}
		}
	}

	private static Map<String, String> loadPropertyFile(File file) throws IOException {
		Properties props = new Properties();
		try (InputStream input = Files.asByteSource(file).openBufferedStream()) {
			props.load(input);
		}
		Map<String, String> asMap = new HashMap<>(props.size());
		for (Map.Entry<Object, Object> entry : props.entrySet()) {
			asMap.put(entry.getKey().toString(), entry.getValue().toString());
		}
		return asMap;
	}

	private static final String PROP_URL = "url";

	private static void download(String url, File dst) throws IOException, URISyntaxException {
		if (url.startsWith(FILE_PROTOCOL)) {
			downloadLocal(url, dst);
		} else {
			downloadRemote(url, dst);
		}
	}

	private static void downloadLocal(String url, File dst) throws IOException, URISyntaxException {

		String[] splitUrl = url.split(JAR_FILE_RESOURCE_SEPARATOR);
		if (splitUrl.length != 2) {
			throw new IllegalArgumentException("Expected a file URL in the format: file:///path-to-dependency.jar!/path-to-file.ext");
		}

		String jarPath = splitUrl[0];
		String filename = splitUrl[1];

		URI jarPathUri = new URI(jarPath);
		try (ZipFile jar = new ZipFile(new File(jarPathUri))) {
			ZipEntry foundEntry = jar.stream()
					.filter(s -> s.getName().equals(filename)).findAny()
					.orElseThrow(() -> new FileNotFoundException("Could not find '" + filename + "' in '" + jarPath + "'"));

			java.nio.file.Files.copy(jar.getInputStream(foundEntry), dst.toPath());
		}
	}

	private static void downloadRemote(String url, File dst) throws IOException {
		OkHttpClient client = new OkHttpClient.Builder()
				.addInterceptor(rateLimitPlugin.createRateLimitInterceptor() == null ? new BlowdryerSetup.EmptyInterceptor() : rateLimitPlugin.createRateLimitInterceptor())
				.build();
		Request.Builder req = new Request.Builder().url(url);
		authPlugin.addAuthToken(url, req);
		try (Response response = client.newCall(req.build()).execute()) {
			if (!response.isSuccessful()) {
				throw new IllegalArgumentException(url + "\nreceived http code " + response.code() + "\n" + response.body().string());
			}
			try (ResponseBody body = response.body();
					BufferedSink sink = Okio.buffer(Okio.sink(dst))) {
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
		String allSafeCharacters = url.replaceAll("[^a-zA-Z0-9-+_.]", "-");
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
		if (Blowdryer.plugin != null) {
			throw new IllegalStateException(errorMessage);
		}
	}

	static void assertPluginNotSet() {
		assertPluginNotSet("You already initialized the `blowdryer` plugin, you can't do this twice.");
	}

	static void setResourcePluginNull() {
		synchronized (Blowdryer.class) {
			Blowdryer.plugin = null;
			Blowdryer.authPlugin = authPluginNone;
		}
	}

	static void setResourcePlugin(ResourcePlugin plugin) {
		setResourcePlugin(plugin, null, null);
	}

	static void setResourcePlugin(ResourcePlugin plugin, AuthPlugin authPlugin, RateLimitPlugin rateLimitPlugin) {
		synchronized (Blowdryer.class) {
			assertPluginNotSet();
			Blowdryer.plugin = plugin;
			Blowdryer.authPlugin = authPlugin == null ? authPluginNone : authPlugin;
			Blowdryer.rateLimitPlugin = rateLimitPlugin;
		}
	}

	private static void assertInitialized() {
		if (plugin == null) {
			throw new IllegalStateException("You needed to initialize the `blowdryer` plugin in the root build.gradle first.");
		}
	}

	@FunctionalInterface
	interface AuthPlugin {
		void addAuthToken(String url, Request.Builder builder) throws MalformedURLException;
	}

	private static final AuthPlugin authPluginNone = (url, builder) -> {};
	private static AuthPlugin authPlugin = authPluginNone;

	@FunctionalInterface
	interface RateLimitPlugin {
		Interceptor createRateLimitInterceptor();
	}

	private static RateLimitPlugin rateLimitPlugin;

	/** Returns the given resource as a File (as configured by {@link BlowdryerSetup}. */
	public static File file(String resourcePath) {
		synchronized (Blowdryer.class) {
			assertInitialized();
			if (plugin instanceof DevPlugin) {
				return new File(((DevPlugin) plugin).root, resourcePath);
			} else {
				int lastDot = resourcePath.lastIndexOf('.');
				String preserveExtension = lastDot == -1 ? null : resourcePath.substring(lastDot);
				return immutableUrl(plugin.toImmutableUrl(resourcePath), preserveExtension);
			}
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

	////////////////
	// Properties //
	////////////////
	/** Returns all of the properties from the given url. */
	private static Map<String, String> props(String resourcePath) {
		synchronized (Blowdryer.class) {
			try {
				assertInitialized();
				if (plugin instanceof DevPlugin) {
					return loadPropertyFile(file(resourcePath));
				} else {
					File file = file(resourcePath);
					Map<String, String> props = fileToProps.get(file);
					if (props != null) {
						return props;
					}
					props = loadPropertyFile(file);
					fileToProps.put(file, props);
					return props;
				}
			} catch (IOException e) {
				throw Errors.asRuntime(e);
			}
		}
	}

	/** Returns the key from the given propFile (adds .properties extension automatically). */
	public static String prop(String propFile, String key) throws IOException {
		Map<String, String> map = props(propFile + ".properties");
		String value = map.get(key);
		if (value == null) {
			throw new IllegalArgumentException(propFile + ".properties does not have key '" + key + "', does have " + map.keySet());
		}
		return value;
	}

	/**
	 * Reads a property from the project, and throws "Undefined 'key': descForError" if it is missing.
	 * Requires the property value to be a String.
	 */
	public static String proj(Project project, String key, String descForError) {
		return proj(project, String.class, key, descForError);
	}

	/**
	 * Reads a property from the project, and throws "Undefined 'key': descForError" if it is missing,
	 * or "Wrong type 'key': descForError - expected java.io.File but was java.io.String"
	 */
	@SuppressWarnings("unchecked")
	public static <T> T proj(Project project, Class<T> clazz, String key, String descForError) {
		T value = projOptional(project, clazz, key, descForError);
		if (value == null) {
			throw new IllegalArgumentException("Undefined '" + key + "': " + descForError +
					"\nset in gradle.properties: https://docs.gradle.org/current/userguide/build_environment.html#sec:gradle_configuration_properties" +
					"\nset in buildscript: https://docs.gradle.org/current/userguide/writing_build_scripts.html#sec:extra_properties" +
					"\nset in environment: https://docs.gradle.org/current/userguide/build_environment.html#sec:project_properties" +
					"\nexact search order: https://docs.gradle.org/current/javadoc/org/gradle/api/Project.html#findProperty-java.lang.String-");
		} else {
			return value;
		}
	}

	/**
	 * Reads a property from the project, returns null if it is not present.
	 */
	@SuppressWarnings("unchecked")
	public static @Nullable <T> T projOptional(Project project, Class<T> clazz, String key, String descForError) {
		Object value = project.findProperty(key);
		if (value == null) {
			return null;
		} else if (!(clazz.isInstance(value))) {
			throw new IllegalArgumentException("Wrong type '" + key + "': " + descForError + " - expected " + clazz + " but was " + value.getClass());
		} else {
			return (T) value;
		}
	}

	/** Alias for {@link #projOptional(Project, String, String)} with {@code String.class}. */
	public static @Nullable String projOptional(Project project, String key, String descForError) {
		return projOptional(project, String.class, key, descForError);
	}

	/** Alias for {@link Blowdryer} which fills in the `project` field of the `proj()` methods automatically. */
	public static class WithProject {
		private final Project project;

		public WithProject(Project project) {
			this.project = project;
		}

		/** Alias for {@link Blowdryer#immutableUrl(String)}. */
		public File immutableUrl(String url) {
			return Blowdryer.immutableUrl(url);
		}

		/** Alias for {@link Blowdryer#immutableUrl(String, String)}. */
		public File immutableUrl(String url, @Nullable String requiredSuffix) {
			return Blowdryer.immutableUrl(url, requiredSuffix);
		}

		/** Alias for {@link Blowdryer#file(String)}. */
		public File file(String resource) {
			return Blowdryer.file(resource);
		}

		/** Alias for {@link Blowdryer#prop(String, String)}. */
		public String prop(String propFile, String key) throws IOException {
			return Blowdryer.prop(propFile, key);
		}

		/** Alias for {@link Blowdryer#proj(Project, String, String)}. */
		public String proj(String key, String descForError) {
			return Blowdryer.proj(project, key, descForError);
		}

		/** Alias for {@link Blowdryer#proj(Project, Class, String, String)}. */
		public <T> T proj(Class<T> clazz, String key, String descForError) {
			return Blowdryer.proj(project, clazz, key, descForError);
		}

		/** Alias for {@link Blowdryer#proj(Project, String, String)}. */
		public @Nullable String projOptional(String key, String descForError) {
			return Blowdryer.projOptional(project, key, descForError);
		}

		/** Alias for {@link Blowdryer#proj(Project, Class, String, String)}. */
		public @Nullable <T> T projOptional(Class<T> clazz, String key, String descForError) {
			return Blowdryer.projOptional(project, clazz, key, descForError);
		}
	}
}
