package ml.echelon133.microblog.post.repository;

import ml.echelon133.microblog.shared.post.Post;
import ml.echelon133.microblog.shared.post.PostDto;
import ml.echelon133.microblog.shared.post.tag.Tag;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.data.domain.Pageable;
import org.springframework.test.context.TestPropertySource;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

/*
    Disable kubernetes during tests to make local execution of tests possible.
    If kubernetes is not disabled, tests won't execute at all because Spring will
    fail to configure kubernetes when run outside it.
 */
@TestPropertySource(properties = "spring.cloud.kubernetes.enabled=false")
@DataJpaTest
@DisplayName("Tests of TagRepository")
public class TagRepositoryTests {

    @Autowired
    private PostRepository postRepository;

    @Autowired
    private TagRepository tagRepository;

    private Post createTaggedPostOnDate(Set<String> tags, Date dateCreated) {
        var foundTags = new HashSet<Tag>();
        tags.forEach(tagName -> {
            tagRepository.findByName(tagName).ifPresentOrElse(
                    // tag present in the database
                    foundTags::add,
                    // tag not present in the database
                    () -> {
                        var tag = tagRepository.save(new Tag(tagName));
                        foundTags.add(tag);
                    }
            );
        });

        // only post's date of creation and its tags matter for these tests
        var post = new Post(UUID.randomUUID(), "some content", foundTags);

        // setting 'dateCreated' before initial saving will result in that date being overwritten
        // because of @DateCreated annotation that is modifying that field
        var savedPost = postRepository.save(post);
        // update the date after initial saving
        savedPost.setDateCreated(dateCreated);
        return postRepository.save(savedPost);
    }

    @Test
    @DisplayName("Custom findPopularTags returns an empty page when not a single post is tagged")
    public void findPopularTags_NoTaggedPosts_ReturnsEmpty() {
        // given
        var oneMinuteAgo = Date.from(Instant.now().minus(1, ChronoUnit.MINUTES));
        Set<String> noTags = Set.of();
        createTaggedPostOnDate(noTags, oneMinuteAgo);
        createTaggedPostOnDate(noTags, oneMinuteAgo);

        // when
        var pageable = Pageable.ofSize(5);
        var sixHoursAgo = Date.from(Instant.now().minus(6, ChronoUnit.HOURS));
        var now = new Date();
        var result = tagRepository.findPopularTags(sixHoursAgo, now, pageable);

        // then
        assertEquals(0, result.getTotalElements());
    }

    @Test
    @DisplayName("Custom findPopularTags returns an empty page when all tagged posts are deleted")
    public void findPopularTags_AllTaggedPostsDeleted_ReturnsEmpty() {
        // given
        var oneMinuteAgo = Date.from(Instant.now().minus(1, ChronoUnit.MINUTES));
        createTaggedPostOnDate(Set.of("test123"), oneMinuteAgo);
        createTaggedPostOnDate(Set.of("test321"), oneMinuteAgo);

        // mark all created posts as deleted
        postRepository.findAll().forEach(post -> {
            post.setDeleted(true);
            postRepository.save(post);
        });

        // when
        var pageable = Pageable.ofSize(5);
        var sixHoursAgo = Date.from(Instant.now().minus(6, ChronoUnit.HOURS));
        var now = new Date();
        var result = tagRepository.findPopularTags(sixHoursAgo, now, pageable);

        // then
        assertEquals(0, result.getTotalElements());
    }

    @Test
    @DisplayName("Custom findPopularTags returns page with tags in correct, descending popularity order")
    public void findPopularTags_MultipleTaggedPosts_ReturnsElementsInCorrectOrder() {
        // given
        var oneMinuteAgo = Date.from(Instant.now().minus(1, ChronoUnit.MINUTES));
        var tag1 = "test1";
        var tag2 = "test2";
        var tag3 = "test3";
        var tag4 = "test4";
        var tagGroup1 = Set.of(tag1);
        var tagGroup2 = Set.of(tag1, tag2);
        var tagGroup3 = Set.of(tag1, tag2, tag3);
        var tagGroup4 = Set.of(tag1, tag2, tag3, tag4);

        // create 10 posts for each tag group, so that:
        // tag1 is used 40 times
        // tag2 is used 30 times
        // tag3 is used 20 times
        // tag4 is used 10 times
        for (int i = 0; i < 10; i++) {
            createTaggedPostOnDate(tagGroup1, oneMinuteAgo);
            createTaggedPostOnDate(tagGroup2, oneMinuteAgo);
            createTaggedPostOnDate(tagGroup3, oneMinuteAgo);
            createTaggedPostOnDate(tagGroup4, oneMinuteAgo);
        }

        // when
        var pageable = Pageable.ofSize(5);
        var sixHoursAgo = Date.from(Instant.now().minus(6, ChronoUnit.HOURS));
        var now = new Date();
        var result = tagRepository.findPopularTags(sixHoursAgo, now, pageable);

        // then
        assertEquals(4, result.getTotalElements());
        var content = result.getContent();
        assertEquals(tag1, content.get(0).getName());
        assertEquals(tag2, content.get(1).getName());
        assertEquals(tag3, content.get(2).getName());
        assertEquals(tag4, content.get(3).getName());
    }

