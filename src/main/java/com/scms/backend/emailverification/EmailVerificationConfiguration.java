package com.scms.backend.emailverification;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties({EmailVerificationProperties.class, EmailDeliveryProperties.class})
class EmailVerificationConfiguration { }
