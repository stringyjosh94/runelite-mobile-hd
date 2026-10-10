package dev.mobile.rlhd.config;

public enum DefaultBoolean {
	DEFAULT,
	ON,
	OFF;

	public boolean get(boolean defaultState) {
		return this == DEFAULT ? defaultState : this == ON;
	}
}
