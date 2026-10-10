package dev.mobile.rlhd.config;

import lombok.AllArgsConstructor;

@AllArgsConstructor
public enum InfernalCape {
	HD("117 HD"),
	LEGACY("Legacy"),
	VANILLA("Vanilla");

	private final String name;

	@Override
	public String toString() {
		return name;
	}
}
