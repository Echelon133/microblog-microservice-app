package ml.echelon133.microblog.post.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import ml.echelon133.microblog.post.exception.PostDeletionForbiddenException;
import ml.echelon133.microblog.post.exception.PostNotFoundException;
import ml.echelon133.microblog.post.service.PostService;
import ml.echelon133.microblog.shared.post.Post;
import ml.echelon133.microblog.shared.post.PostCreationDto;
import ml.echelon133.microblog.shared.post.PostDto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentMatchers;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.test.json.JacksonTester;
import org.springframework.boot.test.json.JsonContent;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableHandlerMethodArgumentResolver;
import org.springframework.http.MediaType;
import org.springframework.security.web.method.annotation.AuthenticationPrincipalArgumentResolver;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.Date;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static ml.echelon133.microblog.shared.auth.test.OAuth2RequestPostProcessor.*;
import static ml.echelon133.microblog.shared.auth.test.TestOpaqueTokenData.*;

/*
    Disable kubernetes during tests to make local execution of tests possible.
    If kubernetes is not disabled, tests won't execute at all because Spring will
    fail to configure kubernetes when run outside it.
 */
@TestPropertySource(properties = "spring.cloud.kubernetes.enabled=false")
@ExtendWith(MockitoExtension.class)
@DisplayName("Tests of PostController")
public class PostControllerTests {

    private MockMvc mvc;

    @Mock
    private PostService postService;

    @InjectMocks
    private PostExceptionHandler postExceptionHandler;

    @InjectMocks
    private PostController postController;

    private JacksonTester<PostCreationDto> jsonPostCreationDto;

    @BeforeEach
    public void beforeEach() {
        JacksonTester.initFields(this, new ObjectMapper());

        mvc = MockMvcBuilders
                .standaloneSetup(postController)
                .setControllerAdvice(postExceptionHandler)
                .setCustomArgumentResolvers(
                        // this is required to resolve @AuthenticationPrincipal in controller methods
                        new AuthenticationPrincipalArgumentResolver(),
                        // this is required to resolve Pageable objects in controller methods
                        new PageableHandlerMethodArgumentResolver()
                )
                .build();
    }

    @Test
    @DisplayName("createPost shows error when content's is empty")
    public void createPost_ContentNotProvided_ReturnsExpectedError() throws Exception {
        mvc.perform(
                        post("/api/posts")
                                .contentType(MediaType.APPLICATION_JSON)
                                .accept(MediaType.APPLICATION_JSON)
                                .with(customBearerToken())
                                .content("{}")
                )
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.messages", hasSize(1)))
                .andExpect(jsonPath("$.messages", hasItem("Post content not provided")));
    }

    @Test
    @DisplayName("createPost shows error when content's length is invalid")
    public void createPost_ContentLengthInvalid_ReturnsExpectedError() throws Exception {
        var invalidContents = List.of(
                // 301 characters (one too many to be valid)
                "YjMN0eBEGvMi0OTUloGIktYL3KLkFRkxj0tiXdywScsti2eTKKzOot6deoC1zp99S6Qu5728HaQ1FneQ9gThdbk0AI26NRZwkK7BjUa5cqhv4OfSxqK5WApzM8oTSNnjRPymUAQCyY4R7ADG8DsG4Th2RlmsPvfDQMVHF2KduLhyKF3j4Xugg9FVpT06eRqFNh3KTR1XVin0TQvMbDSu22HKqrSwJ0HrkkoLFJCUaIwZLA7FBUXu4ySysrnOJeoadfi6EHUkTHFF88OIi2yIH3QqlHPL7emCRluJtHcCSEq1h",
                // 400 characters (100 more than the upper limit)
                "pUfmMvcnY2apDAN8GcE5sRCYMbyrJrS9pUgCXyHKNYPusWr4Dwlg2NSPa3yMJUj4atSuTIzqs4g7wujb4B1O3XkjJnoEgxw9xEhq04md9UfTZUSJfSTpIZNh3GB3Z7IPknxiBEfvq9Qr5FkjmEbVVF8yig0PXF0Jfqwr9nxoNSte6nqcnvkar64VImhv5bWEcivm1mXvS2OmpfbpNPSHVcJMJaZ7CJvVdnqQHLRWkXjjdH033xBvB2mBqva7gvT3MFwnEj0MJHMBGb1Rp8Exm0Xt4Fkqhxy95ZvW3OgsoLssUd0NZ59ZFnN5B8MnkCJg5O6OQUtReFKw617HmH78KX7cAwpgABdd6LrK76fqSTXwrucdw8kzawVEqVtqEP8mSekapg5cJRcbHblh"
        );

        for (var invalidContent : invalidContents) {
            PostCreationDto dto = new PostCreationDto(invalidContent);
            JsonContent<PostCreationDto> json = jsonPostCreationDto.write(dto);
            mvc.perform(
                            post("/api/posts")
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .accept(MediaType.APPLICATION_JSON)
                                    .with(customBearerToken())
                                    .content(json.getJson())
                    )
                    .andExpect(status().isUnprocessableEntity())
                    .andExpect(jsonPath("$.messages", hasSize(1)))
                    .andExpect(jsonPath("$.messages", hasItem("Length of the post invalid")));
        }
    }

