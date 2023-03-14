package ml.echelon133.microblog.user.service;

import ml.echelon133.microblog.shared.user.*;
import ml.echelon133.microblog.user.exception.UserNotFoundException;
import ml.echelon133.microblog.user.exception.UsernameTakenException;
import ml.echelon133.microblog.user.repository.FollowRepository;
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
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

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
    private FollowRepository followRepository;

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
        UserDto userDto = new UserDto(UUID.randomUUID(), "user", "", "", "");
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

    @Test
    @DisplayName("updateUserInfo throws a UserNotFoundException when there is no user")
    public void updateUserInfo_UserDoesNotExist_ThrowsException() {
        // given
        UUID uuid = UUID.randomUUID();
        given(userRepository.existsById(uuid)).willReturn(false);

        // then
        String message = assertThrows(UserNotFoundException.class, () -> {
            userService.updateUserInfo(uuid, null);
        }).getMessage();
        assertEquals(message, String.format("User with id %s could not be found", uuid));
    }

    @Test
    @DisplayName("updateUserInfo updates all fields when all fields provided")
    public void updateUserInfo_AllFieldsProvided_UpdatesAllFields() throws UserNotFoundException {
        // given
        var id = UUID.randomUUID();
        UserUpdateDto allFields = new UserUpdateDto("new name", "http://test.com", "description");
        given(userRepository.existsById(id)).willReturn(true);

        // when
        userService.updateUserInfo(id, allFields);

        // then
        verify(userRepository, times(1)).updateDisplayedName(id, allFields.getDisplayedName());
        verify(userRepository, times(1)).updateAviUrl(id, allFields.getAviUrl());
        verify(userRepository, times(1)).updateDescription(id, allFields.getDescription());
        verify(userRepository, times(1)).findByUserId(id);
    }

    @Test
    @DisplayName("updateUserInfo updates only displayedName if only displayedName provided")
    public void updateUserInfo_OnlyDisplayedNameProvided_UpdatesOnlyDisplayedName() throws UserNotFoundException {
        // given
        var id = UUID.randomUUID();
        UserUpdateDto onlyDName = new UserUpdateDto("new name", null, null);
        given(userRepository.existsById(id)).willReturn(true);

        // when
        userService.updateUserInfo(id, onlyDName);

        // then
        verify(userRepository, times(1)).updateDisplayedName(id, onlyDName.getDisplayedName());
        verify(userRepository, times(0)).updateAviUrl(any(), any());
        verify(userRepository, times(0)).updateDescription(any(), any());
        verify(userRepository, times(1)).findByUserId(id);
    }

    @Test
    @DisplayName("updateUserInfo updates only aviUrl if only aviUrl provided")
    public void updateUserInfo_OnlyAviUrlProvided_UpdatesOnlyAviUrl() throws UserNotFoundException {
        // given
        var id = UUID.randomUUID();
        UserUpdateDto onlyAvi = new UserUpdateDto(null, "http://test.com", null);
        given(userRepository.existsById(id)).willReturn(true);

        // when
        userService.updateUserInfo(id, onlyAvi);

        // then
        verify(userRepository, times(0)).updateDisplayedName(any(), any());
        verify(userRepository, times(1)).updateAviUrl(id, onlyAvi.getAviUrl());
        verify(userRepository, times(0)).updateDescription(any(), any());
        verify(userRepository, times(1)).findByUserId(id);
    }

    @Test
    @DisplayName("updateUserInfo updates only description if only description provided")
    public void updateUserInfo_OnlyDescriptionProvided_UpdatesOnlyDescription() throws UserNotFoundException {
        // given
        var id = UUID.randomUUID();
        UserUpdateDto onlyDesc = new UserUpdateDto(null, null, "desc");
        given(userRepository.existsById(id)).willReturn(true);

        // when
        userService.updateUserInfo(id, onlyDesc);

        // then
        verify(userRepository, times(0)).updateDisplayedName(any(), any());
        verify(userRepository, times(0)).updateAviUrl(any(), any());
        verify(userRepository, times(1)).updateDescription(id, onlyDesc.getDescription());
        verify(userRepository, times(1)).findByUserId(id);
    }

    @Test
    @DisplayName("followExists uses the repository")
    public void followExists_ProvidedIds_UsesRepository() {
        var source = UUID.randomUUID();
        var target = UUID.randomUUID();

        // when
        userService.followExists(source, target);

        // then
        verify(followRepository, times(1))
                .existsById(new FollowId(source, target));
    }

    @Test
    @DisplayName("followUser throws a UserNotFoundException when following user id belongs to a non existent user")
    public void followUser_FollowingUserIdNotFound_ThrowsException() {
        var source = UUID.randomUUID();
        var target = UUID.randomUUID();

        // given
        given(userRepository.existsById(source)).willReturn(false);

        // when
        String message = assertThrows(UserNotFoundException.class, () -> {
            userService.followUser(source, target);
        }).getMessage();

        // then
        assertEquals(message, String.format("User with id %s could not be found", source));
    }

    @Test
    @DisplayName("followUser throws a UserNotFoundException when followed user id belongs to a non existent user")
    public void followUser_FollowedUserIdNotFound_ThrowsException() {
        var source = UUID.randomUUID();
        var target = UUID.randomUUID();

        // given
        given(userRepository.existsById(source)).willReturn(true);
        given(userRepository.existsById(target)).willReturn(false);

        // when
        String message = assertThrows(UserNotFoundException.class, () -> {
            userService.followUser(source, target);
        }).getMessage();

        // then
        assertEquals(message, String.format("User with id %s could not be found", target));
    }

    @Test
    @DisplayName("followUser uses the follow repository")
    public void followUser_UsersFound_UsesRepositories() throws UserNotFoundException {
        var source = UUID.randomUUID();
        var target = UUID.randomUUID();

        // given
        given(userRepository.existsById(source)).willReturn(true);
        given(userRepository.existsById(target)).willReturn(true);
        given(followRepository.existsById(new FollowId(source, target))).willReturn(true);

        // when
        boolean result = userService.followUser(source, target);

        // then
        assertTrue(result);
        verify(followRepository, times(1)).save(
                argThat(a -> a.getFollowId().equals(new FollowId(source, target)))
        );
    }

    @Test
    @DisplayName("unfollowUser uses the follow repository")
    public void unfollowUser_UsersFound_UsesRepositories() {
        var source = UUID.randomUUID();
        var target = UUID.randomUUID();
        var fId = new FollowId(source, target);

        // given
        given(followRepository.existsById(fId)).willReturn(false);

        // when
        boolean result = userService.unfollowUser(source, target);

        // then
        assertTrue(result);
        verify(followRepository, times(1)).deleteById(eq(fId));
        verify(followRepository, times(1)).existsById(eq(fId));
    }
}
