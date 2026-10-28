package com.scms.backend.emailverification;

import java.net.InetAddress;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class RailwayClientIpResolver {
	private final boolean trustProxyHeaders;

	public RailwayClientIpResolver(@Value("${scms.security.trust-railway-proxy-headers:false}") boolean trustProxyHeaders) {
		this.trustProxyHeaders = trustProxyHeaders;
	}

	public String resolve(HttpServletRequest request) {
		if (!trustProxyHeaders) return request.getRemoteAddr();
		String realIp = request.getHeader("X-Real-IP");
		if (realIp != null) {
			realIp = realIp.trim();
			if (isIpLiteral(realIp)) return realIp;
		}
		return request.getRemoteAddr();
	}

	private boolean isIpLiteral(String value) {
		if (value.isEmpty() || !value.matches("[0-9a-fA-F:.]+")
				|| (!value.contains(".") && !value.contains(":"))) return false;
		try {
			return InetAddress.getByName(value).getHostAddress() != null;
		} catch (Exception ignored) {
			return false;
		}
	}
}
