package ml.echelon133.microblog.shared.post;

import ml.echelon133.microblog.shared.base.BaseEntity;
import ml.echelon133.microblog.shared.post.tag.Tag;

import javax.persistence.*;
import java.util.Set;
import java.util.UUID;

@Entity
@Table(indexes = {
        @Index(name = "quoted_posts_index", columnList = "quoted_post_id"),
        @Index(name = "parent_posts_index", columnList = "parent_post_id")
})
public class Post extends BaseEntity {

    @Column(nullable = false, updatable = false)
    private UUID authorId;

    @Column(nullable = false, updatable = false, length = 300)
    private String content;

    @ManyToMany(cascade = {CascadeType.PERSIST, CascadeType.MERGE})
    @JoinTable(
            name = "post_tags",
            joinColumns = @JoinColumn(name = "post_id"),
            inverseJoinColumns = @JoinColumn(name = "tag_id")
    )
    Set<Tag> tags;

    @ManyToOne(fetch = FetchType.LAZY, cascade = CascadeType.MERGE)
    @JoinColumn(name = "quoted_post_id")
    private Post quotedPost;

    @ManyToOne(fetch = FetchType.LAZY, cascade = CascadeType.MERGE)
    @JoinColumn(name = "parent_post_id")
    private Post parentPost;

    private boolean deleted;

    public Post() {}
    public Post(UUID authorId, String content, Set<Tag> tags) {
        this.authorId = authorId;
        this.content = content;
        this.tags = tags;
        this.deleted = false;
    }

    public UUID getAuthorId() {
        return authorId;
    }

    public void setAuthorId(UUID authorId) {
        this.authorId = authorId;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public Set<Tag> getTags() {
        return tags;
    }

    public void setTags(Set<Tag> tags) {
        this.tags = tags;
    }

    public boolean isDeleted() {
        return deleted;
    }

    public void setDeleted(boolean deleted) {
        this.deleted = deleted;
    }

    public Post getQuotedPost() {
        return quotedPost;
    }

    public void setQuotedPost(Post quotedPost) {
        this.quotedPost = quotedPost;
    }

    public Post getParentPost() {
        return parentPost;
    }

    public void setParentPost(Post parentPost) {
        this.parentPost = parentPost;
    }
}
