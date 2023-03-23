package ml.echelon133.microblog.post.repository;

import ml.echelon133.microblog.shared.post.Post;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.TestPropertySource;

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

}
