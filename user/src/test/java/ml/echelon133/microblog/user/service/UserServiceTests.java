package ml.echelon133.microblog.user.service;

import ml.echelon133.microblog.shared.exception.ResourceNotFoundException;
import ml.echelon133.microblog.shared.notification.Notification;
import ml.echelon133.microblog.shared.user.*;
import ml.echelon133.microblog.shared.user.follow.FollowId;
import ml.echelon133.microblog.user.exception.UsernameTakenException;
import ml.echelon133.microblog.user.queue.FollowPublisher;
import ml.echelon133.microblog.user.queue.NotificationPublisher;
import ml.echelon133.microblog.user.repository.FollowRepository;
import ml.echelon133.microblog.user.repository.RoleRepository;
import ml.echelon133.microblog.user.repository.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

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

    @Mock
    private FollowPublisher followPublisher;

    @Mock
    private NotificationPublisher notificationPublisher;

    @InjectMocks
    private UserService userService;

    @Test
    @DisplayName("findById throws a ResourceNotFoundException when there is no user")
    public void findById_UserDoesNotExist_ThrowsException() {
        // given
        UUID uuid = UUID.randomUUID();
        given(userRepository.existsById(uuid)).willReturn(false);

        // when
        String message = assertThrows(ResourceNotFoundException.class, () -> {
           userService.findById(uuid);
        }).getMessage();

        // then
        assertEquals(String.format("user %s could not be found", uuid), message);
    }

    @Test
    @DisplayName("findById does not throw an exception when user exists")
    public void findById_UserExists_DoesNotThrow() throws ResourceNotFoundException {
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
        given(userRepository.existsUserByUsernameIgnoreCase(userCreationDto.getUsername())).willReturn(true);

        // when
        String message = assertThrows(UsernameTakenException.class, () -> {
            userService.setupAndSaveUser(userCreationDto);
        }).getMessage();

        // then
        assertEquals("username has already been taken", message);
    }

    @Test
    @DisplayName("setupAndSaveUser executes all setup steps")
    public void setupAndSaveUser_GivenValidDto_ExecutesSetupSteps() throws UsernameTakenException, ResourceNotFoundException {
        // given
        UserCreationDto userCreationDto = new UserCreationDto();
        userCreationDto.setUsername("test_user");
        userCreationDto.setPassword("test_password");

        var user = new User(userCreationDto.getUsername(), "", userCreationDto.getPassword(), "", Set.of());
        var userId = user.getId();

        given(roleRepository.findByName(Roles.ROLE_USER.name()))
                .willReturn(Optional.of(new Role(Roles.ROLE_USER.name())));
        given(userRepository.existsUserByUsernameIgnoreCase(userCreationDto.getUsername())).willReturn(false);
        given(userRepository.save(argThat(
                a -> a.getUsername().equals(userCreationDto.getUsername())
        ))).willReturn(user);

        // when
        UUID newUserId = userService.setupAndSaveUser(userCreationDto);

        // then
        assertEquals(userId, newUserId);
        // uses password encoder
        verify(passwordEncoder, times(1)).encode(userCreationDto.getPassword());
        // makes the user follow themselves
        verify(followRepository, times(1)).save(argThat(
                a -> a.getFollowId().equals(new FollowId(userId, userId))
        ));
        verify(followPublisher, times(1)).publishFollow(argThat(
                a -> a.getFollowingUser().equals(userId) && a.getFollowingUser().equals(a.getFollowedUser())
        ));
    }

    @Test
    @DisplayName("updateUserInfo throws a ResourceNotFoundException when there is no user")
    public void updateUserInfo_UserDoesNotExist_ThrowsException() {
        // given
        UUID uuid = UUID.randomUUID();
        given(userRepository.existsById(uuid)).willReturn(false);

        // when
        String message = assertThrows(ResourceNotFoundException.class, () -> {
            userService.updateUserInfo(uuid, null);
        }).getMessage();

        // then
        assertEquals(String.format("user %s could not be found", uuid), message);
    }

    @Test
    @DisplayName("updateUserInfo updates all fields when all fields provided")
    public void updateUserInfo_AllFieldsProvided_UpdatesAllFields() throws ResourceNotFoundException {
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
    public void updateUserInfo_OnlyDisplayedNameProvided_UpdatesOnlyDisplayedName() throws ResourceNotFoundException {
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
    public void updateUserInfo_OnlyAviUrlProvided_UpdatesOnlyAviUrl() throws ResourceNotFoundException {
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
    public void updateUserInfo_OnlyDescriptionProvided_UpdatesOnlyDescription() throws ResourceNotFoundException {
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
    @DisplayName("followUser throws a ResourceNotFoundException when following user id belongs to a non existent user")
    public void followUser_FollowingUserIdNotFound_ThrowsException() {
        var source = UUID.randomUUID();
        var target = UUID.randomUUID();

        // given
        given(userRepository.existsById(source)).willReturn(false);

        // when
        String message = assertThrows(ResourceNotFoundException.class, () -> {
            userService.followUser(source, target);
        }).getMessage();

        // then
        assertEquals(String.format("user %s could not be found", source), message);
    }

    @Test
    @DisplayName("followUser throws a ResourceNotFoundException when followed user id belongs to a non existent user")
    public void followUser_FollowedUserIdNotFound_ThrowsException() {
        var source = UUID.randomUUID();
        var target = UUID.randomUUID();

        // given
        given(userRepository.existsById(source)).willReturn(true);
        given(userRepository.existsById(target)).willReturn(false);

        // when
        String message = assertThrows(ResourceNotFoundException.class, () -> {
            userService.followUser(source, target);
        }).getMessage();

        // then
        assertEquals(String.format("user %s could not be found", target), message);
    }

    @Test
    @DisplayName("followUser uses the follow repository")
    public void followUser_UsersFound_UsesRepositories() throws ResourceNotFoundException {
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
        verify(followPublisher, times(1)).publishFollow(
                argThat(a -> a.getFollowingUser().equals(source) && a.getFollowedUser().equals(target))
        );
        verify(notificationPublisher, times(1)).publishNotification(
                argThat(a -> a.getNotificationSource().equals(source) &&
                        a.getUserToNotify().equals(target) &&
                        a.getType().equals(Notification.Type.FOLLOW)
                )
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
        verify(followPublisher, times(1)).publishUnfollow(
                argThat(a -> a.getFollowingUser().equals(source) && a.getFollowedUser().equals(target))
        );
    }

    @Test
    @DisplayName("unfollowUser throws an IllegalArgumentException when user tries to unfollow themselves")
    public void unfollowUser_BothIdsIdentical_ThrowsException() {
        var id = UUID.randomUUID();

        // when
        String message = assertThrows(IllegalArgumentException.class, () -> {
            userService.unfollowUser(id, id);
        }).getMessage();

        // then
        assertEquals("users cannot unfollow themselves", message);
    }

    @Test
    @DisplayName("getUserProfileCounters throws a ResourceNotFoundException when user id belongs to a non existent user")
    public void getUserProfileCounters_UserIdNotFound_ThrowsException() {
        var id = UUID.randomUUID();

        // given
        given(userRepository.existsById(id)).willReturn(false);

        // when
        String message = assertThrows(ResourceNotFoundException.class, () -> {
            userService.getUserProfileCounters(id);
        }).getMessage();

        // then
        assertEquals(String.format("user %s could not be found", id), message);
    }

    @Test
    @DisplayName("getUserProfileCounters does not confuse following with followers, and packs them correctly")
    public void getUserProfileCounters_CountersRead_ReturnsCorrectDto() throws ResourceNotFoundException {
        var id = UUID.randomUUID();

        // given
        given(userRepository.existsById(id)).willReturn(true);
        given(followRepository.countUserFollowing(id)).willReturn(100L);
        given(followRepository.countUserFollowers(id)).willReturn(500L);

        // when
        var result = userService.getUserProfileCounters(id);

        // then
        assertEquals(100L, result.getFollowing());
        assertEquals(500L, result.getFollowers());
    }

    @Test
    @DisplayName("findAllUserFollowing throws a ResourceNotFoundException when followed user id belongs to a non existent user")
    public void findAllUserFollowing_UserIdNotFound_ThrowsException() {
        var id = UUID.randomUUID();
        var pageable = Pageable.ofSize(2);

        // given
        given(userRepository.existsById(id)).willReturn(false);

        // when
        String message = assertThrows(ResourceNotFoundException.class, () -> {
            userService.findAllUserFollowing(id, pageable);
        }).getMessage();

        // then
        assertEquals(String.format("user %s could not be found", id), message);
    }

    @Test
    @DisplayName("findAllUserFollowing uses the follow repository")
    public void findAllUserFollowing_UserFound_UsesRepositories() throws ResourceNotFoundException {
        var id = UUID.randomUUID();
        var userDto = new UserDto(id, "testusername", "", "", "");

        // given
        given(userRepository.existsById(id)).willReturn(true);
        given(followRepository.findAllUserFollowing(
                eq(id), isA(Pageable.class)
        )).willReturn(new PageImpl<>(
                List.of(userDto))
        );

        // when
        var page = userService.findAllUserFollowing(id, Pageable.ofSize(2));

        // then
        assertEquals(1, page.getTotalElements());
        assertTrue(page.stream().anyMatch(e -> e.getId().equals(id)));
    }

    @Test
    @DisplayName("findAllUserFollowers throws a ResourceNotFoundException when followed user id belongs to a non existent user")
    public void findAllUserFollowers_UserIdNotFound_ThrowsException() {
        var id = UUID.randomUUID();
        var pageable = Pageable.ofSize(2);

        // given
        given(userRepository.existsById(id)).willReturn(false);

        // when
        String message = assertThrows(ResourceNotFoundException.class, () -> {
            userService.findAllUserFollowers(id, pageable);
        }).getMessage();

        // then
        assertEquals(String.format("user %s could not be found", id), message);
    }

    @Test
    @DisplayName("findAllUserFollowers uses the follow repository")
    public void findAllUserFollowers_UserFound_UsesRepositories() throws ResourceNotFoundException {
        var id = UUID.randomUUID();
        var userDto = new UserDto(id, "testusername", "", "", "");

        // given
        given(userRepository.existsById(id)).willReturn(true);
        given(followRepository.findAllUserFollowers(
                eq(id), isA(Pageable.class)
        )).willReturn(new PageImpl<>(List.of(userDto)));

        // when
        var page = userService.findAllUserFollowers(id, Pageable.ofSize(2));

        // then
        assertEquals(1, page.getTotalElements());
        assertTrue(page.stream().anyMatch(e -> e.getId().equals(id)));
    }

    @Test
    @DisplayName("findAllKnownUserFollowers throws a ResourceNotFoundException when source user id belongs to a non existent user")
    public void findAllKnownUserFollowers_SourceUserIdNotFound_ThrowsException() {
        var source = UUID.randomUUID();
        var target = UUID.randomUUID();

        var pageable = Pageable.ofSize(10);

        // given
        given(userRepository.existsById(source)).willReturn(false);

        // when
        String message = assertThrows(ResourceNotFoundException.class, () -> {
            userService.findAllKnownUserFollowers(source, target, pageable);
        }).getMessage();

        // then
        assertEquals(String.format("user %s could not be found", source), message);
    }

    @Test
    @DisplayName("findAllKnownUserFollowers throws a ResourceNotFoundException when target user id belongs to a non existent user")
    public void findAllKnownUserFollowers_TargetUserIdNotFound_ThrowsException() {
        var source = UUID.randomUUID();
        var target = UUID.randomUUID();

        var pageable = Pageable.ofSize(10);

        // given
        given(userRepository.existsById(source)).willReturn(true);
        given(userRepository.existsById(target)).willReturn(false);

        // when
        String message = assertThrows(ResourceNotFoundException.class, () -> {
            userService.findAllKnownUserFollowers(source, target, pageable);
        }).getMessage();

        // then
        assertEquals(String.format("user %s could not be found", target), message);
    }

    @Test
    @DisplayName("findAllKnownUserFollowers uses the follow repository")
    public void findAllKnownUserFollowers_UserFound_UsesRepositories() throws ResourceNotFoundException {
        var id = UUID.randomUUID();
        var target = UUID.randomUUID();
        var userDto = new UserDto(id, "testusername", "", "", "");

        var pageable = Pageable.ofSize(10);

        // given
        given(userRepository.existsById(id)).willReturn(true);
        given(userRepository.existsById(target)).willReturn(true);
        given(followRepository.findAllKnownUserFollowers(
                eq(id), eq(target), isA(Pageable.class)
        )).willReturn(new PageImpl<>(List.of(userDto)));

        // when
        var page = userService.findAllKnownUserFollowers(
                id, target, pageable
        );

        // then
        assertEquals(1, page.getTotalElements());
        assertTrue(page.stream().anyMatch(e -> e.getId().equals(id)));
    }

    @Test
    @DisplayName("findByUsername uses the correct repository when exact flag is false")
    public void findByUsername_ExactFalse_CallsCorrectRepository() {
        var username = "test";
        var pageable = Pageable.ofSize(10);
        var exact = false;

        // when
        userService.findByUsername(username, pageable, exact);

        // then
        verify(userRepository, times(1)).findByUsernameContaining(
                eq(username),
                eq(pageable)
        );
    }

    @Test
    @DisplayName("findByUsername uses the correct repository when exact flag is true")
    public void findByUsername_ExactTrue_CallsCorrectRepository() {
        var username = "test";
        var pageable = Pageable.ofSize(10);
        var exact = true;

        // when
        userService.findByUsername(username, pageable, exact);

        // then
        verify(userRepository, times(1)).findByUsernameExact(
                eq(username),
                eq(pageable)
        );
    }
}
