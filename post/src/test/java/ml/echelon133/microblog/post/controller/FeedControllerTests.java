package ml.echelon133.microblog.post.controller;

import ml.echelon133.microblog.post.service.PostService;
import ml.echelon133.microblog.shared.auth.test.TestOpaqueTokenData;
import ml.echelon133.microblog.shared.post.PostDto;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.web.PageableHandlerMethodArgumentResolver;
import org.springframework.http.MediaType;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static ml.echelon133.microblog.shared.auth.test.OAuth2RequestPostProcessor.customBearerToken;
import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
@DisplayName("Tests of FeedController")
public class FeedControllerTests {

    private MockMvc mvc;

    @Mock
    private PostService postService;

    @InjectMocks
    private PostExceptionHandler postExceptionHandler;

    @InjectMocks
    private FeedController feedController;

    @BeforeEach
    public void beforeEach() {
        mvc = MockMvcBuilders
                .standaloneSetup(feedController)
                .setControllerAdvice(postExceptionHandler)
                .setCustomArgumentResolvers(
                        // this is required to resolve Pageable objects in controller methods
                        new PageableHandlerMethodArgumentResolver()
                )
                .build();
    }

    @AfterEach
    public void afterEach() {
        // reset authentication after every test, otherwise the result of tests might
        // depend on the order of their execution

    }

    @Test
    @DisplayName("getFeed sets default values of request params and returns ok")
    public void getFeed_NoAuthAndRequestParamsNotProvided_SetsDefaultsAndReturnsOk() throws Exception {
        var dto = new PostDto(UUID.randomUUID(), new Date(), "test", UUID.randomUUID(), null, null);

        // default values when user is anonymous:
        // * popular is set to false
        // * last is set to 6
        // * pageSize is set to 20
        when(postService.generateFeed(
                eq(Optional.empty()),
                eq(false),
                eq(6),
                argThat(pageable -> pageable.getPageSize() == 20)
        )).thenReturn(new PageImpl<>(List.of(dto)));

        // make sure that the authentication is null
        SecurityContextHolder.getContext().setAuthentication(null);

        mvc.perform(
                        get("/api/feed")
                                .contentType(MediaType.APPLICATION_JSON)
                                .accept(MediaType.APPLICATION_JSON)
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements", is(1)))
                .andExpect(jsonPath("$.content[0].id", is(dto.getId().toString())))
                .andExpect(jsonPath("$.content[0].dateCreated", is(dto.getDateCreated().toInstant().toEpochMilli())))
                .andExpect(jsonPath("$.content[0].content", is(dto.getContent())))
                .andExpect(jsonPath("$.content[0].authorId", is(dto.getAuthorId().toString())))
                .andExpect(jsonPath("$.content[0].quotedPost", nullValue()))
                .andExpect(jsonPath("$.content[0].parentPost", nullValue()));
    }

    @Test
    @DisplayName("getFeed sets custom values of request params and returns ok")
    public void getFeed_AuthAndRequestParamsProvided_OverridesDefaultsAndReturnsOk() throws Exception {
        var dto = new PostDto(UUID.randomUUID(), new Date(), "test", UUID.randomUUID(), null, null);

        var userId = Optional.of(UUID.fromString(TestOpaqueTokenData.PRINCIPAL_ID));
        Boolean popular = true;
        Integer last = 15;
        Integer pageSize = 5;

        when(postService.generateFeed(
                eq(userId),
                eq(popular),
                eq(last),
                argThat(pageable -> pageable.getPageSize() == pageSize)
        )).thenReturn(new PageImpl<>(List.of(dto)));

        mvc.perform(
                        get("/api/feed")
                                .contentType(MediaType.APPLICATION_JSON)
                                .accept(MediaType.APPLICATION_JSON)
                                .with(customBearerToken())
                                .param("popular", popular.toString())
                                .param("size", pageSize.toString())
                                .param("last", last.toString())
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements", is(1)))
                .andExpect(jsonPath("$.content[0].id", is(dto.getId().toString())))
                .andExpect(jsonPath("$.content[0].dateCreated", is(dto.getDateCreated().toInstant().toEpochMilli())))
                .andExpect(jsonPath("$.content[0].content", is(dto.getContent())))
                .andExpect(jsonPath("$.content[0].authorId", is(dto.getAuthorId().toString())))
                .andExpect(jsonPath("$.content[0].quotedPost", nullValue()))
                .andExpect(jsonPath("$.content[0].parentPost", nullValue()));
    }
}
