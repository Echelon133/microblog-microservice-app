package ml.echelon133.microblog.post.repository;

import ml.echelon133.microblog.shared.post.Post;
import ml.echelon133.microblog.shared.post.PostDto;
import ml.echelon133.microblog.shared.post.like.Like;
import ml.echelon133.microblog.shared.user.follow.Follow;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.data.domain.Pageable;
import org.springframework.test.context.TestPropertySource;

import java.sql.Array;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

/*
    Disable kubernetes during tests to make local execution of tests possible.
    If kubernetes is not disabled, tests won't execute at all because Spring will
    fail to configure kubernetes when run outside it.
 */
@TestPropertySource(properties = "spring.cloud.kubernetes.enabled=false")
@DataJpaTest
@DisplayName("Tests of PostRepository")
public class PostRepositoryTests {

    @Autowired
    private PostRepository postRepository;

    @Autowired
    private LikeRepository likeRepository;

    @Autowired
    private FollowRepository followRepository;

    private Post createTestPost(UUID postId, UUID authorId, String content) {
        var post = new Post(authorId, content, Set.of());
        post.setId(postId);
        return postRepository.save(post);
    }

    private Post createTestQuotePost(UUID postId, UUID authorId, String content, Post quotedPost) {
        var post = new Post(authorId, content, Set.of());
        post.setId(postId);
        post.setQuotedPost(quotedPost);
        return postRepository.save(post);
    }

    private Post createTestResponsePost(UUID postId, UUID authorId, String content, Post parentPost) {
        var post = new Post(authorId, content, Set.of());
        post.setId(postId);
        post.setParentPost(parentPost);
        return postRepository.save(post);
    }

    private Post createTestPostOnDateWithLikes(Date date, Integer numberOfLikes) {
        return createTestPostOfUserOnDateWithLikes(UUID.randomUUID(), date, numberOfLikes);
    }

    private Post createTestPostOfUserOnDateWithLikes(UUID authorId, Date date, Integer numberOfLikes) {
        var post = postRepository.save(new Post(authorId, "test content", Set.of()));
        // custom dateCreated value cannot be set before saving the post for the first time, because the
        // @DateCreated annotation causes an overwrite of any value that might have been there, so setting
        // a custom dateCreated value is only possible after the first save
        post.setDateCreated(date);
        post = postRepository.save(post);

        // create likes from random users
        for (int i = 0; i < numberOfLikes; i++) {
            likeRepository.save(new Like(UUID.randomUUID(), post));
        }

        return post;
    }

    private void createFollows(UUID followingUser, List<UUID> followedUsers) {
        followedUsers.forEach(followedUser ->
                followRepository.save(new Follow(followingUser, followedUser))
        );
    }

    @Test
    @DisplayName("Custom findByPostId query returns empty when post does not exist")
    public void findByPostId_PostDoesNotExist_ReturnsEmpty() {
        var postId = UUID.randomUUID();

        // when
        var result = postRepository.findByPostId(postId);

        // then
        assertTrue(result.isEmpty());
    }

    @Test
    @DisplayName("Custom findByPostId query returns empty when post exists but is marked as deleted")
    public void findByPostId_PostExistsButDeleted_ReturnsEmpty() {
        var postId = UUID.randomUUID();
        var authorId = UUID.randomUUID();

        var p = createTestPost(postId, authorId, "");
        p.setDeleted(true);
        postRepository.save(p);

        // when
        var result = postRepository.existsById(postId);
        var resultFiltered = postRepository.findByPostId(postId);

        // then
        assertTrue(result);
        assertTrue(resultFiltered.isEmpty());
    }

    @Test
    @DisplayName("Custom findByPostId query returns not empty when regular post exists")
    public void findByPostId_RegularPostExists_ReturnsNonEmpty() {
        var postId = UUID.randomUUID();
        var authorId = UUID.randomUUID();
        var savedPost = createTestPost(postId, authorId, "test");

        // when
        var resultFiltered = postRepository.findByPostId(postId);

        // then
        assertTrue(resultFiltered.isPresent());

        var post = resultFiltered.get();
        assertEquals(savedPost.getId(), post.getId());
        assertEquals(savedPost.getDateCreated(), post.getDateCreated());
        assertEquals(savedPost.getContent(), post.getContent());
        assertEquals(savedPost.getAuthorId(), post.getAuthorId());
        assertNull(post.getParentPost());
        assertNull(post.getQuotedPost());
    }

