package ml.echelon133.microblog.user.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import ml.echelon133.microblog.shared.user.UserCreationDto;
import ml.echelon133.microblog.shared.user.UserDto;
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
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;
import java.util.UUID;

import static org.hamcrest.Matchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/*
    Disable kubernetes during tests to make local execution of tests possible.
    If kubernetes is not disabled, tests won't execute at all because Spring will
    fail to configure kubernetes when run outside it.
 */
@TestPropertySource(properties = "spring.cloud.kubernetes.enabled=false")
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

    private UserCreationDto createUserCreationDto(String username) {
        UserCreationDto dto = new UserCreationDto();
        dto.setUsername(username);
        dto.setPassword("Validpassword123;");
        dto.setPassword2("Validpassword123;");
        dto.setEmail("test@email.com");
        return dto;
    }

    @BeforeEach
    public void beforeEach() {
        JacksonTester.initFields(this, new ObjectMapper());

        mvc = MockMvcBuilders
                .standaloneSetup(userController)
                .setControllerAdvice(userExceptionHandler)
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
                .andExpect(jsonPath("$.messages", hasItem("Passwords do not match")));
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
                .andExpect(jsonPath("$.messages",
                        hasItem("Email is required")));
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
                    .andExpect(jsonPath("$.messages",
                            hasItem("Email is not valid")));
        }
    }

    @Test
    @DisplayName("registerUser shows error when username taken")
    public void registerUser_UsernameTaken_ReturnsExpectedError() throws Exception {
        String takenUsername = "testuser";

        UserCreationDto dto = createUserCreationDto(takenUsername);
        JsonContent<UserCreationDto> json = jsonUserCreationDto.write(dto);

        when(userService.setupAndSaveUser(ArgumentMatchers.any())).thenThrow(
                new UsernameTakenException(takenUsername)
        );

        mvc.perform(
                post("/api/users/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON)
                        .content(json.getJson())
                )
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.messages", hasSize(1)))
                .andExpect(jsonPath("$.messages",
                        hasItem("User with username testuser already exists")));
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
                .andExpect(jsonPath("$.messages",
                        hasItem(String.format("User with id %s could not be found", uuid))));
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
}
