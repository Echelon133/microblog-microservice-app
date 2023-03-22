package ml.echelon133.microblog.post.repository;

import ml.echelon133.microblog.shared.post.Post;
import ml.echelon133.microblog.shared.post.like.Like;
import ml.echelon133.microblog.shared.post.like.LikeId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.util.UUID;

public interface LikeRepository extends JpaRepository<Like, LikeId> {

    /**
     * Checks if a like exists without needing a reference to a {@link Post} object.
     * Regular {@code existsById} requires a {@code FollowId} object which contains a
     * {@code Post} object. This custom query circumvents that requirement.
     *
     * @param likingUser user who potentially likes a post
     * @param likedPost post which is potentially liked
     * @return {@code true} if a post is liked by a user
     */
    @Query("SELECT count(l) > 0 FROM Like l WHERE l.likeId.likingUser = ?1 AND l.likeId.likedPost.id = ?2 ")
    boolean existsLike(UUID likingUser, UUID likedPost);

    /**
     * Deletes a like if it exists, or does nothing if the like does not exist in the first place.
     *
     * @param likingUser user who wants to have their like deleted
     * @param likedPost post which is to be unliked
     */
    @Modifying
    @Query("DELETE FROM Like l WHERE l.likeId.likingUser = ?1 AND l.likeId.likedPost.id = ?2")
    void deleteLike(UUID likingUser, UUID likedPost);
}