    @Test
    @DisplayName("createPost output ok when content's length is valid")
    public void createPost_ContentLengthValid_ReturnsOk() throws Exception {
        var contents = List.of(
                // minimum length of a post
                "a",
                // 300 characters (maximum valid length)
                "GwqYYnQfiZ3CZdlYxaNDkeAELhKuFZ3Zdv6jdwUTmy7WcDz5iY37wy41lj3WrsM64JZ17QGeGcVGC1b7BdW4gG98f4GN0HxeyWee9hyWNPT18UES2iO4xoF6OV4cMHD8eYiOfEv5cWXqmndzWqUvUNlKAdRthyn6uYgexeHmuKNgvYyEsYedLgZ8V8nSdKrUanOzzFrOE6gfqdKhMx76mChOsdRqa9TjGrlOq7QoS1cUydh76YDD0okyFEQkxxjQA8b3wm2lhQayLL7NCM4aJyALUVMJqiiisRzy3wUHUPZ5"
        );

        for (var content : contents) {
            var randomPostUuid = UUID.randomUUID();
            var post = new Post();
            post.setId(randomPostUuid);

            when(postService.createPost(
                    eq(UUID.fromString(PRINCIPAL_ID)),
                    argThat(dto -> dto.getContent().equals(content))
            )).thenReturn(post);

            PostCreationDto dto = new PostCreationDto(content);
            JsonContent<PostCreationDto> json = jsonPostCreationDto.write(dto);
            mvc.perform(
                            post("/api/posts")
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .accept(MediaType.APPLICATION_JSON)
                                    .with(customBearerToken())
                                    .content(json.getJson())
                    )
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", hasEntry("uuid", randomPostUuid.toString())));
        }
    }

    @Test
    @DisplayName("quotePost shows error when content's is empty")
    public void quotePost_ContentNotProvided_ReturnsExpectedError() throws Exception {
        var quotedPostId = UUID.randomUUID();
        mvc.perform(
                        post("/api/posts/" + quotedPostId + "/quotes")
                                .contentType(MediaType.APPLICATION_JSON)
                                .accept(MediaType.APPLICATION_JSON)
                                .with(customBearerToken())
                                .content("{}")
                )
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.messages", hasSize(1)))
                .andExpect(jsonPath("$.messages", hasItem("Post content not provided")));
    }

