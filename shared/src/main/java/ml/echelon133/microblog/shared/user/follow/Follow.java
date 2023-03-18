package ml.echelon133.microblog.shared.user.follow;

import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import javax.persistence.*;
import java.util.Date;
import java.util.UUID;

@Entity
@EntityListeners(AuditingEntityListener.class)
public class Follow {

    @EmbeddedId
    private FollowId followId;

    @CreatedDate
    private Date dateCreated;

    public Follow() {}
    public Follow(UUID followingUser, UUID followedUser) {
        this.followId = new FollowId(followingUser, followedUser);
    }

    public FollowId getFollowId() {
        return followId;
    }

    public void setFollowId(FollowId followId) {
        this.followId = followId;
    }

    public Date getDateCreated() {
        return dateCreated;
    }

    public void setDateCreated(Date dateCreated) {
        this.dateCreated = dateCreated;
    }
}
