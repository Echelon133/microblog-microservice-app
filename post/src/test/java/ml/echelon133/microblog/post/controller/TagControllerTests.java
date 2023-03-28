package ml.echelon133.microblog.post.controller;

import ml.echelon133.microblog.post.service.TagService;
import ml.echelon133.microblog.shared.post.PostDto;
import ml.echelon133.microblog.shared.post.tag.TagDto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableHandlerMethodArgumentResolver;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.Date;
import java.util.List;
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
@DisplayName("Tests of TagController")
public class TagControllerTests {

    private MockMvc mvc;

    @Mock
    private TagService tagService;

    @InjectMocks
    private PostExceptionHandler postExceptionHandler;

    @InjectMocks
    private TagController tagController;

    @BeforeEach
    public void beforeEach() {
        mvc = MockMvcBuilders
                .standaloneSetup(tagController)
                .setControllerAdvice(postExceptionHandler)
                .setCustomArgumentResolvers(
                        // this is required to resolve Pageable objects in controller methods
                        new PageableHandlerMethodArgumentResolver()
                )
                .build();
    }

    @Test
    @DisplayName("getPopularTags shows error when service method throws")
    public void getPopularTags_ServiceThrows_ReturnsExpectedError() throws Exception {
        when(tagService.findFiveMostPopularInLast(100))
                .thenThrow(new IllegalArgumentException("hours values not in 1-24 range are not valid"));

        mvc.perform(
                        get("/api/tags/popular")
                                .contentType(MediaType.APPLICATION_JSON)
                                .accept(MediaType.APPLICATION_JSON)
                                .with(customBearerToken())
                                .param("last", "100")
                )
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.messages", hasSize(1)))
                .andExpect(jsonPath("$.messages", hasItem("hours values not in 1-24 range are not valid")));
    }

    @Test
    @DisplayName("getPopularTags param 'last' defaults to 1 if not provided")
    public void getPopularTags_ParamNotProvided_DefaultsAndReturnsOk() throws Exception {
        var tag = new TagDto(UUID.randomUUID(), "test");
        when(tagService.findFiveMostPopularInLast(1)).thenReturn(List.of(tag));

        mvc.perform(
                        get("/api/tags/popular")
                                .contentType(MediaType.APPLICATION_JSON)
                                .accept(MediaType.APPLICATION_JSON)
                                .with(customBearerToken())
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].id", is(tag.getId().toString())))
                .andExpect(jsonPath("$[0].name", is(tag.getName())));
    }

    @Test
    @DisplayName("getMostRecentPostsInTag sets a custom default page size")
    public void getMostRecentPostsInTag_NoPageSizeProvided_UsesCustomDefaultPageSizeAndReturnsOk() throws Exception {
        var tag = "test";
        var expectedDefaultPageSize = 20;
        var dto = new PostDto(UUID.randomUUID(), new Date(), "test", UUID.randomUUID(), null, null);

        var page = new PageImpl<>(List.of(dto), Pageable.ofSize(20), 1);
        // returns a page of size 20 only if tagService is given a
        // pageable with pageSize 20, otherwise will return null and the test will fail
        when(tagService.findMostRecentPostsTagged(
                eq(tag),
                argThat(p -> p.getPageSize() == expectedDefaultPageSize)
        )).thenReturn(page);

        mvc.perform(
                        get("/api/tags/" + tag + "/posts")
                                .contentType(MediaType.APPLICATION_JSON)
                                .accept(MediaType.APPLICATION_JSON)
                                .with(customBearerToken())
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.size", is(expectedDefaultPageSize)))
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.content[0].id", is(dto.getId().toString())))
                .andExpect(jsonPath("$.content[0].dateCreated", is(dto.getDateCreated().toInstant().toEpochMilli())))
                .andExpect(jsonPath("$.content[0].content", is(dto.getContent())))
                .andExpect(jsonPath("$.content[0].authorId", is(dto.getAuthorId().toString())))
                .andExpect(jsonPath("$.content[0].quotedPost", nullValue()))
                .andExpect(jsonPath("$.content[0].parentPost", nullValue()));
    }
}
