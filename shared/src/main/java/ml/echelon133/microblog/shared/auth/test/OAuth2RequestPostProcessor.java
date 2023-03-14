package ml.echelon133.microblog.shared.auth.test;

import ml.echelon133.microblog.shared.scope.MicroblogScope;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.core.OAuth2AccessToken;
import org.springframework.security.oauth2.server.resource.authentication.BearerTokenAuthentication;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import java.time.Instant;
import java.util.List;

import static ml.echelon133.microblog.shared.auth.test.TestOpaqueTokenData.*;

public class OAuth2RequestPostProcessor {

    public static RequestPostProcessor customBearerToken() {
        var securityContext = SecurityContextHolder.getContext();

        var principal = new OAuth2TestPrincipal.Builder()
                .setTokenOwnerId(PRINCIPAL_ID)
                .setUsername(USERNAME)
                .setScopes(List.copyOf(MicroblogScope.ALL_SCOPES))
                .build();

        var authorities = principal.getAuthorities();
        var attributes = principal.getAttributes();

        var accessToken = new OAuth2AccessToken(
                OAuth2AccessToken.TokenType.BEARER,
                ACCESS_TOKEN,
                (Instant) attributes.get("iat"),
                (Instant) attributes.get("exp"),
                MicroblogScope.ALL_SCOPES);

        var auth = new BearerTokenAuthentication(principal, accessToken, authorities);
        securityContext.setAuthentication(auth);

        return SecurityMockMvcRequestPostProcessors.securityContext(securityContext);
    }
}
