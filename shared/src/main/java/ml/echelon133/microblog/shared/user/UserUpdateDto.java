package ml.echelon133.microblog.shared.user;

import org.hibernate.validator.constraints.Length;

public class UserUpdateDto {

    @Length(max = 40, message = "displayedName valid length between 0 and 40 characters")
    private String displayedName;

    @Length(max = 200, message = "aviUrl valid length between 0 and 200 characters")
    private String aviUrl;

    @Length(max = 300, message = "description valid length between 0 and 300 characters")
    private String description;

    public UserUpdateDto() {}
    public UserUpdateDto(String displayedName, String aviUrl, String description) {
        this.displayedName = displayedName;
        this.aviUrl = aviUrl;
        this.description = description;
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
