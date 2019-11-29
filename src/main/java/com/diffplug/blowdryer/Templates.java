package com.diffplug.blowdryer;

public enum Templates {
	java8,
	eclipse,
	spotlessJava;

	public String extension() {
		return ".gradle";
	}
}
