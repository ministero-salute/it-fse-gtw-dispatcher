package it.finanze.sanita.fse2.ms.gtw.dispatcher.config;

import static it.finanze.sanita.fse2.ms.gtw.dispatcher.config.Constants.Properties.MS_NAME;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import io.opentelemetry.api.GlobalOpenTelemetry;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.sdk.autoconfigure.AutoConfiguredOpenTelemetrySdk;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Configuration
public class OpenTelemetryConfig {

	@Autowired(required = false)
	private AutoConfiguredOpenTelemetrySdk autoConfiguredOpenTelemetrySdk;

	@PostConstruct
	public void init() {
		if (autoConfiguredOpenTelemetrySdk != null) {
			try {
				GlobalOpenTelemetry.set(autoConfiguredOpenTelemetrySdk.getOpenTelemetrySdk());
			} catch (IllegalStateException e) {
				log.debug("GlobalOpenTelemetry already set (likely a second Spring context in tests), skipping.", e);
			}
		}
	}

	@Bean
	public Tracer tracer() {
		return GlobalOpenTelemetry.getTracer(MS_NAME);
	}
}
