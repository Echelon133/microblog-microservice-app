package ml.echelon133.microblog.auth.service;

import ml.echelon133.microblog.auth.repository.OAuth2AuthorizationRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.OAuth2AccessToken;
import org.springframework.security.oauth2.core.OAuth2AuthorizationCode;
import org.springframework.security.oauth2.core.endpoint.OAuth2AuthorizationRequest;
import org.springframework.security.oauth2.core.endpoint.OAuth2AuthorizationResponseType;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClientRepository;
import org.springframework.test.context.TestPropertySource;

import java.util.*;

import static ml.echelon133.microblog.auth.service.AuthTestData.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.BDDMockito.given;

/*
    Disable kubernetes during tests to make local execution of tests possible.
    If kubernetes is not disabled, tests won't execute at all because Spring will
    fail to configure kubernetes when run outside it.
 */
@TestPropertySource(properties = "spring.cloud.kubernetes.enabled=false")
@ExtendWith(MockitoExtension.class)
@DisplayName("Tests of CustomOAuth2AuthorizationServiceTests")
public class CustomOAuth2AuthorizationServiceTests {

    @Mock
    private RegisteredClientRepository registeredClientRepository;

    @Mock
    private OAuth2AuthorizationRepository authorizationRepository;

    @InjectMocks
    private CustomOAuth2AuthorizationService authorizationService;

    @Test
    @DisplayName("findById returns null when authorization not found")
    public void findById_AuthorizationNotFound_ReturnsNull() {
        // given
        given(authorizationRepository.findById("test")).willReturn(Optional.empty());

        // when
        var auth = authorizationService.findById("test");

        // then
        assertNull(auth);
    }

    @Test
    @DisplayName("findById throws when id empty")
    public void findById_IdEmpty_ThrowsException() {
        // when
        String message = assertThrows(IllegalArgumentException.class, () -> {
            authorizationService.findById("");
        }).getMessage();

        // then
        assertEquals("Id cannot be empty", message);
    }

    @Test
    @DisplayName("findById non null when authorization found")
    public void findById_AuthorizationFound_ReturnsNonNull() {
        // given
        given(registeredClientRepository.findById(Redis.REGISTERED_CLIENT_ID))
                .willReturn(Client.createTestRegisteredClient());
        given(authorizationRepository.findById(Redis.AUTH_ID)).willReturn(
                Optional.of(Redis.createValidRedisOAuth2Authorization())
        );

        // when
        var result = authorizationService.findById(Redis.AUTH_ID);

        // then
        assertNotNull(result);
    }

    @Test
    @DisplayName("unflatten returns correctly rebuilt OAuth2Authorization")
    public void unflatten_RegisteredClientProvided_ReturnsRebuiltAuthorization() {
        // given
        given(registeredClientRepository.findById(Redis.REGISTERED_CLIENT_ID))
                .willReturn(Client.createTestRegisteredClient());

        // when
        var auth = authorizationService.unflatten(
                Redis.createValidRedisOAuth2Authorization()
        );

        // then
        assertEquals(Redis.AUTH_ID, auth.getId());
        assertEquals(Redis.REGISTERED_CLIENT_ID, auth.getRegisteredClientId());
        assertEquals(Redis.PRINCIPAL_NAME, auth.getPrincipalName());
        assertEquals(Redis.AUTHORIZATION_GRANT_TYPE, auth.getAuthorizationGrantType().getValue());

        // check attributes of the authorization
        var attrib = auth.getAttributes();
        var authReq = (OAuth2AuthorizationRequest)attrib.get("org.springframework.security.oauth2.core.endpoint.OAuth2AuthorizationRequest");
        assertNotNull(authReq);
        assertEquals(Redis.AUTHORIZATION_URI, authReq.getAuthorizationUri());
        assertEquals(AuthorizationGrantType.AUTHORIZATION_CODE, authReq.getGrantType());
        assertEquals(OAuth2AuthorizationResponseType.CODE, authReq.getResponseType());
        assertEquals(Client.CLIENT_ID, authReq.getClientId());
        assertEquals(Client.REDIRECT_URI, authReq.getRedirectUri());
        var scopes = authReq.getScopes();
        assertEquals(1, scopes.size());
        assertTrue(scopes.contains(Client.SCOPE));

        // check authorization code
        var authorizationCode = auth.getToken(OAuth2AuthorizationCode.class);
        assertNotNull(authorizationCode);
        assertEquals(Redis.AUTHORIZATION_CODE_VALUE, authorizationCode.getToken().getTokenValue());
        assertEquals(Redis.AUTHORIZATION_CODE_ISSUED_AT, authorizationCode.getToken().getIssuedAt());
        assertEquals(Redis.AUTHORIZATION_CODE_EXPIRES_AT, authorizationCode.getToken().getExpiresAt());
        var metadataMap = authorizationCode.getMetadata();
        assertEquals(1, metadataMap.size());
        assertTrue((boolean)metadataMap.get("metadata.token.invalidated"));

        // check access token
        var accessToken = auth.getAccessToken();
        assertNotNull(accessToken);
        var innerToken = accessToken.getToken();
        assertEquals(OAuth2AccessToken.TokenType.BEARER, innerToken.getTokenType());
        var tokenScopes = innerToken.getScopes();
        assertEquals(1, tokenScopes.size());
        assertTrue(tokenScopes.contains(Client.SCOPE));
        assertEquals(Redis.ACCESS_TOKEN_VALUE, innerToken.getTokenValue());
        assertEquals(Redis.ACCESS_TOKEN_ISSUED_AT, innerToken.getIssuedAt());
        assertEquals(Redis.ACCESS_TOKEN_EXPIRES_AT, innerToken.getExpiresAt());
        var accessTokenClaims = accessToken.getClaims();
        assertEquals(Redis.PRINCIPAL_NAME, accessTokenClaims.get("sub"));
        assertFalse((boolean)accessToken.getMetadata("metadata.token.invalidated"));
    }
}