    @Test
    @DisplayName("quotePost shows error when content's length is invalid")
    public void quotePost_ContentLengthInvalid_ReturnsExpectedError() throws Exception {
        var quotedPostId = UUID.randomUUID();
        var invalidContents = List.of(
                // 301 characters (one too many to be valid)
                "YjMN0eBEGvMi0OTUloGIktYL3KLkFRkxj0tiXdywScsti2eTKKzOot6deoC1zp99S6Qu5728HaQ1FneQ9gThdbk0AI26NRZwkK7BjUa5cqhv4OfSxqK5WApzM8oTSNnjRPymUAQCyY4R7ADG8DsG4Th2RlmsPvfDQMVHF2KduLhyKF3j4Xugg9FVpT06eRqFNh3KTR1XVin0TQvMbDSu22HKqrSwJ0HrkkoLFJCUaIwZLA7FBUXu4ySysrnOJeoadfi6EHUkTHFF88OIi2yIH3QqlHPL7emCRluJtHcCSEq1h",
                // 400 characters (100 more than the upper limit)
                "pUfmMvcnY2apDAN8GcE5sRCYMbyrJrS9pUgCXyHKNYPusWr4Dwlg2NSPa3yMJUj4atSuTIzqs4g7wujb4B1O3XkjJnoEgxw9xEhq04md9UfTZUSJfSTpIZNh3GB3Z7IPknxiBEfvq9Qr5FkjmEbVVF8yig0PXF0Jfqwr9nxoNSte6nqcnvkar64VImhv5bWEcivm1mXvS2OmpfbpNPSHVcJMJaZ7CJvVdnqQHLRWkXjjdH033xBvB2mBqva7gvT3MFwnEj0MJHMBGb1Rp8Exm0Xt4Fkqhxy95ZvW3OgsoLssUd0NZ59ZFnN5B8MnkCJg5O6OQUtReFKw617HmH78KX7cAwpgABdd6LrK76fqSTXwrucdw8kzawVEqVtqEP8mSekapg5cJRcbHblh"
        );

        for (var invalidContent : invalidContents) {
            PostCreationDto dto = new PostCreationDto(invalidContent);
            JsonContent<PostCreationDto> json = jsonPostCreationDto.write(dto);
            mvc.perform(
                            post("/api/posts/" + quotedPostId + "/quotes")
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .accept(MediaType.APPLICATION_JSON)
                                    .with(customBearerToken())
                                    .content(json.getJson())
                    )
                    .andExpect(status().isUnprocessableEntity())
                    .andExpect(jsonPath("$.messages", hasSize(1)))
                    .andExpect(jsonPath("$.messages", hasItem("Length of the post invalid")));
        }
    }

    @Test
    @DisplayName("quotePost shows error when quoted post does not exist")
    public void quotePost_QuotedPostNotFound_ReturnsExpectedError() throws Exception {
        var quotedPostId = UUID.randomUUID();

        var content = "test content";
        PostCreationDto dto = new PostCreationDto(content);
        JsonContent<PostCreationDto> json = jsonPostCreationDto.write(dto);

        when(postService.createQuotePost(
                eq(UUID.fromString(PRINCIPAL_ID)),
                eq(quotedPostId),
                argThat(d -> d.getContent().equals(content))
        )).thenThrow(new PostNotFoundException(quotedPostId));

        mvc.perform(
                    post("/api/posts/" + quotedPostId + "/quotes")
                                .contentType(MediaType.APPLICATION_JSON)
                                .accept(MediaType.APPLICATION_JSON)
                                .with(customBearerToken())
                                .content(json.getJson())
                )
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.messages", hasSize(1)))
                .andExpect(jsonPath("$.messages", hasItem(
                        String.format("Post with id %s could not be found", quotedPostId))
                ));
    }

