package ml.echelon133.microblog.post.exception;

import ml.echelon133.microblog.shared.post.Post;

import java.util.UUID;

/**
 * Exception thrown when {@link Post} with specified {@link UUID}
 * could not be found in the database.
 */
public class PostNotFoundException extends Exception {

    public PostNotFoundException(UUID id) {
        super(String.format("Post with id %s could not be found", id.toString()));
    }
}