    @Test
    @DisplayName("Custom findPopularTags returns page with only as many tags as specified in pageable")
    public void findPopularTags_MultipleTaggedPosts_ReturnsNoMoreThanSpecified() {
        // given
        var oneMinuteAgo = Date.from(Instant.now().minus(1, ChronoUnit.MINUTES));
        var tag1 = "test1";
        var tag2 = "test2";
        var tagGroup1 = Set.of(tag1);
        var tagGroup2 = Set.of(tag1, tag2);

        // create 10 posts for each tag group, so that:
        // tag1 is used 20 times
        // tag2 is used 10 times
        for (int i = 0; i < 10; i++) {
            createTaggedPostOnDate(tagGroup1, oneMinuteAgo);
            createTaggedPostOnDate(tagGroup2, oneMinuteAgo);
        }

        // when
        var pageable = Pageable.ofSize(1);
        var sixHoursAgo = Date.from(Instant.now().minus(6, ChronoUnit.HOURS));
        var now = new Date();
        var result = tagRepository.findPopularTags(sixHoursAgo, now, pageable);

        // then
        assertEquals(1, result.getNumberOfElements());
        var content = result.getContent();
        assertEquals(tag1, content.get(0).getName());
    }

    @Test
    @DisplayName("Custom findPopularTags calculates tag popularity only using posts created between specified dates")
    public void findPopularTags_MultipleTaggedPosts_IgnoresPostsOutsideSpecifiedPeriod() {
        // given
        var tag1 = "test1";
        var tag2 = "test2";
        var tag3 = "test3";
        var tag4 = "test4";
        var tag5 = "test5";
        var tag6 = "test6";
        var tagGroup1 = Set.of(tag1);
        var tagGroup2 = Set.of(tag5);
        var tagGroup3 = Set.of(tag5, tag6);
        var tagGroup4 = Set.of(tag1, tag2, tag3, tag4);

        // create 5 posts for each tag group where all posts with:
        // * tagGroup1 were made 6 hours and 1 minute ago
        // * tagGroup2 were made 5 hours and 59 minutes ago
        // * tagGroup3 were made 1 hour and 1 minute ago
        // * tagGroup4 were made 59 minutes ago
        //
        // tagGroup1 | tagGroup2 | tagGroup3 | tagGroup4
        // 6h01m ago | 5h59m ago | 1h01m ago | 59m ago
        // XXXXXXXXXX|           |           |XXXXXXXXXX
        //
        // tested time period is between 6 hours ago and 1 hour ago, which should result in
        // only posts tagged with tagGroup2 and tagGroup3 being taken into account while calculating
        // tag popularity, so the expected result should be:
        // * tag5 being used 10 times (5 times * two groups)
        // * tag6 being used 5 times (5 times * one group)
        // * other tags being completely ignored, because the posts which use them are not from the specified time period
        //
        var sixHoursAgo = Instant.now().minus(6, ChronoUnit.HOURS);
        var oneHourAgo = Instant.now().minus(1, ChronoUnit.HOURS);
        for (int i = 0; i < 5; i++) {
            createTaggedPostOnDate(tagGroup1, Date.from(sixHoursAgo.minus(1, ChronoUnit.MINUTES)));
            createTaggedPostOnDate(tagGroup2, Date.from(sixHoursAgo.plus(1, ChronoUnit.MINUTES)));
            createTaggedPostOnDate(tagGroup3, Date.from(oneHourAgo.minus(1, ChronoUnit.MINUTES)));
            createTaggedPostOnDate(tagGroup4, Date.from(oneHourAgo.plus(1, ChronoUnit.MINUTES)));
        }

        // when
        var pageable = Pageable.ofSize(5);
        var result = tagRepository.findPopularTags(Date.from(sixHoursAgo), Date.from(oneHourAgo), pageable);

        // then
        assertEquals(2, result.getNumberOfElements());
        var content = result.getContent();
        // only tag5 and tag6 had been posted in the time period which is given to the query
        assertEquals(tag5, content.get(0).getName());
        assertEquals(tag6, content.get(1).getName());
    }

