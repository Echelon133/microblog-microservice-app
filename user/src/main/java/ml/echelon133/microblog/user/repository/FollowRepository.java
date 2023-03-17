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

    /**
     * Counts how many users are being followed by {@code userId}.
     * @param userId id of the user who is on the 'following others' side of the relationship
     * @return how many users are being followed
     */
    @Query("SELECT count(*) FROM Follow f WHERE f.followId.followingUser = ?1 " +
            "AND f.followId.followingUser <> f.followId.followedUser") // exclude mandatory self-follows from the results
    long countUserFollowing(UUID userId);

    /**
     * Counts how many users are following {@code userId}.
     * @param userId id of the user who is on the 'being followed' side of the relationship
     * @return how many users are following
     */
    @Query("SELECT count(*) FROM Follow f WHERE f.followId.followedUser = ?1 " +
            "AND f.followId.followingUser <> f.followId.followedUser") // exclude mandatory self-follows from the results
    long countUserFollowers(UUID userId);

    /**
     * Finds a {@link Page} of users who are being followed by {@code userId}.
     * @param userId id of the user who is on the 'following others' side of the relationship
     * @param pageable information about the page
     * @return a page containing users
     */
    @Query("SELECT NEW ml.echelon133.microblog.shared.user.UserDto(u.id, u.username, u.displayedName, u.aviURL, u.description) " +
            "FROM MBlog_User u JOIN Follow f ON u.id = f.followId.followedUser " +
            "WHERE f.followId.followingUser = ?1 AND f.followId.followingUser <> f.followId.followedUser")
    Page<UserDto> findAllUserFollowing(UUID userId, Pageable pageable);

    /**
     * Finds a {@link Page} of users who are following {@code userId}.
     * @param userId id of the user who is on the 'being followed' side of the relationship
     * @param pageable information about the page
     * @return a page containing users
     */
    @Query("SELECT NEW ml.echelon133.microblog.shared.user.UserDto(u.id, u.username, u.displayedName, u.aviURL, u.description) " +
            "FROM MBlog_User u JOIN Follow f ON u.id = f.followId.followingUser " +
            "WHERE f.followId.followedUser = ?1 AND f.followId.followingUser <> f.followId.followedUser")
    Page<UserDto> findAllUserFollowers(UUID userId, Pageable pageable);

    /**
     * Finds a {@link Page} of users who at the same time follow {@code targetUser} and are also being followed by
     * {@code sourceUser}.
     *
     * @param sourceUser id of the user who wants to know their follows who also follow {@code targetUser}
     * @param targetUser id of the user who is being followed by people known to {@code sourceUser}
     * @param pageable information about the page
     * @return a page containing users
     */
    // finds intersection between those who follow 'targetUser' and those followed by 'sourceUser'
    // then returns all users from that intersection
    @Query("SELECT NEW ml.echelon133.microblog.shared.user.UserDto(u.id, u.username, u.displayedName, u.aviURL, u.description) " +
            "FROM MBlog_User u WHERE u.id " +
            "IN (SELECT f1.followId.followedUser FROM Follow f1 WHERE f1.followId.followingUser = ?1 AND f1.followId.followedUser " +
            "IN (SELECT f2.followId.followingUser FROM Follow f2 WHERE f2.followId.followedUser = ?2))")
    Page<UserDto> findAllKnownUserFollowers(UUID sourceUser, UUID targetUser, Pageable pageable);
}
