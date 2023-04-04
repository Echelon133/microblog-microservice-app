package ml.echelon133.microblog.post.web;

import ml.echelon133.microblog.shared.user.UserDto;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;

import java.util.List;

public class UserServiceClientFallback implements UserServiceClient {

    private static final Logger LOGGER = LogManager.getLogger(UserServiceClientFallback.class);

    @Override
    public Page<UserDto> getUserExact(String username) {
        LOGGER.debug(String.format(
                "Failed to fetch a page with information about user '%s'. Returning a default, empty page", username
        ));
        // default to an empty page, which means that a user might (in worst case scenario) miss a notification about
        // being mentioned in a post
        return new PageImpl<>(List.of());
    }
}
