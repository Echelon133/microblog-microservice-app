package ml.echelon133.microblog.shared.auth;

import org.springframework.security.oauth2.core.OAuth2AuthenticatedPrincipal;

import java.util.Objects;
import java.util.UUID;

public class TokenOwnerIdExtractor {

    public static String TOKEN_OWNER_KEY = "token-owner-id";

    /**
     * Extracts the {@link UUID} of the user from the bearer token owned by that user.
     *
     * @param principal principal containing the token which represents an OAuth2 authenticated user
     * @return id of the user who owns the token contained in the principal
     */
    public static UUID extractTokenOwnerIdFromPrincipal(OAuth2AuthenticatedPrincipal principal) {
        return UUID.fromString(Objects.requireNonNull(principal.getAttribute(TOKEN_OWNER_KEY)));
    }
}
