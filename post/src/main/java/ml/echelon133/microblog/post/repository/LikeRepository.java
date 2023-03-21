package ml.echelon133.microblog.post.repository;

import ml.echelon133.microblog.shared.post.like.Like;
import ml.echelon133.microblog.shared.post.like.LikeId;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LikeRepository extends JpaRepository<Like, LikeId> {
}
