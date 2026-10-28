package com.scms.backend.common.validation;

import java.util.regex.Pattern;

public final class PersonNameRules {
	private static final String TOKEN = "(?:\\p{L}\\p{M}*)+";
	public static final String PATTERN = TOKEN + "(?:(?:\\p{Zs}+|[\\p{Pd}'’.]\\p{Zs}*)" + TOKEN + ")*";
	private static final Pattern COMPILED = Pattern.compile("^" + PATTERN + "$");

	private PersonNameRules() { }

	public static boolean isValid(String value) {
		return value != null && COMPILED.matcher(value).matches();
	}
}
