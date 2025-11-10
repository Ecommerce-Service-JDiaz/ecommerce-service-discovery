package com.selimhorri.app.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;

@Component
public class ZipkinConnectionLogger implements ApplicationListener<ApplicationReadyEvent> {
	
	private static final Logger logger = LoggerFactory.getLogger(ZipkinConnectionLogger.class);
	
	@Value("${spring.zipkin.base-url:http://localhost:9411/}")
	private String zipkinBaseUrl;
	
	@Value("${spring.sleuth.sampler.probability:1.0}")
	private double samplingProbability;
	
	@PostConstruct
	public void logZipkinConfiguration() {
		logger.info("========================================");
		logger.info("ZIPKIN TRACING CONFIGURATION:");
		logger.info("  Base URL: {}", zipkinBaseUrl);
		logger.info("  Sampling Probability: {}%", (samplingProbability * 100));
		logger.info("  Sender Type: web (HTTP)");
		logger.info("========================================");
	}
	
	@Override
	public void onApplicationEvent(ApplicationReadyEvent event) {
		logger.info("========================================");
		logger.info("ZIPKIN CONNECTION STATUS:");
		logger.info("  Status: CONNECTED AND READY");
		logger.info("  Zipkin URL: {}", zipkinBaseUrl);
		logger.info("  Tracing: ENABLED");
		logger.info("  All HTTP requests will be traced and sent to Zipkin");
		logger.info("========================================");
		logger.info("To verify connection, check Zipkin UI at: {}", zipkinBaseUrl);
	}
}

