package ml.echelon133.microblog.shared.post.like;

import ml.echelon133.microblog.shared.post.Post;

import javax.persistence.*;
import java.io.Serializable;
import java.util.Objects;
import java.util.UUID;

@Embeddable
public class LikeId implements Serializable {

    UUID likingUser;

    @ManyToOne(fetch = FetchType.LAZY, cascade = CascadeType.MERGE)
    @JoinColumn(name = "liked_post_id")
    Post likedPost;

    public LikeId() {}
    public LikeId(UUID likingUser, Post likedPost) {
        this.likingUser = likingUser;
        this.likedPost = likedPost;
    }

    public UUID getLikingUser() {
        return likingUser;
    }

    public void setLikingUser(UUID likingUser) {
        this.likingUser = likingUser;
    }

    public Post getLikedPost() {
        return likedPost;
    }

    public void setLikedPost(Post likedPost) {
        this.likedPost = likedPost;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof LikeId likeId)) return false;
        return Objects.equals(likingUser, likeId.likingUser) && Objects.equals(likedPost, likeId.likedPost);
    }

    @Override
    public int hashCode() {
        return Objects.hash(likingUser, likedPost);
    }
}
