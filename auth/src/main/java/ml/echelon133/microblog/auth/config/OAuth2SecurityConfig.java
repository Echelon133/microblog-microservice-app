package ml.echelon133.microblog.auth.config;

import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.source.ImmutableJWKSet;
import com.nimbusds.jose.jwk.source.JWKSource;
import com.nimbusds.jose.proc.SecurityContext;
import ml.echelon133.microblog.shared.scope.MicroblogScope;
import ml.echelon133.microblog.shared.user.User;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.ClientAuthenticationMethod;
import org.springframework.security.oauth2.server.authorization.authentication.*;
import org.springframework.security.oauth2.server.authorization.client.InMemoryRegisteredClientRepository;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClient;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClientRepository;
import org.springframework.security.oauth2.server.authorization.config.annotation.web.configurers.OAuth2AuthorizationServerConfigurer;
import org.springframework.security.oauth2.server.authorization.settings.AuthorizationServerSettings;
import org.springframework.security.oauth2.server.authorization.settings.ClientSettings;
import org.springframework.security.oauth2.server.authorization.settings.OAuth2TokenFormat;
import org.springframework.security.oauth2.server.authorization.settings.TokenSettings;
import org.springframework.security.oauth2.server.authorization.token.OAuth2TokenClaimsContext;
import org.springframework.security.oauth2.server.authorization.token.OAuth2TokenClaimsSet;
import org.springframework.security.oauth2.server.authorization.token.OAuth2TokenCustomizer;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.LoginUrlAuthenticationEntryPoint;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;
import java.util.function.Consumer;

import org.springframework.security.web.util.matcher.RequestMatcher;

import static ml.echelon133.microblog.shared.auth.TokenOwnerIdExtractor.TOKEN_OWNER_KEY;
import static org.springframework.core.Ordered.HIGHEST_PRECEDENCE;

@Configuration
public class OAuth2SecurityConfig {

    private final PasswordEncoder passwordEncoder;

    @Value("${CONFIDENTIAL_CLIENT_ID}")
    private String clientId;

    @Value("${CONFIDENTIAL_CLIENT_SECRET}")
    private String clientSecret;

    @Autowired
    public OAuth2SecurityConfig(PasswordEncoder passwordEncoder) {
        this.passwordEncoder = passwordEncoder;
    }

    @Bean
    @Order(HIGHEST_PRECEDENCE)
    public SecurityFilterChain authorizationServerSecurityFilterChain(HttpSecurity http) throws Exception {
        OAuth2AuthorizationServerConfigurer authorizationServerConfigurer =
                new OAuth2AuthorizationServerConfigurer();
        RequestMatcher endpointsMatcher = authorizationServerConfigurer
                .getEndpointsMatcher();

        authorizationServerConfigurer
                .authorizationEndpoint(authorizationEndpoint ->
                        authorizationEndpoint
                                .authenticationProviders(configureAuthenticationValidator())
                );

        http
                .requestMatcher(endpointsMatcher)
                .authorizeRequests(authorizeRequests ->
                        authorizeRequests.anyRequest().authenticated()
                )
                .exceptionHandling((exceptions) -> exceptions
                        .authenticationEntryPoint(
                                new LoginUrlAuthenticationEntryPoint("/login"))
                )
                .csrf(csrf -> csrf.ignoringRequestMatchers(endpointsMatcher))
                .apply(authorizationServerConfigurer);

        return http.build();
    }

    private Consumer<List<AuthenticationProvider>> configureAuthenticationValidator() {
        return (authenticationProviders) ->
                authenticationProviders.forEach((authenticationProvider) -> {
                    // only validate at the CodeRequest level, because that's when those who request codes provide
                    // their username+password for the first time, which gives us access to their authorities/roles,
                    // creating an opportunity to reject them as early as possible if they do not have correct privileges
                    if (authenticationProvider instanceof OAuth2AuthorizationCodeRequestAuthenticationProvider) {
                        Consumer<OAuth2AuthorizationCodeRequestAuthenticationContext> authenticationValidator =
                                new UserAuthoritiesValidator()
                                        // then call validators of scopes and redirect-uris (which are required)
                                        .andThen(OAuth2AuthorizationCodeRequestAuthenticationValidator.DEFAULT_SCOPE_VALIDATOR)
                                        .andThen(OAuth2AuthorizationCodeRequestAuthenticationValidator.DEFAULT_REDIRECT_URI_VALIDATOR);

                        ((OAuth2AuthorizationCodeRequestAuthenticationProvider) authenticationProvider)
                                .setAuthenticationValidator(authenticationValidator);
                    }
                });
    }

    @Bean
    public TokenSettings tokenSettings() {
        return TokenSettings.builder()
                .accessTokenFormat(OAuth2TokenFormat.REFERENCE)
                .accessTokenTimeToLive(Duration.of(3, ChronoUnit.HOURS))
                .build();
    }

