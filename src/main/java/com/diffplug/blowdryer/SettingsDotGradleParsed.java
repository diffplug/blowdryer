/*
 * Copyright (C) 2023 DiffPlug
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

public class SettingsDotGradleParsed {
	boolean isWindowsNewline;
	String beforePlugins;
	String inPlugins;
	String afterPlugins;

	private static final String PLUGINS_OPEN = "\nplugins {\n";
	private static final String PLUGINS_CLOSE = "\n}\n";

	private static String escape(String input) {
		return input.replace("\n", "⏎");
	}

	SettingsDotGradleParsed(String dirty) {
		isWindowsNewline = dirty.indexOf("\r\n") != -1;
		String unix = isWindowsNewline ? dirty.replace("\r\n", "\n") : dirty;

		int pluginsStart = unix.indexOf(PLUGINS_OPEN);
		if (pluginsStart == -1) {
			throw new IllegalArgumentException("Couldn't find " + escape(PLUGINS_OPEN));
		}
		beforePlugins = unix.substring(0, pluginsStart);
		int pluginsEnd = unix.indexOf("\n}\n", pluginsStart + PLUGINS_OPEN.length());
		if (pluginsEnd == -1) {
			throw new IllegalArgumentException("Couldn't find " + escape(PLUGINS_CLOSE) + " after " + escape(PLUGINS_OPEN));
		}
		inPlugins = unix.substring(pluginsStart + PLUGINS_OPEN.length(), pluginsEnd);
		afterPlugins = unix.substring(pluginsEnd + PLUGINS_CLOSE.length());
	}

	public String inPluginsUnix() {
		return inPlugins;
	}

	public String contentUnix() {
		return beforePlugins + PLUGINS_OPEN + inPlugins + PLUGINS_CLOSE + afterPlugins;
	}

	public String contentCorrectEndings() {
		return isWindowsNewline ? contentUnix().replace("\n", "\r\n") : contentUnix();
	}

	public void setPluginContent(String desiredContent) {
		inPlugins = desiredContent;
	}
}
