package ml.echelon133.microblog.user.repository;

import ml.echelon133.microblog.shared.user.UserDto;
import ml.echelon133.microblog.shared.user.User;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface UserRepository extends CrudRepository<User, UUID> {
    boolean existsUserByUsername(String username);

    @Query("SELECT NEW ml.echelon133.microblog.shared.user.UserDto(u.id, u.username, u.displayedName, u.aviURL) " +
            "FROM MBlog_User u WHERE u.id = ?1")
    UserDto findByUserId(UUID id);
}
