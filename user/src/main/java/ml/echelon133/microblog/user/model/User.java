package ml.echelon133.microblog.user.model;

import ml.echelon133.microblog.shared.base.BaseEntity;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import javax.persistence.*;
import java.util.Collection;
import java.util.Set;

@Entity(name = "MBlog_User")
@Table(
        name = "mblog_users",
        indexes = @Index(name = "username_index", columnList = "username", unique = true)
)
public class User extends BaseEntity implements UserDetails {

    // max length of the username is 30 characters - specified in the
    // ml.echelon133.microblog.shared.user.validator.UsernameValidator
    @Column(unique = true, updatable = false, nullable = false, length = 30)
    private String username;

    @Column(length = 40)
    private String displayedName;

    @Column(length = 1000)
    private String aviURL;

    @Column(updatable = false, nullable = false, length = 1000)
    private String email;

    @Column(length = 1000)
    private String description;

    // max length should fit the output of BCryptPasswordEncoder
    @Column(length = 60)
    private String password;

    @ManyToMany
    @JoinTable(
            name = "user_roles",
            joinColumns = { @JoinColumn(name = "id")},
            inverseJoinColumns = { @JoinColumn(name = "role_id") }
    )
    private Set<Role> roles;

    public User() {}
    public User(String username, String email, String password, String aviURL, Set<Role> roles) {
        this.username = username;
        this.email = email;
        this.password = password;
        this.aviURL = aviURL;
        this.displayedName = username; // the new user's default displayedName should be their username
        this.description = "";
        this.roles = roles;
    }

    public String getDisplayedName() {
        return displayedName;
    }

    public String getAviURL() {
        return aviURL;
    }

    public String getEmail() {
        return email;
    }

    public String getDescription() {
        return description;
    }

    public Set<Role> getRoles() {
        return roles;
    }

    private void setUsername(String username) {
        this.username = username;
    }

    private void setDisplayedName(String displayedName) {
        this.displayedName = displayedName;
    }

    private void setAviURL(String aviURL) {
        this.aviURL = aviURL;
    }

    private void setEmail(String email) {
        this.email = email;
    }

    private void setDescription(String description) {
        this.description = description;
    }

    private void setPassword(String password) {
        this.password = password;
    }

    private void setRoles(Set<Role> roles) {
        this.roles = roles;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return roles;
    }

    @Override
    public String getPassword() {
        return password;
    }

    @Override
    public String getUsername() {
        return username;
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        // enable by default, because there is no email verification in the development version
        return true;
    }
}
