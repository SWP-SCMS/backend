package com.scms.backend.support;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(InitialManagerProperties.class)
class InitialManagerConfiguration {
}