    @Test
    @DisplayName("Custom findByPostId query returns not empty when quote post exists")
    public void findByPostId_QuotePostExists_ReturnsNonEmpty() {
        var quotePostId = UUID.randomUUID();
        var quotedPostId = UUID.randomUUID();
        var savedPost = createTestPost(quotedPostId, UUID.randomUUID(), "test");
        var savedQuote = createTestQuotePost(quotePostId, UUID.randomUUID(), "quote", savedPost);

        // when
        var resultFiltered = postRepository.findByPostId(quotePostId);

        // then
        assertTrue(resultFiltered.isPresent());

        var quotePost = resultFiltered.get();
        assertEquals(savedQuote.getId(), quotePost.getId());
        assertEquals(savedQuote.getDateCreated(), quotePost.getDateCreated());
        assertEquals(savedQuote.getContent(), quotePost.getContent());
        assertEquals(savedQuote.getAuthorId(), quotePost.getAuthorId());
        assertEquals(quotedPostId, quotePost.getQuotedPost());
        assertNull(quotePost.getParentPost());
    }

    @Test
    @DisplayName("Custom findByPostId query returns not empty when response post exists")
    public void findByPostId_ResponsePostExists_ReturnsNonEmpty() {
        var parentPostId = UUID.randomUUID();
        var responsePostId = UUID.randomUUID();
        var savedPost = createTestPost(parentPostId, UUID.randomUUID(), "test");
        var savedResponse = createTestResponsePost(responsePostId, UUID.randomUUID(), "response", savedPost);

        // when
        var resultFiltered = postRepository.findByPostId(responsePostId);

        // then
        assertTrue(resultFiltered.isPresent());

        var responsePost = resultFiltered.get();
        assertEquals(savedResponse.getId(), responsePost.getId());
        assertEquals(savedResponse.getDateCreated(), responsePost.getDateCreated());
        assertEquals(savedResponse.getContent(), responsePost.getContent());
        assertEquals(savedResponse.getAuthorId(), responsePost.getAuthorId());
        assertEquals(parentPostId, responsePost.getParentPost());
        assertNull(responsePost.getQuotedPost());
    }

    @Test
    @DisplayName("Custom findMostRecentPostsOfUser returns an empty page for user who does not have any posts")
    public void findMostRecentPostsOfUser_UserHasNoPosts_ReturnsEmptyPage() {
        var userId = UUID.randomUUID();
        var pageable = Pageable.ofSize(10);

        // when
        var page = postRepository.findMostRecentPostsOfUser(userId, pageable);

        // then
        assertEquals(0, page.getTotalElements());
    }

    @Test
    @DisplayName("Custom findMostRecentPostsOfUser returns an empty page for user who only has deleted posts")
    public void findMostRecentPostsOfUser_UserHasDeletedPosts_ReturnsEmptyPage() {
        var userId = UUID.randomUUID();
        var pageable = Pageable.ofSize(10);

        var post1 = createTestPost(UUID.randomUUID(), userId, "test");
        post1.setDeleted(true);
        postRepository.save(post1);
        var post2 = createTestPost(UUID.randomUUID(), userId, "test");
        post2.setDeleted(true);
        postRepository.save(post2);

        // when
        var page = postRepository.findMostRecentPostsOfUser(userId, pageable);

        // then
        assertEquals(0, page.getTotalElements());
    }

    @Test
    @DisplayName("Custom findMostRecentPostsOfUser query returns only posts which belong to the user")
    public void findMostRecentPostsOfUser_MultiplePostsExist_ReturnsOnlyElementsBelongingToUser() {
        var userId = UUID.randomUUID();
        var pageable = Pageable.ofSize(10);

        var post1 = createTestPost(UUID.randomUUID(), userId, "test");
        var post2 = createTestPost(UUID.randomUUID(), userId, "test");
        // post3 does not belong to the user with 'userId'
        createTestPost(UUID.randomUUID(), UUID.randomUUID(), "test");

        // when
        var page = postRepository.findMostRecentPostsOfUser(userId, pageable);

        // then
        assertEquals(2, page.getTotalElements());
        var postIds = page.getContent().stream().map(PostDto::getId).toList();
        assertTrue(postIds.containsAll(List.of(post1.getId(), post2.getId())));
    }

