package ml.echelon133.microblog.user.repository;

import ml.echelon133.microblog.shared.user.Follow;
import ml.echelon133.microblog.shared.user.FollowId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.UUID;

public interface FollowRepository extends JpaRepository<Follow, FollowId> {

    @Query("SELECT count(*) FROM Follow f WHERE f.followId.followingUser = ?1 " +
            "AND f.followId.followingUser <> f.followId.followedUser") // exclude mandatory self-follows from the results
    long countUserFollowing(UUID userId);

    @Query("SELECT count(*) FROM Follow f WHERE f.followId.followedUser = ?1 " +
            "AND f.followId.followingUser <> f.followId.followedUser") // exclude mandatory self-follows from the results
    long countUserFollowers(UUID userId);
}
