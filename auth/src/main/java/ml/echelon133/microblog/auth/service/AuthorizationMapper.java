package ml.echelon133.microblog.auth.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.Module;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.hibernate5.Hibernate5Module;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import ml.echelon133.microblog.auth.model.RedisOAuth2Authorization;
import ml.echelon133.microblog.shared.user.Role;
import ml.echelon133.microblog.shared.user.User;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataRetrievalFailureException;
import org.springframework.security.jackson2.CoreJackson2Module;
import org.springframework.security.jackson2.SecurityJackson2Modules;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.OAuth2AccessToken;
import org.springframework.security.oauth2.core.endpoint.OAuth2ParameterNames;
import org.springframework.security.oauth2.core.oidc.OidcIdToken;
import org.springframework.security.oauth2.server.authorization.OAuth2Authorization;
import org.springframework.security.oauth2.server.authorization.OAuth2AuthorizationCode;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClient;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClientRepository;
import org.springframework.security.oauth2.server.authorization.jackson2.OAuth2AuthorizationServerJackson2Module;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.*;

/**
 * Enables mapping of {@link OAuth2Authorization} into {@link RedisOAuth2Authorization} and vice-versa.
 *
 * This class exists because there is no official implementation which enables saving {@link OAuth2Authorization} using
 * Redis and the official {@code spring-authorization-server} documentation recommends preprocessing
 * {@link OAuth2Authorization} before saving, because it is:
 * "[...] complex and contains several multi-valued fields as well as numerous arbitrarily long token values,
 * metadata, settings and claims values"
 * <a href="https://docs.spring.io/spring-authorization-server/docs/0.4.0/reference/html/guides/how-to-jpa.html#authorization-schema">Source of the quote</a>
 */
@Service
public class AuthorizationMapper {

    private static final Logger LOGGER = LogManager.getLogger(AuthorizationMapper.class);
    private final RegisteredClientRepository registeredClientRepository;
    private final ObjectMapper objectMapper;

    @Autowired
    public AuthorizationMapper(RegisteredClientRepository registeredClientRepository) {
        this.registeredClientRepository = registeredClientRepository;
        this.objectMapper = new ObjectMapper();

        // enables proper serialization of Instant type
        objectMapper.registerModule(
                new JavaTimeModule()
        );

        // enable proper serialization and deserialization of classes that are provided by spring security
        ClassLoader classLoader = AuthorizationMapper.class.getClassLoader();
        List<Module> securityModules = SecurityJackson2Modules.getModules(classLoader);
        objectMapper.registerModules(securityModules);
        objectMapper.registerModule(new CoreJackson2Module());
        objectMapper.registerModule(new OAuth2AuthorizationServerJackson2Module());

        // when serializing, replace Hibernate's collections with simple java collections
        objectMapper.registerModule(new Hibernate5Module()
                .enable(Hibernate5Module.Feature.REPLACE_PERSISTENT_COLLECTIONS));

        // issue https://github.com/spring-projects/spring-security/issues/4370 specifies
        // that there is an allowlist which says which objects are considered save to deserialize,
        // these mixins have to be registered to enable deserialization of User and Role
        // (UUID and Timestamp also need to be added to the list because they are used within User and Role)
        objectMapper.addMixIn(User.class, AuthorizationMixIns.UserMixIn.class);
        objectMapper.addMixIn(Role.class, AuthorizationMixIns.RoleMixIn.class);
        objectMapper.addMixIn(UUID.class, AuthorizationMixIns.UUIDMixIn.class);
        objectMapper.addMixIn(Timestamp.class, AuthorizationMixIns.TimestampMixIn.class);
    }

