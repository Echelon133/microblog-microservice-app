package ml.echelon133.microblog.post.web;

import ml.echelon133.microblog.shared.user.UserDto;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.data.domain.Page;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

@FeignClient(name = "user-service", url = "http://user:80", fallback=UserServiceClientFallback.class)
public interface UserServiceClient {

    @GetMapping(value = "/api/users")
    Page<UserDto> getUserExact(@RequestParam(name = "username_exact") String username);
}
