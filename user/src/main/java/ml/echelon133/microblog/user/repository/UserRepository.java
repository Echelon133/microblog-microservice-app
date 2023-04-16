package ml.echelon133.microblog.user.repository;

import ml.echelon133.microblog.shared.user.UserDto;
import ml.echelon133.microblog.shared.user.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.PagingAndSortingRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface UserRepository extends PagingAndSortingRepository<User, UUID> {
    boolean existsUserByUsernameIgnoreCase(String username);

    /**
     * Finds a {@link UserDto} representing a user with specified {@link UUID}.
     *
     * @param id id of the user to find
     * @return dto representing the found user
     */
    @Query("SELECT NEW ml.echelon133.microblog.shared.user.UserDto(u.id, u.username, u.displayedName, u.aviURL, u.description) " +
            "FROM MBlog_User u WHERE u.id = ?1")
    UserDto findByUserId(UUID id);

    /**
     * Updates the displayed name of the user with specified {@link UUID}.
     *
     * @param userId id of the user whose displayed name will be updated
     * @param displayedName the new displayed name of the user
     */
    @Modifying
    @Query("UPDATE MBlog_User u SET u.displayedName = ?2 WHERE u.id = ?1")
    void updateDisplayedName(UUID userId, String displayedName);

    /**
     * Updates the avi url of the user with specified {@link UUID}.
     *
     * @param userId id of the user whose avi url will be updated
     * @param aviUrl the new avi url of the user
     */
    @Modifying
    @Query("UPDATE MBlog_User u SET u.aviURL = ?2 WHERE u.id = ?1")
    void updateAviUrl(UUID userId, String aviUrl);

    /**
     * Updates the description of the user with specified {@link UUID}.
     *
     * @param userId id of the user whose description will be updated
     * @param description the new description of the user
     */
    @Modifying
    @Query("UPDATE MBlog_User u SET u.description = ?2 WHERE u.id = ?1")
    void updateDescription(UUID userId, String description);

    /**
     * Finds a {@link Page} of users whose usernames contain a certain phrase.
     *
     * @param phrase phrase which has to occur in the username of found users (case is ignored)
     * @param pageable information about the wanted page
     * @return a page of users whose usernames contain a certain phrase
     */
    @Query("SELECT NEW ml.echelon133.microblog.shared.user.UserDto(u.id, u.username, u.displayedName, u.aviURL, u.description) " +
            "FROM MBlog_User u WHERE lower(u.username) LIKE lower(concat('%', ?1,'%'))")
    Page<UserDto> findByUsernameContaining(String phrase, Pageable pageable);

    /**
     * Finds a {@link Page} containing a user whose username matches the given username
     * exactly (except it's case insensitive).
     *
     * @param username username of the user to find
     * @param pageable information about the wanted page
     * @return a page which might contain the user with specified username
     */
    @Query("SELECT NEW ml.echelon133.microblog.shared.user.UserDto(u.id, u.username, u.displayedName, u.aviURL, u.description) " +
            "FROM MBlog_User u WHERE lower(u.username) = lower(?1)")
    Page<UserDto> findByUsernameExact(String username, Pageable pageable);
}
