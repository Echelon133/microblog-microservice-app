package ml.echelon133.microblog.shared.post;

import ml.echelon133.microblog.shared.base.BaseEntity;
import ml.echelon133.microblog.shared.post.tag.Tag;

import javax.persistence.*;
import java.util.Set;
import java.util.UUID;

@Entity
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
}
