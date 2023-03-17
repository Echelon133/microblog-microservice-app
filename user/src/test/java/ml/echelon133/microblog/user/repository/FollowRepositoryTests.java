package ml.echelon133.microblog.user.repository;

import ml.echelon133.microblog.shared.user.Follow;
import ml.echelon133.microblog.shared.user.User;
import ml.echelon133.microblog.shared.user.UserDto;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.test.context.TestPropertySource;

import java.util.*;
import java.util.stream.Collectors;
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

    @Autowired
    private UserRepository userRepository;

    private Map<String, UUID> usersIds = Map.of(
            "user1", UUID.fromString("bff692a7-2c03-44b4-ad0d-d54d28c91888"),
            "user2", UUID.fromString("baa062bf-01f7-4cd3-983b-642595370b40"),
            "user3", UUID.fromString("e410cd4b-8c85-4426-98ef-92f80c318de3"),
            "user4", UUID.fromString("36b78e45-407a-4fbd-beaa-2a58f8405aea"),
            "user5", UUID.fromString("488384f1-9585-4b17-9f6d-79ac627d241b"),
            "user6", UUID.fromString("2a07a74f-dc25-4025-800c-b9f77eb2c769")
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
        // create actual Users
        usersIds.forEach((username, uuid) -> {
            var user = new User(username, "", "", "", Set.of());
            user.setId(uuid); // make sure we have control over the id of the test user
            userRepository.save(user);
        });

        // make all users follow themselves
        usersIds.forEach((k, v) -> {
            follow(v, v);
        });

        // user1 follows user2, user3, user4, user5
        makeUserFollow("user1", Stream.of("user2", "user3", "user4", "user5"));
        // user2 follows user3, user4
        makeUserFollow("user2", Stream.of("user3", "user4"));
        // user3 follows user5
        makeUserFollow("user3", Stream.of("user5"));
        // user4 follows user5
        makeUserFollow("user4", Stream.of("user5"));
        // user5 does not follow anybody
        // user6 follows user5
        makeUserFollow("user6", Stream.of("user5"));

        // this means that:
        // * user1 (following: 4, followers: 0)
        // * user2 (following: 2, followers: 1)
        // * user3 (following: 1, followers: 2)
        // * user4 (following: 1, followers: 2)
        // * user5 (following: 0, followers: 3)
        // * user6 (following: 1, followers: 0)
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
        setupFollowRelationships();

        var expectedFollowing = Map.of(
                "user1", 4L,
                "user2", 2L,
                "user3", 1L,
                "user4", 1L,
                "user5", 0L,
                "user6", 1L
        );

        expectedFollowing.forEach((username, expectedFollowingCounter) -> {
            // when
            var userFollowing = followRepository.countUserFollowing(usersIds.get(username));
            // then
            assertEquals(expectedFollowingCounter, userFollowing);
        });
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
        setupFollowRelationships();

        var expectedFollowers = Map.of(
                "user1", 0L,
                "user2", 1L,
                "user3", 2L,
                "user4", 2L,
                "user5", 4L,
                "user6", 0L
        );

        expectedFollowers.forEach((username, expectedFollowersCounter) -> {
            // when
            var userFollowing = followRepository.countUserFollowers(usersIds.get(username));
            // then
            assertEquals(expectedFollowersCounter, userFollowing);
        });
    }

    @Test
    @DisplayName("Custom findAllFollowing returns correct pages")
    public void findAllFollowing_SetupFollows_ReturnsCorrectPages() {
        setupFollowRelationships();

        Pageable pageable = Pageable.ofSize(10);

        Map<String, List<String>> expectedFollowing = Map.of(
                "user1", List.of("user2", "user3", "user4", "user5"),
                "user2", List.of("user3", "user4"),
                "user3", List.of("user5"),
                "user4", List.of("user5"),
                "user5", List.of(),
                "user6", List.of("user5")
        );

        expectedFollowing.forEach((username, expectedUsers) -> {
            // when
            var userFollowing = followRepository.findAllUserFollowing(
                    usersIds.get(username), pageable
            );
            // then
            assertPageContainsExpectedUsernames(expectedUsers, userFollowing);
            assertEquals(expectedUsers.size(), userFollowing.getTotalElements());
        });
    }

    @Test
    @DisplayName("Custom findAllFollowers returns correct pages")
    public void findAllFollowers_SetupFollows_ReturnsCorrectPages() {
        setupFollowRelationships();

        Pageable pageable = Pageable.ofSize(10);

        Map<String, List<String>> expectedFollowers = Map.of(
                "user1", List.of(),
                "user2", List.of("user1"),
                "user3", List.of("user1", "user2"),
                "user4", List.of("user1", "user2"),
                "user5", List.of("user1", "user3", "user4", "user6"),
                "user6", List.of()
        );

        expectedFollowers.forEach((username, expectedUsers) -> {
            // when
            var userFollowers = followRepository.findAllUserFollowers(
                    usersIds.get(username), pageable
            );
            // then
            assertPageContainsExpectedUsernames(expectedUsers, userFollowers);
            assertEquals(expectedUsers.size(), userFollowers.getTotalElements());
        });
    }

    @Test
    @DisplayName("Custom findAllKnownUserFollowers returns correct pages")
    public void findAllKnownUserFollowers_TargetUser5_ReturnsCorrectPages() {
        // user5 is being followed by (user1, user3, user4, user6)
        // user1 is following (user2, user3, user4, user5)
        // of all followers of user5, user1 knows (user3, user4, and themselves),
        // but the user should not appear on the list themselves, so the final
        // list of expected users should be (user3, user4)
        setupFollowRelationships();

        Pageable pageable = Pageable.ofSize(10);

        var sourceUser = usersIds.get("user1");
        var targetUser = usersIds.get("user5");
        var expectedKnownUsers = List.of("user3", "user4");

        // when
        var knownUsers = followRepository.findAllKnownUserFollowers(sourceUser, targetUser, pageable);

        // then
        assertPageContainsExpectedUsernames(expectedKnownUsers, knownUsers);
        assertEquals(expectedKnownUsers.size(), knownUsers.getTotalElements());
    }

    private static void assertPageContainsExpectedUsernames(List<String> expectedUsernames, Page<UserDto> foundUsers) {
        var foundUsernames = foundUsers.stream().map(
                UserDto::getUsername
        ).sorted().collect(Collectors.toList());
        assertEquals(expectedUsernames, foundUsernames);
    }
}
