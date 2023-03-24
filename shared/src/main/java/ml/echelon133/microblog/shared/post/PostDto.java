package ml.echelon133.microblog.shared.post;

import java.util.Date;
import java.util.UUID;

public class PostDto {

    private UUID id;
    private Date dateCreated;
    private String content;
    private UUID authorId;
    private UUID quotedPost;
    private UUID parentPost;

    public PostDto() {}
    public PostDto(UUID id, Date dateCreated, String content, UUID authorId, UUID quotedPost, UUID parentPost) {
        this.id = id;
        this.dateCreated = dateCreated;
        this.content = content;
        this.authorId = authorId;
        this.quotedPost = quotedPost;
        this.parentPost = parentPost;
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public Date getDateCreated() {
        return dateCreated;
    }

    public void setDateCreated(Date dateCreated) {
        this.dateCreated = dateCreated;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public UUID getAuthorId() {
        return authorId;
    }

    public void setAuthorId(UUID authorId) {
        this.authorId = authorId;
    }

    public UUID getQuotedPost() {
        return quotedPost;
    }

    public void setQuotedPost(UUID quotedPost) {
        this.quotedPost = quotedPost;
    }

    public UUID getParentPost() {
        return parentPost;
    }

    public void setParentPost(UUID parentPost) {
        this.parentPost = parentPost;
    }
}
