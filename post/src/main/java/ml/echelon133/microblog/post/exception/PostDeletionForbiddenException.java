package ml.echelon133.microblog.post.exception;

import java.util.UUID;

public class PostDeletionForbiddenException extends Exception {

    public PostDeletionForbiddenException(UUID userId, UUID postId) {
        super(String.format("User with id '%s' cannot delete a post with id '%s'", userId, postId));
    }
}
