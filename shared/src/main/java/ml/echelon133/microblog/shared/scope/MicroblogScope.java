package ml.echelon133.microblog.shared.scope;

import java.util.Set;

/**
 * Constants representing scopes used in this microblog application.
 */
public class MicroblogScope {

    public static final Set<String> ALL_SCOPES;

    /**
     * Posts, replies, and quotes that are available to users.
     */
    public static final String POST_READ = "post.read";

    /**
     * Creation of posts, replies, and quotes.
     */
    public static final String POST_WRITE = "post.write";

    /**
     * Reading information about user accounts, such as their:
     * <ul>
     *     <li>username</li>
     *     <li>displayed username</li>
     *     <li>avatar url</li>
     *     <li>profile description</li>
     * </ul>
     */
    public static final String USER_READ = "user.read";

    /**
     * Modifying information about the user own account, such as their:
     * <ul>
     *     <li>profile description</li>
     *     <li>avatar image</li>
     *     <li>displayed username</li>
     * </ul>
     */
    public static final String USER_WRITE = "user.write";

    /**
     * Reading who follows and who is being followed by users.
     */
    public static final String FOLLOW_READ = "follow.read";

    /**
     * Following and unfollowing other users.
     */
    public static final String FOLLOW_WRITE = "follow.write";

    /**
     * Reading likes of posts, replies, and quotes.
     */
    public static final String LIKE_READ = "like.read";

    /**
     * Liking and unliking posts, replies, and quotes.
     */
    public static final String LIKE_WRITE = "like.write";

    /**
     * Reading information about user's notifications.
     */
    public static final String NOTIFICATION_READ = "notification.read";

    /**
     * Modifying notifications to mark them as 'read'.
     */
    public static final String NOTIFICATION_WRITE = "notification.write";

    /**
     * Scopes only available to administrators.
     */
    public static class Admin {

        public static final Set<String> ALL_ADMIN_SCOPES;

        /**
         * Reading reports submitted by users.
         */
        public static final String REPORT_READ = "report.read";

        /**
         * Accepting/rejecting reports (which might result in deletion of content which violates rules).
         */
        public static final String REPORT_WRITE = "report.write";

        static {
            ALL_ADMIN_SCOPES = Set.of(REPORT_READ, REPORT_WRITE);
        }
    }

    static {
        ALL_SCOPES = Set.of(
                POST_READ, POST_WRITE,
                USER_READ, USER_WRITE,
                FOLLOW_READ, FOLLOW_WRITE,
                LIKE_READ, LIKE_WRITE,
                NOTIFICATION_READ, NOTIFICATION_WRITE
        );
    }

    /**
     * Prefixes a scope name with 'SCOPE_', which is the prefix of every scope-based {@link org.springframework.security.core.GrantedAuthority}.
     * @param scope scope name of the scope to be prefixed
     * @return prefixed scope
     */
    public static String prefix(String scope) {
        return "SCOPE_" + scope;
    }
}
