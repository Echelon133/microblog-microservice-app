package ml.echelon133.microblog.post.repository;

import ml.echelon133.microblog.shared.post.Post;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface PostRepository extends JpaRepository<Post, UUID> {
    boolean existsPostByIdAndDeletedFalse(UUID postId);
}
