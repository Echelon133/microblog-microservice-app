package ml.echelon133.microblog.auth.config;

import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.source.ImmutableJWKSet;
import com.nimbusds.jose.jwk.source.JWKSource;
import com.nimbusds.jose.proc.SecurityContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.OAuth2AuthorizationServerConfiguration;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.ClientAuthenticationMethod;
import org.springframework.security.oauth2.core.OAuth2TokenFormat;
import org.springframework.security.oauth2.server.authorization.client.InMemoryRegisteredClientRepository;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClient;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClientRepository;
import org.springframework.security.oauth2.server.authorization.config.ClientSettings;
import org.springframework.security.oauth2.server.authorization.config.ProviderSettings;
import org.springframework.security.oauth2.server.authorization.config.TokenSettings;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.LoginUrlAuthenticationEntryPoint;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

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
        OAuth2AuthorizationServerConfiguration.applyDefaultSecurity(http);

        http
                .exceptionHandling((exceptions) -> exceptions
                        .authenticationEntryPoint(
                                new LoginUrlAuthenticationEntryPoint("/login"))
                );

        return http.build();
    }

    @Bean
    public TokenSettings tokenSettings() {
        return TokenSettings.builder()
                .accessTokenFormat(OAuth2TokenFormat.REFERENCE)
                .accessTokenTimeToLive(Duration.of(3, ChronoUnit.HOURS))
                .build();
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
                // temporary scope used during initial testing
                .scope("test")
                .tokenSettings(tokenSettings())
                .clientSettings(ClientSettings.builder().requireAuthorizationConsent(false).build())
                .build();

        return new InMemoryRegisteredClientRepository(publicClient, confidentialClient);
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
    public ProviderSettings providerSettings() {
        return ProviderSettings.builder().build();
    }
}
