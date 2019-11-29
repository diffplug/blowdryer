package com.diffplug.blowdryer;

import org.gradle.api.Plugin;
import org.gradle.api.Project;

public class BlowdryerPlugin implements Plugin<Project> {
	@Override
	public void apply(Project project) {
		project.getExtensions().create(BlowdryerExtension.NAME, BlowdryerExtension.class);
	}
}
