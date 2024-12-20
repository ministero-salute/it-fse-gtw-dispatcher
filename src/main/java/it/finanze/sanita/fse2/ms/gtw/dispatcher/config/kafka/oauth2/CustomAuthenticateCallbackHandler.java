//Copyright (c) Microsoft Corporation. All rights reserved.
//Licensed under the MIT License.

package it.finanze.sanita.fse2.ms.gtw.dispatcher.config.kafka.oauth2;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URI;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;

import javax.security.auth.callback.Callback;
import javax.security.auth.callback.UnsupportedCallbackException;
import javax.security.auth.login.AppConfigurationEntry;

import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.KafkaException;
import org.apache.kafka.common.security.auth.AuthenticateCallbackHandler;
import org.apache.kafka.common.security.oauthbearer.OAuthBearerToken;
import org.apache.kafka.common.security.oauthbearer.OAuthBearerTokenCallback;

import com.microsoft.aad.msal4j.ClientCredentialFactory;
import com.microsoft.aad.msal4j.ClientCredentialParameters;
import com.microsoft.aad.msal4j.ConfidentialClientApplication;
import com.microsoft.aad.msal4j.IAuthenticationResult;
import com.microsoft.aad.msal4j.IClientCredential;

import it.finanze.sanita.fse2.ms.gtw.dispatcher.exceptions.BusinessException;
import it.finanze.sanita.fse2.ms.gtw.dispatcher.utility.FileUtility;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class CustomAuthenticateCallbackHandler implements AuthenticateCallbackHandler {

    private String tenantId;
	
    private String appId;
	
    private String pfxPathName;
    
    private String pwd;
	
    private ConfidentialClientApplication aadClient;
    private ClientCredentialParameters aadParameters;

    @Override
    public void configure(Map<String, ?> configs, String mechanism, List<AppConfigurationEntry> jaasConfigEntries) {
        String bootstrapServer = Arrays.asList(configs.get(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG)).get(0).toString();
       
        bootstrapServer = bootstrapServer.replaceAll("\\[|\\]", "");
        URI uri = URI.create("https://" + bootstrapServer);
        String sbUri = uri.getScheme() + "://" + uri.getHost();
        this.aadParameters =
                ClientCredentialParameters.builder(Collections.singleton(sbUri + "/.default"))
                .build();
        this.tenantId = "https://login.microsoftonline.com/"+ Arrays.asList(configs.get("kafka.oauth.tenantId")).get(0).toString();
        this.appId = Arrays.asList(configs.get("kafka.oauth.appId")).get(0).toString();
        this.pfxPathName = Arrays.asList(configs.get("kafka.oauth.pfxPathName")).get(0).toString();
        this.pwd = Arrays.asList(configs.get("kafka.oauth.pwd")).get(0).toString();

    }

    public void handle(Callback[] callbacks) throws IOException, UnsupportedCallbackException {
        for (Callback callback: callbacks) {
            if (callback instanceof OAuthBearerTokenCallback) {
                try {
                    OAuthBearerToken token = getOAuthBearerToken();
                    OAuthBearerTokenCallback oauthCallback = (OAuthBearerTokenCallback) callback;
                    oauthCallback.token(token);
                } catch (InterruptedException e){
                    Thread.currentThread().interrupt();
                    throw new BusinessException("Thread was interrupted while handling callback", e);
                } catch (ExecutionException | TimeoutException e) {
                    throw new BusinessException("Error while handling callback");
                }
            } else {
                throw new UnsupportedCallbackException(callback);
            }
        }
    }

    private OAuthBearerToken getOAuthBearerToken() throws MalformedURLException, InterruptedException, ExecutionException, TimeoutException {
        if (this.aadClient == null) {
            synchronized(this) {
                	IClientCredential credential = null;
            	    try (FileInputStream certificato = new FileInputStream(new File(pfxPathName))) {
                		credential = ClientCredentialFactory.createFromCertificate(certificato, this.pwd);	
                	} catch(Exception ex) {
                		log.error("Error while try to crate credential from certificate");
                		throw new BusinessException(ex);
                	}
                    this.aadClient = ConfidentialClientApplication.builder(this.appId, credential)
                            .authority(this.tenantId)
                            .build();
            }
        }

        IAuthenticationResult authResult = this.aadClient.acquireToken(this.aadParameters).get();
        log.info("Token oauth2 acquired");
        return new OAuthBearerTokenImp(authResult.accessToken(), authResult.expiresOnDate());
    }

    public void close() throws KafkaException {
        // NOOP
    }
}