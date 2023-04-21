package ml.echelon133.microblog.post.exception;

/**
 * Exception thrown when a user tries to delete a post which does not belong to them.
 */
public class PostDeletionForbiddenException extends Exception {

    public PostDeletionForbiddenException() {
        super("users can only delete their own posts");
    }
}
