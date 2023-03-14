package ml.echelon133.microblog.shared.auth;

import org.springframework.security.authorization.AuthorizationDecision;
import org.springframework.security.authorization.AuthorizationManager;
import org.springframework.security.core.Authentication;
import org.springframework.util.Assert;

import java.util.Arrays;
import java.util.List;
import java.util.function.Supplier;

/**
 * AuthorizationManager which enables checking if a user has multiple authorities.
 *
 * @param <T>
 */
public class MultiAuthorizationManager<T> implements AuthorizationManager<T> {

    private final List<AuthorizationManager<T>> authorizationManagers;

    public MultiAuthorizationManager(List<AuthorizationManager<T>> authorizationManagers) {
        this.authorizationManagers = authorizationManagers;
    }

    @Override
    public AuthorizationDecision check(Supplier<Authentication> authentication, T object) {
        for (var authManager : this.authorizationManagers) {
            var decision = authManager.check(authentication, object);
            if (decision == null || !decision.isGranted()) {
                return new AuthorizationDecision(false);
            }
        }
        return new AuthorizationDecision(true);
    }

    public static <T> MultiAuthorizationManager<T> hasAll(AuthorizationManager... authManagers) {
        Assert.notEmpty(authManagers, "at least one authorizationManager has to be provided");
        Assert.noNullElements(authManagers, "null authorizationManager not allowed");
        return new MultiAuthorizationManager(Arrays.asList(authManagers));
    }
}
