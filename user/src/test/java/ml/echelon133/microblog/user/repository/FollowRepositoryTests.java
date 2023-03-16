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

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
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

    @Test
    @DisplayName("Custom findAllFollowing returns correct pages")
    public void findAllFollowing_SetupFollows_ReturnsCorrectPages() {
        // setup
        // * user1 to have following: 2 (user2, user3)
        // * user2 to have following: 2 (user3, user4)
        // * user3 to have following: 0
        // * user4 to have following: 0
        setupFollowRelationships();

        Pageable pageable = Pageable.ofSize(2);

        // when
        var user1Following = followRepository.findAllUserFollowing(
                usersIds.get("user1"), pageable
        );
        var user2Following = followRepository.findAllUserFollowing(
                usersIds.get("user2"), pageable
        );
        var user3Following = followRepository.findAllUserFollowing(
                usersIds.get("user3"), pageable
        );
        var user4Following = followRepository.findAllUserFollowing(
                usersIds.get("user4"), pageable
        );

        // then
        assertPageContainsExpectedUsernames(List.of("user2", "user3"), user1Following);
        assertEquals(2, user1Following.getTotalElements());

        assertPageContainsExpectedUsernames(List.of("user3", "user4"), user2Following);
        assertEquals(2, user2Following.getTotalElements());

        assertEquals(0, user3Following.getTotalElements());
        assertEquals(0, user4Following.getTotalElements());
    }

    @Test
    @DisplayName("Custom findAllFollowers returns correct pages")
    public void findAllFollowers_SetupFollows_ReturnsCorrectPages() {
        // setup
        // * user1 to have followers: 0
        // * user2 to have followers: 1 (user1)
        // * user3 to have followers: 2 (user1, user2)
        // * user4 to have followers: 1 (user2)
        setupFollowRelationships();

        Pageable pageable = Pageable.ofSize(2);

        // when
        var user1Followers = followRepository.findAllUserFollowers(
                usersIds.get("user1"), pageable
        );
        var user2Followers = followRepository.findAllUserFollowers(
                usersIds.get("user2"), pageable
        );
        var user3Followers = followRepository.findAllUserFollowers(
                usersIds.get("user3"), pageable
        );
        var user4Followers = followRepository.findAllUserFollowers(
                usersIds.get("user4"), pageable
        );

        // then
        assertEquals(0, user1Followers.getTotalElements());

        assertPageContainsExpectedUsernames(List.of("user1"), user2Followers);
        assertEquals(1, user2Followers.getTotalElements());

        assertPageContainsExpectedUsernames(List.of("user1", "user2"), user3Followers);
        assertEquals(2, user3Followers.getTotalElements());

        assertPageContainsExpectedUsernames(List.of("user2"), user4Followers);
        assertEquals(1, user4Followers.getTotalElements());
    }

    private static void assertPageContainsExpectedUsernames(List<String> expectedUsernames, Page<UserDto> foundUsers) {
        var foundUsernames = foundUsers.stream().map(
                UserDto::getUsername
        ).sorted().collect(Collectors.toList());
        assertEquals(expectedUsernames, foundUsernames);
    }
}
