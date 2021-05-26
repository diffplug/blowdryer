/*
 * Copyright (C) 2019-2021 DiffPlug
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
import groovy.lang.Closure;
import java.io.File;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.Objects;
import java.util.function.Function;
import javax.annotation.Nullable;
import org.jetbrains.annotations.NotNull;

/** Configures where {@link Blowdryer#file(String)} downloads files from. */
public class BlowdryerSetup {
	static final String NAME = "blowdryerSetup";

	private static final String GITHUB_HOST = "raw.githubusercontent.com";
	private static final String GITLAB_HOST = "gitlab.com";

	private final File referenceDirectory;

	/** Pass in the directory that will be used to resolve string arguments to devLocal. */
	public BlowdryerSetup(File referenceDirectory) {
		Blowdryer.setResourcePluginNull(); // because of gradle daemon
		this.referenceDirectory = referenceDirectory;
	}

	private String repoSubfolder = "src/main/resources";

	/**
	 * Default value is `src/main/resources`.  If you change, you must change as the *first* call.
	 * 
	 * The nice thing about the default `src/main/resources` is that if you ever want to, you could
	 * copy the blowdryer code into your blowdryer repo, and deploy your own plugin that pulls resources
	 * from the local jar rather than from github.  Keeping the default lets you switch to that approach
	 * in the future without moving your scripts.
	 */
	public void repoSubfolder(String repoSubfolder) {
		Blowdryer.assertPluginNotSet("You have to call `repoSubfolder` first.");
		this.repoSubfolder = assertNoLeadingOrTrailingSlash(repoSubfolder);
	}

	public enum GitAnchorType {
		TAG, COMMIT, TREE
	}

	/** Sets the source where we will grab these scripts. */
	public GitHub github(String repoOrg, GitAnchorType anchorType, String anchor) {
		// anchorType isn't used right now, but makes it easier to read what "anchor" is
		return new GitHub(repoOrg, anchor);
	}

	public class GitHub {
		private String repoOrg;
		private String anchor;
		private @Nullable String authToken;

		private GitHub(String repoOrg, String anchor) {
			Blowdryer.assertPluginNotSet();
			this.repoOrg = assertNoLeadingOrTrailingSlash(repoOrg);
			this.anchor = assertNoLeadingOrTrailingSlash(anchor);
			setGlobals();
		}

		public GitHub authToken(String authToken) {
			this.authToken = authToken;
			return setGlobals();
		}

		private GitHub setGlobals() {
			Blowdryer.setResourcePluginNull();
			String root = "https://" + GITHUB_HOST + "/" + repoOrg + "/" + anchor + "/";
			Blowdryer.setResourcePlugin(resource -> root + getFullResourcePath(resource), authToken == null ? null : (url, builder) -> {
				if (url.startsWith(root)) {
					builder.addHeader("Authorization", "Bearer " + authToken);
				}
			});
			return this;
		}
	}

	/** Sets the source where we will grab these scripts. */
	public GitLab gitlab(String repoOrg, GitAnchorType anchorType, String anchor) {
		// anchorType isn't used right now, but makes it easier to read what "anchor" is
		return new GitLab(repoOrg, anchor);
	}

	public class GitLab {
		private String repoOrg;
		private String anchor;
		private @Nullable String authToken;
		private String protocol, host;

		private GitLab(String repoOrg, String anchor) {
			Blowdryer.assertPluginNotSet();
			this.repoOrg = assertNoLeadingOrTrailingSlash(repoOrg);
			this.anchor = assertNoLeadingOrTrailingSlash(anchor);
			customDomainHttps(GITLAB_HOST);
		}

		public GitLab authToken(String authToken) {
			this.authToken = authToken;
			return setGlobals();
		}

		public GitLab customDomainHttp(String domain) {
			return customProtocolAndDomain("http://", domain);
		}

		public GitLab customDomainHttps(String domain) {
			return customProtocolAndDomain("https://", domain);
		}

		private GitLab customProtocolAndDomain(String protocol, String domain) {
			this.protocol = protocol;
			this.host = domain;
			return setGlobals();
		}

		private GitLab setGlobals() {
			Blowdryer.setResourcePluginNull();
			String urlStart = protocol + host + "/api/v4/projects/" + encodeUrlPart(repoOrg) + "/repository/files/";
			String urlEnd = "/raw?ref=" + encodeUrlPart(anchor);
			Blowdryer.setResourcePlugin(resource -> urlStart + encodeUrlPart(getFullResourcePath(resource)) + urlEnd, authToken == null ? null : (url, builder) -> {
				if (url.startsWith(urlStart)) {
					builder.addHeader("Authorization", "Bearer " + authToken);
				}
			});
			return this;
		}
	}

	/**
	 * Uses the provided {@code jarFilePath} to extract a file resource.
	 * @param jarFilePath Absolute path to JAR on the file system.
	 */
	public void localJar(String jarFilePath) {
		Blowdryer.setResourcePluginNull();
		File localJar = new File(jarFilePath);//findLocalJar(dependency);
		String rootUrl = "file:///" + localJar.getAbsolutePath() + "!/";
		Blowdryer.setResourcePlugin(resource -> rootUrl + resource);
	}

	@NotNull
	private String getFullResourcePath(String resource) {
		return (repoSubfolder.isEmpty() ? "" : repoSubfolder + "/") + resource;
	}

	/** Sets the mapping from `file(String)` to `immutableUrl(String)`. */
	public void experimental(Closure<String> function) {
		experimental(function::call);
	}

	/** Sets the mapping from `file(String)` to `immutableUrl(String)`. */
	public void experimental(Function<String, String> function) {
		Blowdryer.setResourcePlugin(function::apply);
	}

	/** Sets the source to be the given local folder, usually for developing changes before they are pushed to git. */
	public void devLocal(Object devPath) {
		Objects.requireNonNull(devPath);
		File devPathFile;
		if (devPath instanceof File) {
			devPathFile = (File) devPath;
		} else if (devPath instanceof String) {
			devPathFile = new File(referenceDirectory, (String) devPath);
		} else {
			throw new IllegalArgumentException("Expected a String or File, was a " + devPath.getClass());
		}
		File projectRoot = Errors.rethrow().get(devPathFile::getCanonicalFile);
		File resourceRoot = new File(projectRoot, repoSubfolder);
		Blowdryer.setResourcePlugin(new Blowdryer.DevPlugin(resourceRoot));
	}

	private static String assertNoLeadingOrTrailingSlash(String input) {
		Objects.requireNonNull(input);
		if (input.isEmpty()) {
			return input;
		}
		if (input.charAt(0) == '/') {
			throw new IllegalArgumentException("Remove the leading slash");
		}
		if (input.charAt(input.length() - 1) == '/') {
			throw new IllegalArgumentException("Remove the trailing slash");
		}
		return input;
	}

	private static String encodeUrlPart(String part) {
		try {
			return URLEncoder.encode(part, "UTF-8");
		} catch (UnsupportedEncodingException e) {
			throw new IllegalArgumentException("error encoding part", e);
		}
	}
}
