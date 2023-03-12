package ml.echelon133.microblog.user.service;

import ml.echelon133.microblog.shared.user.UserCreationDto;
import ml.echelon133.microblog.shared.user.UserDto;
import ml.echelon133.microblog.user.exception.UserNotFoundException;
import ml.echelon133.microblog.user.exception.UsernameTakenException;
import ml.echelon133.microblog.shared.user.Role;
import ml.echelon133.microblog.shared.user.User;
import ml.echelon133.microblog.user.repository.RoleRepository;
import ml.echelon133.microblog.user.repository.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.TestPropertySource;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;

/*
    Disable kubernetes during tests to make local execution of tests possible.
    If kubernetes is not disabled, tests won't execute at all because Spring will
    fail to configure kubernetes when run outside it.
 */
@TestPropertySource(properties = "spring.cloud.kubernetes.enabled=false")
@ExtendWith(MockitoExtension.class)
@DisplayName("Tests of UserService")
public class UserServiceTests {

    @Mock
    private UserRepository userRepository;

    @Mock
    private RoleRepository roleRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private UserService userService;

    @Test
    @DisplayName("findById throws a UserNotFoundException when there is no user")
    public void findById_UserDoesNotExist_ThrowsException() {
        // given
        UUID uuid = UUID.randomUUID();
        given(userRepository.existsById(uuid)).willReturn(false);

        // then
        String message = assertThrows(UserNotFoundException.class, () -> {
           userService.findById(uuid);
        }).getMessage();
        assertEquals(message, String.format("User with id %s could not be found", uuid));
    }

    @Test
    @DisplayName("findById does not throw an exception when user exists")
    public void findById_UserExists_DoesNotThrow() throws UserNotFoundException {
        // given
        UserDto userDto = new UserDto(UUID.randomUUID(), "user", "", "");
        given(userRepository.existsById(userDto.getId())).willReturn(true);
        given(userRepository.findByUserId(userDto.getId())).willReturn(userDto);

        // when
        UserDto foundUser = userService.findById(userDto.getId());

        // then
        assertNotNull(foundUser); // ensure that repository is called properly
    }

    @Test
    @DisplayName("setupAndSaveUser throws a UsernameTakenException when username is taken")
    public void setupAndSaveUser_UsernameTaken_ThrowsException() {
        // given
        UserCreationDto userCreationDto = new UserCreationDto();
        userCreationDto.setUsername("test_user");
        given(userRepository.existsUserByUsername(userCreationDto.getUsername())).willReturn(true);

        // then
        String message = assertThrows(UsernameTakenException.class, () -> {

            userService.setupAndSaveUser(userCreationDto);
        }).getMessage();
        assertEquals(message, "User with username test_user already exists");
    }

    @Test
    @DisplayName("setupAndSaveUser actually uses a password encoder")
    public void setupAndSaveUser_GivenPlainPassword_UsesPasswordEncoder() throws UsernameTakenException {
        // given
        UserCreationDto userCreationDto = new UserCreationDto();
        userCreationDto.setUsername("test_user");
        userCreationDto.setPassword("test_password");
        given(roleRepository.findByName("ROLE_USER"))
                .willReturn(Optional.of(new Role("ROLE_USER")));
        given(userRepository.existsUserByUsername(userCreationDto.getUsername())).willReturn(false);
        given(passwordEncoder.encode(userCreationDto.getPassword())).willReturn("encoded_test_password");
        given(userRepository.save(any())).willReturn(new User()); // avoid null ptr exception

        // when
        UUID ignored = userService.setupAndSaveUser(userCreationDto);

        // then
        // no assertions, this test will fail if encode() of password encoder
        // is not called in the setupAndSaveUser, because mockito will complain
        // about unnecessary stubbing of passwordEncoder
    }
}
