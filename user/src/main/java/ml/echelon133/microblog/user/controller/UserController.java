package ml.echelon133.microblog.user.controller;

import ml.echelon133.microblog.shared.user.UserCreationDto;
import ml.echelon133.microblog.shared.user.UserDto;
import ml.echelon133.microblog.user.exception.UserDataInvalidException;
import ml.echelon133.microblog.user.exception.UserNotFoundException;
import ml.echelon133.microblog.user.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.support.DefaultMessageSourceResolvable;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.List;
import java.util.Map;
import java.util.UUID;

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
            throw new UserDataInvalidException(List.of("Payload with new user data not provided"));
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
}