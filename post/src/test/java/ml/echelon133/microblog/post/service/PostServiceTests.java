package ml.echelon133.microblog.post.service;

import ml.echelon133.microblog.post.exception.PostDeletionForbiddenException;
import ml.echelon133.microblog.post.exception.PostNotFoundException;
import ml.echelon133.microblog.post.exception.TagNotFoundException;
import ml.echelon133.microblog.post.repository.LikeRepository;
import ml.echelon133.microblog.post.repository.PostRepository;
import ml.echelon133.microblog.shared.post.Post;
import ml.echelon133.microblog.shared.post.PostCreationDto;
import ml.echelon133.microblog.shared.post.PostDto;
import ml.echelon133.microblog.shared.post.like.Like;
import ml.echelon133.microblog.shared.post.tag.Tag;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentMatcher;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Pageable;
import org.springframework.test.context.TestPropertySource;
import org.springframework.util.StringUtils;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
@DisplayName("Tests of PostService")
public class PostServiceTests {

    @Mock
    private PostRepository postRepository;

    @Mock
    private LikeRepository likeRepository;

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

    private static class ResponsePostsEqualMatcher implements ArgumentMatcher<Post> {
        private PostsEqualMatcher postsEqualMatcher;
        private UUID expectedParentPostId;

        private ResponsePostsEqualMatcher(UUID expectedAuthorId, String expectedContent, List<String> expectedTags, UUID expectedParentPostId) {
            this.postsEqualMatcher = PostsEqualMatcher.postThat(expectedAuthorId, expectedContent, expectedTags);
            this.expectedParentPostId = expectedParentPostId;
        }

        @Override
        public boolean matches(Post argument) {
            return this.postsEqualMatcher.matches(argument) &&
                   argument.getParentPost().getId().equals(expectedParentPostId);
        }

        public static ResponsePostsEqualMatcher responseThat(UUID expectedAuthorId, String expectedContent,
                                                       List<String> expectedTags, UUID expectedParentPostId) {
            return new ResponsePostsEqualMatcher(expectedAuthorId, expectedContent, expectedTags, expectedParentPostId);
        }
    }

    private static class LikeEqualMatcher implements ArgumentMatcher<Like> {
        private UUID likingUserId;
        private UUID likedPostId;

        private LikeEqualMatcher(UUID likingUserId, UUID likedPostId) {
            this.likingUserId = likingUserId;
            this.likedPostId = likedPostId;
        }

        @Override
        public boolean matches(Like argument) {
            return argument.getLikeId().getLikingUser().equals(likingUserId) &&
                   argument.getLikeId().getLikedPost().getId().equals(likedPostId);
        }

