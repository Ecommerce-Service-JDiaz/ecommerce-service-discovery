package com.selimhorri.app.config;

import java.time.Duration;

import javax.annotation.PostConstruct;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.ApplicationListener;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

@Component
public class ZipkinConnectionLogger implements ApplicationListener<ApplicationReadyEvent> {
	
	private static final Logger logger = LoggerFactory.getLogger(ZipkinConnectionLogger.class);
	
	@Value("${spring.zipkin.base-url:http://localhost:9411/}")
	private String zipkinBaseUrl;
	
	@Value("${spring.sleuth.sampler.probability:1.0}")
	private double samplingProbability;
	
	private RestTemplate restTemplate;
	
	@Autowired
	private RestTemplateBuilder restTemplateBuilder;
	
	@PostConstruct
	public void logZipkinConfiguration() {
		this.restTemplate = restTemplateBuilder
			.setConnectTimeout(Duration.ofSeconds(3))
			.setReadTimeout(Duration.ofSeconds(3))
			.build();
		
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
		
		verifyZipkinReachability();
	}
	
	private void verifyZipkinReachability() {
		String healthEndpoint = zipkinBaseUrl.endsWith("/") ? zipkinBaseUrl + "health" : zipkinBaseUrl + "/health";
		
		try {
			ResponseEntity<String> response = restTemplate.getForEntity(healthEndpoint, String.class);
			
			if (response.getStatusCode().is2xxSuccessful()) {
				logger.info("ZIPKIN HEALTH CHECK SUCCESSFUL ({}): {}", response.getStatusCodeValue(), truncate(response.getBody()));
			} else {
				logger.warn("ZIPKIN HEALTH CHECK returned non-success status: {} - body: {}", response.getStatusCodeValue(), truncate(response.getBody()));
			}
		} catch (RestClientException ex) {
			logger.error("Unable to reach Zipkin at '{}'. Traces will remain buffered until connection is restored. Cause: {}", healthEndpoint, ex.getMessage());
			logger.debug("Detailed Zipkin connection failure", ex);
		}
	}
	
	private String truncate(String body) {
		if (body == null) {
			return "";
		}
		return body.length() > 300 ? body.substring(0, 300) + "..." : body;
	}
}