    @Test
    @DisplayName("quotePost output ok when content's length is valid")
    public void quotePost_ContentLengthValid_ReturnsOk() throws Exception {
        var quotedPostId = UUID.randomUUID();
        var contents = List.of(
                // minimum length of a post
                "a",
                // 300 characters (maximum valid length)
                "GwqYYnQfiZ3CZdlYxaNDkeAELhKuFZ3Zdv6jdwUTmy7WcDz5iY37wy41lj3WrsM64JZ17QGeGcVGC1b7BdW4gG98f4GN0HxeyWee9hyWNPT18UES2iO4xoF6OV4cMHD8eYiOfEv5cWXqmndzWqUvUNlKAdRthyn6uYgexeHmuKNgvYyEsYedLgZ8V8nSdKrUanOzzFrOE6gfqdKhMx76mChOsdRqa9TjGrlOq7QoS1cUydh76YDD0okyFEQkxxjQA8b3wm2lhQayLL7NCM4aJyALUVMJqiiisRzy3wUHUPZ5"
        );

        for (var content : contents) {
            var randomPostUuid = UUID.randomUUID();
            var post = new Post();
            post.setId(randomPostUuid);

            when(postService.createQuotePost(
                    eq(UUID.fromString(PRINCIPAL_ID)),
                    eq(quotedPostId),
                    argThat(dto -> dto.getContent().equals(content))
            )).thenReturn(post);

            PostCreationDto dto = new PostCreationDto(content);
            JsonContent<PostCreationDto> json = jsonPostCreationDto.write(dto);
            mvc.perform(
                        post("/api/posts/" + quotedPostId + "/quotes")
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .accept(MediaType.APPLICATION_JSON)
                                    .with(customBearerToken())
                                    .content(json.getJson())
                    )
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", hasEntry("uuid", randomPostUuid.toString())));
        }
    }

    @Test
    @DisplayName("respondToPost shows error when content's is empty")
    public void respondToPost_ContentNotProvided_ReturnsExpectedError() throws Exception {
        var parentPostId = UUID.randomUUID();
        mvc.perform(
                        post("/api/posts/" + parentPostId + "/responses")
                                .contentType(MediaType.APPLICATION_JSON)
                                .accept(MediaType.APPLICATION_JSON)
                                .with(customBearerToken())
                                .content("{}")
                )
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.messages", hasSize(1)))
                .andExpect(jsonPath("$.messages", hasItem("Post content not provided")));
    }

    @Test
    @DisplayName("respondToPost shows error when content's length is invalid")
    public void respondToPost_ContentLengthInvalid_ReturnsExpectedError() throws Exception {
        var parentPostId = UUID.randomUUID();
        var invalidContents = List.of(
                // 301 characters (one too many to be valid)
                "YjMN0eBEGvMi0OTUloGIktYL3KLkFRkxj0tiXdywScsti2eTKKzOot6deoC1zp99S6Qu5728HaQ1FneQ9gThdbk0AI26NRZwkK7BjUa5cqhv4OfSxqK5WApzM8oTSNnjRPymUAQCyY4R7ADG8DsG4Th2RlmsPvfDQMVHF2KduLhyKF3j4Xugg9FVpT06eRqFNh3KTR1XVin0TQvMbDSu22HKqrSwJ0HrkkoLFJCUaIwZLA7FBUXu4ySysrnOJeoadfi6EHUkTHFF88OIi2yIH3QqlHPL7emCRluJtHcCSEq1h",
                // 400 characters (100 more than the upper limit)
                "pUfmMvcnY2apDAN8GcE5sRCYMbyrJrS9pUgCXyHKNYPusWr4Dwlg2NSPa3yMJUj4atSuTIzqs4g7wujb4B1O3XkjJnoEgxw9xEhq04md9UfTZUSJfSTpIZNh3GB3Z7IPknxiBEfvq9Qr5FkjmEbVVF8yig0PXF0Jfqwr9nxoNSte6nqcnvkar64VImhv5bWEcivm1mXvS2OmpfbpNPSHVcJMJaZ7CJvVdnqQHLRWkXjjdH033xBvB2mBqva7gvT3MFwnEj0MJHMBGb1Rp8Exm0Xt4Fkqhxy95ZvW3OgsoLssUd0NZ59ZFnN5B8MnkCJg5O6OQUtReFKw617HmH78KX7cAwpgABdd6LrK76fqSTXwrucdw8kzawVEqVtqEP8mSekapg5cJRcbHblh"
        );

        for (var invalidContent : invalidContents) {
            PostCreationDto dto = new PostCreationDto(invalidContent);
            JsonContent<PostCreationDto> json = jsonPostCreationDto.write(dto);
            mvc.perform(
                            post("/api/posts/" + parentPostId + "/responses")
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .accept(MediaType.APPLICATION_JSON)
                                    .with(customBearerToken())
                                    .content(json.getJson())
                    )
                    .andExpect(status().isUnprocessableEntity())
                    .andExpect(jsonPath("$.messages", hasSize(1)))
                    .andExpect(jsonPath("$.messages", hasItem("Length of the post invalid")));
        }
    }

