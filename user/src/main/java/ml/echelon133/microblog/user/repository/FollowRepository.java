package ml.echelon133.microblog.user.repository;

import ml.echelon133.microblog.shared.user.Follow;
import ml.echelon133.microblog.shared.user.FollowId;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FollowRepository extends JpaRepository<Follow, FollowId> {}