    @Test
    @DisplayName("Custom findMostRecentPostsOfUser query returns correctly sorted projections")
    public void findMostRecentPostsOfUser_MultiplePostsExist_ReturnsElementsInRightOrder() throws InterruptedException {
        var userId = UUID.randomUUID();
        var pageable = Pageable.ofSize(10);

        // post from 10 minutes before now
        var post1 = createTestPost(UUID.randomUUID(), userId, "post1");
        post1.setDateCreated(Date.from(Instant.now().minus(10, ChronoUnit.MINUTES)));
        postRepository.save(post1);

        // post from 5 minutes before now
        var post2 = createTestPost(UUID.randomUUID(), userId, "post2");
        post2.setDateCreated(Date.from(Instant.now().minus(5, ChronoUnit.MINUTES)));
        postRepository.save(post2);

        // post from now
        var post3 = createTestPost(UUID.randomUUID(), userId, "post3");

        // when
        var page = postRepository.findMostRecentPostsOfUser(userId, pageable);

        // then
        assertEquals(3, page.getTotalElements());
        // most recent post should be at the top
        var content = page.getContent();
        assertEquals(post3.getId(), content.get(0).getId());
        assertEquals(post2.getId(), content.get(1).getId());
        assertEquals(post1.getId(), content.get(2).getId());
    }

    @Test
    @DisplayName("Custom findMostRecentQuotesOfPost returns an empty page for post without quotes")
    public void findMostRecentQuotesOfPost_PostHasNoQuotes_ReturnsEmptyPage() {
        var postId = UUID.randomUUID();
        var pageable = Pageable.ofSize(10);

        createTestPost(postId, UUID.randomUUID(), "post");

        // when
        var page = postRepository.findMostRecentQuotesOfPost(postId, pageable);

        // then
        assertEquals(0, page.getTotalElements());
    }

    @Test
    @DisplayName("Custom findMostRecentQuotesOfPost returns an empty page for post with deleted quotes")
    public void findMostRecentQuotesOfPost_PostHasDeletedQuotes_ReturnsEmptyPage() {
        var postId = UUID.randomUUID();
        var pageable = Pageable.ofSize(10);

        var post1 = createTestPost(postId, UUID.randomUUID(), "test");
        var quote1 = createTestQuotePost(UUID.randomUUID(), UUID.randomUUID(), "quote", post1);
        quote1.setDeleted(true);
        postRepository.save(quote1);

        // when
        var page = postRepository.findMostRecentQuotesOfPost(postId, pageable);

        // then
        assertEquals(0, page.getTotalElements());
    }

    @Test
    @DisplayName("Custom findMostRecentQuotesOfPost query returns only quotes which quote particular post")
    public void findMostRecentQuotesOfPost_MultipleQuotesExist_ReturnsOnlyElementsQuotingSpecificPost() {
        var postId = UUID.randomUUID();
        var pageable = Pageable.ofSize(10);

        var post1 = createTestPost(postId, UUID.randomUUID(), "test");
        var post2 = createTestPost(UUID.randomUUID(), UUID.randomUUID(), "test");
        // quote1 and quote2 both quote post1
        var quote1 = createTestQuotePost(UUID.randomUUID(), UUID.randomUUID(), "quote", post1);
        var quote2 = createTestQuotePost(UUID.randomUUID(), UUID.randomUUID(), "quote", post1);
        // quotes post2, shouldn't appear in the results
        createTestQuotePost(UUID.randomUUID(), UUID.randomUUID(), "quote", post2);
        // responds to post1, shouldn't appear in the results
        createTestResponsePost(UUID.randomUUID(), UUID.randomUUID(), "response", post1);

        // when
        var page = postRepository.findMostRecentQuotesOfPost(postId, pageable);

        // then
        assertEquals(2, page.getTotalElements());
        var postIds = page.getContent().stream().map(PostDto::getId).toList();
        assertTrue(postIds.containsAll(List.of(quote1.getId(), quote2.getId())));
    }

