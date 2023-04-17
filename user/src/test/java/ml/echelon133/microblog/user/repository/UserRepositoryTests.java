package ml.echelon133.microblog.user.repository;

import ml.echelon133.microblog.shared.user.Roles;
import ml.echelon133.microblog.shared.user.UserDto;
import ml.echelon133.microblog.shared.user.Role;
import ml.echelon133.microblog.shared.user.User;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.test.context.TestPropertySource;

import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;

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
        Role savedRole = roleRepository.save(new Role(Roles.ROLE_USER.name()));
        User u = new User(
                username,
                "test@gmail.com",
                "",
                "",
                Set.of(savedRole)
        );
        u.setDescription("test description");
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
        assertEquals(userDto.getDescription(), savedUser.getDescription());
    }

    @Test
    @DisplayName("Custom findByUserId query returns null if the user does not exist")
    public void findByUserId_UserDoesNotExist_ReturnsNull() {
        // when
        UserDto userDto = userRepository.findByUserId(UUID.randomUUID());

        // then
        assertNull(userDto);
    }

    @Test
    @DisplayName("Custom updateDisplayedName query updates only specific user's displayed name")
    public void updateDisplayedName_ChangingDisplayedName_UpdatesOnlyOneUser() {
        // given
        User u1 = createTestUser("test1");
        User u2 = createTestUser("test2");

        // when
        userRepository.updateDisplayedName(u1.getId(), "asdf");
        userRepository.updateDisplayedName(u2.getId(), "qwerty");

        UserDto u1Dto = userRepository.findByUserId(u1.getId());
        UserDto u2Dto = userRepository.findByUserId(u2.getId());

        // then
        assertEquals("asdf", u1Dto.getDisplayedName());
        assertEquals("qwerty", u2Dto.getDisplayedName());
    }

    @Test
    @DisplayName("Custom updateAviUrl query updates only specific user's aviUrl")
    public void updateAviUrl_ChangingAviUrl_UpdatesOnlyOneUser() {
        // given
        User u1 = createTestUser("test1");
        User u2 = createTestUser("test2");

        // when
        userRepository.updateAviUrl(u1.getId(), "http://test.com");
        userRepository.updateAviUrl(u2.getId(), "http://example.com");

        UserDto u1Dto = userRepository.findByUserId(u1.getId());
        UserDto u2Dto = userRepository.findByUserId(u2.getId());

        // then
        assertEquals("http://test.com", u1Dto.getAviUrl());
        assertEquals("http://example.com", u2Dto.getAviUrl());
    }

    @Test
    @DisplayName("Custom updateDescription query updates only specific user's description")
    public void updateDescription_ChangingDescription_UpdatesOnlyOneUser() {
        // given
        User u1 = createTestUser("test1");
        User u2 = createTestUser("test2");

        // when
        userRepository.updateDescription(u1.getId(), "description1");
        userRepository.updateDescription(u2.getId(), "description2");

        UserDto u1Dto = userRepository.findByUserId(u1.getId());
        UserDto u2Dto = userRepository.findByUserId(u2.getId());

        // then
        assertEquals("description1", u1Dto.getDescription());
        assertEquals("description2", u2Dto.getDescription());
    }

    @Test
    @DisplayName("Custom findByUsernameContaining query works with Pageable")
    public void findByUsernameContaining_MultipleUsersExist_ReturnsPagesWithProjections() {
        Pageable page = Pageable.ofSize(2);

        var testUsers = List.of("test1", "test2", "test3", "utest4", "test5", "test6");

        // given
        var allUsers = Stream.concat(testUsers.stream(), Stream.of("asdf", "qwerty"));
        allUsers.forEach(this::createTestUser);

        // when
        page = page.first();
        Page<UserDto> firstPage = userRepository.findByUsernameContaining("test", page);
        page = page.next();
        Page<UserDto> secondPage = userRepository.findByUsernameContaining("test", page);
        page = page.next();
        Page<UserDto> thirdPage = userRepository.findByUsernameContaining("test", page);

        // then
        var collected = Stream.concat(Stream.concat(firstPage.get(), secondPage.get()), thirdPage.get());
        var collectedUsernames = collected.map(UserDto::getUsername).toList();

        assertEquals(6, firstPage.getTotalElements()); // 6 users expected
        assertEquals(3, firstPage.getTotalPages()); // 6 (users expected) / 2 (page size) = 3 pages
        for (var testUser : testUsers) {
            assertTrue(collectedUsernames.contains(testUser));
        }
    }

    @Test
    @DisplayName("Custom findByUsernameContaining query ignores username case")
    public void findByUsernameContaining_MultipleUsersExist_ReturnsIgnoringCase() {
        Pageable page = Pageable.ofSize(2);

        var testUsers = List.of("test1", "test2", "test3", "utest4", "test5", "test6");

        // given
        var allUsers = Stream.concat(testUsers.stream(), Stream.of("asdf", "qwerty"));
        allUsers.forEach(this::createTestUser);

        // when
        page = page.first();
        Page<UserDto> firstPage = userRepository.findByUsernameContaining("TEST", page);
        page = page.next();
        Page<UserDto> secondPage = userRepository.findByUsernameContaining("TeST", page);
        page = page.next();
        Page<UserDto> thirdPage = userRepository.findByUsernameContaining("tEST", page);

        // then
        var collected = Stream.concat(Stream.concat(firstPage.get(), secondPage.get()), thirdPage.get());
        var collectedUsernames = collected.map(UserDto::getUsername).toList();

        assertEquals(6, firstPage.getTotalElements()); // 6 users expected
        assertEquals(3, firstPage.getTotalPages()); // 6 (users expected) / 2 (page size) = 3 pages
        for (var testUser : testUsers) {
            assertTrue(collectedUsernames.contains(testUser));
        }
    }

    @Test
    @DisplayName("Custom findByUsernameExact query works with Pageable")
    public void findByUsernameExact_MultipleUsersExist_ReturnsPagesWithProjections() {
        Pageable page = Pageable.ofSize(2);

        var testUsers = List.of("test1", "test2");

        // given
        var allUsers = Stream.concat(testUsers.stream(), Stream.of("asdf", "qwerty"));
        allUsers.forEach(this::createTestUser);

        // when
        page = page.first();
        Page<UserDto> firstPage = userRepository.findByUsernameExact("test1", page);

        // then
        assertEquals(1, firstPage.getTotalElements());
        assertEquals(1, firstPage.getTotalPages());
        var allUsernames = firstPage.getContent().stream().map(UserDto::getUsername).toList();
        assertTrue(allUsernames.contains("test1"));
    }

    @Test
    @DisplayName("Custom findByUsernameExact query ignores username case")
    public void findByUsernameExact_MultipleUsersExist_ReturnsIgnoringCase() {
        Pageable page = Pageable.ofSize(2);

        var testUsers = List.of("test1", "test2");

        // given
        var allUsers = Stream.concat(testUsers.stream(), Stream.of("asdf", "qwerty"));
        allUsers.forEach(this::createTestUser);

        // when
        page = page.first();
        Page<UserDto> firstPage = userRepository.findByUsernameExact("test1".toUpperCase(), page);

        // then
        assertEquals(1, firstPage.getTotalElements());
        assertEquals(1, firstPage.getTotalPages());
        var allUsernames = firstPage.getContent().stream().map(UserDto::getUsername).toList();
        assertTrue(allUsernames.contains("test1"));
    }
}
