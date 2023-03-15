package ml.echelon133.microblog.user.repository;

import ml.echelon133.microblog.shared.user.Follow;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.TestPropertySource;

import java.util.Map;
import java.util.UUID;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;

/*
    Disable kubernetes during tests to make local execution of tests possible.
    If kubernetes is not disabled, tests won't execute at all because Spring will
    fail to configure kubernetes when run outside it.
 */
@TestPropertySource(properties = "spring.cloud.kubernetes.enabled=false")
@DataJpaTest
@DisplayName("Tests of FollowRepository")
public class FollowRepositoryTests {

    @Autowired
    private FollowRepository followRepository;

    private Map<String, UUID> usersIds = Map.of(
            "user1", UUID.fromString("bff692a7-2c03-44b4-ad0d-d54d28c91888"),
            "user2", UUID.fromString("baa062bf-01f7-4cd3-983b-642595370b40"),
            "user3", UUID.fromString("e410cd4b-8c85-4426-98ef-92f80c318de3"),
            "user4", UUID.fromString("36b78e45-407a-4fbd-beaa-2a58f8405aea")
    );

    private void follow(UUID source, UUID target) {
        followRepository.saveAndFlush(new Follow(source, target));
    }

    private void makeUserFollow(String source, Stream<String> targets) {
        var sourceId = usersIds.get(source);

        targets.forEach(t -> {
            var targetId = usersIds.get(t);
            follow(sourceId, targetId);
        });
    }

    private void setupFollowRelationships() {
        // make all users follow themselves
        usersIds.forEach((k, v) -> {
            follow(v, v);
        });

        // user1 follows user2, user3
        makeUserFollow("user1", Stream.of("user2", "user3"));
        // user2 follows user3, user4
        makeUserFollow("user2", Stream.of("user3", "user4"));
    }

    @Test
    @DisplayName("Custom countUserFollowing returns 0 for id with no follow relationships")
    public void countUserFollowing_IdWithNoFollows_ReturnsZero() {
        var id = UUID.randomUUID();

        // when
        var counter = followRepository.countUserFollowing(id);

        // then
        assertEquals(0, counter);
    }

    @Test
    @DisplayName("Custom countUserFollowing returns correct counts")
    public void countUserFollowing_IdWithFollows_ReturnsCorrectCount() {
        // setup
        // * user1 to have following: 2
        // * user2 to have following: 2
        // * user3 to have following: 0
        // * user4 to have following: 0
        setupFollowRelationships();

        // when
        var user1Following = followRepository.countUserFollowing(
                usersIds.get("user1")
        );
        var user2Following = followRepository.countUserFollowing(
                usersIds.get("user2")
        );
        var user3Following = followRepository.countUserFollowing(
                usersIds.get("user3")
        );
        var user4Following = followRepository.countUserFollowing(
                usersIds.get("user4")
        );

        // then
        assertEquals(2, user1Following);
        assertEquals(2, user2Following);
        assertEquals(0, user3Following);
        assertEquals(0, user4Following);
    }

    @Test
    @DisplayName("Custom countUserFollowers returns 0 for id with no follow relationships")
    public void countUserFollowers_IdWithNoFollows_ReturnsZero() {
        var id = UUID.randomUUID();

        // when
        var counter = followRepository.countUserFollowers(id);

        // then
        assertEquals(0, counter);
    }

    @Test
    @DisplayName("Custom countUserFollowers returns correct counts")
    public void countUserFollowers_IdWithFollows_ReturnsCorrectCount() {
        // setup
        // * user1 to have followers: 0
        // * user2 to have followers: 1
        // * user3 to have followers: 2
        // * user4 to have followers: 1
        setupFollowRelationships();

        // when
        var user1Followers = followRepository.countUserFollowers(
                usersIds.get("user1")
        );
        var user2Followers = followRepository.countUserFollowers(
                usersIds.get("user2")
        );
        var user3Followers = followRepository.countUserFollowers(
                usersIds.get("user3")
        );
        var user4Followers = followRepository.countUserFollowers(
                usersIds.get("user4")
        );

        // then
        assertEquals(0, user1Followers);
        assertEquals(1, user2Followers);
        assertEquals(2, user3Followers);
        assertEquals(1, user4Followers);
    }
}