    @Test
    @DisplayName("Custom findMostRecentQuotesOfPost query returns correctly sorted projections")
    public void findMostRecentQuotesOfPost_MultiplePostsExist_ReturnsElementsInRightOrder() throws InterruptedException {
        var postId = UUID.randomUUID();
        var pageable = Pageable.ofSize(10);

        var post1 = createTestPost(postId, UUID.randomUUID(), "post1");

        //  quote from 10 minutes before now
        var quote1 = createTestQuotePost(UUID.randomUUID(), UUID.randomUUID(), "quote1", post1);
        quote1.setDateCreated(Date.from(Instant.now().minus(10, ChronoUnit.MINUTES)));
        postRepository.save(quote1);

        // quote from 5 minutes before now
        var quote2 = createTestQuotePost(UUID.randomUUID(), UUID.randomUUID(), "quote2", post1);
        quote2.setDateCreated(Date.from(Instant.now().minus(5, ChronoUnit.MINUTES)));
        postRepository.save(quote2);

        // quote from now
        var quote3 = createTestQuotePost(UUID.randomUUID(), UUID.randomUUID(), "quote3", post1);

        // when
        var page = postRepository.findMostRecentQuotesOfPost(postId, pageable);

        // then
        assertEquals(3, page.getTotalElements());
        // most recent quotes should be at the top
        var content = page.getContent();
        assertEquals(quote3.getId(), content.get(0).getId());
        assertEquals(quote2.getId(), content.get(1).getId());
        assertEquals(quote1.getId(), content.get(2).getId());
    }

    @Test
    @DisplayName("Custom findMostRecentResponsesToPost returns an empty page for post without responses")
    public void findMostRecentResponsesToPost_PostHasNoResponses_ReturnsEmptyPage() {
        var postId = UUID.randomUUID();
        var pageable = Pageable.ofSize(10);

        createTestPost(postId, UUID.randomUUID(), "post");

        // when
        var page = postRepository.findMostRecentResponsesToPost(postId, pageable);

        // then
        assertEquals(0, page.getTotalElements());
    }

    @Test
    @DisplayName("Custom findMostRecentResponsesToPost returns an empty page for post with deleted responses")
    public void findMostRecentResponsesToPost_PostHasDeletedResponses_ReturnsEmptyPage() {
        var postId = UUID.randomUUID();
        var pageable = Pageable.ofSize(10);

        var post1 = createTestPost(postId, UUID.randomUUID(), "test");
        var response1 = createTestResponsePost(UUID.randomUUID(), UUID.randomUUID(), "response", post1);
        response1.setDeleted(true);
        postRepository.save(response1);

        // when
        var page = postRepository.findMostRecentResponsesToPost(postId, pageable);

        // then
        assertEquals(0, page.getTotalElements());
    }

    @Test
    @DisplayName("Custom findMostRecentResponsesToPost query returns only responses which respond to particular post")
    public void findMostRecentResponsesToPost_MultipleResponsesExist_ReturnsOnlyElementsRespondingToSpecificPost() {
        var postId = UUID.randomUUID();
        var pageable = Pageable.ofSize(10);

        var post1 = createTestPost(postId, UUID.randomUUID(), "test");
        var post2 = createTestPost(UUID.randomUUID(), UUID.randomUUID(), "test");
        // response1 and response2 both respond to post1
        var response1 = createTestResponsePost(UUID.randomUUID(), UUID.randomUUID(), "response", post1);
        var response2 = createTestResponsePost(UUID.randomUUID(), UUID.randomUUID(), "response", post1);
        // responds to post2, shouldn't appear in the results
        createTestResponsePost(UUID.randomUUID(), UUID.randomUUID(), "response", post2);
        // quotes post1, shouldn't appear in the results
        createTestQuotePost(UUID.randomUUID(), UUID.randomUUID(), "quote", post1);

        // when
        var page = postRepository.findMostRecentResponsesToPost(postId, pageable);

        // then
        assertEquals(2, page.getTotalElements());
        var postIds = page.getContent().stream().map(PostDto::getId).toList();
        assertTrue(postIds.containsAll(List.of(response1.getId(), response2.getId())));
    }

    @Test
    @DisplayName("Custom findMostRecentResponsesToPost query returns correctly sorted projections")
    public void findMostRecentResponsesToPost_MultiplePostsExist_ReturnsElementsInRightOrder() throws InterruptedException {
        var postId = UUID.randomUUID();
        var pageable = Pageable.ofSize(10);

        var post1 = createTestPost(postId, UUID.randomUUID(), "post1");

        //  response from 10 minutes before now
        var response1 = createTestResponsePost(UUID.randomUUID(), UUID.randomUUID(), "response", post1);
        response1.setDateCreated(Date.from(Instant.now().minus(10, ChronoUnit.MINUTES)));
        postRepository.save(response1);

        // response from 5 minutes before now
        var response2 = createTestResponsePost(UUID.randomUUID(), UUID.randomUUID(), "response2", post1);
        response2.setDateCreated(Date.from(Instant.now().minus(5, ChronoUnit.MINUTES)));
        postRepository.save(response2);

        // response from now
        var response3 = createTestResponsePost(UUID.randomUUID(), UUID.randomUUID(), "response3", post1);

        // when
        var page = postRepository.findMostRecentResponsesToPost(postId, pageable);

        // then
        assertEquals(3, page.getTotalElements());
        // most recent responses should be at the top
        var content = page.getContent();
        assertEquals(response3.getId(), content.get(0).getId());
        assertEquals(response2.getId(), content.get(1).getId());
        assertEquals(response1.getId(), content.get(2).getId());
    }

