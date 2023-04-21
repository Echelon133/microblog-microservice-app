package ml.echelon133.microblog.user.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import ml.echelon133.microblog.shared.user.follow.FollowDto;
import ml.echelon133.microblog.shared.user.UserCreationDto;
import ml.echelon133.microblog.shared.user.UserDto;
import ml.echelon133.microblog.shared.user.UserUpdateDto;
import ml.echelon133.microblog.user.exception.UserNotFoundException;
import ml.echelon133.microblog.user.exception.UsernameTakenException;
import ml.echelon133.microblog.user.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentMatchers;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.test.json.JacksonTester;
import org.springframework.boot.test.json.JsonContent;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableHandlerMethodArgumentResolver;
import org.springframework.http.MediaType;
import org.springframework.security.web.method.annotation.AuthenticationPrincipalArgumentResolver;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;
import java.util.UUID;

import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isA;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import static ml.echelon133.microblog.shared.auth.test.OAuth2RequestPostProcessor.*;
import static ml.echelon133.microblog.shared.auth.test.TestOpaqueTokenData.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("Tests of UserController")
public class UserControllerTests {

    private MockMvc mvc;

    @Mock
    private UserService userService;

    @InjectMocks
    private UserController userController;

    @InjectMocks
    private UserExceptionHandler userExceptionHandler;

    private JacksonTester<UserCreationDto> jsonUserCreationDto;
    private JacksonTester<UserUpdateDto> jsonUserUpdateDto;

    private UserCreationDto createUserCreationDto(String username) {
        UserCreationDto dto = new UserCreationDto();
        dto.setUsername(username);
        dto.setPassword("Validpassword123;");
        dto.setPassword2("Validpassword123;");
        dto.setEmail("test@email.com");
        return dto;
    }

    private UserUpdateDto createUserUpdateDto() {
        // all lengths are max limits for each field, i.e. one more character in a field and
        // it's no longer an acceptable length
        return new UserUpdateDto(
                // displayedName is 40 characters
                "m4vE8PHHHvYsLXLlWNuFu7lOIKkjKc7R3zjUkBit",
                // aviUrl is 200 characters
                "64qa7JD6wQv2T8hGI6d30t92iTHsGfiV879nomat7lIAITGrZJv2YOfvmXmOOUeLEeq6Kl9GTg446FFhfpmqec9gajJY3q3tdoHIo5oAvNST8CxKSBiTl40C4xoVJFACpmjdwf09oZEORdL8Qp2q4wCqFdyuQL5IfAWGv2oW2Vcet3PpbYFZ9yzzDOf0RwxASabfDOhp",
                // description is 300 characters
                "2iKCU1QgmFprvxotHE7QyUFpmEUsSMmt12xrk0rRsNDCVAev8KtKQ4fonuwGzlU2ZRajDWrdA6cJnRvWkTuqvzrHHUPGFDEJof3r01qc8nQScOji78A2SczjMySZVdkm8aXT1uFt9bjmQ6Wny2HrY8E5QXhFoJWIGJKo4yWoIhqdvm0saFc9aDl4WAxzNIWhs4N3v61idq6R7j1yRyE1W1heM3pJR96yG4xmNNYar8IfhifgWGazPJBPTEVBK5oWEwlEvoidEn5oE97T7eTbwaUUI5wCZ8PioAy2u3dOet6e"
        );
    }

    @BeforeEach
    public void beforeEach() {
        JacksonTester.initFields(this, new ObjectMapper());

        mvc = MockMvcBuilders
                .standaloneSetup(userController)
                .setControllerAdvice(userExceptionHandler)
                .setCustomArgumentResolvers(
                        // this is required to resolve @AuthenticationPrincipal in controller methods
                        new AuthenticationPrincipalArgumentResolver(),
                        // this is required to resolve Pageable objects in controller methods
                        new PageableHandlerMethodArgumentResolver()
                )
                .build();
    }

