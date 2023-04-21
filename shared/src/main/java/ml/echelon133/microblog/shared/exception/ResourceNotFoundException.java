package ml.echelon133.microblog.shared.exception;

import java.util.UUID;

public class ResourceNotFoundException extends Exception {

    public ResourceNotFoundException(Class cls, UUID id) {
        super(String.format("%s %s could not be found", cls.getSimpleName().toLowerCase(), id));
    }
}