    @Bean
    public OAuth2TokenCustomizer<OAuth2TokenClaimsContext> accessTokenCustomizer() {
        // enrich the access token with the id of the token owner, so that
        // resource servers have access to it during token introspection
        return context -> {
            OAuth2TokenClaimsSet.Builder claims = context.getClaims();
            // this type assumption should always be correct because in the current flow
            // the first step of auth (GET /oauth2/authorize) entails the user exchanging
            // their username and password for a code needed in the second step
            UsernamePasswordAuthenticationToken principal = context.getPrincipal();
            // this cast shouldn't fail because the tokens are filled with the
            // UserDetails object which is returned by the custom UserDetailsService
            User innerPrincipal = (User)principal.getPrincipal();
            claims.claim(TOKEN_OWNER_KEY, innerPrincipal.getId());
        };
    }

    @Bean
    public RegisteredClientRepository registeredClientRepository() {

        /*
        If the password encoder is not a no-op, not encoding the client secret
        will result in "invalid_client" error during authentication, because
        ClientSecretAuthenticationProvider will fail during passwordEncoder.matches()
        since the plaintext provided during client configuration won't be recognized
        as an output of a password encoder
        */
        var confidentialClientSecret = passwordEncoder.encode(clientSecret);
        /*
        Configure the confidential client which will let other services access
        the token introspection endpoint at /oauth2/introspect. It's impossible
        to introspect an access token without having another form of authorization,
        because https://www.rfc-editor.org/rfc/rfc7662 specifies that:
            "To prevent token scanning attacks, the [introspection] endpoint MUST
            also require some form of authorization to access this endpoint, such as client
            authentication"
         */
        var confidentialClient = RegisteredClient.withId(UUID.randomUUID().toString())
                .clientId(clientId)
                .clientSecret(confidentialClientSecret)
                .clientAuthenticationMethod(ClientAuthenticationMethod.CLIENT_SECRET_BASIC)
                .authorizationGrantType(AuthorizationGrantType.CLIENT_CREDENTIALS)
                .tokenSettings(tokenSettings())
                .build();

        /*
        AuthorizationGrantType.AUTHORIZATION_CODE combined with ClientAuthenticationMethod.NONE enables
        the Authorization Code with PKCE flow. This flow is for public (non-confidential) clients which
        cannot protect their secrets.
         */
        var publicClient = RegisteredClient.withId(UUID.randomUUID().toString())
                .clientId("public-client")
                .clientSecret("{noop}secret") // does not matter because it's not used in this flow
                .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
                .clientAuthenticationMethod(ClientAuthenticationMethod.NONE)
                // redirectUri is required, set it to this placeholder value during the development
                .redirectUri("http://127.0.0.1:9999")
                .scopes((scope) -> scope.addAll(MicroblogScope.ALL_SCOPES))
                .tokenSettings(tokenSettings())
                .clientSettings(ClientSettings.builder().requireAuthorizationConsent(false).build())
                .build();

        /*
        This client is set up just like the public-client, except it only grants admin scopes.
         */
        var adminClient = RegisteredClient.withId(UUID.randomUUID().toString())
                .clientId("admin-client")
                .clientSecret("{noop}secret1") // does not matter because it's not used in this flow
                .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
                .clientAuthenticationMethod(ClientAuthenticationMethod.NONE)
                // redirectUri is required, set it to this placeholder value during the development
                .redirectUri("http://127.0.0.1:8888")
                .scopes((scope) -> scope.addAll(MicroblogScope.Admin.ALL_ADMIN_SCOPES))
                .tokenSettings(tokenSettings())
                .clientSettings(ClientSettings.builder().requireAuthorizationConsent(false).build())
                .build();

        return new InMemoryRegisteredClientRepository(adminClient, publicClient, confidentialClient);
    }

    @Bean
    public JWKSource<SecurityContext> jwkSource() {
        KeyPair keyPair = generateRsaKey();
        RSAPublicKey publicKey = (RSAPublicKey) keyPair.getPublic();
        RSAPrivateKey privateKey = (RSAPrivateKey) keyPair.getPrivate();
        RSAKey rsaKey = new RSAKey.Builder(publicKey)
                .privateKey(privateKey)
                .keyID(UUID.randomUUID().toString())
                .build();
        JWKSet jwkSet = new JWKSet(rsaKey);
        return new ImmutableJWKSet<>(jwkSet);
    }

    private static KeyPair generateRsaKey() {
        KeyPair keyPair;
        try {
            KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance("RSA");
            keyPairGenerator.initialize(2048);
            keyPair = keyPairGenerator.generateKeyPair();
        }
        catch (Exception ex) {
            throw new IllegalStateException(ex);
        }
        return keyPair;
    }

    @Bean
    public AuthorizationServerSettings providerSettings() {
        return AuthorizationServerSettings.builder().build();
    }
}
