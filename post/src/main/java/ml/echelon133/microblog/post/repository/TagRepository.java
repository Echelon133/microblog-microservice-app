package ml.echelon133.microblog.post.repository;

import ml.echelon133.microblog.shared.post.tag.Tag;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface TagRepository extends JpaRepository<Tag, UUID> {
    Optional<Tag> findByName(String name);
}
