package ml.echelon133.microblog.shared.user;

import ml.echelon133.microblog.shared.base.BaseEntity;
import org.springframework.security.core.GrantedAuthority;

import javax.persistence.Entity;

@Entity
public class Role extends BaseEntity implements GrantedAuthority {

    private String name;

    public Role() {}
    public Role(String name) {
        this.name = name;
    }

    @Override
    public String getAuthority() {
        return name;
    }

    public void setAuthority(String name) {
        this.name = name;
    }
}
