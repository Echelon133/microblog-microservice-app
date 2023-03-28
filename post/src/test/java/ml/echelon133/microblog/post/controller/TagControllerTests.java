package ml.echelon133.microblog.post.controller;

import ml.echelon133.microblog.post.service.TagService;
import ml.echelon133.microblog.shared.post.tag.TagDto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.web.PageableHandlerMethodArgumentResolver;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;
import java.util.UUID;

import static ml.echelon133.microblog.shared.auth.test.OAuth2RequestPostProcessor.customBearerToken;
import static org.hamcrest.Matchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
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
                .andExpect(jsonPath("$[0].name", is(tag.getName())))
                .andDo(print());
    }
}
