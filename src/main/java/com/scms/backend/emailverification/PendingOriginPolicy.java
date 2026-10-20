package com.scms.backend.emailverification;

import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
class PendingOriginPolicy {
	private final Set<String> allowed;
	PendingOriginPolicy(@Value("${scms.cors.allowed-origins}") String origins){allowed=Arrays.stream(origins.split(","))
		.map(String::trim).filter(value->!value.isEmpty()).collect(Collectors.toUnmodifiableSet());}
	boolean allows(String origin){return origin!=null&&allowed.contains(origin);}
}
