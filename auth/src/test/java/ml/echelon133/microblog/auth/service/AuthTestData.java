package ml.echelon133.microblog.auth.service;

import ml.echelon133.microblog.auth.model.RedisOAuth2Authorization;
import ml.echelon133.microblog.shared.user.Roles;
import ml.echelon133.microblog.shared.user.User;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.oauth2.core.*;
import org.springframework.security.oauth2.core.endpoint.OAuth2AuthorizationRequest;
import org.springframework.security.oauth2.server.authorization.OAuth2Authorization;
import org.springframework.security.oauth2.server.authorization.OAuth2AuthorizationCode;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClient;

import java.time.Instant;
import java.util.Map;
import java.util.Set;

import static ml.echelon133.microblog.shared.auth.TokenOwnerIdExtractor.TOKEN_OWNER_KEY;

public class AuthTestData {

    public static class Client {

        public static String CLIENT_ID = "public-client";
        public static String REDIRECT_URI = "http://127.0.0.1:9999";
        public static String SCOPE = "test";
        public static String REGISTERED_CLIENT_ID = "7564760f-1c90-427b-aa1a-448b05c87884";

        public static RegisteredClient createTestRegisteredClient() {
            return RegisteredClient
                    .withId(REGISTERED_CLIENT_ID)
                    .clientId(CLIENT_ID)
                    .clientSecret("test-secret") // does not matter in a public client
                    .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
                    .clientAuthenticationMethod(ClientAuthenticationMethod.NONE)
                    .redirectUri(REDIRECT_URI)
                    .scope(SCOPE)
                    .build();
        }
    }

    public static class Redis {
        public static String AUTHORIZATION_URI = "http://localhost:8090/oauth2/authorize";
        public static String AUTH_ID = "2e472653-dba0-482a-b25d-967ecde9461e";
        public static String PRINCIPAL_NAME = "testuser";
        public static String AUTHORIZED_SCOPES = Client.SCOPE;
        public static String ATTRIBUTES =
"""
{"@class":"java.util.Collections$UnmodifiableMap","org.springframework.security.oauth2.core.endpoint.OAuth2AuthorizationRequest":{"@class":"org.springframework.security.oauth2.core.endpoint.OAuth2AuthorizationRequest","authorizationUri":"$authorizationUri","authorizationGrantType":{"value":"authorization_code"},"responseType":{"value":"code"},"clientId":"$clientId","redirectUri":"$redirectUri","scopes":["java.util.Collections$UnmodifiableSet",["$scope"]],"state":null,"additionalParameters":{"@class":"java.util.Collections$UnmodifiableMap","code_challenge":"iHp4fcsRkj-RWbPpotjqHug0Vcp2PAPR7E8zUzG_vFQ","code_challenge_method":"S256"},"authorizationRequestUri":"http://localhost:8090/oauth2/authorize?response_type=code&client_id=public-client&scope=test&redirect_uri=http://127.0.0.1:9999&code_challenge=iHp4fcsRkj-RWbPpotjqHug0Vcp2PAPR7E8zUzG_vFQ&code_challenge_method=S256","attributes":{"@class":"java.util.Collections$UnmodifiableMap"}},"java.security.Principal":{"@class":"org.springframework.security.authentication.UsernamePasswordAuthenticationToken","authorities":["java.util.Collections$UnmodifiableRandomAccessList",[{"@class":"ml.echelon133.microblog.shared.user.Role","id":["java.util.UUID","7b6b132f-8a19-43bc-af0f-277b863ad7e2"],"version":0,"authority":"$role"}]],"details":{"@class":"org.springframework.security.web.authentication.WebAuthenticationDetails","remoteAddress":"127.0.0.1","sessionId":"47F6058F0633F61965113A852EC58F86"},"authenticated":true,"principal":{"@class":"ml.echelon133.microblog.shared.user.User","id":["java.util.UUID","32c16f5d-aca8-488e-8ce8-65f25866b82b"],"version":0,"username":"$username","enabled":true,"accountNonExpired":true,"accountNonLocked":true,"credentialsNonExpired":true},"credentials":null}}
"""
        .replace("$clientId", Client.CLIENT_ID)
        .replace("$redirectUri", Client.REDIRECT_URI)
        .replace("$username", PRINCIPAL_NAME)
        .replace("$authorizationUri", AUTHORIZATION_URI)
        .replace("$role", Roles.ROLE_USER.name());