        public static LikeEqualMatcher likeThat(UUID expectedLikingUserId,
                                                UUID expectedLikedPostId) {
            return new LikeEqualMatcher(expectedLikingUserId, expectedLikedPostId);
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
        given(postRepository.existsPostByIdAndDeletedFalse(quotedPostId)).willReturn(false);

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
        given(postRepository.existsPostByIdAndDeletedFalse(TestPost.ID)).willReturn(true);
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
    @DisplayName("createResponsePost throws a PostNotFoundException when parent post does not exist")
    public void createResponsePost_ParentPostNotFound_ThrowsException() {
        var parentPostId = UUID.randomUUID();

        // given
        given(postRepository.existsPostByIdAndDeletedFalse(parentPostId)).willReturn(false);

        // when
        String message = assertThrows(PostNotFoundException.class, () -> {
            postService.createResponsePost(UUID.randomUUID(), parentPostId, new PostCreationDto());
        }).getMessage();

        // then
        assertEquals(String.format("Post with id %s could not be found", parentPostId), message);
    }

    @Test
    @DisplayName("createResponsePost saves a response post when parent post found and is not marked as deleted")
    public void createResponsePost_ParentPostNotDeleted_SavesQuote() throws Exception {
        var post = TestPost.createTestPost();

        // given
        given(postRepository.existsPostByIdAndDeletedFalse(TestPost.ID)).willReturn(true);
        given(postRepository.getReferenceById(TestPost.ID)).willReturn(post);

        // when
        postService.createResponsePost(TestPost.AUTHOR_ID, TestPost.ID, new PostCreationDto(""));

        // then
        verify(tagService, times(0)).findByName(any());
        verify(postRepository, times(1)).save(argThat(
                ResponsePostsEqualMatcher.responseThat(TestPost.AUTHOR_ID, "", List.of(), TestPost.ID)
        ));
    }

    @Test
    @DisplayName("likeExists returns true when like exist")
    public void likeExists_LikeFound_ReturnsTrue() {
        var userId = UUID.randomUUID();
        var postId = UUID.randomUUID();

        // given
        given(likeRepository.existsLike(userId, postId)).willReturn(true);

        // when
        boolean result = postService.likeExists(userId, postId);

        // then
        assertTrue(result);
    }

    @Test
    @DisplayName("likeExists returns false when like does not exist")
    public void likeExists_LikeNotFound_ReturnsFalse() {
        var userId = UUID.randomUUID();
        var postId = UUID.randomUUID();

        // given
        given(likeRepository.existsLike(userId, postId)).willReturn(false);

        // when
        boolean result = postService.likeExists(userId, postId);

        // then
        assertFalse(result);
    }

    @Test
    @DisplayName("likePost throws a PostNotFoundException when post about to be liked does not exist")
    public void likePost_PostIdNotFound_ThrowsException() {
        var postId = UUID.randomUUID();

        // given
        given(postRepository.existsPostByIdAndDeletedFalse(postId)).willReturn(false);

        // when
        String message = assertThrows(PostNotFoundException.class, () -> {
            postService.likePost(UUID.randomUUID(), postId);
        }).getMessage();

        // then
        assertEquals(String.format("Post with id %s could not be found", postId), message);
    }

    @Test
    @DisplayName("likePost returns true when like exist")
    public void likePost_PostLiked_ReturnsTrue() throws PostNotFoundException {
        var userId = UUID.randomUUID();
        var post = TestPost.createTestPost();

        // given
        given(postRepository.existsPostByIdAndDeletedFalse(TestPost.ID)).willReturn(true);
        given(postRepository.getReferenceById(TestPost.ID)).willReturn(post);
        given(likeRepository.existsLike(userId, TestPost.ID)).willReturn(true);

        // when
        boolean result = postService.likePost(userId, TestPost.ID);

        // then
        assertTrue(result);
        verify(likeRepository, times(1)).save(argThat(
                LikeEqualMatcher.likeThat(userId, TestPost.ID)
        ));
    }

    @Test
    @DisplayName("likePost returns false when like does not exist")
    public void likePost_PostNotLiked_ReturnsFalse() throws PostNotFoundException {
        var userId = UUID.randomUUID();
        var post = TestPost.createTestPost();

        // given
        given(postRepository.existsPostByIdAndDeletedFalse(TestPost.ID)).willReturn(true);
        given(postRepository.getReferenceById(TestPost.ID)).willReturn(post);
        given(likeRepository.existsLike(userId, TestPost.ID)).willReturn(false);

        // when
        boolean result = postService.likePost(userId, TestPost.ID);

        // then
        assertFalse(result);
        verify(likeRepository, times(1)).save(argThat(
                LikeEqualMatcher.likeThat(userId, TestPost.ID)
        ));
    }

    @Test
    @DisplayName("unlikePost throws a PostNotFoundException when post about to be unliked does not exist")
    public void unlikePost_PostIdNotFound_ThrowsException() {
        var postId = UUID.randomUUID();

        // given
        given(postRepository.existsPostByIdAndDeletedFalse(postId)).willReturn(false);

        // when
        String message = assertThrows(PostNotFoundException.class, () -> {
            postService.unlikePost(UUID.randomUUID(), postId);
        }).getMessage();

        // then
        assertEquals(String.format("Post with id %s could not be found", postId), message);
    }

    @Test
    @DisplayName("unlikePost returns true when like deleted")
    public void unlikePost_PostNotLiked_ReturnsTrue() throws PostNotFoundException {
        var userId = UUID.randomUUID();

        // given
        given(postRepository.existsPostByIdAndDeletedFalse(TestPost.ID)).willReturn(true);
        given(likeRepository.existsLike(userId, TestPost.ID)).willReturn(false);

        // when
        boolean result = postService.unlikePost(userId, TestPost.ID);

        // then
        assertTrue(result);
        verify(likeRepository, times(1)).deleteLike(
                eq(userId), eq(TestPost.ID)
        );
    }

    @Test
    @DisplayName("unlikePost returns false when like not deleted")
    public void unlikePost_PostLiked_ReturnsFalse() throws PostNotFoundException {
        var userId = UUID.randomUUID();

        // given
        given(postRepository.existsPostByIdAndDeletedFalse(TestPost.ID)).willReturn(true);
        given(likeRepository.existsLike(userId, TestPost.ID)).willReturn(true);

        // when
        boolean result = postService.unlikePost(userId, TestPost.ID);

        // then
        assertFalse(result);
        verify(likeRepository, times(1)).deleteLike(
                eq(userId), eq(TestPost.ID)
        );
    }

    @Test
    @DisplayName("deletePost throws a PostNotFoundException when post about to be deleted does not exist")
    public void deletePost_PostIdNotFound_ThrowsException() {
        var userId = UUID.randomUUID();
        var postId = UUID.randomUUID();

        // given
        given(postRepository.findById(postId)).willReturn(Optional.empty());

        // when
        String message = assertThrows(PostNotFoundException.class, () -> {
            postService.deletePost(userId, postId);
        }).getMessage();

        // then
        assertEquals(String.format("Post with id %s could not be found", postId), message);
    }

    @Test
    @DisplayName("deletePost throws a PostNotFoundException when post already deleted")
    public void deletePost_PostAlreadyDeleted_ThrowsException() {
        var userId = UUID.randomUUID();
        var post = TestPost.createTestPost();
        post.setDeleted(true);
        var postId = post.getId();

        // given
        given(postRepository.findById(postId)).willReturn(Optional.of(post));

        // when
        String message = assertThrows(PostNotFoundException.class, () -> {
            postService.deletePost(userId, postId);
        }).getMessage();

        // then
        assertEquals(String.format("Post with id %s could not be found", postId), message);
    }

    @Test
    @DisplayName("deletePost throws a PostDeletionForbiddenException when user is not the author of the post")
    public void deletePost_PostNotOwnedByUser_ThrowsException() {
        var userId = UUID.randomUUID();
        var post = TestPost.createTestPost();
        var postId = post.getId();

        // given
        given(postRepository.findById(postId)).willReturn(Optional.of(post));

        // when
        String message = assertThrows(PostDeletionForbiddenException.class, () -> {
            postService.deletePost(userId, postId);
        }).getMessage();

        // then
        assertEquals(
                String.format("User with id '%s' cannot delete a post with id '%s'", userId, postId),
                message
        );
    }

    @Test
    @DisplayName("deletePost marks the post as deleted before saving")
    public void deletePost_PostOwnedByUser_MarksPostAsDeleted() throws Exception {
        var post = TestPost.createTestPost();
        var userId = post.getAuthorId();
        var postId = post.getId();

        // given
        given(postRepository.findById(postId)).willReturn(Optional.of(post));

        // when
        postService.deletePost(userId, postId);

        // then
        verify(postRepository, times(1)).save(argThat(a ->
                a.getId().equals(postId) && a.getAuthorId().equals(userId) && a.isDeleted()
        ));
    }

    @Test
    @DisplayName("findById throws a PostNotFoundException when there is no post")
    public void findById_PostNotFound_ThrowsException() {
        // given
        UUID uuid = UUID.randomUUID();
        given(postRepository.findByPostId(uuid)).willReturn(Optional.empty());

        // when
        String message = assertThrows(PostNotFoundException.class, () -> {
            postService.findById(uuid);
        }).getMessage();

        // then
        assertEquals(String.format("Post with id %s could not be found", uuid), message);
    }

    @Test
    @DisplayName("findById does not throw an exception when post exists")
    public void findById_PostFound_DoesNotThrow() throws PostNotFoundException {
        // given
        PostDto dto = new PostDto(UUID.randomUUID(), new Date(), "", UUID.randomUUID(), null, null);
        given(postRepository.findByPostId(dto.getId())).willReturn(Optional.of(dto));

        // when
        var foundPost = postService.findById(dto.getId());

        // then
        assertEquals(dto, foundPost);
    }

    @Test
    @DisplayName("findMostRecentPostsOfUser calls the repository method")
    public void findMostRecentPostsOfUser_ProvidedArguments_CallsRepository() {
        var userId = UUID.randomUUID();
        var pageable = Pageable.ofSize(10);

        // when
        postService.findMostRecentPostsOfUser(userId, pageable);

        // then
        verify(postRepository, times(1)).findMostRecentPostsOfUser(eq(userId), eq(pageable));
    }

    @Test
    @DisplayName("findMostRecentQuotesOfPost calls the repository method")
    public void findMostRecentQuotesOfPost_ProvidedArguments_CallsRepository() {
        var postId = UUID.randomUUID();
        var pageable = Pageable.ofSize(10);

        // when
        postService.findMostRecentQuotesOfPost(postId, pageable);

        // then
        verify(postRepository, times(1)).findMostRecentQuotesOfPost(eq(postId), eq(pageable));
    }

    @Test
    @DisplayName("findMostRecentResponsesToPost calls the repository method")
    public void findMostRecentResponsesToPost_ProvidedArguments_CallsRepository() {
        var postId = UUID.randomUUID();
        var pageable = Pageable.ofSize(10);

        // when
        postService.findMostRecentResponsesToPost(postId, pageable);

        // then
        verify(postRepository, times(1)).findMostRecentResponsesToPost(eq(postId), eq(pageable));
    }
}
