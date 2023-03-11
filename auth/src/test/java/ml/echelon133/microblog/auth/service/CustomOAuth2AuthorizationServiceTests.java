package ml.echelon133.microblog.auth.service;

import ml.echelon133.microblog.auth.repository.OAuth2AuthorizationRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataRetrievalFailureException;
import org.springframework.security.oauth2.core.*;
import org.springframework.security.oauth2.core.endpoint.OAuth2AuthorizationRequest;
import org.springframework.security.oauth2.core.endpoint.OAuth2AuthorizationResponseType;
import org.springframework.security.oauth2.core.oidc.OidcIdToken;
import org.springframework.security.oauth2.server.authorization.OAuth2Authorization;
import org.springframework.security.oauth2.server.authorization.OAuth2AuthorizationCode;
import org.springframework.security.oauth2.server.authorization.OAuth2TokenType;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClientRepository;
import org.springframework.test.context.TestPropertySource;

import java.time.Instant;
import java.util.*;

import static ml.echelon133.microblog.auth.service.AuthTestData.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

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
        given(registeredClientRepository.findById(Client.REGISTERED_CLIENT_ID))
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
    @DisplayName("unflatten throws when registered client is null")
    public void unflatten_RegisteredClientNull_ThrowsException() {
        // given
        given(registeredClientRepository.findById(Client.REGISTERED_CLIENT_ID))
                .willReturn(null);

        // when
        String message = assertThrows(DataRetrievalFailureException.class, () -> {
            authorizationService.unflatten(Redis.createValidRedisOAuth2Authorization());
        }).getMessage();

        // then
        assertEquals(String.format(
                "The RegisteredClient with id '%s' was not found in the RegisteredClientRepository",
                Client.REGISTERED_CLIENT_ID
        ), message);
    }

    @Test
    @DisplayName("unflatten returns correctly rebuilt OAuth2Authorization")
    public void unflatten_RegisteredClientProvided_ReturnsRebuiltAuthorization() {
        // given
        given(registeredClientRepository.findById(Client.REGISTERED_CLIENT_ID))
                .willReturn(Client.createTestRegisteredClient());

        // when
        var auth = authorizationService.unflatten(
                Redis.createValidRedisOAuth2Authorization()
        );

        // then
        assertEquals(Redis.AUTH_ID, auth.getId());
        assertEquals(Client.REGISTERED_CLIENT_ID, auth.getRegisteredClientId());
        assertEquals(Redis.PRINCIPAL_NAME, auth.getPrincipalName());
        assertEquals(Redis.AUTHORIZATION_GRANT_TYPE, auth.getAuthorizationGrantType().getValue());
        var authorizedScopes = auth.getAuthorizedScopes();
        assertEquals(1, authorizedScopes.size());
        assertTrue(authorizedScopes.contains(Client.SCOPE));

        // check attributes of the authorization
        var attrib = auth.getAttributes();
        var authReq = (OAuth2AuthorizationRequest)attrib.get("org.springframework.security.oauth2.core.endpoint.OAuth2AuthorizationRequest");
        assertNotNull(authReq);
        assertEquals(Redis.AUTHORIZATION_URI, authReq.getAuthorizationUri());
        assertEquals(AuthorizationGrantType.AUTHORIZATION_CODE, authReq.getGrantType());
        assertEquals(OAuth2AuthorizationResponseType.CODE, authReq.getResponseType());
        assertEquals(Client.CLIENT_ID, authReq.getClientId());
        assertEquals(Client.REDIRECT_URI, authReq.getRedirectUri());

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

    @Test
    @DisplayName("save calls the repository")
    public void save_WhenProvidedAuthorization_CallsRepository() {
        var auth = Auth.createValidOAuth2Authorization();

        // when
        authorizationService.save(auth);

        // then
        verify(authorizationRepository, times(1)).save(any());
    }

    @Test
    @DisplayName("remove calls the repository")
    public void remove_WhenProvidedAuthorization_CallsRepository() {
        var auth = Auth.createValidOAuth2Authorization();

        // when
        authorizationService.remove(auth);

        // then
        verify(authorizationRepository, times(1)).deleteById(auth.getId());
    }

    @Test
    @DisplayName("findByToken throws when token empty")
    public void findByToken_TokenEmpty_ThrowsException() {
        // when
        String message = assertThrows(IllegalArgumentException.class, () -> {
            authorizationService.findByToken("", null);
        }).getMessage();

        // then
        assertEquals("Token cannot be empty", message);
    }

    @Test
    @DisplayName("findByToken searches among both authorization codes and access tokens when token type null")
    public void findByToken_TokenTypeNull_SearchesAmongBothTokenTypes() {
        var token = "test-token-value";

        // given
        given(authorizationRepository.findByAuthorizationCodeValue(token)).willReturn(Optional.empty());
        given(authorizationRepository.findByAccessTokenValue(token)).willReturn(Optional.empty());

        // when
        var auth = authorizationService.findByToken(token, null);

        // then
        assertNull(auth);
    }

    @Test
    @DisplayName("findByToken searches among authorization codes when token type 'code'")
    public void findByToken_TokenTypeCode_SearchesAmongAuthorizationCodes() {
        var token = "test-token-value";

        // given
        given(authorizationRepository.findByAuthorizationCodeValue(token)).willReturn(Optional.empty());

        // when
        var auth = authorizationService.findByToken(token, new OAuth2TokenType("code"));

        // then
        assertNull(auth);
    }

    @Test
    @DisplayName("findByToken searches among access tokens when token type 'access_token'")
    public void findByToken_TokenTypeAccessToken_SearchesAmongAccessTokens() {
        var token = "test-token-value";

        // given
        given(authorizationRepository.findByAccessTokenValue(token)).willReturn(Optional.empty());

        // when
        var auth = authorizationService.findByToken(token, OAuth2TokenType.ACCESS_TOKEN);

        // then
        assertNull(auth);
    }

    @Test
    @DisplayName("findByToken does not search repository when token type unknown")
    public void findByToken_TokenTypeUnknown_DoesNotSearchRepository() {
        var token = "test-token-value";

        // when
        var auth = authorizationService.findByToken(token, new OAuth2TokenType("some-unknown-type"));

        // then
        assertNull(auth);
    }

    @Test
    @DisplayName("flatten throws an exception when authorization contains a refresh token")
    public void flatten_AuthorizationContainsRefreshToken_ThrowsException() {
        var invalidAuth = OAuth2Authorization
                .from(Auth.createValidOAuth2Authorization())
                .refreshToken(new OAuth2RefreshToken(
                        "some-token-value",
                        Instant.now(),
                        Instant.now()
                ))
                .build();

        // when
        String message = assertThrows(IllegalArgumentException.class, () -> {
            authorizationService.flatten(invalidAuth);
        }).getMessage();

        // then
        assertEquals("This OAuth2AuthorizationService does not support refresh tokens", message);
    }

    @Test
    @DisplayName("flatten throws an exception when authorization contains an openid token")
    public void flatten_AuthorizationContainsOidcToken_ThrowsException() {
        var invalidAuth = OAuth2Authorization
                .from(Auth.createValidOAuth2Authorization())
                .token(new OidcIdToken(
                        "some-token-value",
                        Instant.now(),
                        Instant.now(),
                        Map.of("test-claim", "test-claim-value")
                ))
                .build();

        // when
        String message = assertThrows(IllegalArgumentException.class, () -> {
            authorizationService.flatten(invalidAuth);
        }).getMessage();

        // then
        assertEquals("This OAuth2AuthorizationService does not support openid tokens", message);
    }

    @Test
    @DisplayName("flatten returns correctly flattened RedisOAuth2Authorization")
    public void flatten_CorrectlyFormedAuthorization_ReturnsFlattenedAuthorization() {
        // when
        var auth = authorizationService.flatten(
                Auth.createValidOAuth2Authorization()
        );

        // then
        assertEquals(Redis.AUTH_ID, auth.getId());
        assertEquals(Client.REGISTERED_CLIENT_ID, auth.getRegisteredClientId());
        assertEquals(Redis.PRINCIPAL_NAME, auth.getPrincipalName());
        assertEquals(Redis.AUTHORIZATION_GRANT_TYPE, auth.getAuthorizationGrantType());
        assertEquals(Client.SCOPE, auth.getAuthorizedScopes());

        var attributes = auth.getAttributes();
        assertTrue(attributes.contains("org.springframework.security.oauth2.core.endpoint.OAuth2AuthorizationRequest"));
        assertTrue(attributes.contains("java.security.Principal"));
        assertEquals(Redis.AUTHORIZATION_CODE_VALUE, auth.getAuthorizationCodeValue());
        assertEquals(Redis.AUTHORIZATION_CODE_ISSUED_AT, auth.getAuthorizationCodeIssuedAt());
        assertEquals(Redis.AUTHORIZATION_CODE_EXPIRES_AT, auth.getAuthorizationCodeExpiresAt());
        var authorizationCodeMetadata = auth.getAuthorizationCodeMetadata();
        assertTrue(authorizationCodeMetadata.contains("\"metadata.token.invalidated\":true"));

        assertEquals(Redis.ACCESS_TOKEN_VALUE, auth.getAccessTokenValue());
        assertEquals(Redis.ACCESS_TOKEN_ISSUED_AT, auth.getAccessTokenIssuedAt());
        assertEquals(Redis.ACCESS_TOKEN_EXPIRES_AT, auth.getAccessTokenExpiresAt());
        assertEquals(OAuth2AccessToken.TokenType.BEARER.getValue(), auth.getAccessTokenType());
        assertEquals(Client.SCOPE, auth.getAccessTokenScopes());
        var accessTokenMetadata = auth.getAccessTokenMetadata();
        assertTrue(accessTokenMetadata.contains("\"metadata.token.invalidated\":false"));
    }
}
