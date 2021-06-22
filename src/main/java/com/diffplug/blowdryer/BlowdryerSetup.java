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


import com.diffplug.common.annotations.VisibleForTesting;
import com.diffplug.common.base.Errors;
import com.google.gson.Gson;
import groovy.lang.Closure;
import java.io.File;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Base64;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

import javax.annotation.Nullable;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Request.Builder;
import okhttp3.Response;
import okhttp3.ResponseBody;
import org.jetbrains.annotations.NotNull;

/** Configures where {@link Blowdryer#file(String)} downloads files from. */
public class BlowdryerSetup {
	static final String NAME = "blowdryerSetup";

	private static final String GITHUB_HOST = "raw.githubusercontent.com";
	private static final String GITLAB_HOST = "gitlab.com";
	private static final String BITBUCKET_HOST = "api.bitbucket.org/2.0/repositories";

	private static final String HTTP_PROTOCOL = "http://";
	private static final String HTTPS_PROTOCOL = "https://";

	private final File referenceDirectory;

	/** Pass in the directory that will be used to resolve string arguments to devLocal. */
	public BlowdryerSetup(File referenceDirectory) {
		Blowdryer.setResourcePluginNull(); // because of gradle daemon
		this.referenceDirectory = referenceDirectory;
	}

	private static final String REPO_SUBFOLDER_DEFAULT = "src/main/resources";
	private String repoSubfolder = REPO_SUBFOLDER_DEFAULT;

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
			String root = HTTPS_PROTOCOL + GITHUB_HOST + "/" + repoOrg + "/" + anchor + "/";
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
			return customProtocolAndDomain(HTTP_PROTOCOL, domain);
		}

		public GitLab customDomainHttps(String domain) {
			return customProtocolAndDomain(HTTPS_PROTOCOL, domain);
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

	public enum BitbucketType {
		CLOUD, SERVER
	}

	/** Sets the source where we will grab these scripts. */
	public Bitbucket bitbucket(String repoOrg, GitAnchorType anchorType, String anchor) {
		return new Bitbucket(repoOrg, anchorType, anchor, BitbucketType.CLOUD);
	}

	public class Bitbucket {

		private String repoOrg;
		private String repoName;
		private String anchor;
		private GitAnchorType anchorType;
		private BitbucketType bitbucketType;
		private @Nullable String authToken;
		private String protocol, host;

		private Bitbucket(String repoOrg, GitAnchorType anchorType, String anchor, BitbucketType bitbucketType) {
			Blowdryer.assertPluginNotSet();
			final String[] repoOrgAndName = assertNoLeadingOrTrailingSlash(repoOrg).split("/");
			if (repoOrgAndName.length != 2) {
				throw new IllegalArgumentException("repoOrg must be in format 'repoOrg/repoName'");
			}
			this.repoOrg = repoOrgAndName[0];
			this.repoName = repoOrgAndName[1];
			this.anchorType = anchorType;
			this.bitbucketType = bitbucketType;
			this.anchor = assertNoLeadingOrTrailingSlash(anchor);
			customDomainHttps(BITBUCKET_HOST);
		}

		/**
		 * Only supported for Bitbucket Server 5.5+
		 * Format: "personalAccessToken"
		 */
		public Bitbucket server() {
			this.bitbucketType = BitbucketType.SERVER;
			return setGlobals();
		}

		public Bitbucket serverAuth(String personalAccessToken) {
			this.authToken = String.format("Bearer %s", personalAccessToken);
			this.bitbucketType = BitbucketType.SERVER;
			return setGlobals();
		}

		/**
		 * Only available for Bitbucket Cloud.
		 * Format: "username:appPassword"
		 */
		public Bitbucket cloudAuth(String usernameAndAppPassword) {
			final String encoding = Base64.getEncoder().encodeToString((usernameAndAppPassword)
          .getBytes(StandardCharsets.UTF_8));
			this.authToken = String.format("Basic %s", encoding);
			this.bitbucketType = BitbucketType.CLOUD;
			return setGlobals();
		}

		public Bitbucket customDomainHttp(String domain) {
			return customProtocolAndDomain(HTTP_PROTOCOL, domain);
		}

		public Bitbucket customDomainHttps(String domain) {
			return customProtocolAndDomain(HTTPS_PROTOCOL, domain);
		}

		private Bitbucket customProtocolAndDomain(String protocol, String domain) {
			this.protocol = protocol;
			this.host = domain;
			return setGlobals();
		}

		private Bitbucket setGlobals() {
			Blowdryer.setResourcePluginNull();
			String urlStart = getUrlStart();

			Blowdryer.setResourcePlugin(resource -> getFullUrl(urlStart, encodeUrlParts(getFullResourcePath(resource))), (url, builder) -> {
				if (authToken != null) {
					builder.addHeader("Authorization", authToken);
				}
			});
			return this;
		}

		// Bitbucket Cloud and Bitbucket Server (premium, company hosted) has different url structures.
		// Bitbucket Cloud uses "org/repo" in URLs, where org is your (or someone else's) account name.
		// Bitbucket Server uses "projects/PROJECT_KEY/repos/REPO_NAME" in urls.

		private String getUrlStart() {
			if (isServer()) {
				return String.format("%s%s/projects/%s/repos/%s", protocol, host, repoOrg, repoName);
			} else {
				return String.format("%s%s/%s/%s", protocol, host, repoOrg, repoName);
			}
		}

		private String getFullUrl(String urlStart, String filePath) {
			if (isServer()) {
				return String.format("%s/raw/%s?at=%s", urlStart, filePath, encodeUrlPart(getAnchor()));
			} else {
				return String.format("%s/src/%s/%s", urlStart, encodeUrlParts(getAnchorForCloudAsHash()), filePath);
			}
		}

		private boolean isServer() {
			return BitbucketType.SERVER.equals(this.bitbucketType);
		}

		private String getAnchor() {
			switch (anchorType) {
				case COMMIT:
					return anchor;
				case TAG:
					return String.format("refs/tags/%s", anchor);
				default:
					throw new UnsupportedOperationException(String.format("%s hash resolution is not supported.", anchorType));
			}
		}

    private String getAnchorForCloudAsHash() {
      switch (anchorType) {
				case COMMIT:
					return anchor;
        case TAG:
          anchor = getCommitHash("refs/tags/");
					anchorType = GitAnchorType.COMMIT;
					return anchor;
        default:
					throw new UnsupportedOperationException("TREE hash resolution is not supported.");
      }
    }

		// Bitbucket API: https://developer.atlassian.com/bitbucket/api/2/reference/resource/repositories/%7Bworkspace%7D/%7Brepo_slug%7D/src/%7Bcommit%7D/%7Bpath%7D
		private String getCommitHash(String baseRefs) {
			String requestUrl = String.format("%s/%s%s", getUrlStart(), baseRefs, encodeUrlParts(anchor));

      return getCommitHashFromBitbucket(requestUrl);
    }

    @VisibleForTesting
		String getCommitHashFromBitbucket(String requestUrl) {
			OkHttpClient client = new OkHttpClient.Builder().build();
			Builder requestBuilder = new Builder()
				.url(requestUrl);
			if (authToken != null) {
				requestBuilder
					.addHeader("Authorization", authToken);
			}
			Request request = requestBuilder.build();

			try (Response response = client.newCall(request).execute()) {
				if (!response.isSuccessful()) {
					throw new IllegalArgumentException(String.format("%s\nreceived http code %s \n %s", request.url(), response.code(),
						Objects.requireNonNull(response.body()).string()));
				}
				try (ResponseBody body = response.body()) {
					RefsTarget refsTarget = new Gson().fromJson(Objects.requireNonNull(body).string(), RefsTarget.class);
					return refsTarget.target.hash;
				}
			} catch (Exception e) {
				throw new IllegalArgumentException("Body was expected to be non-null", e);
			}
		}

		// Do not encode '/'.
		private String encodeUrlParts(String part) {
			return Arrays.stream(part.split("/"))
				.map(BlowdryerSetup::encodeUrlPart)
				.collect(Collectors.joining("/"));
		}

		private class RefsTarget {

		    private final Target target;

        private RefsTarget(Target target) {
            this.target = target;
        }

        private class Target {

		        private final String hash;

            private Target(String hash) {
                this.hash = hash;
            }

        }
    }
  }

	/**
	 * Uses the provided {@code jarFile} to extract a file resource.
	 * @param jarFile Absolute path to JAR on the file system.
	 */
	public void localJar(File jarFile) {
		Objects.requireNonNull(jarFile, "jarFile must not be null.");
		Blowdryer.setResourcePluginNull();
		if (!repoSubfolder.equals(REPO_SUBFOLDER_DEFAULT)) {
			throw new IllegalArgumentException("repoSubfolder has no effect when reading from a jar, delete the call to repoSubfolder.");
		}

		String rootUrl = "file:///" + jarFile.getAbsolutePath() + "!/";
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