    @Test
    @DisplayName("Custom generateFeedForAnonymousUser returns an empty page when there aren't any posts")
    public void generateFeedForAnonymousUser_NoPosts_ReturnsEmpty() {
        // when
        var page = postRepository.generateFeedForAnonymousUser(
                Date.from(Instant.now().minus(24, ChronoUnit.HOURS)),
                Date.from(Instant.now()),
                Pageable.ofSize(20)
        );

        // then
        assertTrue(page.isEmpty());
    }

    @Test
    @DisplayName("Custom generateFeedForAnonymousUser returns an empty page when there are only deleted posts")
    public void generateFeedForAnonymousUser_AllPostsDeleted_ReturnsEmpty() {
        // given
        var oneHourAgo = Date.from(Instant.now().minus(1, ChronoUnit.HOURS));
        var post1 = createTestPostOnDateWithLikes(oneHourAgo, 0);
        var post2 = createTestPostOnDateWithLikes(oneHourAgo, 1);
        post1.setDeleted(true);
        post2.setDeleted(true);
        postRepository.save(post1);
        postRepository.save(post2);

        // when
        var page = postRepository.generateFeedForAnonymousUser(
                Date.from(Instant.now().minus(24, ChronoUnit.HOURS)),
                Date.from(Instant.now()),
                Pageable.ofSize(20)
        );

        // then
        assertTrue(page.isEmpty());
    }

    @Test
    @DisplayName("Custom generateFeedForAnonymousUser returns a page with posts sorted by likes descending")
    public void generateFeedForAnonymousUser_PostsLiked_ReturnsPostsInCorrectOrder() {
        // given
        var expectedNumberOfPosts = 20;
        var oneMinuteAgo = Date.from(Instant.now().minus(1, ChronoUnit.MINUTES));

        List<Post> expectedPostOrder = new ArrayList<>();
        // create all posts on the same date, but with each post having one like
        // fewer than the post before it
        for (int likes = expectedNumberOfPosts - 1; likes >= 0; likes--) {
            var post = createTestPostOnDateWithLikes(oneMinuteAgo, likes);
            expectedPostOrder.add(post);
        }

        // when
        var page = postRepository.generateFeedForAnonymousUser(
                Date.from(Instant.now().minus(24, ChronoUnit.HOURS)),
                Date.from(Instant.now()),
                Pageable.ofSize(20)
        );

        // then
        assertEquals(expectedNumberOfPosts, page.getTotalElements());
        var content = page.getContent();
        // order in 'expectedPostOrder' and order in 'content' should be the same
        for (int i = 0; i < expectedNumberOfPosts - 1; i++) {
            assertEquals(expectedPostOrder.get(i).getId(), content.get(i).getId());
        }
    }

    @Test
    @DisplayName("Custom generateFeedForAnonymousUser only returns posts from correct time period")
    public void generateFeedForAnonymousUser_PostsOnMultipleDates_OnlyReturnsPostsFromCorrectTimePeriod() {
        // given
        var sixHoursAgo = Instant.now().minus(6, ChronoUnit.HOURS);
        var oneHourAgo = Instant.now().minus(1, ChronoUnit.HOURS);

        // create a post 6h01m ago
        createTestPostOnDateWithLikes(Date.from(sixHoursAgo.minus(1, ChronoUnit.MINUTES)), 0);
        // create a post 5h59m ago
        var expected1 =
                createTestPostOnDateWithLikes(Date.from(sixHoursAgo.plus(1, ChronoUnit.MINUTES)), 0);
        // create a post 1h01m ago
        var expected2 =
                createTestPostOnDateWithLikes(Date.from(oneHourAgo.minus(1, ChronoUnit.MINUTES)), 0);
        // create a post 59m ago
        createTestPostOnDateWithLikes(Date.from(oneHourAgo.plus(1, ChronoUnit.MINUTES)), 0);

        // expect only these because they had been posted in the time period between
        // 6h ago and 1h ago, other two posts are from outside that time period
        var expectedPostIds = List.of(expected1.getId(), expected2.getId());

        // when
        var page = postRepository.generateFeedForAnonymousUser(
                Date.from(sixHoursAgo),
                Date.from(oneHourAgo),
                Pageable.ofSize(20)
        );

        // then
        assertEquals(2, page.getTotalElements());
        var foundPostIds = page.getContent().stream().map(PostDto::getId).toList();
        assertTrue(expectedPostIds.containsAll(foundPostIds));
    }

