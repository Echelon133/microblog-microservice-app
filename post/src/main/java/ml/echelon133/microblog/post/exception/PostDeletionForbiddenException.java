package ml.echelon133.microblog.post.exception;

import java.util.UUID;

/**
 * Exception thrown when a user tries to delete a post which does not belong to them.
 */
public class PostDeletionForbiddenException extends Exception {

    public PostDeletionForbiddenException(UUID userId, UUID postId) {
        super(String.format("User with id '%s' cannot delete a post with id '%s'", userId, postId));
    }
}
