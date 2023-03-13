package ml.echelon133.microblog.shared.user;

import java.util.UUID;

public class UserDto {
    private UUID id;
    private String username;
    private String displayedName;
    private String aviUrl;
    private String description;

    public UserDto() {}
    public UserDto(UUID id, String username, String displayedName, String aviUrl, String description) {
        this.id = id;
        this.username = username;
        this.displayedName = displayedName;
        this.aviUrl = aviUrl;
        this.description = description;
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
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

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }
}
