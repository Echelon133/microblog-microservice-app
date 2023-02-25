package ml.echelon133.microblog.user.repository;

import ml.echelon133.microblog.shared.user.UserDto;
import ml.echelon133.microblog.user.model.Role;
import ml.echelon133.microblog.user.model.User;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.TestPropertySource;

import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/*
    Disable kubernetes during tests to make local execution of tests possible.
    If kubernetes is not disabled, tests won't execute at all because Spring will
    fail to configure kubernetes when run outside it.
 */
@TestPropertySource(properties = "spring.cloud.kubernetes.enabled=false")
@DataJpaTest
@DisplayName("Tests of UserRepository")
public class UserRepositoryTests {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoleRepository roleRepository;

    private User createTestUser(String username) {
        Role savedRole = roleRepository.save(new Role("ROLE_USER"));
        User u = new User(
                username,
                "test@gmail.com",
                "",
                "",
                Set.of(savedRole)
        );
        return userRepository.save(u);
    }

    @Test
    @DisplayName("Custom findByUserId query returns a DTO projection of the user")
    public void findByUserId_UserExists_ReturnsDTO() {
        // given
        User savedUser = createTestUser("test_user");

        // when
        UserDto userDto = userRepository.findByUserId(savedUser.getId());

        // then
        assertEquals(userDto.getId(), savedUser.getId());
        assertEquals(userDto.getUsername(), savedUser.getUsername());
        assertEquals(userDto.getDisplayedName(), savedUser.getDisplayedName());
        assertEquals(userDto.getAviUrl(), savedUser.getAviURL());
    }

    @Test
    @DisplayName("Custom findByUserId query returns null if the user does not exist")
    public void findByUserId_UserDoesNotExist_ReturnsNull() {
        // when
        UserDto userDto = userRepository.findByUserId(UUID.randomUUID());

        // then
        assertNull(userDto);
    }
}
