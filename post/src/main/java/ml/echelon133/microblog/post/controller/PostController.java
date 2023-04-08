package ml.echelon133.microblog.post.controller;

import ml.echelon133.microblog.post.exception.InvalidPostContentException;
import ml.echelon133.microblog.post.exception.PostDeletionForbiddenException;
import ml.echelon133.microblog.post.exception.PostNotFoundException;
import ml.echelon133.microblog.post.service.PostService;
import ml.echelon133.microblog.shared.post.PostCountersDto;
import ml.echelon133.microblog.shared.post.PostCreationDto;
import ml.echelon133.microblog.shared.post.PostDto;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.support.DefaultMessageSourceResolvable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.OAuth2AuthenticatedPrincipal;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static ml.echelon133.microblog.shared.auth.TokenOwnerIdExtractor.extractTokenOwnerIdFromPrincipal;

@RestController
@RequestMapping("/api/posts")
public class PostController {

    private PostService postService;

    @Autowired
    public PostController(PostService postService) {
        this.postService = postService;
    }

    @GetMapping("/{id}")
    public PostDto getPost(@PathVariable UUID id) throws PostNotFoundException {
        return postService.findById(id);
    }

    @GetMapping("/{id}/post-counters")
    public PostCountersDto getPostCounters(@PathVariable UUID id) throws PostNotFoundException {
        return postService.findPostCounters(id);
    }

    @GetMapping("/{id}/quotes")
    public Page<PostDto> getMostRecentQuotesOfPost(Pageable pageable, @PathVariable UUID id) {
        return postService.findMostRecentQuotesOfPost(id, pageable);
    }

    @GetMapping("/{id}/responses")
    public Page<PostDto> getMostRecentResponsesToPost(Pageable pageable, @PathVariable UUID id) {
        return postService.findMostRecentResponsesToPost(id, pageable);
    }

    @GetMapping
    public Page<PostDto> getMostRecentUserPosts(Pageable pageable, @RequestParam(name = "user_id") UUID userId) {
        return postService.findMostRecentPostsOfUser(userId, pageable);
    }

    @PostMapping
    public Map<String, UUID> createPost(@Valid @RequestBody PostCreationDto dto, BindingResult result,
                                        @AuthenticationPrincipal OAuth2AuthenticatedPrincipal principal)
            throws InvalidPostContentException {

        var id = extractTokenOwnerIdFromPrincipal(principal);

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

    @DeleteMapping("/{postId}")
    public Map<String, Boolean> deletePost(@PathVariable UUID postId,
                                           @AuthenticationPrincipal OAuth2AuthenticatedPrincipal principal)
            throws PostNotFoundException, PostDeletionForbiddenException {

        var id = extractTokenOwnerIdFromPrincipal(principal);
        return Map.of("deleted", postService.deletePost(id, postId).isDeleted());
    }

    @PostMapping("/{quotedPostId}/quotes")
    public Map<String, UUID> quotePost(@Valid @RequestBody PostCreationDto dto, BindingResult result,
                                       @PathVariable UUID quotedPostId,
                                       @AuthenticationPrincipal OAuth2AuthenticatedPrincipal principal)
            throws InvalidPostContentException, PostNotFoundException {

        var id = extractTokenOwnerIdFromPrincipal(principal);

        if (result.hasErrors()) {
            List<String> errorMessages = result
                    .getAllErrors()
                    .stream()
                    .map(DefaultMessageSourceResolvable::getDefaultMessage)
                    .toList();
            throw new InvalidPostContentException(errorMessages);
        }

        return Map.of("uuid", postService.createQuotePost(id, quotedPostId, dto).getId());
    }

    @PostMapping("/{parentPostId}/responses")
    public Map<String, UUID> respondToPost(@Valid @RequestBody PostCreationDto dto, BindingResult result,
                                           @PathVariable UUID parentPostId,
                                           @AuthenticationPrincipal OAuth2AuthenticatedPrincipal principal)
            throws InvalidPostContentException, PostNotFoundException {

        var id = extractTokenOwnerIdFromPrincipal(principal);

        if (result.hasErrors()) {
            List<String> errorMessages = result
                    .getAllErrors()
                    .stream()
                    .map(DefaultMessageSourceResolvable::getDefaultMessage)
                    .toList();
            throw new InvalidPostContentException(errorMessages);
        }

        return Map.of("uuid", postService.createResponsePost(id, parentPostId, dto).getId());
    }

    @GetMapping("/{postId}/like")
    public Map<String, Boolean> getLike(@PathVariable UUID postId,
                                        @AuthenticationPrincipal OAuth2AuthenticatedPrincipal principal) {
        var id = extractTokenOwnerIdFromPrincipal(principal);
        return Map.of("likes", postService.likeExists(id, postId));
    }

    @PostMapping("/{postId}/like")
    public Map<String, Boolean> createLike(@PathVariable UUID postId,
                                           @AuthenticationPrincipal OAuth2AuthenticatedPrincipal principal)
            throws PostNotFoundException {

        var id = extractTokenOwnerIdFromPrincipal(principal);
        return Map.of("likes", postService.likePost(id, postId));
    }

    @DeleteMapping("/{postId}/like")
    public Map<String, Boolean> deleteLike(@PathVariable UUID postId,
                                           @AuthenticationPrincipal OAuth2AuthenticatedPrincipal principal)
            throws PostNotFoundException {

        var id = extractTokenOwnerIdFromPrincipal(principal);

        // negate the value, because unlikePost returns true when like gets deleted, whereas this method
        // returns information about the existence of the like relationship
        return Map.of("likes", !postService.unlikePost(id, postId));
    }
}
