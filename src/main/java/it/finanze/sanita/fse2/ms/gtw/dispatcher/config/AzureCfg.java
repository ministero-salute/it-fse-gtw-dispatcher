package it.finanze.sanita.fse2.ms.gtw.dispatcher.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;

@Configuration
@Slf4j
@Data
public class AzureCfg {

	@Value("${azure.kms.tenant-id}")
	private String tenantId;

	@Value("${azure.kms.client-id}")
	private String clientId;

	@Value("${azure.kms.client-secret}")
	private String clientSecret;

	@Value("${azure.kms.master-key-name}")
	private String masterKeyName;

	@Value("${azure.kms.key-vault-endpoint}")
	private String keyVaultEndpoint;
	
	@Value("${azure.kms.secret-user}")
	private String secretUser;

	@Value("${azure.kms.secret-pass}")
	private String secretPass;
	
	@Value("${azure.kms.key-vault-certificate-path}")
	private String keyVaultCertificatePath;
	
	@Value("${azure.kms.key-vault-certificate-pass}")
	private String keyVaultCertificatePass;
}
