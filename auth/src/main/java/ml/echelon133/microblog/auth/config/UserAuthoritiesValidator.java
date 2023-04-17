package ml.echelon133.microblog.auth.config;

import ml.echelon133.microblog.shared.user.Roles;
import ml.echelon133.microblog.shared.user.User;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2ErrorCodes;
import ml.echelon133.microblog.shared.scope.MicroblogScope;
import org.springframework.security.oauth2.server.authorization.authentication.OAuth2AuthorizationCodeRequestAuthenticationContext;
import org.springframework.security.oauth2.server.authorization.authentication.OAuth2AuthorizationCodeRequestAuthenticationException;
import org.springframework.security.oauth2.server.authorization.authentication.OAuth2AuthorizationCodeRequestAuthenticationToken;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClient;

import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;

import static org.springframework.security.oauth2.core.endpoint.OAuth2ParameterNames.ERROR_URI;

/**
 * Validates if a user who has a certain set of {@link org.springframework.security.core.GrantedAuthority} should be
 * allowed to receive a code from a particular {@link RegisteredClient}.
 *
 * This is required to avoid a situation where a regular user is able to receive an access token containing
 * scopes which should only be accessible to administrators.
 *
 * A regular user with <strong>ROLE_USER</strong> must not be allowed to request and receive scopes
 * from {@link MicroblogScope.Admin}.
 *
 */
public class UserAuthoritiesValidator implements Consumer<OAuth2AuthorizationCodeRequestAuthenticationContext> {

    @Override
    public void accept(OAuth2AuthorizationCodeRequestAuthenticationContext authenticationContext) {
        OAuth2AuthorizationCodeRequestAuthenticationToken authorizationCodeRequestAuthentication =
                authenticationContext.getAuthentication();

        // this 'accept' method is called twice during code request phase of the auth flow:
        // - before the user provides their username+password (principal is anonymous)
        // - after the user provides their username+password (principal contains the User loaded from the UserDetailsService)
        //
        // make sure the token is not anonymous, because that proves this method is being called for the second time,
        // after loading the User object from the database
        if (authorizationCodeRequestAuthentication.getPrincipal() instanceof UsernamePasswordAuthenticationToken upToken) {
            // this cast is safe because UsernamePasswordAuthenticationToken is guaranteed to hold User objects from
            // UserDetailsService
            User user = (User)upToken.getPrincipal();

            RegisteredClient registeredClient = authenticationContext.getRegisteredClient();
            List<String> userAuthorities = user.getAuthorities().stream().map(GrantedAuthority::getAuthority).toList();

            // there is nothing to validate if the logged-in user is not a regular user with ROLE_USER
            if (userAuthorities.contains(Roles.ROLE_USER.name())) {
                // check if the registered client distributes any scopes which are reserved for administrators
                var containsAnyAdminScopes = !Collections.disjoint(
                        registeredClient.getScopes(),
                        MicroblogScope.Admin.ALL_ADMIN_SCOPES
                );
                if (containsAnyAdminScopes) {
                    OAuth2Error error = new OAuth2Error(
                            OAuth2ErrorCodes.ACCESS_DENIED,
                            "user is not authorized to receive requested scopes",
                            ERROR_URI
                    );
                    throw new OAuth2AuthorizationCodeRequestAuthenticationException(error, null);
                }
            }
        }

    }
}
