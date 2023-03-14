package ml.echelon133.microblog.shared.user;

import javax.persistence.Embeddable;
import java.io.Serializable;
import java.util.Objects;
import java.util.UUID;

@Embeddable
public class FollowId implements Serializable {

    UUID followingUser;
    UUID followedUser;

    public FollowId() {}
    public FollowId(UUID followingUser, UUID followedUser) {
        this.followingUser = followingUser;
        this.followedUser = followedUser;
    }

    public UUID getFollowingUser() {
        return followingUser;
    }

    public UUID getFollowedUser() {
        return followedUser;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof FollowId followId)) return false;
        return Objects.equals(followingUser, followId.followingUser) && Objects.equals(followedUser, followId.followedUser);
    }

    @Override
    public int hashCode() {
        return Objects.hash(followingUser, followedUser);
    }
}