    @Test
    @DisplayName("Custom findMostRecentPostsTagged returns an empty page when not a single post is tagged")
    public void findMostRecentPostsTagged_NoTaggedPosts_ReturnsEmpty() {
        // given
        var now = new Date();
        Set<String> noTags = Set.of();
        createTaggedPostOnDate(noTags, now);
        createTaggedPostOnDate(noTags, now);

        // when
        var pageable = Pageable.ofSize(5);
        var result = tagRepository.findMostRecentPostsTagged("test", pageable);

        // then
        assertEquals(0, result.getTotalElements());
    }

    @Test
    @DisplayName("Custom findMostRecentPostsTagged returns an empty page when all tagged posts are deleted")
    public void findMostRecentPostsTagged_AllTaggedPostsDeleted_ReturnsEmpty() {
        // given
        var tag = "test";
        var now = new Date();
        createTaggedPostOnDate(Set.of(tag), now);
        createTaggedPostOnDate(Set.of(tag), now);

        // mark all created posts as deleted
        postRepository.findAll().forEach(post -> {
            post.setDeleted(true);
            postRepository.save(post);
        });

        // when
        var pageable = Pageable.ofSize(5);
        var result = tagRepository.findMostRecentPostsTagged(tag, pageable);

        // then
        assertEquals(0, result.getTotalElements());
    }

    @Test
    @DisplayName("Custom findMostRecentPostsTagged returns page with posts in correct, descending recency order")
    public void findMostRecentPostsTagged_MultipleTaggedPosts_ReturnsElementsInCorrectOrder() {
        // given
        var tag = "test";

        // create 5 posts, each being posted one hour earlier than the last one
        List<Post> expectedPostOrder = new ArrayList<>();
        for (int hour = 0; hour < 5; hour++) {
            var post = createTaggedPostOnDate(Set.of(tag),
                    Date.from(Instant.now().minus(hour, ChronoUnit.HOURS)));
            expectedPostOrder.add(post);
        }

        // when
        var pageable = Pageable.ofSize(5);
        var result = tagRepository.findMostRecentPostsTagged(tag, pageable);

        // then
        assertEquals(5, result.getTotalElements());
        var content = result.getContent();
        assertEquals(expectedPostOrder.get(0).getId(), content.get(0).getId());
        assertEquals(expectedPostOrder.get(1).getId(), content.get(1).getId());
        assertEquals(expectedPostOrder.get(2).getId(), content.get(2).getId());
        assertEquals(expectedPostOrder.get(3).getId(), content.get(3).getId());
        assertEquals(expectedPostOrder.get(4).getId(), content.get(4).getId());
    }

    @Test
    @DisplayName("Custom findMostRecentPostsTagged returns page with posts only tagged by one tag")
    public void findMostRecentPostsTagged_MultipleTaggedPosts_ReturnsOnlyFromExactTag() {
        // given
        var tag = "test";
        var tag1 = "test1";
        var tag2 = "1test";

        // create 5 posts tagged by tag (the most recent post being from one hour ago)
        List<UUID> expectedPostIds = new ArrayList<>();
        for (int hour = 0; hour < 5; hour++) {
            var post = createTaggedPostOnDate(Set.of(tag), Date.from(Instant.now().minus(hour, ChronoUnit.HOURS)));
            expectedPostIds.add(post.getId());
        }
        // create 5 posts tagged by tag1 (all posted now)
        for (int i = 0; i < 5; i++) {
            createTaggedPostOnDate(Set.of(tag1), new Date());
        }
        // create 5 posts tagged by tag2 (all posted now)
        for (int i = 0; i < 5; i++) {
            createTaggedPostOnDate(Set.of(tag2), new Date());
        }

        // when
        var pageable = Pageable.ofSize(5);
        var result = tagRepository.findMostRecentPostsTagged(tag, pageable);

        // then
        assertEquals(5, result.getTotalElements());
        var foundPostIds = result.getContent().stream().map(PostDto::getId).sorted().toList();
        assertTrue(expectedPostIds.containsAll(foundPostIds));
    }
}
