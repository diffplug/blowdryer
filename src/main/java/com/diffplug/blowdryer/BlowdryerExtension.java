package com.diffplug.blowdryer;

import java.io.File;
import java.util.Arrays;
import java.util.Collection;
import java.util.Objects;

import org.gradle.api.Project;

import com.diffplug.common.base.Errors;

public class BlowdryerExtension {
	static final String NAME = "blowdryer";

	private final Project project;

	public BlowdryerExtension(Project project) {
		this.project = Objects.requireNonNull(project);
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
		AsFile.assertPluginNotSet("You have to call `repoSubfolder` first.");
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
		AsFile.setResourcePlugin(resource -> {
			return root + resource;
		});
	}

	/** Sets the source to be the given local folder, usually for developing changes before they are pushed to git. */
	public void devLocal(Object devPath) {
		File projectRoot = Errors.rethrow().get(() -> project.file(devPath).getCanonicalFile());
		File resourceRoot = new File(projectRoot, repoSubfolder);
		AsFile.setResourcePlugin(new AsFile.DevPlugin(resourceRoot));
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

	public void applyFrom(String... scripts) {
		applyFrom(Arrays.asList(scripts));
	}

	public void applyFrom(Collection<String> scripts) {
		for (String script : scripts) {
			project.apply(cfg -> {
				cfg.from(AsFile.resource(script));
			});
		}
	}
}
