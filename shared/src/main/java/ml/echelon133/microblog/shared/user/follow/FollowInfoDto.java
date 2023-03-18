package ml.echelon133.microblog.shared.user.follow;

import java.io.Serializable;
import java.util.UUID;

public class FollowInfoDto implements Serializable {

    private UUID followingUser;
    private UUID followedUser;

    public FollowInfoDto() {}
    public FollowInfoDto(UUID followingUser, UUID followedUser) {
        this.followingUser = followingUser;
        this.followedUser = followedUser;
    }

    public UUID getFollowingUser() {
        return followingUser;
    }

    public void setFollowingUser(UUID followingUser) {
        this.followingUser = followingUser;
    }

    public UUID getFollowedUser() {
        return followedUser;
    }

    public void setFollowedUser(UUID followedUser) {
        this.followedUser = followedUser;
    }
}