        public static String AUTHORIZATION_GRANT_TYPE = "authorization_code";
        public static String AUTHORIZATION_CODE_VALUE = "yN1duEo3MU3VktHzoviIA8oAmu8cuHM9KCdtBgO_IBbJAKNjJM9MXtKYeaScXU9CRbY8hNQg5JL5wmZV6jl61vyXRPySlA4TMIdzxsiM8cN77-COTjGv1viryu3gprB2";
        public static Instant AUTHORIZATION_CODE_ISSUED_AT = Instant.parse("2023-03-07T19:42:43.001922826Z");
        public static Instant AUTHORIZATION_CODE_EXPIRES_AT = Instant.parse("2023-03-07T19:47:43.001922826Z");
        public static String AUTHORIZATION_CODE_METADATA =
"""
{"@class":"java.util.Collections$UnmodifiableMap","metadata.token.invalidated":true}
""";
        public static String ACCESS_TOKEN_VALUE = "4Ad-8usEDseoLclllJJtUuYamnXdjop4Sid8uoPG-VaU9zjK3kIbWr5UN1QfKAUjhbd41ttkIHq8s_DJU39Kz8N2GytnO8N1C5dsx_y_SQKOiOXHj1x_uCuKOBLYD4-1";
        public static Instant ACCESS_TOKEN_ISSUED_AT = Instant.parse("2023-03-07T19:44:46.947811513Z");
        public static Instant ACCESS_TOKEN_EXPIRES_AT = Instant.parse("2023-03-07T22:44:46.947811513Z");
        public static String ACCESS_TOKEN_TYPE = "Bearer";
        public static String ACCESS_TOKEN_SCOPES = Client.SCOPE;
        public static String ACCESS_TOKEN_METADATA =
"""
{"@class":"java.util.Collections$UnmodifiableMap","metadata.token.claims":{"@class":"java.util.Collections$UnmodifiableMap","sub":"$sub","aud":["java.util.Collections$SingletonList",["$clientId"]],"nbf":["java.time.Instant",1678356768.377238978],"scope":["java.util.Collections$UnmodifiableSet",["$scope"]],"iss":["java.net.URL","http://localhost:8090"],"$token-key":["java.util.UUID","32c16f5d-aca8-488e-8ce8-65f25866b82b"],"exp":["java.time.Instant",1678367568.377238978],"iat":["java.time.Instant",1678356768.377238978],"jti":"7b2b9eb5-142f-4d95-9432-d0b0421fdb72"},"metadata.token.invalidated":false}
"""
        .replace("$token-key", TOKEN_OWNER_KEY)
        .replace("$sub", PRINCIPAL_NAME)
        .replace("$clientId", Client.CLIENT_ID)
        .replace("$scope", Client.SCOPE);

        /**
         * Builds a {@link RedisOAuth2Authorization} object which precisely emulates
         * a flattened version of a real {@link org.springframework.security.oauth2.server.authorization.OAuth2Authorization}
         * containing an access token. This object can be used to test whether the code which rebuilds the
         * {@link org.springframework.security.oauth2.server.authorization.OAuth2Authorization} from the flattened
         * database object is actually correct.
         *
         * @return a {@link RedisOAuth2Authorization} which represents an authorization containing an access token
         */
        public static RedisOAuth2Authorization createValidRedisOAuth2Authorization() {
            RedisOAuth2Authorization auth = new RedisOAuth2Authorization();
            auth.setId(AUTH_ID);
            auth.setRegisteredClientId(Client.REGISTERED_CLIENT_ID);
            auth.setPrincipalName(PRINCIPAL_NAME);
            auth.setAttributes(ATTRIBUTES);
            auth.setAuthorizedScopes(AUTHORIZED_SCOPES);
            auth.setAuthorizationGrantType(AUTHORIZATION_GRANT_TYPE);
            auth.setAuthorizationCodeValue(AUTHORIZATION_CODE_VALUE);
            auth.setAuthorizationCodeIssuedAt(AUTHORIZATION_CODE_ISSUED_AT);
            auth.setAuthorizationCodeExpiresAt(AUTHORIZATION_CODE_EXPIRES_AT);
            auth.setAuthorizationCodeMetadata(AUTHORIZATION_CODE_METADATA);
            auth.setAccessTokenValue(ACCESS_TOKEN_VALUE);
            auth.setAccessTokenIssuedAt(ACCESS_TOKEN_ISSUED_AT);
            auth.setAccessTokenExpiresAt(ACCESS_TOKEN_EXPIRES_AT);
            auth.setAccessTokenType(ACCESS_TOKEN_TYPE);
            auth.setAccessTokenScopes(ACCESS_TOKEN_SCOPES);
            auth.setAccessTokenMetadata(ACCESS_TOKEN_METADATA);
            return auth;
        }
    }

    public static class Auth {

        /**
         * Builds an {@link OAuth2Authorization} which can be used to test whether the
         * {@link CustomOAuth2AuthorizationService} correctly flattens objects before
         * saving them to the database.
         *
         * @return an {@link OAuth2Authorization}
         */
        public static OAuth2Authorization createValidOAuth2Authorization() {
            var authorizationRequest = OAuth2AuthorizationRequest.authorizationCode()
                    .authorizationUri(Redis.AUTHORIZATION_URI)
                    .clientId(Client.CLIENT_ID)
                    .redirectUri(Client.REDIRECT_URI)
                    .scope(Client.SCOPE)
                    .additionalParameters(Map.of(
                            "code_challenge", "iHp4fcsRkj-RWbPpotjqHug0Vcp2PAPR7E8zUzG_vFQ",
                            "code_challenge_method", "S256"
                    ))
                    .build();

            return OAuth2Authorization
                    .withRegisteredClient(Client.createTestRegisteredClient())
                    .id(Redis.AUTH_ID)
                    .principalName(Redis.PRINCIPAL_NAME)
                    .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
                    .token(new OAuth2AuthorizationCode(
                            Redis.AUTHORIZATION_CODE_VALUE,
                            Redis.AUTHORIZATION_CODE_ISSUED_AT,
                            Redis.AUTHORIZATION_CODE_EXPIRES_AT
                    ), (metadata) -> metadata.put("metadata.token.invalidated", true))
                    .accessToken(new OAuth2AccessToken(
                            OAuth2AccessToken.TokenType.BEARER,
                            Redis.ACCESS_TOKEN_VALUE,
                            Redis.ACCESS_TOKEN_ISSUED_AT,
                            Redis.ACCESS_TOKEN_EXPIRES_AT,
                            Set.of(Redis.ACCESS_TOKEN_SCOPES)
                    ))
                    .attributes((attrib) -> attrib.putAll(Map.of(
                            "org.springframework.security.oauth2.core.endpoint.OAuth2AuthorizationRequest",
                            authorizationRequest,
                            "java.security.Principal",
                            new UsernamePasswordAuthenticationToken(new User(Redis.PRINCIPAL_NAME, "", "", "", Set.of()), null, Set.of())
                    )))
                    .authorizedScopes(Set.of(Client.SCOPE))
                    .build();
        }
    }
}
