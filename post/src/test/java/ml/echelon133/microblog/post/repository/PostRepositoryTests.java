package ml.echelon133.microblog.post.repository;

import ml.echelon133.microblog.shared.post.Post;
import ml.echelon133.microblog.shared.post.PostDto;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.data.domain.Pageable;
import org.springframework.test.context.TestPropertySource;

import java.sql.Date;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Set;
import java.util.UUID;

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
}
