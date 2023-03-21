package ml.echelon133.microblog.post.service;

import ml.echelon133.microblog.post.exception.PostNotFoundException;
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
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
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

    private static class QuotePostsEqualMatcher implements ArgumentMatcher<Post> {
        private PostsEqualMatcher postsEqualMatcher;
        private UUID expectedQuotedPostId;

        private QuotePostsEqualMatcher(UUID expectedAuthorId, String expectedContent, List<String> expectedTags, UUID expectedQuotedPostId) {
            this.postsEqualMatcher = PostsEqualMatcher.postThat(expectedAuthorId, expectedContent, expectedTags);
            this.expectedQuotedPostId = expectedQuotedPostId;
        }

        @Override
        public boolean matches(Post argument) {
            return this.postsEqualMatcher.matches(argument) &&
                   argument.getQuotedPost().getId().equals(expectedQuotedPostId);
        }

        public static QuotePostsEqualMatcher quoteThat(UUID expectedAuthorId, String expectedContent,
                                                       List<String> expectedTags, UUID expectedQuotedPostId) {
            return new QuotePostsEqualMatcher(expectedAuthorId, expectedContent, expectedTags, expectedQuotedPostId);
        }
    }

    private static class TestPost {
        private static UUID ID = UUID.randomUUID();
        private static UUID AUTHOR_ID = UUID.randomUUID();
        private static String CONTENT = "test";
        private static Set<Tag> TAGS = Set.of();

        public static Post createTestPost() {
            var post = new Post(AUTHOR_ID, CONTENT, TAGS);
            post.setId(ID);
            return post;
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

    @Test
    @DisplayName("createQuotePost throws a PostNotFoundException when quoted post does not exist")
    public void createQuotePost_QuotedPostNotFound_ThrowsException() {
        var quotedPostId = UUID.randomUUID();

        // given
        given(postRepository.existsById(quotedPostId)).willReturn(false);

        // when
        String message = assertThrows(PostNotFoundException.class, () -> {
            postService.createQuotePost(UUID.randomUUID(), quotedPostId, new PostCreationDto());
        }).getMessage();

        // then
        assertEquals(String.format("Post with id %s could not be found", quotedPostId), message);
    }

    @Test
    @DisplayName("createQuotePost saves a quote post when quoted post found and is not marked as deleted")
    public void createQuotePost_QuotedPostNotDeleted_SavesQuote() throws Exception {
        var post = TestPost.createTestPost();

        // given
        given(postRepository.existsById(TestPost.ID)).willReturn(true);
        given(postRepository.getReferenceById(TestPost.ID)).willReturn(post);

        // when
        postService.createQuotePost(TestPost.AUTHOR_ID, TestPost.ID, new PostCreationDto(""));

        // then
        verify(tagService, times(0)).findByName(any());
        verify(postRepository, times(1)).save(argThat(
                QuotePostsEqualMatcher.quoteThat(TestPost.AUTHOR_ID, "", List.of(), TestPost.ID)
        ));
    }

    @Test
    @DisplayName("createQuotePost throws a PostNotFoundException when quoted post exists but is marked as deleted")
    public void createQuotePost_QuotedPostDeleted_ThrowsException() {
        var quotedPost = TestPost.createTestPost();
        quotedPost.setDeleted(true);

        // given
        given(postRepository.existsById(quotedPost.getId())).willReturn(true);
        given(postRepository.getReferenceById(quotedPost.getId())).willReturn(quotedPost);

        // when
        String message = assertThrows(PostNotFoundException.class, () -> {
            postService.createQuotePost(UUID.randomUUID(), quotedPost.getId(), new PostCreationDto());
        }).getMessage();

        // then
        assertEquals(String.format("Post with id %s could not be found", quotedPost.getId()), message);
    }
}
