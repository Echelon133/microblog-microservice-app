package ml.echelon133.microblog.shared.user;

public class FollowDto {
    private long following;
    private long followers;

    public FollowDto() {}
    public FollowDto(long following, long followers) {
        this.following = following;
        this.followers = followers;
    }

    public long getFollowing() {
        return following;
    }

    public void setFollowing(long following) {
        this.following = following;
    }

    public long getFollowers() {
        return followers;
    }

    public void setFollowers(long followers) {
        this.followers = followers;
    }
}
