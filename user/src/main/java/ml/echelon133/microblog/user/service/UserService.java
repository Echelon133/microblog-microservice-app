package ml.echelon133.microblog.user.service;

import ml.echelon133.microblog.shared.user.*;
import ml.echelon133.microblog.user.exception.UserNotFoundException;
import ml.echelon133.microblog.user.exception.UsernameTakenException;
import ml.echelon133.microblog.user.repository.RoleRepository;
import ml.echelon133.microblog.user.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import javax.transaction.Transactional;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;

    @Autowired
    public UserService(UserRepository userRepository, RoleRepository roleRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.passwordEncoder = passwordEncoder;
    }

    private void throwIfUserNotFound(UUID id) throws UserNotFoundException {
        if (!userRepository.existsById(id)) {
            throw new UserNotFoundException(id);
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
        if (userRepository.existsUserByUsername(dto.getUsername())) {
            throw new UsernameTakenException(dto.getUsername());
        }

        String encodedPassword = passwordEncoder.encode(dto.getPassword());
        Set<Role> roles = Set.of(getDefaultUserRole());

        User newUser = new User(dto.getUsername(), dto.getEmail(), encodedPassword, "", roles);

        return userRepository.save(newUser).getId();
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
}
