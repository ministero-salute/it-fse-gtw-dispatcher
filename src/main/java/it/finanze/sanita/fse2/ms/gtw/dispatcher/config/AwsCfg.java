package it.finanze.sanita.fse2.ms.gtw.dispatcher.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;

@Configuration
@Slf4j
@Data
public class AwsCfg {

	@Value("${aws.region:#{null}}")
	private String region;

	@Value("${aws.arn.master-key-name:#{null}}")
	private String masterKeyArn;
}
