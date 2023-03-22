package ml.echelon133.microblog.shared.post.like;

import ml.echelon133.microblog.shared.post.Post;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import javax.persistence.EmbeddedId;
import javax.persistence.Entity;
import javax.persistence.EntityListeners;
import javax.persistence.Table;
import java.util.Date;
import java.util.UUID;

@Entity
@Table(name = "post_likes")
@EntityListeners(AuditingEntityListener.class)
public class Like {

    @EmbeddedId
    private LikeId likeId;

    @CreatedDate
    private Date dateCreated;

    public Like() {}
    public Like(UUID likingUser, Post likedPost) {
        this.likeId = new LikeId(likingUser, likedPost);
    }

    public LikeId getLikeId() {
        return likeId;
    }

    public void setLikeId(LikeId likeId) {
        this.likeId = likeId;
    }

    public Date getDateCreated() {
        return dateCreated;
    }

    public void setDateCreated(Date dateCreated) {
        this.dateCreated = dateCreated;
    }
}
