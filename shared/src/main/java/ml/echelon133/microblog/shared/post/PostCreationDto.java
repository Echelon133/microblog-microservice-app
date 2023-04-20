package ml.echelon133.microblog.shared.post;

import org.hibernate.validator.constraints.Length;

import javax.validation.constraints.NotNull;

public class PostCreationDto {

    @Length(min = 1, max = 300, message = "content's valid length between 1 and 300 characters")
    @NotNull(message = "Post content not provided")
    private String content;

    public PostCreationDto() {}
    public PostCreationDto(String content) {
        this.content = content;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }
}
