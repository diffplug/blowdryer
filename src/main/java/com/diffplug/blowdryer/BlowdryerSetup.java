/*
 * Copyright 2020 DiffPlug
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

/** Configures where {@link Blowdryer#file(String)} downloads files from. */
public class BlowdryerSetup {
	static final String NAME = "blowdryerSetup";

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
	public void github(String repoOrg, GitAnchorType anchorType, String anchor) {
		assertNoLeadingOrTrailingSlash(repoOrg);
		assertNoLeadingOrTrailingSlash(anchor);
		String root = "https://raw.githubusercontent.com/" + repoOrg + "/" + anchor + "/" + repoSubfolder + "/";
		Blowdryer.setResourcePlugin(resource -> root + resource);
	}

	/** Sets the source where we will grab these scripts. */
	public void gitlab(String repoOrg, GitAnchorType anchorType, String anchor) {
		assertNoLeadingOrTrailingSlash(repoOrg);
		assertNoLeadingOrTrailingSlash(anchor);
		Blowdryer.setResourcePlugin(resource -> "https://gitlab.com/api/v4/projects/"
				+ encodeUrlPart(repoOrg) + "/repository/files/"
				+ encodeUrlPart((repoSubfolder.isEmpty() ? "" : repoSubfolder + "/") + resource) + "/raw?ref="
				+ encodeUrlPart(anchor));
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
