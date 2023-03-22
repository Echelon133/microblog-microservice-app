package ml.echelon133.microblog.post.repository;

import ml.echelon133.microblog.shared.post.Post;
import ml.echelon133.microblog.shared.post.like.Like;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.TestPropertySource;

import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/*
    Disable kubernetes during tests to make local execution of tests possible.
    If kubernetes is not disabled, tests won't execute at all because Spring will
    fail to configure kubernetes when run outside it.
 */
@TestPropertySource(properties = "spring.cloud.kubernetes.enabled=false")
@DataJpaTest
@DisplayName("Tests of LikeRepository")
public class LikeRepositoryTests {

    @Autowired
    private LikeRepository likeRepository;

    @Autowired
    private PostRepository postRepository;

    @Test
    @DisplayName("Custom existsLike returns false when like does not exist")
    public void existsLike_LikeDoesNotExist_ReturnsFalse() {
        var likingUser = UUID.randomUUID();
        var likedPost = UUID.randomUUID();

        // when
        var result = likeRepository.existsLike(likingUser, likedPost);

        // then
        assertFalse(result);
    }

    @Test
    @DisplayName("Custom existsLike returns true when like does exist")
    public void existsLike_LikeDoesExist_ReturnsTrue() {
        var likingUser = UUID.randomUUID();
        var likedPost = UUID.randomUUID();

        // given
        var post = new Post(likingUser, "", Set.of());
        post.setId(likedPost);
        post = postRepository.save(post);
        likeRepository.save(new Like(likingUser, post));

        // when
        var result = likeRepository.existsLike(likingUser, likedPost);

        // then
        assertTrue(result);
    }

    @Test
    @DisplayName("Custom deleteLike does nothing when non existent like deleted")
    public void deleteLike_LikeDoesNotExist_DoesNothing() {
        var likingUser = UUID.randomUUID();
        var likedPost = UUID.randomUUID();

        // when
        likeRepository.deleteLike(likingUser, likedPost);
        var result = likeRepository.existsLike(likingUser, likedPost);

        // then
        assertFalse(result);
    }

    @Test
    @DisplayName("Custom deleteLike deletes a like if it exists")
    public void deleteLike_LikeDoesExist_DeletesLike() {
        var likingUser = UUID.randomUUID();
        var likedPost = UUID.randomUUID();

        // given
        var post = new Post(likingUser, "", Set.of());
        post.setId(likedPost);
        post = postRepository.save(post);
        likeRepository.save(new Like(likingUser, post));

        // when
        var beforeDeletion = likeRepository.existsLike(likingUser, likedPost);
        likeRepository.deleteLike(likingUser, likedPost);
        var afterDeletion = likeRepository.existsLike(likingUser, likedPost);

        // then
        assertTrue(beforeDeletion);
        assertFalse(afterDeletion);
    }
}
