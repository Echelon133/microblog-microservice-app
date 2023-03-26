package ml.echelon133.microblog.shared.post.tag;

import java.util.UUID;

public class TagDto {

    private UUID id;
    private String name;

    public TagDto() {}
    public TagDto(UUID id, String name) {
        this.id = id;
        this.name = name;
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
}
