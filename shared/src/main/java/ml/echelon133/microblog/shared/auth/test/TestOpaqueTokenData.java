package ml.echelon133.microblog.shared.auth.test;

import ml.echelon133.microblog.shared.user.UserDto;

import java.util.UUID;

public class TestOpaqueTokenData {

    public static final String PRINCIPAL_ID = "13bd7469-24db-4275-b112-393bce762699";
    public static final String ACCESS_TOKEN = "test-access-token";
    public static final String USERNAME = "testusername";
    public static final UserDto PRINCIPAL_DTO = new UserDto(UUID.fromString(PRINCIPAL_ID), USERNAME, "", "");

}
