package ml.echelon133.microblog.post.service;

import ml.echelon133.microblog.post.exception.TagNotFoundException;
import ml.echelon133.microblog.post.repository.PostRepository;
import ml.echelon133.microblog.shared.post.Post;
import ml.echelon133.microblog.shared.post.PostCreationDto;
import ml.echelon133.microblog.shared.post.tag.Tag;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentMatcher;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.context.TestPropertySource;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

/*
    Disable kubernetes during tests to make local execution of tests possible.
    If kubernetes is not disabled, tests won't execute at all because Spring will
    fail to configure kubernetes when run outside it.
 */
@TestPropertySource(properties = "spring.cloud.kubernetes.enabled=false")
@ExtendWith(MockitoExtension.class)
@DisplayName("Tests of PostService")
public class PostServiceTests {

    @Mock
    private PostRepository postRepository;

    @Mock
    private TagService tagService;

    @InjectMocks
    private PostService postService;

    private static class PostsEqualMatcher implements ArgumentMatcher<Post> {
        private UUID expectedAuthorId;
        private String expectedContent;
        private List<String> expectedTags;

        private PostsEqualMatcher(UUID expectedAuthorId, String expectedContent, List<String> expectedTags) {
            this.expectedAuthorId = expectedAuthorId;
            this.expectedContent = expectedContent;
            // all detected tags are represented in all lowercase to avoid duplication of tags
            this.expectedTags = expectedTags.stream().map(String::toLowerCase).toList();
        }

        @Override
        public boolean matches(Post argument) {
            var allTags = argument.getTags()
                    .stream().map(Tag::getName)
                    .toList();
            return argument.getAuthorId().equals(expectedAuthorId) &&
                   argument.getContent().equals(expectedContent)   &&
                   allTags.containsAll(expectedTags);
        }

        public static PostsEqualMatcher postThat(UUID expectedAuthorId, String expectedContent, List<String> expectedTags) {
            return new PostsEqualMatcher(expectedAuthorId, expectedContent, expectedTags);
        }
    }

    @Test
    @DisplayName("createPost finds new tags in post's content")
    public void createPost_ContentWithNewTags_FindsAllTags() throws Exception {
        var authorId = UUID.randomUUID();
        var tag1 = "test";
        var tag2 = "anothertest";
        var postDto = new PostCreationDto(String.format("This is #%s and #%s", tag1, tag2));

        // given
        given(tagService.findByName(tag1)).willThrow(new TagNotFoundException(tag1));
        given(tagService.findByName(tag2)).willThrow(new TagNotFoundException(tag2));

        // when
        postService.createPost(authorId, postDto);

        // then
        verify(postRepository, times(1)).save(argThat(
                PostsEqualMatcher.postThat(authorId, postDto.getContent(), List.of(tag1, tag2))
        ));
    }

    @Test
    @DisplayName("createPost finds existing tags in post's content")
    public void createPost_ContentWithExistingTags_FindsAllTags() throws Exception {
        var authorId = UUID.randomUUID();
        var tag1 = "test";
        var tag2 = "anothertest";
        var postDto = new PostCreationDto(String.format("This is #%s and #%s", tag1, tag2));

        // given
        given(tagService.findByName(tag1)).willReturn(new Tag(tag1));
        given(tagService.findByName(tag2)).willReturn(new Tag(tag2));

        // when
        postService.createPost(authorId, postDto);

        // then
        verify(postRepository, times(1)).save(argThat(
                PostsEqualMatcher.postThat(authorId, postDto.getContent(), List.of(tag1, tag2))
        ));
    }

    @Test
    @DisplayName("createPost does not find invalid tags in post's content")
    public void createPost_ContentWithInvalidTags_DoesNotFindAnyTags() throws Exception {
        var authorId = UUID.randomUUID();

        // valid tags start with a hash symbol, consist of only [A-Za-z0-9] characters
        // are between 2-50 characters long (not counting the hash symbol)
        var invalidTags = List.of(
                "#a", "#b", "c",                       // too short
                "#..", "#**", "#()", "#/\\&&", "#%%%%" // right length, invalid characters
        );

        var content = StringUtils.collectionToCommaDelimitedString(invalidTags);
        var postDto = new PostCreationDto(content);

        // when
        postService.createPost(authorId, postDto);

        // then
        verify(postRepository, times(1)).save(argThat(
                PostsEqualMatcher.postThat(authorId, postDto.getContent(), List.of())
        ));
        verify(tagService, times(0)).findByName(any());
    }

    @Test
    @DisplayName("createPost does not detect more than 50 characters of a tag")
    public void createPost_ContentWithTooLongTags_UsesOnlyUpTo50() throws Exception {
        var authorId = UUID.randomUUID();

        // tags with exactly 50 characters
        var tags = List.of(
                "VWYsloV4W4G4vYzhe0zZaIiV1RH8KcEdI3mJSg6afyhErlxQq5",
                "4uUQNAYsXqkQ21mQyMpjblXke4ZqOjEz5XXC5L7r4VSLYhb4zv",
                "KNDD7vCRcQuZxCGM5babbt090u2lcDB3QHvGWhhjVUwwNKUnf1"
        );

        // begin tags with '#' and add one more character to make tags longer
        // than 50 characters, which should result in that extra character being ignored
        var longerTags = tags.stream().map(tag -> "#" + tag + "a").toList();

        var content = StringUtils.collectionToCommaDelimitedString(longerTags);
        var postDto = new PostCreationDto(content);

        // given
        given(tagService.findByName(any())).willThrow(new TagNotFoundException(""));

        // when
        postService.createPost(authorId, postDto);

        // then
        verify(postRepository, times(1)).save(argThat(
                PostsEqualMatcher.postThat(authorId, postDto.getContent(), tags)
        ));
        verify(tagService, times(3)).findByName(any());
    }

    @Test
    @DisplayName("createPost does not count multiple identical tags separately")
    public void createPost_ContentWithDuplicateTags_OnlyDetectsOneTag() throws Exception {
        var authorId = UUID.randomUUID();

        // tags which should be seen as the same tag
        var tags = List.of(
                "TEST", "test", "TeSt", "tEsT", "TesT", "tESt", "test"
        );

        var content = StringUtils.collectionToCommaDelimitedString(
                tags.stream().map(tag -> "#" + tag).toList()
        );
        var postDto = new PostCreationDto(content);

        // given
        given(tagService.findByName(any())).willThrow(new TagNotFoundException(""));

        // when
        postService.createPost(authorId, postDto);

        // then
        var expectedTags = List.of("test");
        verify(postRepository, times(1)).save(argThat(
                PostsEqualMatcher.postThat(authorId, postDto.getContent(), expectedTags)
        ));
        verify(tagService, times(1)).findByName(expectedTags.get(0));
    }
}
