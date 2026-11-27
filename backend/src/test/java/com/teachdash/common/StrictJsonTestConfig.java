package com.teachdash.common;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Import;

/** Brings the application's strict JSON mapper configuration into a slice test. */
@TestConfiguration(proxyBeanMethods = false)
@Import({
		StrictStringJsonConfig.class,
		StrictIntegerJsonConfig.class,
		StrictBooleanJsonConfig.class,
		NulRejectingJsonConfig.class
})
public class StrictJsonTestConfig {
}
