package ml.echelon133.microblog.post.exception;

import ml.echelon133.microblog.shared.post.tag.Tag;

import java.util.UUID;

/**
 * Exception thrown when a {@link Tag} with specified {@link UUID} or name
 * could not be found in the database.
 */
public class TagNotFoundException extends Exception {

    public TagNotFoundException(String tagName) {
        super(String.format("Tag '%s' could not be found", tagName));
    }

    public TagNotFoundException(UUID id) {
        super(String.format("Tag with id %s could not be found", id.toString()));
    }
}
