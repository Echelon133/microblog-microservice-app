package ml.echelon133.microblog.user.controller;

import ml.echelon133.microblog.shared.user.follow.FollowDto;
import ml.echelon133.microblog.shared.user.UserCreationDto;
import ml.echelon133.microblog.shared.user.UserDto;
import ml.echelon133.microblog.shared.user.UserUpdateDto;
import ml.echelon133.microblog.user.exception.UserDataInvalidException;
import ml.echelon133.microblog.user.exception.UserNotFoundException;
import ml.echelon133.microblog.user.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.support.DefaultMessageSourceResolvable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.OAuth2AuthenticatedPrincipal;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Stream;

import static ml.echelon133.microblog.shared.auth.TokenOwnerIdExtractor.extractTokenOwnerIdFromPrincipal;

@RestController
@RequestMapping("/api/users")
public class UserController {

    private UserService userService;

    @Autowired
    public UserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping("/{id}")
    public UserDto getUser(@PathVariable UUID id) throws UserNotFoundException {
        return userService.findById(id);
    }

    @PostMapping("/register")
    public Map<String, UUID> registerUser(@Valid @RequestBody(required = false) UserCreationDto dto, BindingResult result)
            throws Exception {

        if (dto == null) {
            throw new UserDataInvalidException(List.of("payload with new user data not provided"));
        }

        if (result.hasErrors()) {
            List<String> errorMessages = result
                    .getAllErrors()
                    .stream()
                    .map(DefaultMessageSourceResolvable::getDefaultMessage)
                    .toList();
            throw new UserDataInvalidException(errorMessages);
        }

        return Map.of("uuid", userService.setupAndSaveUser(dto));
    }

    @GetMapping("/me")
    public UserDto getMe(@AuthenticationPrincipal OAuth2AuthenticatedPrincipal principal) throws UserNotFoundException {
        var id = extractTokenOwnerIdFromPrincipal(principal);
        return userService.findById(id);
    }

    @PatchMapping("/me")
    public UserDto patchMe(@Valid @RequestBody(required = false) UserUpdateDto dto,
                            BindingResult result,
                            @AuthenticationPrincipal OAuth2AuthenticatedPrincipal principal) throws Exception {

        var id = extractTokenOwnerIdFromPrincipal(principal);

        if (result.hasErrors()) {
            List<String> errorMessages = result
                    .getAllErrors()
                    .stream()
                    .map(DefaultMessageSourceResolvable::getDefaultMessage)
                    .toList();
            throw new UserDataInvalidException(errorMessages);
        }

        return userService.updateUserInfo(id, dto);
    }

    @GetMapping
    public Page<UserDto> searchUser(Pageable pageable,
                                    @RequestParam(value = "username_contains", required = false, defaultValue = "")
                                            String usernameContains,
                                    @RequestParam(value = "username_exact", required = false, defaultValue = "")
                                            String usernameExact) {


        var blankParamCount = Stream.of(usernameContains, usernameExact).map(String::isBlank).filter(p -> p).count();

        // invalid requests have either both params set or neither
        if (blankParamCount != 1) {
            String message;
            if (blankParamCount == 2) {
                message = "either 'username_contains' or 'username_exact' request param is required";
            } else {
                message = "only one of 'username_contains' or 'username_exact' request params can be provided at a time";
            }
            throw new IllegalArgumentException(message);
        } else {
            String username;
            boolean exact = false;

            if (!usernameExact.isBlank()) {
                // usernameExact is not blank, so search for exact matches
                exact = true;
                username = usernameExact;
            } else {
                // usernameContains is not blank, don't search for exact matches
                username = usernameContains;
            }
            return userService.findByUsername(username, pageable, exact);
        }
    }

    @GetMapping("/{targetId}/follow")
    public Map<String, Boolean> getFollow(@AuthenticationPrincipal OAuth2AuthenticatedPrincipal principal,
                                          @PathVariable UUID targetId) {
        var id = extractTokenOwnerIdFromPrincipal(principal);

        var follows = userService.followExists(id, targetId);
        return Map.of("follows", follows);
    }

    @PostMapping("/{targetId}/follow")
    public Map<String, Boolean> createFollow(@AuthenticationPrincipal OAuth2AuthenticatedPrincipal principal,
                                             @PathVariable UUID targetId) throws UserNotFoundException {
        var id = extractTokenOwnerIdFromPrincipal(principal);

        var follows = userService.followUser(id, targetId);
        return Map.of("follows", follows);
    }

    @DeleteMapping("/{targetId}/follow")
    public Map<String, Boolean> deleteFollow(@AuthenticationPrincipal OAuth2AuthenticatedPrincipal principal,
                                             @PathVariable UUID targetId) {
        var id = extractTokenOwnerIdFromPrincipal(principal);

        // negate the value, because unfollowUser returns true when user gets deleted, whereas this method
        // returns information about the existence of the follow relationship
        var follows = !userService.unfollowUser(id, targetId);
        return Map.of("follows", follows);
    }

    @GetMapping("/{id}/profile-counters")
    public FollowDto getProfileCounters(@PathVariable UUID id) throws UserNotFoundException {
        return userService.getUserProfileCounters(id);
    }

    @GetMapping("/{id}/following")
    public Page<UserDto> getFollowing(Pageable pageable, @PathVariable UUID id) throws UserNotFoundException {
        return userService.findAllUserFollowing(id, pageable);
    }

    @GetMapping("/{id}/followers")
    public Page<UserDto> getFollowers(Pageable pageable,
                                      @PathVariable UUID id,
                                      @AuthenticationPrincipal OAuth2AuthenticatedPrincipal principal,
                                      @RequestParam(required = false) boolean known) throws UserNotFoundException {
        Page<UserDto> page;
        if (known) {
            var authId = extractTokenOwnerIdFromPrincipal(principal);
            page = userService.findAllKnownUserFollowers(authId, id, pageable);
        } else {
            page =  userService.findAllUserFollowers(id, pageable);
        }
        return page;
    }
}