package dev.mobile.rlhd.config;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public enum MoonBehavior {
	DISABLED("Disabled"),
	REALISTIC("Realistic orbit"),
	MIRRORED("Mirror the sun"),
	STATIC("Static"),
	;

	private final String name;

	@Override
	public String toString() {
		return name;
	}
}