    @Test
    @DisplayName("registerUser shows error when null payload")
    public void registerUser_PayloadNull_ReturnsExpectedErrors() throws Exception {
        mvc.perform(
                post("/api/users/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON)
                )
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.messages", hasSize(1)))
                .andExpect(jsonPath("$.messages", hasItem("Payload with new user data not provided")));
    }

    @Test
    @DisplayName("registerUser shows error when username invalid")
    public void registerUser_UsernameInvalid_ReturnsExpectedError() throws Exception {
        var invalidUsernames = List.of(
                "",                                 // too short
                ";sadfasdf",                        // contains semicolon
                "aaa...",                           // contains dot
                "ASDF___",                          // contains underscore
                "///",                              // contains slashes
                "\\\\",                             // contains backslashes
                "test  username",                   // contains space
                "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa"); // too long (longer than 30 characters)


        for (String invalidUsername : invalidUsernames) {
            UserCreationDto dto = createUserCreationDto(invalidUsername);
            JsonContent<UserCreationDto> json = jsonUserCreationDto.write(dto);

            mvc.perform(
                    post("/api/users/register")
                            .contentType(MediaType.APPLICATION_JSON)
                            .accept(MediaType.APPLICATION_JSON)
                            .content(json.getJson())
                    )
                    .andExpect(status().isUnprocessableEntity())
                    .andExpect(jsonPath("$.messages", hasSize(1)))
                    .andExpect(jsonPath("$.messages", hasItem("Username is not valid")));
        }
    }

    @Test
    @DisplayName("registerUser shows error when password not complex enough")
    public void registerUser_PasswordInvalidComplexity_ReturnsExpectedError() throws Exception {
        UserCreationDto dto = createUserCreationDto("testuser");

        var invalidPasswords = List.of(
                "", "a", "aa", "aaa", "aaab", "aaabb", "aaabbb", "aaabbbc",          // too short
                "aaaaaaaaaabbbbbbbbbbaaaaaaaaaabbbbbbbbbbaaaaaaaaaabbbbbbbbbbccccc", // too long
                "TESTPASSWORD123;",                                                  // no small characters
                "testpassword123;",                                                  // no upper characters
                "Testpassword123a"                                                   // no special character
        );

        for (String invalidPassword : invalidPasswords) {
            dto.setPassword(invalidPassword);
            dto.setPassword2(invalidPassword);
            JsonContent<UserCreationDto> json = jsonUserCreationDto.write(dto);

            mvc.perform(
                    post("/api/users/register")
                            .contentType(MediaType.APPLICATION_JSON)
                            .accept(MediaType.APPLICATION_JSON)
                            .content(json.getJson())
                    )
                    .andExpect(status().isUnprocessableEntity())
                    .andExpect(jsonPath("$.messages", hasSize(1)))
                    .andExpect(jsonPath("$.messages",
                            hasItem("Password does not satisfy complexity requirements")));
        }
    }

    @Test
    @DisplayName("registerUser shows error when passwords do not match")
    public void registerUser_PasswordDoNotMatch_ReturnsExpectedError() throws Exception {
        UserCreationDto dto = createUserCreationDto("testuser");
        dto.setPassword2("Different_password123;");
        JsonContent<UserCreationDto> json = jsonUserCreationDto.write(dto);

        mvc.perform(
                post("/api/users/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON)
                        .content(json.getJson())
                )
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.messages", hasSize(1)))
                .andExpect(jsonPath("$.messages", hasItem("passwords do not match")));
    }

    @Test
    @DisplayName("registerUser shows error when email blank")
    public void registerUser_EmailBlank_ReturnsExpectedError() throws Exception {
        UserCreationDto dto = createUserCreationDto("testuser");
        dto.setEmail("");

        JsonContent<UserCreationDto> json = jsonUserCreationDto.write(dto);

        mvc.perform(
                post("/api/users/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON)
                        .content(json.getJson())
                )
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.messages", hasSize(1)))
                .andExpect(jsonPath("$.messages", hasItem("email is required")));
    }

    @Test
    @DisplayName("registerUser shows error when email invalid")
    public void registerUser_EmailInvalid_ReturnsExpectedError() throws Exception {
        UserCreationDto dto = createUserCreationDto("testuser");

        // basic examples to make sure that the validation is there - current validator is provided by
        // the validation library, so it does not make sense to write more elaborate tests, because then
        // we'd be actually testing code from another library
        var invalidEmails = List.of(
            "a", "test", "testtest@", "@asdf", "mail@@gmail.com"
        );

        for (String invalidEmail : invalidEmails) {
            dto.setEmail(invalidEmail);
            JsonContent<UserCreationDto> json = jsonUserCreationDto.write(dto);

            mvc.perform(
                    post("/api/users/register")
                            .contentType(MediaType.APPLICATION_JSON)
                            .accept(MediaType.APPLICATION_JSON)
                            .content(json.getJson())
                    )
                    .andExpect(status().isUnprocessableEntity())
                    .andExpect(jsonPath("$.messages", hasSize(1)))
                    .andExpect(jsonPath("$.messages", hasItem("email is not valid")));
        }
    }

    @Test
    @DisplayName("registerUser shows error when username taken")
    public void registerUser_UsernameTaken_ReturnsExpectedError() throws Exception {
        String takenUsername = "testuser";

        UserCreationDto dto = createUserCreationDto(takenUsername);
        JsonContent<UserCreationDto> json = jsonUserCreationDto.write(dto);

        when(userService.setupAndSaveUser(ArgumentMatchers.any())).thenThrow(
                new UsernameTakenException()
        );

        mvc.perform(
                post("/api/users/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON)
                        .content(json.getJson())
                )
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.messages", hasSize(1)))
                .andExpect(jsonPath("$.messages", hasItem("username has already been taken")));
    }

    @Test
    @DisplayName("registerUser output ok when new user input is valid")
    public void registerUser_NewUserValid_ReturnsOk() throws Exception {
        UUID newUserUuid = UUID.randomUUID();
        UserCreationDto dto = createUserCreationDto("testuser");
        JsonContent<UserCreationDto> json = jsonUserCreationDto.write(dto);

        when(userService.setupAndSaveUser(ArgumentMatchers.any())).thenReturn(newUserUuid);

        mvc.perform(
                post("/api/users/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON)
                        .content(json.getJson())
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasEntry("uuid", newUserUuid.toString())));
    }

    @Test
    @DisplayName("getUser shows error when user does not exist")
    public void getUser_UserDoesNotExist_ReturnsExpectedError() throws Exception {
        UUID uuid = UUID.randomUUID();

        when(userService.findById(uuid)).thenThrow(
                new UserNotFoundException(uuid)
        );

        mvc.perform(
                get("/api/users/" + uuid)
                        .accept(MediaType.APPLICATION_JSON)
                )
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.messages", hasSize(1)))
                .andExpect(jsonPath("$.messages", hasItem(String.format("user %s could not be found", uuid))));
    }

    @Test
    @DisplayName("getUser output ok when user exists")
    public void getUser_UserExists_ReturnsOk() throws Exception {
        UUID uuid = UUID.randomUUID();
        UserDto foundUser = new UserDto(uuid, "test", "test", "", "test");

        when(userService.findById(uuid)).thenReturn(foundUser);

        mvc.perform(
                get("/api/users/" + uuid)
                        .accept(MediaType.APPLICATION_JSON)
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(uuid.toString())))
                .andExpect(jsonPath("$.username", is(foundUser.getUsername())))
                .andExpect(jsonPath("$.displayedName", is(foundUser.getDisplayedName())))
                .andExpect(jsonPath("$.aviUrl", is(foundUser.getAviUrl())))
                .andExpect(jsonPath("$.description", is(foundUser.getDescription())));
    }

    @Test
    @DisplayName("getMe output ok when principal provided")
    public void getMe_ProvidedPrincipal_ReturnsOk() throws Exception {
        when(userService.findById(UUID.fromString(PRINCIPAL_ID))).thenReturn(PRINCIPAL_DTO);

        mvc.perform(
                        get("/api/users/me")
                                .accept(MediaType.APPLICATION_JSON)
                                .with(customBearerToken())
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(PRINCIPAL_DTO.getId().toString())))
                .andExpect(jsonPath("$.username", is(PRINCIPAL_DTO.getUsername())))
                .andExpect(jsonPath("$.displayedName", is(PRINCIPAL_DTO.getDisplayedName())))
                .andExpect(jsonPath("$.aviUrl", is(PRINCIPAL_DTO.getAviUrl())))
                .andExpect(jsonPath("$.description", is(PRINCIPAL_DTO.getDescription())));
    }

    @Test
    @DisplayName("getMe shows error when user does not exist")
    public void getMe_UserDoesNotExist_ReturnsExpectedError() throws Exception {
        var id = UUID.fromString(PRINCIPAL_ID);

        when(userService.findById(id)).thenThrow(
                new UserNotFoundException(id)
        );

        mvc.perform(
                        get("/api/users/me")
                                .accept(MediaType.APPLICATION_JSON)
                                .with(customBearerToken())
                )
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.messages", hasSize(1)))
                .andExpect(jsonPath("$.messages", hasItem(String.format("user %s could not be found", id))));
    }

    @Test
    @DisplayName("patchMe shows error when displayedName too long")
    public void patchMe_DisplayedNameTooLong_ReturnsExpectedError() throws Exception {
        UserUpdateDto dto = createUserUpdateDto();
        // make displayedName one character longer, which is no longer valid
        dto.setDisplayedName(dto.getDisplayedName() + "a");
        // set other fields to null
        dto.setDescription(null);
        dto.setAviUrl(null);

        JsonContent<UserUpdateDto> json = jsonUserUpdateDto.write(dto);

        mvc.perform(
                        patch("/api/users/me")
                                .accept(MediaType.APPLICATION_JSON)
                                .contentType(MediaType.APPLICATION_JSON)
                                .with(customBearerToken())
                                .content(json.getJson())
                )
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.messages", hasSize(1)))
                .andExpect(jsonPath("$.messages",
                        hasItem("Field 'displayedName' cannot be longer than 40 characters")));
    }

    @Test
    @DisplayName("patchMe shows error when aviUrl too long")
    public void patchMe_AviUrlTooLong_ReturnsExpectedError() throws Exception {
        UserUpdateDto dto = createUserUpdateDto();
        // make aviUrl one character longer, which is no longer valid
        dto.setAviUrl(dto.getAviUrl() + "a");
        // set other fields to null
        dto.setDisplayedName(null);
        dto.setDescription(null);

        JsonContent<UserUpdateDto> json = jsonUserUpdateDto.write(dto);

        mvc.perform(
                        patch("/api/users/me")
                                .accept(MediaType.APPLICATION_JSON)
                                .contentType(MediaType.APPLICATION_JSON)
                                .with(customBearerToken())
                                .content(json.getJson())
                )
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.messages", hasSize(1)))
                .andExpect(jsonPath("$.messages",
                        hasItem("Field 'aviUrl' cannot be longer than 200 characters")));
    }

    @Test
    @DisplayName("patchMe shows error when description too long")
    public void patchMe_DescriptionTooLong_ReturnsExpectedError() throws Exception {
        UserUpdateDto dto = createUserUpdateDto();
        // make description one character longer, which is no longer valid
        dto.setDescription(dto.getDescription() + "a");
        // set other fields to null
        dto.setAviUrl(null);
        dto.setDisplayedName(null);

        JsonContent<UserUpdateDto> json = jsonUserUpdateDto.write(dto);

        mvc.perform(
                        patch("/api/users/me")
                                .accept(MediaType.APPLICATION_JSON)
                                .contentType(MediaType.APPLICATION_JSON)
                                .with(customBearerToken())
                                .content(json.getJson())
                )
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.messages", hasSize(1)))
                .andExpect(jsonPath("$.messages",
                        hasItem("Field 'description' cannot be longer than 300 characters")));
    }

    @Test
    @DisplayName("patchMe returns ok when fields valid")
    public void patchMe_FieldsValid_ReturnsOk() throws Exception {
        UserUpdateDto dto = createUserUpdateDto();

        JsonContent<UserUpdateDto> json = jsonUserUpdateDto.write(dto);

        when(userService.updateUserInfo(ArgumentMatchers.any(), ArgumentMatchers.any()))
                .thenReturn(PRINCIPAL_DTO);

        mvc.perform(
                        patch("/api/users/me")
                                .accept(MediaType.APPLICATION_JSON)
                                .contentType(MediaType.APPLICATION_JSON)
                                .with(customBearerToken())
                                .content(json.getJson())
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username", is(PRINCIPAL_DTO.getUsername())))
                .andExpect(jsonPath("$.displayedName", is(PRINCIPAL_DTO.getDisplayedName())))
                .andExpect(jsonPath("$.aviUrl", is(PRINCIPAL_DTO.getAviUrl())))
                .andExpect(jsonPath("$.description", is(PRINCIPAL_DTO.getDescription())));
    }


    @Test
    @DisplayName("searchUser returns error when required request param not provided")
    public void searchUser_NoRequiredParamProvided_ReturnsStatusBadRequest() throws Exception {
        mvc.perform(
                        get("/api/users")
                                .accept(MediaType.APPLICATION_JSON)
                                .with(customBearerToken())
                )
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.messages", hasSize(1)))
                .andExpect(jsonPath("$.messages",
                        hasItem("either 'username_contains' or 'username_exact' request param is required")));;
    }

    @Test
    @DisplayName("searchUser returns error when two mutually exclusive request params are provided at once")
    public void searchUser_MutuallyExclusiveParamsProvided_ReturnsStatusBadRequest() throws Exception {
        mvc.perform(
                        get("/api/users")
                                .accept(MediaType.APPLICATION_JSON)
                                .with(customBearerToken())
                                .param("username_contains", "test")
                                .param("username_exact", "test")
                )
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.messages", hasSize(1)))
                .andExpect(jsonPath("$.messages",
                        hasItem("only one of 'username_contains' or 'username_exact' request params can be provided at a time")));
    }

    @Test
    @DisplayName("searchUser returns ok when 'username_contains' param provided")
    public void searchUser_UsernameContainsParamProvided_ReturnsOk() throws Exception {
        var username = "asdf";
        var userDto = new UserDto(UUID.randomUUID(), username, "", "", "");
        Page<UserDto> page = new PageImpl<>(List.of(userDto), Pageable.ofSize(10), 1);

        when(userService.findByUsername(
                argThat(u -> u.equals(username)),
                ArgumentMatchers.any(Pageable.class),
                eq(false))
        ).thenReturn(page);

        mvc.perform(
                        get("/api/users")
                                .accept(MediaType.APPLICATION_JSON)
                                .with(customBearerToken())
                                .param("username_contains", username)
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements", is(1)))
                .andExpect(jsonPath("$.last", is(true)))
                .andExpect(jsonPath("$.content[0].id", is(userDto.getId().toString())))
                .andExpect(jsonPath("$.content[0].username", is(userDto.getUsername())))
                .andExpect(jsonPath("$.content[0].displayedName", is(userDto.getDisplayedName())))
                .andExpect(jsonPath("$.content[0].aviUrl", is(userDto.getAviUrl())))
                .andExpect(jsonPath("$.content[0].description", is(userDto.getDescription())));
    }

    @Test
    @DisplayName("searchUser returns ok when 'username_exact' param provided")
    public void searchUser_UsernameExactParamProvided_ReturnsOk() throws Exception {
        var username = "asdf";
        var userDto = new UserDto(UUID.randomUUID(), username, "", "", "");
        Page<UserDto> page = new PageImpl<>(List.of(userDto), Pageable.ofSize(10), 1);

        when(userService.findByUsername(
                argThat(u -> u.equals(username)),
                ArgumentMatchers.any(Pageable.class),
                eq(true))
        ).thenReturn(page);

        mvc.perform(
                        get("/api/users")
                                .accept(MediaType.APPLICATION_JSON)
                                .with(customBearerToken())
                                .param("username_exact", username)
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements", is(1)))
                .andExpect(jsonPath("$.last", is(true)))
                .andExpect(jsonPath("$.content[0].id", is(userDto.getId().toString())))
                .andExpect(jsonPath("$.content[0].username", is(userDto.getUsername())))
                .andExpect(jsonPath("$.content[0].displayedName", is(userDto.getDisplayedName())))
                .andExpect(jsonPath("$.content[0].aviUrl", is(userDto.getAviUrl())))
                .andExpect(jsonPath("$.content[0].description", is(userDto.getDescription())));
    }

    @Test
    @DisplayName("getFollow returns ok when follow exists")
    public void getFollow_FollowExists_ReturnsOk() throws Exception {
        var sourceId = UUID.fromString(PRINCIPAL_ID);
        var targetId = UUID.randomUUID();

        when(userService.followExists(sourceId, targetId)).thenReturn(true);

        mvc.perform(
                    get("/api/users/" + targetId + "/follow")
                                .accept(MediaType.APPLICATION_JSON)
                                .with(customBearerToken())
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.follows", is(true)));
    }

    @Test
    @DisplayName("createFollow returns error when service throws")
    public void createFollow_ServiceThrows_ReturnsExpectedError() throws Exception {
        var sourceId = UUID.fromString(PRINCIPAL_ID);
        var targetId = UUID.randomUUID();

        when(userService.followUser(sourceId, targetId))
                .thenThrow(new UserNotFoundException(targetId));

        mvc.perform(
                        post("/api/users/" + targetId + "/follow")
                                .accept(MediaType.APPLICATION_JSON)
                                .with(customBearerToken())
                )
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.messages", hasSize(1)))
                .andExpect(jsonPath("$.messages", hasItem(String.format("user %s could not be found", targetId))));
    }

    @Test
    @DisplayName("createFollow returns ok when follow created")
    public void createFollow_FollowCreated_ReturnsOk() throws Exception {
        var sourceId = UUID.fromString(PRINCIPAL_ID);
        var targetId = UUID.randomUUID();

        when(userService.followUser(sourceId, targetId)).thenReturn(true);

        mvc.perform(
                        post("/api/users/" + targetId + "/follow")
                                .accept(MediaType.APPLICATION_JSON)
                                .with(customBearerToken())
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.follows", is(true)));
    }

    @Test
    @DisplayName("deleteFollow returns ok when follow deleted")
    public void deleteFollow_FollowDeleted_ReturnsOk() throws Exception {
        var sourceId = UUID.fromString(PRINCIPAL_ID);
        var targetId = UUID.randomUUID();

        when(userService.unfollowUser(sourceId, targetId)).thenReturn(true);

        mvc.perform(
                        delete("/api/users/" + targetId + "/follow")
                                .accept(MediaType.APPLICATION_JSON)
                                .with(customBearerToken())
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.follows", is(false)));
    }

    @Test
    @DisplayName("deleteFollow returns error when user tries to unfollow themselves")
    public void deleteFollow_UserUnfollowsThemselves_ReturnsExpectedError() throws Exception {
        var id = UUID.fromString(PRINCIPAL_ID);

        when(userService.unfollowUser(id, id)).thenCallRealMethod();

        mvc.perform(
                        delete("/api/users/" + id + "/follow")
                                .accept(MediaType.APPLICATION_JSON)
                                .with(customBearerToken())
                )
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.messages", hasSize(1)))
                .andExpect(jsonPath("$.messages",
                        hasItem("Users cannot unfollow themselves")));
    }

    @Test
    @DisplayName("getProfileCounters returns error when service throws")
    public void getProfileCounters_ServiceThrows_ReturnsExpectedError() throws Exception {
        var id = UUID.fromString(PRINCIPAL_ID);

        when(userService.getUserProfileCounters(id))
                .thenThrow(new UserNotFoundException(id));

        mvc.perform(
                        get("/api/users/" + id + "/profile-counters")
                                .accept(MediaType.APPLICATION_JSON)
                                .with(customBearerToken())
                )
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.messages", hasSize(1)))
                .andExpect(jsonPath("$.messages", hasItem(String.format("user %s could not be found", id))));
    }

    @Test
    @DisplayName("getProfileCounters returns ok when counters read")
    public void getProfileCounters_CountersRead_ReturnsOk() throws Exception {
        var id = UUID.fromString(PRINCIPAL_ID);

        when(userService.getUserProfileCounters(id))
                .thenReturn(new FollowDto(100L, 500L));

        mvc.perform(
                        get("/api/users/" + id + "/profile-counters")
                                .accept(MediaType.APPLICATION_JSON)
                                .with(customBearerToken())
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.following", is(100)))
                .andExpect(jsonPath("$.followers", is(500)));
    }

    @Test
    @DisplayName("getFollowing returns error when service throws")
    public void getFollowing_ServiceThrows_ReturnsExpectedError() throws Exception {
        var id = UUID.randomUUID();

        when(userService.findAllUserFollowing(eq(id), isA(Pageable.class))).thenThrow(new UserNotFoundException(id));

        mvc.perform(
                        get("/api/users/" + id + "/following")
                                .accept(MediaType.APPLICATION_JSON)
                                .with(customBearerToken())
                )
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.messages", hasSize(1)))
                .andExpect(jsonPath("$.messages", hasItem(String.format("user %s could not be found", id))));
    }

    @Test
    @DisplayName("getFollowing returns ok when page found")
    public void getFollowing_PageFound_ReturnsOk() throws Exception {
        var id = UUID.randomUUID();
        var userDto = new UserDto(id, "asdf123", "", "", "");

        Page<UserDto> page = new PageImpl<>(List.of(userDto), Pageable.ofSize(10), 1);

        when(userService.findAllUserFollowing(eq(id), isA(Pageable.class))).thenReturn(page);

        mvc.perform(
                        get("/api/users/" + id + "/following")
                                .accept(MediaType.APPLICATION_JSON)
                                .with(customBearerToken())
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements", is(1)))
                .andExpect(jsonPath("$.last", is(true)))
                .andExpect(jsonPath("$.content[0].id", is(userDto.getId().toString())))
                .andExpect(jsonPath("$.content[0].username", is(userDto.getUsername())))
                .andExpect(jsonPath("$.content[0].displayedName", is(userDto.getDisplayedName())))
                .andExpect(jsonPath("$.content[0].aviUrl", is(userDto.getAviUrl())))
                .andExpect(jsonPath("$.content[0].description", is(userDto.getDescription())));
    }

    @Test
    @DisplayName("getFollowers returns error when service throws")
    public void getFollowers_ServiceThrows_ReturnsExpectedError() throws Exception {
        var id = UUID.randomUUID();

        when(userService.findAllUserFollowers(eq(id), isA(Pageable.class))).thenThrow(new UserNotFoundException(id));

        mvc.perform(
                        get("/api/users/" + id + "/followers")
                                .accept(MediaType.APPLICATION_JSON)
                                .with(customBearerToken())
                )
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.messages", hasSize(1)))
                .andExpect(jsonPath("$.messages", hasItem(String.format("user %s could not be found", id))));
    }

    @Test
    @DisplayName("getFollowers returns ok when page found and parameter 'known' not provided")
    public void getFollowers_PageFoundAndKnownParamNotProvided_ReturnsOk() throws Exception {
        var id = UUID.randomUUID();
        var userDto = new UserDto(id, "asdf123", "", "", "");

        Page<UserDto> page = new PageImpl<>(List.of(userDto), Pageable.ofSize(10), 1);

        when(userService.findAllUserFollowers(eq(id), isA(Pageable.class))).thenReturn(page);

        mvc.perform(
                        get("/api/users/" + id + "/followers")
                                .accept(MediaType.APPLICATION_JSON)
                                .with(customBearerToken())
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements", is(1)))
                .andExpect(jsonPath("$.last", is(true)))
                .andExpect(jsonPath("$.content[0].id", is(userDto.getId().toString())))
                .andExpect(jsonPath("$.content[0].username", is(userDto.getUsername())))
                .andExpect(jsonPath("$.content[0].displayedName", is(userDto.getDisplayedName())))
                .andExpect(jsonPath("$.content[0].aviUrl", is(userDto.getAviUrl())))
                .andExpect(jsonPath("$.content[0].description", is(userDto.getDescription())));
    }

    @Test
    @DisplayName("getFollowers returns ok when page found and parameter 'known' is false")
    public void getFollowers_PageFoundAndKnownParamFalse_ReturnsOk() throws Exception {
        var id = UUID.randomUUID();
        var userDto = new UserDto(id, "asdf123", "", "", "");

        Page<UserDto> page = new PageImpl<>(List.of(userDto), Pageable.ofSize(10), 1);

        when(userService.findAllUserFollowers(eq(id), isA(Pageable.class))).thenReturn(page);

        mvc.perform(
                        get("/api/users/" + id + "/followers")
                                .accept(MediaType.APPLICATION_JSON)
                                .with(customBearerToken())
                                .param("known", "false")
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements", is(1)))
                .andExpect(jsonPath("$.last", is(true)))
                .andExpect(jsonPath("$.content[0].id", is(userDto.getId().toString())))
                .andExpect(jsonPath("$.content[0].username", is(userDto.getUsername())))
                .andExpect(jsonPath("$.content[0].displayedName", is(userDto.getDisplayedName())))
                .andExpect(jsonPath("$.content[0].aviUrl", is(userDto.getAviUrl())))
                .andExpect(jsonPath("$.content[0].description", is(userDto.getDescription())));
    }

    @Test
    @DisplayName("getFollowers returns ok when page of known users found and parameter 'known' is true")
    public void getFollowers_PageFoundAndKnownParamTrue_ReturnsOk() throws Exception {
        var id = UUID.fromString(PRINCIPAL_ID);
        var targetId = UUID.randomUUID();
        var userDto = new UserDto(id, "asdf123", "", "", "");

        Page<UserDto> page = new PageImpl<>(List.of(userDto), Pageable.ofSize(10), 1);

        when(userService.findAllKnownUserFollowers(eq(id), eq(targetId), isA(Pageable.class))).thenReturn(page);

        mvc.perform(
                        get("/api/users/" + targetId + "/followers")
                                .accept(MediaType.APPLICATION_JSON)
                                .with(customBearerToken())
                                .param("known", "true")
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements", is(1)))
                .andExpect(jsonPath("$.last", is(true)))
                .andExpect(jsonPath("$.content[0].id", is(userDto.getId().toString())))
                .andExpect(jsonPath("$.content[0].username", is(userDto.getUsername())))
                .andExpect(jsonPath("$.content[0].displayedName", is(userDto.getDisplayedName())))
                .andExpect(jsonPath("$.content[0].aviUrl", is(userDto.getAviUrl())))
                .andExpect(jsonPath("$.content[0].description", is(userDto.getDescription())));
    }
}
