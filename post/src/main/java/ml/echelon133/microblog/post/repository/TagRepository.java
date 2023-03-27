package ml.echelon133.microblog.post.repository;

import ml.echelon133.microblog.shared.post.tag.Tag;
import ml.echelon133.microblog.shared.post.tag.TagDto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.Date;
import java.util.Optional;
import java.util.UUID;

public interface TagRepository extends JpaRepository<Tag, UUID> {
    Optional<Tag> findByName(String name);

    /**
     * Finds a {@link Page} of tags which had been the most popular in the period between the {@code start} and {@code end}
     * date.
     *
     * @param start date which represents the start of the tag popularity evaluation period
     * @param end date which represents the end of the tag popularity evaluation period
     * @param pageable all information about the wanted page
     * @return a page of tags sorted from the most popular to the least popular in the specified time period
     */
    @Query("SELECT NEW ml.echelon133.microblog.shared.post.tag.TagDto(t.id, t.name) " +
            "FROM Post p JOIN p.tags t WHERE p.dateCreated BETWEEN ?1 AND ?2 GROUP BY t.name ORDER BY COUNT(p.id) DESC")
    Page<TagDto> findPopularTags(Date start, Date end, Pageable pageable);
}