    @Test
    @DisplayName("respondToPost shows error when parent post does not exist")
    public void respondToPost_ParentPostNotFound_ReturnsExpectedError() throws Exception {
        var parentPostId = UUID.randomUUID();

        var content = "test content";
        PostCreationDto dto = new PostCreationDto(content);
        JsonContent<PostCreationDto> json = jsonPostCreationDto.write(dto);

        when(postService.createResponsePost(
                eq(UUID.fromString(PRINCIPAL_ID)),
                eq(parentPostId),
                argThat(d -> d.getContent().equals(content))
        )).thenThrow(new PostNotFoundException(parentPostId));

        mvc.perform(
                        post("/api/posts/" + parentPostId + "/responses")
                                .contentType(MediaType.APPLICATION_JSON)
                                .accept(MediaType.APPLICATION_JSON)
                                .with(customBearerToken())
                                .content(json.getJson())
                )
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.messages", hasSize(1)))
                .andExpect(jsonPath("$.messages", hasItem(
                        String.format("Post with id %s could not be found", parentPostId))
                ));
    }

    @Test
    @DisplayName("respondToPost output ok when content's length is valid")
    public void respondToPost_ContentLengthValid_ReturnsOk() throws Exception {
        var parentPostId = UUID.randomUUID();
        var contents = List.of(
                // minimum length of a post
                "a",
                // 300 characters (maximum valid length)
                "GwqYYnQfiZ3CZdlYxaNDkeAELhKuFZ3Zdv6jdwUTmy7WcDz5iY37wy41lj3WrsM64JZ17QGeGcVGC1b7BdW4gG98f4GN0HxeyWee9hyWNPT18UES2iO4xoF6OV4cMHD8eYiOfEv5cWXqmndzWqUvUNlKAdRthyn6uYgexeHmuKNgvYyEsYedLgZ8V8nSdKrUanOzzFrOE6gfqdKhMx76mChOsdRqa9TjGrlOq7QoS1cUydh76YDD0okyFEQkxxjQA8b3wm2lhQayLL7NCM4aJyALUVMJqiiisRzy3wUHUPZ5"
        );

        for (var content : contents) {
            var randomPostUuid = UUID.randomUUID();
            var post = new Post();
            post.setId(randomPostUuid);

            when(postService.createResponsePost(
                    eq(UUID.fromString(PRINCIPAL_ID)),
                    eq(parentPostId),
                    argThat(dto -> dto.getContent().equals(content))
            )).thenReturn(post);

            PostCreationDto dto = new PostCreationDto(content);
            JsonContent<PostCreationDto> json = jsonPostCreationDto.write(dto);
            mvc.perform(
                            post("/api/posts/" + parentPostId + "/responses")
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .accept(MediaType.APPLICATION_JSON)
                                    .with(customBearerToken())
                                    .content(json.getJson())
                    )
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", hasEntry("uuid", randomPostUuid.toString())));
        }
    }

