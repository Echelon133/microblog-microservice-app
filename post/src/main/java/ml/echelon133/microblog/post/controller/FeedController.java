package ml.echelon133.microblog.post.controller;

import ml.echelon133.microblog.post.service.PostService;
import ml.echelon133.microblog.shared.post.PostDto;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.core.OAuth2AuthenticatedPrincipal;
import org.springframework.security.oauth2.server.resource.authentication.BearerTokenAuthentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Optional;
import java.util.UUID;

import static ml.echelon133.microblog.shared.auth.TokenOwnerIdExtractor.*;

@RestController
@RequestMapping("/api/feed")
public class FeedController {

    private PostService postService;

    @Autowired
    public FeedController(PostService postService) {
        this.postService = postService;
    }

    @GetMapping
    public Page<PostDto> getFeed(@PageableDefault(size = 20) Pageable pageable,
                                 @RequestParam(defaultValue = "false", required = false) boolean popular,
                                 @RequestParam(defaultValue = "6", required = false) Integer last) {

        var auth = SecurityContextHolder.getContext().getAuthentication();

        Optional<UUID> userId = Optional.empty();

        // only set userId when it's accessible through an authenticated user, otherwise assume the user
        // is anonymous
        if (auth instanceof BearerTokenAuthentication token) {
            OAuth2AuthenticatedPrincipal principal = (OAuth2AuthenticatedPrincipal)token.getPrincipal();
            var id = extractTokenOwnerIdFromPrincipal(principal);
            userId = Optional.of(id);
        }

        return postService.generateFeed(userId, popular, last, pageable);
    }
}
