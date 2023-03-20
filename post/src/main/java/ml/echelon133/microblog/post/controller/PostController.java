package ml.echelon133.microblog.post.controller;

import ml.echelon133.microblog.post.exception.InvalidPostContentException;
import ml.echelon133.microblog.post.service.PostService;
import ml.echelon133.microblog.shared.post.PostCreationDto;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.support.DefaultMessageSourceResolvable;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.OAuth2AuthenticatedPrincipal;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

@RestController
@RequestMapping("/api/posts")
public class PostController {

    private PostService postService;

    @Autowired
    public PostController(PostService postService) {
        this.postService = postService;
    }

    @PostMapping
    public Map<String, UUID> createPost(@Valid @RequestBody PostCreationDto dto, BindingResult result,
                                        @AuthenticationPrincipal OAuth2AuthenticatedPrincipal principal)
            throws InvalidPostContentException {

        var id = UUID.fromString(Objects.requireNonNull(principal.getAttribute("token-owner-id")));

        if (result.hasErrors()) {
            List<String> errorMessages = result
                    .getAllErrors()
                    .stream()
                    .map(DefaultMessageSourceResolvable::getDefaultMessage)
                    .toList();
            throw new InvalidPostContentException(errorMessages);
        }

        return Map.of("uuid", postService.createPost(id, dto).getId());
    }
}
