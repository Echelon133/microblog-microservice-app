package ml.echelon133.microblog.shared.user;

import ml.echelon133.microblog.shared.user.validator.PasswordsMatch;
import ml.echelon133.microblog.shared.user.validator.ValidPassword;
import ml.echelon133.microblog.shared.user.validator.ValidUsername;
import org.hibernate.validator.constraints.Length;

import javax.validation.constraints.Email;
import javax.validation.constraints.NotEmpty;

@PasswordsMatch
public class UserCreationDto {

    @ValidUsername
    private String username;

    @NotEmpty(message = "Email is required")
    @Email(message = "Email is not valid")
    private String email;

    @ValidPassword
    private String password;
    private String password2;

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getPassword2() {
        return password2;
    }

    public void setPassword2(String password2) {
        this.password2 = password2;
    }
}
