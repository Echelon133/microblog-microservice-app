package ml.echelon133.microblog.post.service;

import ml.echelon133.microblog.post.exception.TagNotFoundException;
import ml.echelon133.microblog.post.repository.TagRepository;
import ml.echelon133.microblog.shared.post.tag.Tag;
import ml.echelon133.microblog.shared.post.tag.TagDto;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.List;
import java.util.regex.Pattern;

@Service
public class TagService {

    public static Pattern HASHTAG_PATTERN = Pattern.compile("#([a-zA-Z0-9]{2,50})");

    private TagRepository tagRepository;
    private Clock clock;

    @Autowired
    public TagService(TagRepository tagRepository, Clock clock) {
        this.tagRepository = tagRepository;
        this.clock = clock;
    }

    /**
     * Finds a {@link Tag} by name.
     * @param name unique name of the tag
     * @return {@link Tag} object if found
     * @throws TagNotFoundException thrown when {@link Tag} with specified name was not found
     */
    public Tag findByName(String name) throws TagNotFoundException {
        return tagRepository.findByName(name).orElseThrow(() -> new TagNotFoundException(name));
    }

    /**
     * Finds at most five most popular tags in the (now minus {@code hours}) to (now) time period.
     * @param hours how many hours back should the query go at most during calculation of the most popular tags
     * @return a list of five tags which have been the most popular in the (now-{@code hours}) to (now) time period
     * @throws IllegalArgumentException when {@code hours} is not in 1-24 range
     */
    public List<TagDto> findFiveMostPopularInLast(Integer hours) throws IllegalArgumentException {
        if (hours > 24 || hours <= 0) {
            throw new IllegalArgumentException("hours values not in 1-24 range are not valid");
        }

        return tagRepository.findPopularTags(
                Date.from(Instant.now(clock).minus(hours, ChronoUnit.HOURS)),
                Date.from(Instant.now(clock)),
                Pageable.ofSize(5)).getContent();
    }
}
