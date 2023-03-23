package ml.echelon133.microblog.post.repository;

import ml.echelon133.microblog.shared.post.Post;
import ml.echelon133.microblog.shared.post.PostDto;
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
}
