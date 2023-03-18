package ml.echelon133.microblog.post.repository;

import ml.echelon133.microblog.shared.user.follow.Follow;
import ml.echelon133.microblog.shared.user.follow.FollowId;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FollowRepository extends JpaRepository<Follow, FollowId> {}
