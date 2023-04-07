package ml.echelon133.microblog.auth.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataRetrievalFailureException;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.OAuth2AccessToken;
import org.springframework.security.oauth2.core.OAuth2RefreshToken;
import org.springframework.security.oauth2.core.endpoint.OAuth2AuthorizationRequest;
import org.springframework.security.oauth2.core.endpoint.OAuth2AuthorizationResponseType;
import org.springframework.security.oauth2.core.oidc.OidcIdToken;
import org.springframework.security.oauth2.server.authorization.OAuth2Authorization;
import org.springframework.security.oauth2.server.authorization.OAuth2AuthorizationCode;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClientRepository;

import java.time.Instant;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
@DisplayName("Tests of AuthorizationMapper")
public class AuthorizationMapperTests {

    @Mock
    private RegisteredClientRepository registeredClientRepository;

    @InjectMocks
    private AuthorizationMapper authorizationMapper;

    @Test
    @DisplayName("unflatten throws when registered client is null")
    public void unflatten_RegisteredClientNull_ThrowsException() {
        // given
        given(registeredClientRepository.findById(AuthTestData.Client.REGISTERED_CLIENT_ID))
                .willReturn(null);

        // when
        String message = assertThrows(DataRetrievalFailureException.class, () -> {
            authorizationMapper.unflatten(AuthTestData.Redis.createValidRedisOAuth2Authorization());
        }).getMessage();

        // then
        assertEquals(String.format(
                "The RegisteredClient with id '%s' was not found in the RegisteredClientRepository",
                AuthTestData.Client.REGISTERED_CLIENT_ID
        ), message);
    }

    @Test
    @DisplayName("unflatten returns correctly rebuilt OAuth2Authorization")
    public void unflatten_RegisteredClientProvided_ReturnsRebuiltAuthorization() {
        // given
        given(registeredClientRepository.findById(AuthTestData.Client.REGISTERED_CLIENT_ID))
                .willReturn(AuthTestData.Client.createTestRegisteredClient());

        // when
        var auth = authorizationMapper.unflatten(
                AuthTestData.Redis.createValidRedisOAuth2Authorization()
        );

        // then
        assertEquals(AuthTestData.Redis.AUTH_ID, auth.getId());
        assertEquals(AuthTestData.Client.REGISTERED_CLIENT_ID, auth.getRegisteredClientId());
        assertEquals(AuthTestData.Redis.PRINCIPAL_NAME, auth.getPrincipalName());
        assertEquals(AuthTestData.Redis.AUTHORIZATION_GRANT_TYPE, auth.getAuthorizationGrantType().getValue());
        var authorizedScopes = auth.getAuthorizedScopes();
        assertEquals(1, authorizedScopes.size());
        assertTrue(authorizedScopes.contains(AuthTestData.Client.SCOPE));

        // check attributes of the authorization
        var attrib = auth.getAttributes();
        var authReq = (OAuth2AuthorizationRequest)attrib.get("org.springframework.security.oauth2.core.endpoint.OAuth2AuthorizationRequest");
        assertNotNull(authReq);
        assertEquals(AuthTestData.Redis.AUTHORIZATION_URI, authReq.getAuthorizationUri());
        assertEquals(AuthorizationGrantType.AUTHORIZATION_CODE, authReq.getGrantType());
        assertEquals(OAuth2AuthorizationResponseType.CODE, authReq.getResponseType());
        assertEquals(AuthTestData.Client.CLIENT_ID, authReq.getClientId());
        assertEquals(AuthTestData.Client.REDIRECT_URI, authReq.getRedirectUri());

        // check authorization code
        var authorizationCode = auth.getToken(OAuth2AuthorizationCode.class);
        assertNotNull(authorizationCode);
        assertEquals(AuthTestData.Redis.AUTHORIZATION_CODE_VALUE, authorizationCode.getToken().getTokenValue());
        assertEquals(AuthTestData.Redis.AUTHORIZATION_CODE_ISSUED_AT, authorizationCode.getToken().getIssuedAt());
        assertEquals(AuthTestData.Redis.AUTHORIZATION_CODE_EXPIRES_AT, authorizationCode.getToken().getExpiresAt());
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
        assertTrue(tokenScopes.contains(AuthTestData.Client.SCOPE));
        assertEquals(AuthTestData.Redis.ACCESS_TOKEN_VALUE, innerToken.getTokenValue());
        assertEquals(AuthTestData.Redis.ACCESS_TOKEN_ISSUED_AT, innerToken.getIssuedAt());
        assertEquals(AuthTestData.Redis.ACCESS_TOKEN_EXPIRES_AT, innerToken.getExpiresAt());
        var accessTokenClaims = accessToken.getClaims();
        assertEquals(AuthTestData.Redis.PRINCIPAL_NAME, accessTokenClaims.get("sub"));
        assertFalse((boolean)accessToken.getMetadata("metadata.token.invalidated"));
    }

