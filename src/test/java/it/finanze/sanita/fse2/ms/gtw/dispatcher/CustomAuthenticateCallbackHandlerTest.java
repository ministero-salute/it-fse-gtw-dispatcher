/*
 * SPDX-License-Identifier: AGPL-3.0-or-later
 * 
 * Copyright (C) 2023 Ministero della Salute
 * 
 * This program is free software: you can redistribute it and/or modify it under the terms of the GNU Affero General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU Affero General Public License for more details.
 * 
 * You should have received a copy of the GNU Affero General Public License along with this program. If not, see <https://www.gnu.org/licenses/>.
 */
package it.finanze.sanita.fse2.ms.gtw.dispatcher;

import com.microsoft.aad.msal4j.ClientCredentialParameters;
import com.microsoft.aad.msal4j.ConfidentialClientApplication;
import com.microsoft.aad.msal4j.IAuthenticationResult;
import it.finanze.sanita.fse2.ms.gtw.dispatcher.config.kafka.oauth2.CustomAuthenticateCallbackHandler;
import it.finanze.sanita.fse2.ms.gtw.dispatcher.exceptions.BusinessException;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.common.security.oauthbearer.OAuthBearerTokenCallback;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import javax.security.auth.callback.Callback;
import javax.security.auth.callback.UnsupportedCallbackException;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

import static it.finanze.sanita.fse2.ms.gtw.dispatcher.config.Constants.Profile.TEST;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.TestInstance.Lifecycle.PER_CLASS;
import static org.mockito.Mockito.*;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT;

@Slf4j
@SpringBootTest(webEnvironment = RANDOM_PORT)
@ActiveProfiles(TEST)
@TestInstance(PER_CLASS)
class CustomAuthenticateCallbackHandlerTest {

    @Mock
    private ConfidentialClientApplication mockClientApp;

    @Mock
    private IAuthenticationResult mockAuthResult;

    @InjectMocks
    private CustomAuthenticateCallbackHandler handler;

    @BeforeEach
    void setup() {
        Map<String, Object> configs = new HashMap<>();
        configs.put("bootstrap.servers", "localhost:8080");
        configs.put("kafka.oauth.tenantId", "my-tenant");
        configs.put("kafka.oauth.appId", "my-app-id");
        configs.put("kafka.oauth.pfxPathName", "/dummy/path/to/cert.pfx");
        configs.put("kafka.oauth.pwd", "changeit");

        handler.configure(configs, "OAUTHBEARER", Collections.emptyList());
    }

    @Test
    void testHandleSuccess() throws Exception {
        CompletableFuture<IAuthenticationResult> future = new CompletableFuture<>();
        future.complete(mockAuthResult);
        when(mockAuthResult.accessToken()).thenReturn("fake-token");
        when(mockAuthResult.expiresOnDate()).thenReturn(new Date(System.currentTimeMillis() + 3600_000));
        when(mockClientApp.acquireToken(Mockito.any(ClientCredentialParameters.class))).thenReturn(future);

        OAuthBearerTokenCallback callback = mock(OAuthBearerTokenCallback.class);

        handler.handle(new Callback[]{callback});

        Mockito.verify(callback, times(1)).token(any());
    }

    @Test
    void testHandleUnsupportedCallback() {
        Callback unsupported = mock(Callback.class);

        assertThrows(UnsupportedCallbackException.class, () -> handler.handle(new Callback[]{unsupported}));
    }

    @Test
    void testHandleInterruptedException() throws Exception {
        CompletableFuture<IAuthenticationResult> future = mock(CompletableFuture.class);
        when(future.get()).thenThrow(new InterruptedException());
        when(mockClientApp.acquireToken(any(ClientCredentialParameters.class))).thenReturn(future);

        OAuthBearerTokenCallback callback = mock(OAuthBearerTokenCallback.class);
        assertThrows(BusinessException.class, () -> handler.handle(new Callback[]{callback}));
    }

}

