package ml.echelon133.microblog.shared.user;

import java.util.UUID;

public class UserDto {
    private UUID id;
    private String name;
    private String displayedName;
    private String aviUrl;

    public UserDto() {}

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

    public String getDisplayedName() {
        return displayedName;
    }

    public void setDisplayedName(String displayedName) {
        this.displayedName = displayedName;
    }

    public String getAviUrl() {
        return aviUrl;
    }

    public void setAviUrl(String aviUrl) {
        this.aviUrl = aviUrl;
    }
}