    /**
     * Maps complex {@link OAuth2Authorization} into a simple, flat {@link RedisOAuth2Authorization} structure
     * that Redis knows how to store.
     *
     * @param authorization representation of an authorization object that will be flattened
     * @return flattened version of {@link OAuth2Authorization} that can be saved in a Redis database
     * @throws IllegalArgumentException when the {@link OAuth2Authorization} contains a refresh token or
     * an oidc token, which are currently not supported by this authorization server
     */
    public RedisOAuth2Authorization flatten(OAuth2Authorization authorization) throws IllegalArgumentException {
        RedisOAuth2Authorization flatAuth = new RedisOAuth2Authorization();

        LOGGER.debug(String.format(
                "Begin flattening of authorization with id '%s', registered client id '%s', for principal '%s' with " +
                        "authorization_grant_type '%s'%n",
                authorization.getId(),
                authorization.getRegisteredClientId(),
                authorization.getPrincipalName(),
                authorization.getAuthorizationGrantType()
        ));

        flatAuth.setId(authorization.getId());
        flatAuth.setRegisteredClientId(authorization.getRegisteredClientId());
        flatAuth.setPrincipalName(authorization.getPrincipalName());
        flatAuth.setAuthorizationGrantType(authorization.getAuthorizationGrantType().getValue());

        String flatAttributesMap = writeMapAsString(authorization.getAttributes());
        flatAuth.setAttributes(flatAttributesMap);

        String flatAuthorizedScopes = StringUtils.collectionToCommaDelimitedString(authorization.getAuthorizedScopes());
        flatAuth.setAuthorizedScopes(flatAuthorizedScopes);

        String state = authorization.getAttribute(OAuth2ParameterNames.STATE);
        flatAuth.setState(state);

        OAuth2Authorization.Token<OAuth2AuthorizationCode> authorizationCode =
                authorization.getToken(OAuth2AuthorizationCode.class);

        if (authorizationCode != null) {
            var innerToken = authorizationCode.getToken();
            flatAuth.setAuthorizationCodeValue(innerToken.getTokenValue());
            flatAuth.setAuthorizationCodeIssuedAt(innerToken.getIssuedAt());
            flatAuth.setAuthorizationCodeExpiresAt(innerToken.getExpiresAt());

            String authorizationCodeMetadata = writeMapAsString(authorizationCode.getMetadata());
            flatAuth.setAuthorizationCodeMetadata(authorizationCodeMetadata);
        }

        OAuth2Authorization.Token<OAuth2AccessToken> accessToken =
                authorization.getAccessToken();

        if (accessToken != null) {
            var innerToken = accessToken.getToken();
            flatAuth.setAccessTokenValue(innerToken.getTokenValue());
            flatAuth.setAccessTokenIssuedAt(innerToken.getIssuedAt());
            flatAuth.setAccessTokenExpiresAt(innerToken.getExpiresAt());

            String accessTokenMetadata = writeMapAsString(accessToken.getMetadata());
            flatAuth.setAccessTokenMetadata(accessTokenMetadata);
            flatAuth.setAccessTokenType(innerToken.getTokenType().getValue());

            if (!CollectionUtils.isEmpty(innerToken.getScopes())) {
                var accessTokenScopes = StringUtils.collectionToDelimitedString(innerToken.getScopes(), ",");
                flatAuth.setAccessTokenScopes(accessTokenScopes);
            }
        }

        Assert.isNull(
                authorization.getRefreshToken(),
                "This OAuth2AuthorizationService does not support refresh tokens"
        );
        Assert.isNull(
                authorization.getToken(OidcIdToken.class),
                "This OAuth2AuthorizationService does not support openid tokens"
        );

        return flatAuth;
    }