    @Test
    @DisplayName("Custom generateFeedForUser_Popular returns an empty page when there aren't any posts")
    public void generateFeedForUser_Popular_NoPosts_ReturnsEmpty() {
        var user = UUID.randomUUID();

        // when
        var page = postRepository.generateFeedForUser_Popular(
                user,
                Date.from(Instant.now().minus(24, ChronoUnit.HOURS)),
                Date.from(Instant.now()),
                Pageable.ofSize(20)
        );

        // then
        assertTrue(page.isEmpty());
    }

    @Test
    @DisplayName("Custom generateFeedForUser_Popular returns an empty page when there are only deleted posts")
    public void generateFeedForUser_Popular_AllPostsDeleted_ReturnsEmpty() {
        // given
        var userId = UUID.randomUUID();
        var followedId = UUID.randomUUID();
        // make userId follow the other user
        createFollows(userId, List.of(followedId));
        // create a deleted post as the followed user
        var oneHourAgo = Date.from(Instant.now().minus(1, ChronoUnit.HOURS));
        var post1 = createTestPostOfUserOnDateWithLikes(followedId, oneHourAgo, 10);
        post1.setDeleted(true);
        postRepository.save(post1);

        // when
        var page = postRepository.generateFeedForUser_Popular(
                userId,
                Date.from(Instant.now().minus(24, ChronoUnit.HOURS)),
                Date.from(Instant.now()),
                Pageable.ofSize(20)
        );

        // then
        assertTrue(page.isEmpty());
    }

    @Test
    @DisplayName("Custom generateFeedForUser_Popular returns a page with posts from followed users, sorted by likes descending")
    public void generateFeedForUser_Popular_UsersFollowed_ReturnsPostsInCorrectOrder() {
        // given
        var userId = UUID.randomUUID();
        var otherUsersIds = List.of(UUID.randomUUID(), UUID.randomUUID());

        // make userId follow two other users
        createFollows(userId, otherUsersIds);

        var oneHourAgo = Date.from(Instant.now().minus(1, ChronoUnit.HOURS));
        // make each user who is being followed by userId create a post
        var post1 = createTestPostOfUserOnDateWithLikes(otherUsersIds.get(0), oneHourAgo, 10);
        var post2 = createTestPostOfUserOnDateWithLikes(otherUsersIds.get(1), oneHourAgo, 5);

        // when
        var page = postRepository.generateFeedForUser_Popular(
                userId,
                Date.from(Instant.now().minus(24, ChronoUnit.HOURS)),
                Date.from(Instant.now()),
                Pageable.ofSize(20)
        );

        // then
        assertEquals(2, page.getTotalElements());
        var content = page.getContent();
        assertEquals(post1.getId(), content.get(0).getId());
        assertEquals(post2.getId(), content.get(1).getId());
    }

    @Test
    @DisplayName("Custom generateFeedForUser_Popular returns a page with posts only from followed users")
    public void generateFeedForUser_Popular_PostsLiked_ReturnsPostsOnlyFromFollowedUsers() {
        // given
        var userId = UUID.randomUUID();
        var followedUserId = UUID.randomUUID();
        var notFollowedUserId = UUID.randomUUID();

        // make userId follow only a single user
        createFollows(userId, List.of(followedUserId));

        var oneHourAgo = Date.from(Instant.now().minus(1, ChronoUnit.HOURS));
        // make each user create a post - one user is being followed while the other is not
        var post1 = createTestPostOfUserOnDateWithLikes(followedUserId, oneHourAgo, 0);
        createTestPostOfUserOnDateWithLikes(notFollowedUserId, oneHourAgo, 5);

        // when
        var page = postRepository.generateFeedForUser_Popular(
                userId,
                Date.from(Instant.now().minus(24, ChronoUnit.HOURS)),
                Date.from(Instant.now()),
                Pageable.ofSize(20)
        );

        // then
        assertEquals(1, page.getTotalElements());
        var postIds = page.getContent().stream().map(PostDto::getId).toList();
        assertTrue(postIds.contains(post1.getId()));
    }

