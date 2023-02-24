package ml.echelon133.microblog.shared.base;

import org.hibernate.Hibernate;
import org.springframework.data.annotation.CreatedDate;

import javax.persistence.*;
import javax.validation.constraints.NotNull;
import java.io.Serializable;
import java.util.Date;
import java.util.Objects;
import java.util.UUID;

@MappedSuperclass
public abstract class BaseEntity implements Serializable {

    /*
    Create a new UUID every time an entity is instantiated to make equals/hashCode
    implementation easier. If entities never have null IDs then their IDs can be used
    to compare them. There is no setter for IDs, because entities won't be:
        * directly read from the database
        * mapped from DTOs

    Any modifications of existing entities must be done using custom queries.
     */
    @Id
    private final UUID id = UUID.randomUUID();

    @Version
    @NotNull
    private Long version;

    @CreatedDate
    private Date dateCreated;

    public UUID getId() {
        return id;
    }

    public Long getVersion() {
        return version;
    }

    public void setVersion(Long version) {
        this.version = version;
    }

    public Date getDateCreated() {
        return dateCreated;
    }

    public void setDateCreated(Date dateCreated) {
        this.dateCreated = dateCreated;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || Hibernate.getClass(this) != Hibernate.getClass(o)) return false;
        BaseEntity other = (BaseEntity)o;
        return Objects.equals(id, other.id);
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }
}
