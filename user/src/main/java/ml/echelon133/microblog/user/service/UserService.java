package ml.echelon133.microblog.user.service;

import ml.echelon133.microblog.shared.notification.Notification;
import ml.echelon133.microblog.shared.notification.NotificationCreationDto;
import ml.echelon133.microblog.shared.user.*;
import ml.echelon133.microblog.shared.user.follow.Follow;
import ml.echelon133.microblog.shared.user.follow.FollowDto;
import ml.echelon133.microblog.shared.user.follow.FollowId;
import ml.echelon133.microblog.shared.user.follow.FollowInfoDto;
import ml.echelon133.microblog.user.exception.UserNotFoundException;
import ml.echelon133.microblog.user.exception.UsernameTakenException;
import ml.echelon133.microblog.user.queue.FollowPublisher;
import ml.echelon133.microblog.user.queue.NotificationPublisher;
import ml.echelon133.microblog.user.repository.FollowRepository;
import ml.echelon133.microblog.user.repository.RoleRepository;
import ml.echelon133.microblog.user.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import javax.transaction.Transactional;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final FollowRepository followRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final FollowPublisher followPublisher;
    private final NotificationPublisher notificationPublisher;

    @Autowired
    public UserService(UserRepository userRepository,
                       FollowRepository followRepository,
                       RoleRepository roleRepository,
                       PasswordEncoder passwordEncoder,
                       FollowPublisher followPublisher,
                       NotificationPublisher notificationPublisher) {
        this.userRepository = userRepository;
        this.followRepository = followRepository;
        this.roleRepository = roleRepository;
        this.passwordEncoder = passwordEncoder;
        this.followPublisher = followPublisher;
        this.notificationPublisher = notificationPublisher;
    }

    private void throwIfUserNotFound(UUID id) throws UserNotFoundException {
        if (!userRepository.existsById(id)) {
            throw new UserNotFoundException(id);
        }
    }

    private void throwIfEitherUserNotFound(UUID source, UUID target) throws UserNotFoundException {
        if (!userRepository.existsById(source)) {
            throw new UserNotFoundException(source);
        }
        if (!userRepository.existsById(target)) {
            throw new UserNotFoundException(target);
        }
    }

    /**
     * Gets the default user role from the database. If such role does not exist,
     * this method creates it and saves it.
     *
     * @return default user role from the database
     */
    private Role getDefaultUserRole() {
        Optional<Role> defaultUserRole = roleRepository.findByName("ROLE_USER");
        if (defaultUserRole.isEmpty()) {
            Role role = new Role("ROLE_USER");
            return roleRepository.save(role);
        }
        return defaultUserRole.get();
    }

    /**
     * Configures and saves a new user. {@link UserCreationDto} <strong>needs to be validated before being passed to this
     * method</strong> and errors collected into a {@link org.springframework.validation.BindingResult} cannot
     * be ignored.
     *
     * @param dto pre-validated dto containing new user's information
     * @return a UUID of the new user
     * @throws UsernameTakenException thrown when the username is already taken
     */
    @Transactional
    public UUID setupAndSaveUser(UserCreationDto dto) throws UsernameTakenException {
        if (userRepository.existsUserByUsernameIgnoreCase(dto.getUsername())) {
            throw new UsernameTakenException(dto.getUsername());
        }

        String encodedPassword = passwordEncoder.encode(dto.getPassword());
        Set<Role> roles = Set.of(getDefaultUserRole());

        User newUser = new User(dto.getUsername(), dto.getEmail(), encodedPassword, "", roles);

        var savedUserId = userRepository.save(newUser).getId();

        // make every user follow themselves to simplify the queries which generate user's feed
        followRepository.save(new Follow(savedUserId, savedUserId));
        followPublisher.publishFollow(new FollowInfoDto(savedUserId, savedUserId));

        return savedUserId;
    }

    /**
     * Updates user info, such as:
     * <ul>
     *     <li>displayed username</li>
     *     <li>profile description</li>
     *     <li>avatar url</li>
     * </ul>
     *
     * {@link UserUpdateDto} <strong>needs to be validated before being passed to this method</strong> and errors
     * collected into a {@link org.springframework.validation.BindingResult} cannot be ignored.
     *
     * @param userId id of the user whose profile info needs to be updated
     * @param dto pre-validated dto containing information that needs to be placed in the database
     * @return a {@link UserDto} containing the applied update
     * @throws UserNotFoundException thrown when the user with specified id does not exist
     */
    @Transactional
    public UserDto updateUserInfo(UUID userId, UserUpdateDto dto) throws UserNotFoundException {
        throwIfUserNotFound(userId);

        if (dto.getDisplayedName() != null) {
            userRepository.updateDisplayedName(userId, dto.getDisplayedName());
        }

        if (dto.getDescription() != null) {
            userRepository.updateDescription(userId, dto.getDescription());
        }

        if (dto.getAviUrl() != null) {
            userRepository.updateAviUrl(userId, dto.getAviUrl());
        }

        return userRepository.findByUserId(userId);
    }

    /**
     * Projects the user with specified {@link java.util.UUID} into a DTO object.
     *
     * @param id id of the user
     * @return DTO projection of the user
     * @throws UserNotFoundException thrown when the user with specified id does not exist
     */
    @Transactional
    public UserDto findById(UUID id) throws UserNotFoundException {
        throwIfUserNotFound(id);
        return userRepository.findByUserId(id);
    }

    /**
     * Creates a {@link Page} containing user projections of users whose username either:
     * <ul>
     *     <li>contains a given {@code username} (case ignored, multiple results)</li>
     *     <li>contains an exact username (case ignored, only 1 result at most)</li>
     * </ul>
     *
     * @param username full username or a phrase which should occur within a username
     * @param pageable information about the wanted page
     * @param exact whether the username should be an exact match (if found, guaranteed only 1 result)
     * @return a {@link Page} containing results of a search query
     */
    public Page<UserDto> findByUsername(String username, Pageable pageable, boolean exact) {
        if (exact) {
            return userRepository.findByUsernameExact(username, pageable);
        }
        return userRepository.findByUsernameContaining(username, pageable);
    }

    /**
     * Checks if there is a follow relationship between the users with given {@link UUID}s.
     *
     * @param followSource id of the user who is potentially following
     * @param followTarget id of the user who is potentially being followed
     * @return {@code true} if there is a follow relationship between the users
     */
    public boolean followExists(UUID followSource, UUID followTarget) {
        return followRepository.existsById(new FollowId(followSource, followTarget));
    }

    /**
     * Creates a follow relationship between the users with given {@link UUID}s.
     *
     * @param followSource id of the user following
     * @param followTarget id of the user being followed
     * @return {@code true} if a follow has been created
     * @throws UserNotFoundException when either {@code followSource} or {@code followTarget} does not represent an actual user
     */
    @Transactional
    public boolean followUser(UUID followSource, UUID followTarget) throws UserNotFoundException {
        throwIfEitherUserNotFound(followSource, followTarget);
        followRepository.save(new Follow(followSource, followTarget));
        followPublisher.publishFollow(new FollowInfoDto(followSource, followTarget));
        notificationPublisher.publishNotification(
                new NotificationCreationDto(followTarget, followSource, Notification.Type.FOLLOW)
        );
        return followExists(followSource, followTarget);
    }

    /**
     * Removes a follow relationship between the users with given {@link UUID}s.
     * @param followSource id of the user unfollowing
     * @param followTarget id of the user being unfollowed
     * @return {@code true} if a follow no longer exists
     */
    @Transactional
    public boolean unfollowUser(UUID followSource, UUID followTarget) {
        // do not let users unfollow themselves, because it breaks the invariant established
        // during creation of the user
        if (followSource.equals(followTarget)) {
            throw new IllegalArgumentException("Users cannot unfollow themselves");
        }

        followRepository.deleteById(new FollowId(followSource, followTarget));
        followPublisher.publishUnfollow(new FollowInfoDto(followSource, followTarget));
        return !followExists(followSource, followTarget);
    }

    /**
     * Returns counters which show how many users are being followed by the user
     * and how many follow the user.
     *
     * @param userId id of the user whose counters are being read
     * @return DTO containing both counters
     * @throws UserNotFoundException thrown when the user with specified id does not exist
     */
    public FollowDto getUserProfileCounters(UUID userId) throws UserNotFoundException {
        throwIfUserNotFound(userId);
        var following = followRepository.countUserFollowing(userId);
        var followers = followRepository.countUserFollowers(userId);
        return new FollowDto(following, followers);
    }

    /**
     * Creates a {@link Page} containing user projections of users who are being followed by the user with
     * {@code userId}.
     *
     * @param userId the id of the user who is following
     * @param pageable information about the wanted page
     * @return a {@link Page} containing found user projections
     * @throws UserNotFoundException thrown when the user with specified id does not exist
     */
    public Page<UserDto> findAllUserFollowing(UUID userId, Pageable pageable) throws UserNotFoundException {
        throwIfUserNotFound(userId);
        return followRepository.findAllUserFollowing(userId, pageable);
    }

    /**
     * Creates a {@link Page} containing user projections of users who are following the user with
     * {@code userId}.
     *
     * @param userId the id of the user who is being followed
     * @param pageable information about the wanted page
     * @return a {@link Page} containing found user projections
     * @throws UserNotFoundException thrown when the user with specified id does not exist
     */
    public Page<UserDto> findAllUserFollowers(UUID userId, Pageable pageable) throws UserNotFoundException {
        throwIfUserNotFound(userId);
        return followRepository.findAllUserFollowers(userId, pageable);
    }

    /**
     * Creates a {@link Page} containing user projections of users who are following {@code targetId}
     * while being followed by {@code sourceId}.
     *
     * @param sourceId the id of the user who asks for known followers
     * @param targetId the id of the user who is targeted for evaluation of users known by {@code sourceId}
     * @param pageable information about the wanted page
     * @return a {@link Page} containing found user projections
     * @throws UserNotFoundException thrown when the user with specified id does not exist
     */
    public Page<UserDto> findAllKnownUserFollowers(UUID sourceId, UUID targetId, Pageable pageable) throws UserNotFoundException {
        throwIfEitherUserNotFound(sourceId, targetId);
        return followRepository.findAllKnownUserFollowers(sourceId, targetId, pageable);
    }
}