    @Test
    @DisplayName("Custom generateFeedForUser_Popular only returns posts from correct time period")
    public void generateFeedForUser_Popular_PostsOnMultipleDates_OnlyReturnsPostsFromCorrectTimePeriod() {
        // given
        var userId = UUID.randomUUID();
        var otherUserIds = List.of(UUID.randomUUID(), UUID.randomUUID());
        // make userId follow two other users
        createFollows(userId, otherUserIds);

        var sixHoursAgo = Instant.now().minus(6, ChronoUnit.HOURS);
        var oneHourAgo = Instant.now().minus(1, ChronoUnit.HOURS);
        // create a post 6h01m ago
        createTestPostOfUserOnDateWithLikes(otherUserIds.get(0), Date.from(sixHoursAgo.minus(1, ChronoUnit.MINUTES)), 0);
        // create a post 5h59m ago
        var expected1 = createTestPostOfUserOnDateWithLikes(
                otherUserIds.get(1),
                Date.from(sixHoursAgo.plus(1, ChronoUnit.MINUTES)),
                0);
        // create a post 1h01m ago
        var expected2 = createTestPostOfUserOnDateWithLikes(
                otherUserIds.get(0),
                Date.from(oneHourAgo.minus(1, ChronoUnit.MINUTES)),
                0);
        // create a post 59m ago
        createTestPostOfUserOnDateWithLikes(otherUserIds.get(1), Date.from(oneHourAgo.plus(1, ChronoUnit.MINUTES)), 0);

        // expect only these because they had been posted in the time period between
        // 6h ago and 1h ago, other two posts are from outside that time period
        var expectedPostIds = List.of(expected1.getId(), expected2.getId());

        // when
        var page = postRepository.generateFeedForUser_Popular(
                userId,
                Date.from(sixHoursAgo),
                Date.from(oneHourAgo),
                Pageable.ofSize(20)
        );

        // then
        assertEquals(2, page.getTotalElements());
        var foundPostIds = page.getContent().stream().map(PostDto::getId).toList();
        assertTrue(expectedPostIds.containsAll(foundPostIds));
    }

    @Test
    @DisplayName("Custom generateFeedForUser_MostRecent returns an empty page when there aren't any posts")
    public void generateFeedForUser_MostRecent_NoPosts_ReturnsEmpty() {
        var user = UUID.randomUUID();

        // when
        var page = postRepository.generateFeedForUser_MostRecent(
                user,
                Date.from(Instant.now().minus(24, ChronoUnit.HOURS)),
                Date.from(Instant.now()),
                Pageable.ofSize(20)
        );

        // then
        assertTrue(page.isEmpty());
    }

    @Test
    @DisplayName("Custom generateFeedForUser_MostRecent returns an empty page when there are only deleted posts")
    public void generateFeedForUser_MostRecent_AllPostsDeleted_ReturnsEmpty() {
        // given
        var userId = UUID.randomUUID();
        var followedId = UUID.randomUUID();
        // make userId follow the other user
        createFollows(userId, List.of(followedId));
        // create a deleted post as the followed user
        var oneHourAgo = Date.from(Instant.now().minus(1, ChronoUnit.HOURS));
        var post1 = createTestPostOfUserOnDateWithLikes(followedId, oneHourAgo, 10);
        post1.setDeleted(true);
        postRepository.save(post1);

        // when
        var page = postRepository.generateFeedForUser_MostRecent(
                userId,
                Date.from(Instant.now().minus(24, ChronoUnit.HOURS)),
                Date.from(Instant.now()),
                Pageable.ofSize(20)
        );

        // then
        assertTrue(page.isEmpty());
    }

