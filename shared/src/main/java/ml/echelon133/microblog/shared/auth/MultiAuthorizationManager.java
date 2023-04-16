package ml.echelon133.microblog.shared.auth;

import org.springframework.security.authorization.AuthorizationDecision;
import org.springframework.security.authorization.AuthorizationManager;
import org.springframework.security.core.Authentication;
import org.springframework.util.Assert;

import java.util.Arrays;
import java.util.List;
import java.util.function.Supplier;

/**
 * An {@link AuthorizationManager} which determines if every {@link AuthorizationManager} provided in the constructor
 * grants access to a specific authentication.
 *
 * Even a single {@link AuthorizationManager} not granting access to an authentication means that access is not
 * granted at all.
 *
 * @param <T> the type of object that the authorization check is being done on
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

    /**
     * Creates a {@link MultiAuthorizationManager} which iterates over all provided {@link AuthorizationManager}s
     * and only grants a positive {@link AuthorizationDecision} if all the authorization managers grant positive
     * {@link AuthorizationDecision}.
     *
     * Even a single negative {@link AuthorizationDecision} from one of the {@link AuthorizationManager}s means
     * that the final decision is negative.
     *
     * @param authManagers all managers which have to grant a positive {@link AuthorizationDecision} to a specific authentication
     * @param <T> the type of object that the authorization check is being done on
     * @return {@link AuthorizationManager} which checks multiple {@link AuthorizationManager}s at once
     */
    public static <T> MultiAuthorizationManager<T> hasAll(AuthorizationManager... authManagers) {
        Assert.notEmpty(authManagers, "at least one authorizationManager has to be provided");
        Assert.noNullElements(authManagers, "null authorizationManager not allowed");
        return new MultiAuthorizationManager(Arrays.asList(authManagers));
    }
}