    /**
     * Maps flat {@link RedisOAuth2Authorization} structure that's loaded from Redis into complex
     * {@link OAuth2Authorization}.
     *
     * @param authorization flat authorization object that's loaded from Redis store
     * @return complex {@link OAuth2Authorization} that's been rebuilt from a {@link RedisOAuth2Authorization}
     */
    public OAuth2Authorization unflatten(RedisOAuth2Authorization authorization) {
        LOGGER.debug(String.format(
                "Begin unflattening of authorization with id '%s', registered client id '%s', for principal '%s' with " +
                        "authorization_grant_type '%s'%n",
                authorization.getId(),
                authorization.getRegisteredClientId(),
                authorization.getPrincipalName(),
                authorization.getAuthorizationGrantType()
        ));

        String registeredClientId = authorization.getRegisteredClientId();
        RegisteredClient registeredClient = this.registeredClientRepository.findById(registeredClientId);

        if (registeredClient == null) {
            throw new DataRetrievalFailureException(
                    "The RegisteredClient with id '" + registeredClientId + "' was not found in the RegisteredClientRepository");
        }

        Map<String, Object> attributesMap = readStringIntoMap(authorization.getAttributes());

        Set<String> authorizedScopes = StringUtils.commaDelimitedListToSet(authorization.getAuthorizedScopes());

        OAuth2Authorization.Builder builder = OAuth2Authorization.withRegisteredClient(registeredClient);
        builder
                .id(authorization.getId())
                .principalName(authorization.getPrincipalName())
                .authorizationGrantType(new AuthorizationGrantType(authorization.getAuthorizationGrantType()))
                .attributes((attrs) -> attrs.putAll(attributesMap))
                .authorizedScopes(authorizedScopes);

        String state = authorization.getState();
        if (StringUtils.hasText(state)) {
            builder.attribute(OAuth2ParameterNames.STATE, state);
        }

        Instant tokenIssuedAt;
        Instant tokenExpiresAt;
        String authorizationCodeValue = authorization.getAuthorizationCodeValue();

        if (StringUtils.hasText(authorizationCodeValue)) {
            tokenIssuedAt = authorization.getAuthorizationCodeIssuedAt();
            tokenExpiresAt = authorization.getAuthorizationCodeExpiresAt();
            Map<String, Object> authorizationCodeMetadata = readStringIntoMap(authorization.getAuthorizationCodeMetadata());

            OAuth2AuthorizationCode authorizationCode = new OAuth2AuthorizationCode(
                    authorizationCodeValue, tokenIssuedAt, tokenExpiresAt);
            builder.token(authorizationCode, (metadata) -> metadata.putAll(authorizationCodeMetadata));
        }

        String accessTokenValue = authorization.getAccessTokenValue();
        if (StringUtils.hasText(accessTokenValue)) {
            tokenIssuedAt = authorization.getAccessTokenIssuedAt();
            tokenExpiresAt = authorization.getAccessTokenExpiresAt();
            Map<String, Object> accessTokenMetadata = readStringIntoMap(authorization.getAccessTokenMetadata());
            OAuth2AccessToken.TokenType tokenType = null;
            if (OAuth2AccessToken.TokenType.BEARER.getValue().equalsIgnoreCase(authorization.getAccessTokenType())) {
                tokenType = OAuth2AccessToken.TokenType.BEARER;
            }

            Set<String> scopes = Collections.emptySet();
            String accessTokenScopes = authorization.getAccessTokenScopes();
            if (accessTokenScopes != null) {
                scopes = StringUtils.commaDelimitedListToSet(accessTokenScopes);
            }
            OAuth2AccessToken accessToken = new OAuth2AccessToken(tokenType, accessTokenValue, tokenIssuedAt, tokenExpiresAt, scopes);
            builder.token(accessToken, (metadata) -> metadata.putAll(accessTokenMetadata));
        }

        // ignore openid and refresh tokens and just build the Authorization without them, because
        // this implementation of an auth server does not use openid or refresh tokens
        return builder.build();
    }

    private String writeMapAsString(Map<String, Object> map) {
        try {
            return this.objectMapper.writeValueAsString(map);
        } catch (Exception ex) {
            throw new IllegalArgumentException(ex.getMessage(), ex);
        }
    }

    private Map<String, Object> readStringIntoMap(String data) {
        try {
            return this.objectMapper.readValue(data, new TypeReference<Map<String, Object>>() {});
        } catch (Exception ex) {
            throw new IllegalArgumentException(ex.getMessage(), ex);
        }
    }
}
