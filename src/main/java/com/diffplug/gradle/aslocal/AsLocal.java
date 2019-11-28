package com.diffplug.gradle.aslocal;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

import org.gradle.api.Project;
import org.gradle.internal.impldep.com.google.common.hash.Hashing;

public class AsLocal {
	private static Map<String, File> urlToContent = new HashMap<>();

	public static File url(Project p, String url) throws IOException {
		synchronized (AsLocal.class) {
			File result = urlToContent.get(url);
			if (result != null) {
				return result;
			}
		}
		File asBuildDir = new File(p.getRootProject().getBuildDir(), "aslocal");
		Files.createDirectories(asBuildDir.toPath());
	}

	private static final int MAX_FILE_LENGTH = 200;
	private static final int ABBREVIATED = 70;

	static String filenameSafe(String url) {
		String allSafeCharacters = url.replaceAll("[^a-zA-Z0-9-_\\.]", "-");
		String noDuplicateDash = url.replaceAll("-+", "-");
		if (noDuplicateDash.length() > MAX_FILE_LENGTH) {
			byte[] hash = Hashing.sha256()
				.hashString(url, StandardCharsets.UTF_8)
				.asBytes();
			String hashed = Base64.getEncoder().encodeToString(hash);
			return noDuplicateDash.substring(0, ABBREVIATED) + noDuplicateDash()
		}
	}
}