    @Test
    @DisplayName("getLike returns ok when like exists")
    public void getLike_LikeExists_ReturnsOk() throws Exception {
        var postId = UUID.randomUUID();

        when(postService.likeExists(
                UUID.fromString(PRINCIPAL_ID),
                postId)
        ).thenReturn(true);

        mvc.perform(
                        get("/api/posts/" + postId + "/like")
                                .accept(MediaType.APPLICATION_JSON)
                                .with(customBearerToken())
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.likes", is(true)));
    }

    @Test
    @DisplayName("createLike returns error when service throws")
    public void createLike_ServiceThrows_ReturnsExpectedError() throws Exception {
        var postId = UUID.randomUUID();

        when(postService.likePost(
                UUID.fromString(PRINCIPAL_ID),
                postId)
        ).thenThrow(new PostNotFoundException(postId));

        mvc.perform(
                        post("/api/posts/" + postId + "/like")
                                .accept(MediaType.APPLICATION_JSON)
                                .with(customBearerToken())
                )
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.messages", hasSize(1)))
                .andExpect(jsonPath("$.messages",
                        hasItem(String.format("Post with id %s could not be found", postId))));
    }

    @Test
    @DisplayName("createLike returns ok when like created")
    public void createLike_LikeCreated_ReturnsOk() throws Exception {
        var postId = UUID.randomUUID();

        when(postService.likePost(
                UUID.fromString(PRINCIPAL_ID),
                postId)
        ).thenReturn(true);

        mvc.perform(
                        post("/api/posts/" + postId + "/like")
                                .accept(MediaType.APPLICATION_JSON)
                                .with(customBearerToken())
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.likes", is(true)));
    }

    @Test
    @DisplayName("deleteLike returns ok when like deleted")
    public void deleteLike_LikeDeleted_ReturnsOk() throws Exception {
        var postId = UUID.randomUUID();

        when(postService.unlikePost(
                UUID.fromString(PRINCIPAL_ID),
                postId)
        ).thenReturn(true);

        mvc.perform(
                        delete("/api/posts/" + postId + "/like")
                                .accept(MediaType.APPLICATION_JSON)
                                .with(customBearerToken())
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.likes", is(false)));
    }

    @Test
    @DisplayName("deleteLike returns error when service throws")
    public void deleteLike_ServiceThrows_ReturnsExpectedError() throws Exception {
        var postId = UUID.randomUUID();

        when(postService.unlikePost(
                UUID.fromString(PRINCIPAL_ID),
                postId)
        ).thenThrow(new PostNotFoundException(postId));

        mvc.perform(
                        delete("/api/posts/" + postId + "/like")
                                .accept(MediaType.APPLICATION_JSON)
                                .with(customBearerToken())
                )
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.messages", hasSize(1)))
                .andExpect(jsonPath("$.messages",
                        hasItem(String.format("Post with id %s could not be found", postId))));
    }

    @Test
    @DisplayName("deletePost returns error when service throws PostNotFoundException")
    public void deletePost_ServiceThrowsPostNotFound_ReturnsExpectedError() throws Exception {
        var userId = UUID.fromString(PRINCIPAL_ID);
        var postId = UUID.randomUUID();

        when(postService.deletePost(userId, postId))
                .thenThrow(new PostNotFoundException(postId));

        mvc.perform(
                        delete("/api/posts/" + postId)
                                .accept(MediaType.APPLICATION_JSON)
                                .with(customBearerToken())
                )
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.messages", hasSize(1)))
                .andExpect(jsonPath("$.messages",
                        hasItem(String.format("Post with id %s could not be found", postId))));
    }

    @Test
    @DisplayName("deletePost returns error when service throws PostDeletionForbiddenException")
    public void deletePost_ServiceThrowsPostDeletionForbidden_ReturnsExpectedError() throws Exception {
        var userId = UUID.fromString(PRINCIPAL_ID);
        var postId = UUID.randomUUID();

        when(postService.deletePost(userId, postId))
                .thenThrow(new PostDeletionForbiddenException(userId, postId));

        mvc.perform(
                        delete("/api/posts/" + postId)
                                .accept(MediaType.APPLICATION_JSON)
                                .with(customBearerToken())
                )
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.messages", hasSize(1)))
                .andExpect(jsonPath("$.messages",
                        hasItem(String.format("User with id '%s' cannot delete a post with id '%s'", userId, postId))));
    }

    @Test
    @DisplayName("deletePost returns ok when post deleted")
    public void deletePost_PostDeleted_ReturnsOk() throws Exception {
        var userId = UUID.fromString(PRINCIPAL_ID);
        var post = new Post(userId, "", Set.of());
        post.setDeleted(true);
        var postId = post.getId();

        when(postService.deletePost(userId, postId))
                .thenReturn(post);

        mvc.perform(
                        delete("/api/posts/" + postId)
                                .accept(MediaType.APPLICATION_JSON)
                                .with(customBearerToken())
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.deleted", is(true)));
    }

    @Test
    @DisplayName("deletePost returns error when service throws PostNotFoundException")
    public void getPost_ServiceThrows_ReturnsExpectedError() throws Exception {
        var postId = UUID.randomUUID();

        when(postService.findById(postId)).thenThrow(new PostNotFoundException(postId));

        mvc.perform(
                        get("/api/posts/" + postId)
                                .accept(MediaType.APPLICATION_JSON)
                                .with(customBearerToken())
                )
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.messages", hasSize(1)))
                .andExpect(jsonPath("$.messages",
                        hasItem(String.format("Post with id %s could not be found", postId))));
    }

    @Test
    @DisplayName("getPost returns ok when post exists")
    public void getPost_PostExists_ReturnsOk() throws Exception {
        var postId = UUID.randomUUID();
        var postDto = new PostDto(postId, new Date(), "post", UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID());

        when(postService.findById(postId)).thenReturn(postDto);

        mvc.perform(
                        get("/api/posts/" + postId)
                                .accept(MediaType.APPLICATION_JSON)
                                .with(customBearerToken())
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(postId.toString())))
                .andExpect(jsonPath("$.dateCreated", is(postDto.getDateCreated().toInstant().toEpochMilli())))
                .andExpect(jsonPath("$.content", is(postDto.getContent())))
                .andExpect(jsonPath("$.authorId", is(postDto.getAuthorId().toString())))
                .andExpect(jsonPath("$.quotedPost", is(postDto.getQuotedPost().toString())))
                .andExpect(jsonPath("$.parentPost", is(postDto.getParentPost().toString())));
    }

    @Test
    @DisplayName("getMostRecentUserPosts returns ok when posts found")
    public void getMostRecentUserPosts_PostsFound_ReturnsOk() throws Exception {
        var userId = UUID.randomUUID();
        var dto = new PostDto(UUID.randomUUID(), new Date(), "post", userId, UUID.randomUUID(), UUID.randomUUID());

        var page = new PageImpl<>(List.of(dto));

        when(postService.findMostRecentPostsOfUser(eq(userId), isA(Pageable.class)))
                .thenReturn(page);

        mvc.perform(
                        get("/api/posts")
                                .accept(MediaType.APPLICATION_JSON)
                                .with(customBearerToken())
                                .param("user_id", userId.toString())
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements", is(1)))
                .andExpect(jsonPath("$.last", is(true)))
                .andExpect(jsonPath("$.content[0].id", is(dto.getId().toString())))
                .andExpect(jsonPath("$.content[0].dateCreated", is(dto.getDateCreated().toInstant().toEpochMilli())))
                .andExpect(jsonPath("$.content[0].content", is(dto.getContent())))
                .andExpect(jsonPath("$.content[0].authorId", is(dto.getAuthorId().toString())))
                .andExpect(jsonPath("$.content[0].quotedPost", is(dto.getQuotedPost().toString())))
                .andExpect(jsonPath("$.content[0].parentPost", is(dto.getParentPost().toString())));
    }

    @Test
    @DisplayName("getMostRecentUserPosts returns error when param 'user_id' not provided")
    public void getMostRecentUserPosts_UserIdNotProvided_ReturnsExpectedError() throws Exception {
        mvc.perform(
                        get("/api/posts")
                                .accept(MediaType.APPLICATION_JSON)
                                .with(customBearerToken())
                )
                .andExpect(status().isBadRequest());
    }
}
