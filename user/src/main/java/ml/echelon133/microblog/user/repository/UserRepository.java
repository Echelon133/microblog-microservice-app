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

    @Query("SELECT NEW ml.echelon133.microblog.shared.user.UserDto(u.id, u.username, u.displayedName, u.aviURL, u.description) " +
            "FROM MBlog_User u WHERE u.id = ?1")
    UserDto findByUserId(UUID id);

    @Modifying
    @Query("UPDATE MBlog_User u SET u.displayedName = ?2 WHERE u.id = ?1")
    void updateDisplayedName(UUID userId, String displayedName);

    @Modifying
    @Query("UPDATE MBlog_User u SET u.aviURL = ?2 WHERE u.id = ?1")
    void updateAviUrl(UUID userId, String aviUrl);

    @Modifying
    @Query("UPDATE MBlog_User u SET u.description = ?2 WHERE u.id = ?1")
    void updateDescription(UUID userId, String description);

    @Query("SELECT NEW ml.echelon133.microblog.shared.user.UserDto(u.id, u.username, u.displayedName, u.aviURL, u.description) " +
            "FROM MBlog_User u WHERE lower(u.username) LIKE lower(concat('%', ?1,'%'))")
    Page<UserDto> findByUsernameContaining(String username, Pageable pageable);
}
