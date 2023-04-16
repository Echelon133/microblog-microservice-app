package ml.echelon133.microblog.post.repository;

import ml.echelon133.microblog.shared.post.Post;
import ml.echelon133.microblog.shared.post.PostDto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Date;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface PostRepository extends JpaRepository<Post, UUID> {
    boolean existsPostByIdAndDeletedFalse(UUID postId);
    long countByQuotedPostIdAndDeletedFalse(UUID postId);
    long countByParentPostIdAndDeletedFalse(UUID postId);

    /**
     * Finds a post with given id, but only if that post is not marked as deleted.
     *
     * @param id id of the post to find
     * @return an optional which may or may not contain a post
     */
    @Query("SELECT NEW ml.echelon133.microblog.shared.post.PostDto(p.id, p.dateCreated, p.content, p.authorId, p.quotedPost.id, p.parentPost.id) " +
            "FROM Post p WHERE p.id = ?1 AND p.deleted = false")
    Optional<PostDto> findByPostId(UUID id);

    /**
     * Finds a {@link Page} of posts of {@code userId} sorted by their recency.
     * Posts which are marked as deleted will be ignored, as the results of this
     * query are public.
     *
     * @param userId id of the user whose posts will be fetched
     * @param pageable all information about the wanted page
     * @return a page of posts sorted from the most recent to the least recent
     */
    @Query("SELECT NEW ml.echelon133.microblog.shared.post.PostDto(p.id, p.dateCreated, p.content, p.authorId, p.quotedPost.id, p.parentPost.id) " +
           "FROM Post p WHERE p.authorId = ?1 AND p.deleted = false ORDER BY p.dateCreated desc")
    Page<PostDto> findMostRecentPostsOfUser(UUID userId, Pageable pageable);

    /**
     * Finds a {@link Page} of quotes of {@code postId} sorted by their recency.
     * Quotes which are marked as deleted will be ignored, as the results of this
     * query are public.
     *
     * @param postId id of the post whose quotes will be fetched
     * @param pageable all information about the wanted page
     * @return a page of quotes of post sorted from the most recent to the least recent
     */
    @Query("SELECT NEW ml.echelon133.microblog.shared.post.PostDto(p.id, p.dateCreated, p.content, p.authorId, p.quotedPost.id, p.parentPost.id) " +
            "FROM Post p WHERE p.quotedPost.id = ?1 AND p.deleted = false ORDER BY p.dateCreated desc")
    Page<PostDto> findMostRecentQuotesOfPost(UUID postId, Pageable pageable);

    /**
     * Finds a {@link Page} of responses to {@code postId} sorted by their recency.
     * Responses which are marked as deleted will be ignored, as the results of this
     * query are public.
     *
     * @param postId id of the post whose responses will be fetched
     * @param pageable all information about the wanted page
     * @return a page of responses to post sorted from the most recent to the least recent
     */
    @Query("SELECT NEW ml.echelon133.microblog.shared.post.PostDto(p.id, p.dateCreated, p.content, p.authorId, p.quotedPost.id, p.parentPost.id) " +
            "FROM Post p WHERE p.parentPost.id = ?1 AND p.deleted = false ORDER BY p.dateCreated desc")
    Page<PostDto> findMostRecentResponsesToPost(UUID postId, Pageable pageable);

    /**
     * Generates a {@link Page} containing a feed for an anonymous user. Because there is no
     * information about what the user might like, the feed simply consists of the most popular posts in the time
     * period between {@code start} and {@code end} dates. Currently, post's popularity is calculated based
     * on the number of likes the post has.
     *
     * @param start date which represents the start of the post's popularity evaluation period
     * @param end date which represents the end of the post's popularity evaluation period
     * @param pageable all information about the wanted page
     * @return a page of posts sorted from the most popular to the least popular
     */
    @Query("SELECT NEW ml.echelon133.microblog.shared.post.PostDto(p.id, p.dateCreated, p.content, p.authorId, p.quotedPost.id, p.parentPost.id) " +
            "FROM Post p LEFT JOIN Like l ON p.id = l.likeId.likedPost.id " +
            "WHERE p.deleted = false AND p.dateCreated BETWEEN ?1 AND ?2 " +
            "GROUP BY (l.likeId.likedPost.id, p.id) " +
            "ORDER BY COUNT(l.likeId.likedPost.id) DESC")
    Page<PostDto> generateFeedWithMostPopularPostsForAnonymous(Date start, Date end, Pageable pageable);

    /**
     * Generates a {@link Page} containing a feed for a user with an account. The feed consists of the most popular
     * posts in the time period between {@code start} and {@code end} dates, filtered to only contain posts of
     * users who are being followed by the authenticated user. This means that the user will only see posts
     * from users who are known to them.
     *
     * Currently, post's popularity is calculated based on the number of likes the post has.
     *
     * @param userId id of the user for whom the feed will be generated
     * @param start date which represents the start of the post's popularity evaluation period
     * @param end date which represents the end of the post's popularity evaluation period
     * @param pageable all information about the wanted page
     * @return a page of posts sorted from the most popular to the least popular
     */
    @Query("SELECT NEW ml.echelon133.microblog.shared.post.PostDto(p.id, p.dateCreated, p.content, p.authorId, p.quotedPost.id, p.parentPost.id) " +
            "FROM Post p " +
            "LEFT JOIN Like l ON p.id = l.likeId.likedPost.id " +
            "WHERE p.authorId IN (SELECT f.followId.followedUser FROM Follow f WHERE f.followId.followingUser = ?1) " +
            "AND p.deleted = false AND p.dateCreated BETWEEN ?2 AND ?3 " +
            "GROUP BY (l.likeId.likedPost.id, p.id) " +
            "ORDER BY COUNT(l.likeId.likedPost.id) DESC")
    Page<PostDto> generateFeedWithMostPopularPostsForUser(UUID userId, Date start, Date end, Pageable pageable);

    /**
     * Generates a {@link Page} containing a feed for a user with an account. The feed consists of the most recent
     * posts in the time period between {@code start} and {@code end} dates, filtered to only contain posts of
     * users who are being followed by the authenticated user. This means that the user will only see posts
     * from users who are known to them.
     *
     * @param userId id of the user for whom the feed will be generated
     * @param start date which represents the start of the post's recency evaluation period
     * @param end date which represents the end of the post's recency evaluation period
     * @param pageable all information about the wanted page
     * @return a page of posts sorted from the most recent to the least recent
     */
    @Query("SELECT NEW ml.echelon133.microblog.shared.post.PostDto(p.id, p.dateCreated, p.content, p.authorId, p.quotedPost.id, p.parentPost.id) " +
            "FROM Post p " +
            "WHERE p.authorId IN (SELECT f.followId.followedUser FROM Follow f WHERE f.followId.followingUser = ?1) " +
            "AND p.deleted = false AND p.dateCreated BETWEEN ?2 AND ?3 " +
            "ORDER BY p.dateCreated DESC")
    Page<PostDto> generateFeedWithMostRecentPostsForUser(UUID userId, Date start, Date end, Pageable pageable);
}