    @Test
    @DisplayName("flatten throws an exception when authorization contains a refresh token")
    public void flatten_AuthorizationContainsRefreshToken_ThrowsException() {
        var invalidAuth = OAuth2Authorization
                .from(AuthTestData.Auth.createValidOAuth2Authorization())
                .refreshToken(new OAuth2RefreshToken(
                        "some-token-value",
                        Instant.now(),
                        Instant.now()
                ))
                .build();

        // when
        String message = assertThrows(IllegalArgumentException.class, () -> {
            authorizationMapper.flatten(invalidAuth);
        }).getMessage();

        // then
        assertEquals("This OAuth2AuthorizationService does not support refresh tokens", message);
    }

    @Test
    @DisplayName("flatten throws an exception when authorization contains an openid token")
    public void flatten_AuthorizationContainsOidcToken_ThrowsException() {
        var invalidAuth = OAuth2Authorization
                .from(AuthTestData.Auth.createValidOAuth2Authorization())
                .token(new OidcIdToken(
                        "some-token-value",
                        Instant.now(),
                        Instant.now(),
                        Map.of("test-claim", "test-claim-value")
                ))
                .build();

        // when
        String message = assertThrows(IllegalArgumentException.class, () -> {
            authorizationMapper.flatten(invalidAuth);
        }).getMessage();

        // then
        assertEquals("This OAuth2AuthorizationService does not support openid tokens", message);
    }

    @Test
    @DisplayName("flatten returns correctly flattened RedisOAuth2Authorization")
    public void flatten_CorrectlyFormedAuthorization_ReturnsFlattenedAuthorization() {
        // when
        var auth = authorizationMapper.flatten(
                AuthTestData.Auth.createValidOAuth2Authorization()
        );

        // then
        assertEquals(AuthTestData.Redis.AUTH_ID, auth.getId());
        assertEquals(AuthTestData.Client.REGISTERED_CLIENT_ID, auth.getRegisteredClientId());
        assertEquals(AuthTestData.Redis.PRINCIPAL_NAME, auth.getPrincipalName());
        assertEquals(AuthTestData.Redis.AUTHORIZATION_GRANT_TYPE, auth.getAuthorizationGrantType());
        assertEquals(AuthTestData.Client.SCOPE, auth.getAuthorizedScopes());

        var attributes = auth.getAttributes();
        assertTrue(attributes.contains("org.springframework.security.oauth2.core.endpoint.OAuth2AuthorizationRequest"));
        assertTrue(attributes.contains("java.security.Principal"));
        assertEquals(AuthTestData.Redis.AUTHORIZATION_CODE_VALUE, auth.getAuthorizationCodeValue());
        assertEquals(AuthTestData.Redis.AUTHORIZATION_CODE_ISSUED_AT, auth.getAuthorizationCodeIssuedAt());
        assertEquals(AuthTestData.Redis.AUTHORIZATION_CODE_EXPIRES_AT, auth.getAuthorizationCodeExpiresAt());
        var authorizationCodeMetadata = auth.getAuthorizationCodeMetadata();
        assertTrue(authorizationCodeMetadata.contains("\"metadata.token.invalidated\":true"));

        assertEquals(AuthTestData.Redis.ACCESS_TOKEN_VALUE, auth.getAccessTokenValue());
        assertEquals(AuthTestData.Redis.ACCESS_TOKEN_ISSUED_AT, auth.getAccessTokenIssuedAt());
        assertEquals(AuthTestData.Redis.ACCESS_TOKEN_EXPIRES_AT, auth.getAccessTokenExpiresAt());
        assertEquals(OAuth2AccessToken.TokenType.BEARER.getValue(), auth.getAccessTokenType());
        assertEquals(AuthTestData.Client.SCOPE, auth.getAccessTokenScopes());
        var accessTokenMetadata = auth.getAccessTokenMetadata();
        assertTrue(accessTokenMetadata.contains("\"metadata.token.invalidated\":false"));
    }
}
