package ml.echelon133.microblog.user.controller;

import ml.echelon133.microblog.shared.user.UserDto;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.UUID;

@RestController
@RequestMapping("/api/users")
public class UserController {

    // temporary mock 'usersService'
    private static HashMap<UUID, UserDto> users;
    static {
        users = new HashMap<>();

        UserDto u1 = new UserDto();
        UUID u1Uuid = UUID.fromString("e2e322cb-2a0a-42a7-b0f5-7581628833a0");
        u1.setId(u1Uuid);
        u1.setName("test_user");
        u1.setDisplayedName("");
        u1.setAviUrl("");
        users.put(u1Uuid, u1);

        UserDto u2 = new UserDto();
        UUID u2Uuid = UUID.fromString("283bf869-6ba8-4f07-9d2d-d3d4bd9b82fc");
        u2.setId(u2Uuid);
        u2.setName("another_test_user");
        u2.setDisplayedName("");
        u2.setAviUrl("");
        users.put(u2Uuid, u2);
    }

    @GetMapping("/{id}")
    public UserDto getUser(@PathVariable String id) {
        return users.getOrDefault(UUID.fromString(id), new UserDto());
    }
}
