package ml.echelon133.microblog.shared.post.tag;

import ml.echelon133.microblog.shared.base.BaseEntity;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Index;
import javax.persistence.Table;

@Entity
@Table(indexes = @Index(columnList = "name", unique = true))
public class Tag extends BaseEntity {

    @Column(unique = true, nullable = false, updatable = false, length = 50)
    private String name;

    public Tag() {}
    public Tag(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
}
