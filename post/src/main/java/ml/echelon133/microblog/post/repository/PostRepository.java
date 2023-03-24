package ml.echelon133.microblog.post.repository;

import ml.echelon133.microblog.shared.post.Post;
import ml.echelon133.microblog.shared.post.PostDto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface PostRepository extends JpaRepository<Post, UUID> {
    boolean existsPostByIdAndDeletedFalse(UUID postId);

    @Query("SELECT NEW ml.echelon133.microblog.shared.post.PostDto(p.id, p.dateCreated, p.content, p.authorId, p.quotedPost.id, p.parentPost.id) " +
            "FROM Post p WHERE p.id = ?1 AND p.deleted = false")
    Optional<PostDto> findByPostId(UUID id);

    @Query("SELECT NEW ml.echelon133.microblog.shared.post.PostDto(p.id, p.dateCreated, p.content, p.authorId, p.quotedPost.id, p.parentPost.id) " +
           "FROM Post p WHERE p.authorId = ?1 AND p.deleted = false ORDER BY p.dateCreated desc")
    Page<PostDto> findMostRecentPostsOfUser(UUID userId, Pageable pageable);

    @Query("SELECT NEW ml.echelon133.microblog.shared.post.PostDto(p.id, p.dateCreated, p.content, p.authorId, p.quotedPost.id, p.parentPost.id) " +
            "FROM Post p WHERE p.quotedPost.id = ?1 AND p.deleted = false ORDER BY p.dateCreated desc")
    Page<PostDto> findMostRecentQuotesOfPost(UUID postId, Pageable pageable);

    @Query("SELECT NEW ml.echelon133.microblog.shared.post.PostDto(p.id, p.dateCreated, p.content, p.authorId, p.quotedPost.id, p.parentPost.id) " +
            "FROM Post p WHERE p.parentPost.id = ?1 AND p.deleted = false ORDER BY p.dateCreated desc")
    Page<PostDto> findMostRecentResponsesToPost(UUID postId, Pageable pageable);
}
