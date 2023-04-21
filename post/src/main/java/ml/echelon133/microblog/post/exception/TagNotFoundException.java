package ml.echelon133.microblog.post.exception;

import ml.echelon133.microblog.shared.post.tag.Tag;

import java.util.UUID;

/**
 * Exception thrown when a {@link Tag} with specified name could not be found in the database.
 */
public class TagNotFoundException extends Exception {

    public TagNotFoundException(String tagName) {
        super(String.format("tag #%s could not be found", tagName));
    }
}