    @Test
    @DisplayName("Custom generateFeedForUser_MostRecent returns a page with posts from followed users, sorted by date descending")
    public void generateFeedForUser_MostRecent_UsersFollowed_ReturnsPostsInCorrectOrder() {
        // given
        var userId = UUID.randomUUID();
        var otherUserId = UUID.randomUUID();

        // make userId follow the other user
        createFollows(userId, List.of(otherUserId));

        // create posts, each one being an hour apart
        var expectedPostOrder = new ArrayList<>();
        for (int hours = 0; hours < 5; hours++) {
            var hourAgo = Instant.now().minus(1, ChronoUnit.HOURS);
            var post = createTestPostOfUserOnDateWithLikes(
                    otherUserId,
                    Date.from(hourAgo.minus(hours, ChronoUnit.HOURS)),
                    10);
            expectedPostOrder.add(post.getId());

        }

        // when
        var page = postRepository.generateFeedForUser_MostRecent(
                userId,
                Date.from(Instant.now().minus(24, ChronoUnit.HOURS)),
                Date.from(Instant.now()),
                Pageable.ofSize(20)
        );

        // then
        assertEquals(5, page.getTotalElements());
        var content = page.getContent().stream().map(PostDto::getId).toList();
        for (int i = 0; i < expectedPostOrder.size(); i++) {
            assertEquals(expectedPostOrder.get(i), content.get(i));
        }
    }

    @Test
    @DisplayName("Custom generateFeedForUser_MostRecent returns a page with posts only from followed users")
    public void generateFeedForUser_MostRecent_PostsLiked_ReturnsPostsOnlyFromFollowedUsers() {
        // given
        var userId = UUID.randomUUID();
        var followedUserId = UUID.randomUUID();
        var notFollowedUserId = UUID.randomUUID();

        // make userId follow only a single user
        createFollows(userId, List.of(followedUserId));

        var oneHourAgo = Date.from(Instant.now().minus(1, ChronoUnit.HOURS));
        // make each user create a post - one user is being followed while the other is not
        var post1 = createTestPostOfUserOnDateWithLikes(followedUserId, oneHourAgo, 0);
        createTestPostOfUserOnDateWithLikes(notFollowedUserId, oneHourAgo, 5);

        // when
        var page = postRepository.generateFeedForUser_MostRecent(
                userId,
                Date.from(Instant.now().minus(24, ChronoUnit.HOURS)),
                Date.from(Instant.now()),
                Pageable.ofSize(20)
        );

        // then
        assertEquals(1, page.getTotalElements());
        var postIds = page.getContent().stream().map(PostDto::getId).toList();
        assertTrue(postIds.contains(post1.getId()));
    }

    @Test
    @DisplayName("Custom generateFeedForUser_MostRecent only returns posts from correct time period")
    public void generateFeedForUser_MostRecent_PostsOnMultipleDates_OnlyReturnsPostsFromCorrectTimePeriod() {
        // given
        var userId = UUID.randomUUID();
        var otherUserIds = List.of(UUID.randomUUID(), UUID.randomUUID());
        // make userId follow two other users
        createFollows(userId, otherUserIds);

        var sixHoursAgo = Instant.now().minus(6, ChronoUnit.HOURS);
        var oneHourAgo = Instant.now().minus(1, ChronoUnit.HOURS);
        // create a post 6h01m ago
        createTestPostOfUserOnDateWithLikes(otherUserIds.get(0), Date.from(sixHoursAgo.minus(1, ChronoUnit.MINUTES)), 0);
        // create a post 5h59m ago
        var expected1 = createTestPostOfUserOnDateWithLikes(
                otherUserIds.get(1),
                Date.from(sixHoursAgo.plus(1, ChronoUnit.MINUTES)),
                0);
        // create a post 1h01m ago
        var expected2 = createTestPostOfUserOnDateWithLikes(
                otherUserIds.get(0),
                Date.from(oneHourAgo.minus(1, ChronoUnit.MINUTES)),
                0);
        // create a post 59m ago
        createTestPostOfUserOnDateWithLikes(otherUserIds.get(1), Date.from(oneHourAgo.plus(1, ChronoUnit.MINUTES)), 0);

        // expect only these because they had been posted in the time period between
        // 6h ago and 1h ago, other two posts are from outside that time period
        var expectedPostIds = List.of(expected1.getId(), expected2.getId());

        // when
        var page = postRepository.generateFeedForUser_MostRecent(
                userId,
                Date.from(sixHoursAgo),
                Date.from(oneHourAgo),
                Pageable.ofSize(20)
        );

        // then
        assertEquals(2, page.getTotalElements());
        var foundPostIds = page.getContent().stream().map(PostDto::getId).toList();
        assertTrue(expectedPostIds.containsAll(foundPostIds));
    }
}
