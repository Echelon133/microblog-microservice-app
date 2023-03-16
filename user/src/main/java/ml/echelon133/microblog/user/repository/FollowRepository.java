package ml.echelon133.microblog.user.repository;

import ml.echelon133.microblog.shared.user.Follow;
import ml.echelon133.microblog.shared.user.FollowId;
import ml.echelon133.microblog.shared.user.UserDto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
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

    @Query("SELECT NEW ml.echelon133.microblog.shared.user.UserDto(u.id, u.username, u.displayedName, u.aviURL, u.description) " +
            "FROM MBlog_User u JOIN Follow f ON u.id = f.followId.followedUser " +
            "WHERE f.followId.followingUser = ?1 AND f.followId.followingUser <> f.followId.followedUser")
    Page<UserDto> findAllUserFollowing(UUID userId, Pageable pageable);

    @Query("SELECT NEW ml.echelon133.microblog.shared.user.UserDto(u.id, u.username, u.displayedName, u.aviURL, u.description) " +
            "FROM MBlog_User u JOIN Follow f ON u.id = f.followId.followingUser " +
            "WHERE f.followId.followedUser = ?1 AND f.followId.followingUser <> f.followId.followedUser")
    Page<UserDto> findAllUserFollowers(UUID userId, Pageable pageable);
}
