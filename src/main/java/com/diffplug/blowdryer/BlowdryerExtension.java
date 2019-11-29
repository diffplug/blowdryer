package com.diffplug.blowdryer;

import java.io.File;
import java.security.CodeSource;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

import javax.annotation.Nullable;

import org.gradle.api.Project;

public class BlowdryerExtension {
	static final String NAME = "blowdryer";

	private final Project project;

	public BlowdryerExtension(Project project) {
		this.project = Objects.requireNonNull(project);
	}

	public void template(Templates template) {
		if (devBlowdryer == null) {
			CodeSource src = BlowdryerExtension.class.getProtectionDomain().getCodeSource();
			
		}
	}

	public void templates(Templates... templates) {
		templates(Arrays.asList(templates));
	}

	public void templates(List<Templates> templates) {
		templates.forEach(this::template);
	}

	public @Nullable File devBlowdryer;
}
