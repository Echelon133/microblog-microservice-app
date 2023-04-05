package ml.echelon133.microblog.post.service;

import ml.echelon133.microblog.post.exception.PostDeletionForbiddenException;
import ml.echelon133.microblog.post.exception.PostNotFoundException;
import ml.echelon133.microblog.post.exception.TagNotFoundException;
import ml.echelon133.microblog.post.queue.NotificationPublisher;
import ml.echelon133.microblog.post.repository.LikeRepository;
import ml.echelon133.microblog.post.repository.PostRepository;
import ml.echelon133.microblog.post.web.UserServiceClient;
import ml.echelon133.microblog.shared.notification.Notification;
import ml.echelon133.microblog.shared.notification.NotificationDto;
import ml.echelon133.microblog.shared.post.Post;
import ml.echelon133.microblog.shared.post.PostCountersDto;
import ml.echelon133.microblog.shared.post.PostCreationDto;
import ml.echelon133.microblog.shared.post.PostDto;
import ml.echelon133.microblog.shared.post.like.Like;
import ml.echelon133.microblog.shared.post.tag.Tag;
import ml.echelon133.microblog.shared.user.UserDto;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import javax.transaction.Transactional;
import java.time.Clock;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class PostService {

    private Pattern usernamePattern = Pattern.compile("@([A-Za-z0-9]{1,30})");
    private PostRepository postRepository;
    private LikeRepository likeRepository;
    private TagService tagService;
    private Clock clock;
    private NotificationPublisher notificationPublisher;
    private UserServiceClient userServiceClient;

    @Autowired
    public PostService(PostRepository postRepository,
                       LikeRepository likeRepository,
                       TagService tagService,
                       Clock clock,
                       NotificationPublisher notificationPublisher,
                       UserServiceClient userServiceClient) {
        this.postRepository = postRepository;
        this.likeRepository = likeRepository;
        this.tagService = tagService;
        this.clock = clock;
        this.notificationPublisher = notificationPublisher;
        this.userServiceClient = userServiceClient;
    }

    private void throwIfPostNotFound(UUID id) throws PostNotFoundException {
        if (!postRepository.existsPostByIdAndDeletedFalse(id)) {
            throw new PostNotFoundException(id);
        }
    }

    /**
     * Projects the post/quote/response with specified {@link java.util.UUID} into a DTO object.
     *
     * @param id id of the post/quote/response
     * @return DTO projection of the post
     * @throws PostNotFoundException thrown when the post does not exist or is marked as deleted
     */
    public PostDto findById(UUID id) throws PostNotFoundException {
        return postRepository.findByPostId(id).orElseThrow(() ->
                new PostNotFoundException(id)
        );
    }

    /**
     * Generates user's feed. Strategies of generation are different depending on the provided arguments.
     *
     * There are three possibilities:
     * <ul>
     *     <li>If {@code userId} is not empty and {@code popular} is set to true, feed will consist of the most popular
     *     posts made by users who are being followed by the user with {@code userId}.</li>
     *     <li>If {@code userId} is not empty and {@code popular} is set to false, feed will consist of the most recent
     *     posts made by users who are being followed by the user with {@code userId}.</li>
     *     <li>If {@code userId} is empty, feed will consist of the most popular posts (without any filtering
     *     based on post's author). Argument {@code popular} is ignored in this scenario.</li>
     * </ul>
     *
     * @param userId id of the user for whom the feed will be generated, leave empty if the user is anonymous
     * @param popular whether the posts on the feed should be selected by their popularity, when {@code false} it selects posts based on their recency
     * @param hours how many hours old should the oldest post on the feed be, posts which are older will not show up on the feed
     * @param pageable all information about the wanted page
     * @return a {@link Page} containing posts which together create user's feed
     * @throws IllegalArgumentException if {@code hours} value is not in 1-24 range
     */
    public Page<PostDto> generateFeed(Optional<UUID> userId, boolean popular, Integer hours, Pageable pageable) {
        if (hours > 24 || hours <= 0) {
            throw new IllegalArgumentException("hours values not in 1-24 range are not valid");
        }

        var start = Date.from(Instant.now(clock).minus(hours, ChronoUnit.HOURS));
        var end = Date.from(Instant.now(clock));

        Page<PostDto> page;
        if (userId.isPresent()) {
            if (popular) {
                page = postRepository.generateFeedWithMostPopularPostsForUser(userId.get(), start, end, pageable);
            } else {
                page = postRepository.generateFeedWithMostRecentPostsForUser(userId.get(), start, end, pageable);
            }
        } else {
            page = postRepository.generateFeedWithMostPopularPostsForAnonymous(start, end, pageable);
        }

        return page;
    }

    /**
     * Returns counters which show how many likes, quotes, and responses a post has.
     *
     * @param postId id of the post whose counters are being read
     * @return DTO containing counters of likes, quotes, and responses
     * @throws PostNotFoundException thrown when the post with specified id does not exist
     */
    @Transactional
    public PostCountersDto findPostCounters(UUID postId) throws PostNotFoundException {
        throwIfPostNotFound(postId);

        var likes = likeRepository.countByLikeIdLikedPostId(postId);
        var quotes = postRepository.countByQuotedPostIdAndDeletedFalse(postId);
        var responses = postRepository.countByParentPostIdAndDeletedFalse(postId);
        return new PostCountersDto(likes, quotes, responses);
    }

    /**
     * Creates a {@link Page} containing projections of posts which belong to a user with {@code userId}.
     * The most recent posts appear first and posts marked as deleted do not appear at all.
     *
     * @param userId id of the user whose posts will be fetched
     * @param pageable information about the wanted page
     * @return a {@link Page} containing posts
     */
    public Page<PostDto> findMostRecentPostsOfUser(UUID userId, Pageable pageable) {
        return postRepository.findMostRecentPostsOfUser(userId, pageable);
    }

    /**
     * Creates a {@link Page} containing projections of quotes which quote the post with {@code postId}.
     * The most recent quotes appear first and quotes marked as deleted do not appear at all.
     *
     * @param postId id of the post whose quotes will be fetched
     * @param pageable information about the wanted page
     * @return a {@link Page} containing quotes
     */
    public Page<PostDto> findMostRecentQuotesOfPost(UUID postId, Pageable pageable) {
        return postRepository.findMostRecentQuotesOfPost(postId, pageable);
    }

    /**
     * Creates a {@link Page} containing projections of responses which respond to the post with {@code postId}.
     * The most recent responses appear first and responses marked as deleted do not appear at all.
     *
     * @param postId id of the post whose responses will be fetched
     * @param pageable information about the wanted page
     * @return a {@link Page} containing responses
     */
    public Page<PostDto> findMostRecentResponsesToPost(UUID postId, Pageable pageable) {
        return postRepository.findMostRecentResponsesToPost(postId, pageable);
    }

    /**
     * Processes the content of a new post and returns a saved {@link Post}.
     *
     * Currently, the processing consists of:
     * <ul>
     *     <li>detecting all valid hashtags used in the content of a post and associating
     *      the post with these hashtags</li>
     *      <li>detecting all mentioned users and notifying them about being mentioned in the post</li>
     * </ul>.
     *
     * <strong>This method should only be given pre-validated DTOs, because it does not run any checks
     * of the validity of the post's content.</strong>
     *
     * @param post {@link Post} object containing pre-validated content
     * @return saved {@link Post}
     */
    @Transactional
    private Post processPostAndSave(Post post) {
        Set<Tag> tags = findTagsInContent(post.getContent());
        post.setTags(tags);

        var savedPost = postRepository.save(post);
        notifyMentionedUsers(savedPost);
        return savedPost;
    }

    /**
     * Creates a regular post and returns a saved {@link Post}.
     *
     * <strong>This method should only be given pre-validated DTOs, because it does not run any checks
     * of the validity of the post's content.</strong>
     *
     * @param postAuthorId id of the post author
     * @param dto pre-validated DTO containing the content of a new post
     * @return saved {@link Post}
     */
    @Transactional
    public Post createPost(UUID postAuthorId, PostCreationDto dto) {
        var post = new Post(postAuthorId, dto.getContent(), Set.of());
        return processPostAndSave(post);
    }

    /**
     * Creates a quote post and returns a saved {@link Post}.
     *
     * <strong>This method should only be given pre-validated DTOs, because it does not run any checks
     * of the validity of the post's content.</strong>
     *
     * If the user does not quote themselves, a notification event is created and published on the
     * message queue, where another service can read it and transform it into a notification that can
     * be fetched by a user.
     *
     * @param quoteAuthorId id of the user who wants to quote another post
     * @param quotedPostId id of the post being quoted
     * @param dto pre-validated DTO containing the content of a new quote
     * @throws PostNotFoundException when post being quoted does not exist or is marked as deleted
     * @return saved {@link Post}
     */
    @Transactional
    public Post createQuotePost(UUID quoteAuthorId, UUID quotedPostId, PostCreationDto dto) throws PostNotFoundException {
        Optional<Post> quotedPost = postRepository.findById(quotedPostId);

        if (quotedPost.isEmpty() || quotedPost.get().isDeleted()) {
            throw new PostNotFoundException(quotedPostId);
        }

        var unwrappedPost = quotedPost.get();

        Post quotingPost = new Post(quoteAuthorId, dto.getContent(), Set.of());
        quotingPost.setQuotedPost(unwrappedPost);
        var savedQuotingPost = processPostAndSave(quotingPost);

        // do not notify the user if they are quoting their own post
        if (!quoteAuthorId.equals(unwrappedPost.getAuthorId())) {
            notificationPublisher.publishNotification(new NotificationDto(
                    unwrappedPost.getAuthorId(),
                    savedQuotingPost.getId(),
                    Notification.Type.QUOTE)
            );
        }

        return savedQuotingPost;
    }

    /**
     * Creates a response post and returns a saved {@link Post}.
     *
     * <strong>This method should only be given pre-validated DTOs, because it does not run any checks
     * of the validity of the post's content.</strong>
     *
     * If the user does not respond to themselves, a notification event is created and published on the
     * message queue, where another service can read it and transform it into a notification that can
     * be fetched by a user.
     *
     * @param responseAuthorId id of the user who wants to respond to another post
     * @param parentPostId id of the post being responded to
     * @param dto pre-validated DTO containing the content of a new quote
     * @throws PostNotFoundException when post being responded to does not exist or is marked as deleted
     * @return saved {@link Post}
     */
    @Transactional
    public Post createResponsePost(UUID responseAuthorId, UUID parentPostId, PostCreationDto dto) throws PostNotFoundException {
        Optional<Post> parentPost = postRepository.findById(parentPostId);

        if (parentPost.isEmpty() || parentPost.get().isDeleted()) {
            throw new PostNotFoundException(parentPostId);
        }

        var unwrappedPost = parentPost.get();

        Post responsePost = new Post(responseAuthorId, dto.getContent(), Set.of());
        responsePost.setParentPost(parentPost.get());
        var savedResponsePost = processPostAndSave(responsePost);

        // do not notify the user if they are responding to their own post
        if (!responseAuthorId.equals(unwrappedPost.getAuthorId())) {
            notificationPublisher.publishNotification(new NotificationDto(
                    parentPost.get().getAuthorId(),
                    savedResponsePost.getId(),
                    Notification.Type.RESPONSE)
            );
        }

        return savedResponsePost;
    }

    /**
     * Marks a post as deleted if the user requesting a deletion is the author of the post.
     *
     * @param requestingUserId id of the user who requests post be deleted
     * @param postId id of the post to be deleted
     * @return {@link Post} object of the post that was marked as deleted
     * @throws PostNotFoundException when post with {@code postId} does not exist or is already marked as deleted
     * @throws PostDeletionForbiddenException when user requesting post's deletion is not the author of that post
     */
    @Transactional
    public Post deletePost(UUID requestingUserId, UUID postId) throws PostNotFoundException,
            PostDeletionForbiddenException {

        var foundPost = postRepository
                .findById(postId)
                .filter(p -> !p.isDeleted())
                .orElseThrow(() -> new PostNotFoundException(postId));

        if (!foundPost.getAuthorId().equals(requestingUserId)) {
            throw new PostDeletionForbiddenException(requestingUserId, postId);
        }

        foundPost.setDeleted(true);
        return postRepository.save(foundPost);
    }

    /**
     * Finds all strings in the post's content which are recognized as tags and returns
     * them as {@link Tag} objects.
     *
     * @param content content of the post searched for tags
     * @return tag objects representing all tags found in the content
     */
    private Set<Tag> findTagsInContent(String content) {
        // look for the hashtag pattern in the content
        Matcher m = TagService.HASHTAG_PATTERN.matcher(content);

        Set<String> uniqueTags = new HashSet<>();

        // find all tags that were used and save only unique ones
        while (m.find()) {
            // every tag name should have all characters lower case
            uniqueTags.add(m.group(1).toLowerCase());
        }

        Set<Tag> allFoundTags = new HashSet<>();
        for (String tagName : uniqueTags) {
            // for every tag name check if that tag already exists
            // in the database
            try {
                Tag dbTag = tagService.findByName(tagName);
                allFoundTags.add(dbTag);
            } catch (TagNotFoundException ex) {
                // tag doesn't exist in the database
                // create a new tag
                allFoundTags.add(new Tag(tagName));
            }
        }
        return allFoundTags;
    }

    /**
     * Finds all strings in the post's content which are recognized as mentions of other users
     * and sends notifications to these users informing them about being mentioned.
     *
     * Strings which are recognized as mentions start with a character '@' (e.g. '@someuser' would
     * result in a notification being sent to a user 'someuser', unless that user does not exist).
     *
     * This method does not send notifications to users who:
     * <ul>
     *     <li>couldn't be fetched from a remote service containing user information</li>
     *     <li>are mentioning themselves in their post's content</li>
     * </ul>
     *
     * @param notifyingPost post which contents have to be searched for mentions
     */
    private void notifyMentionedUsers(Post notifyingPost) {
        Matcher m = usernamePattern.matcher(notifyingPost.getContent());
        Set<String> uniqueUsernames = new HashSet<>();
        // find all unique usernames
        while (m.find()) {
            uniqueUsernames.add(m.group(1));
        }

        uniqueUsernames.forEach(System.out::println);
        // try to fetch every single mentioned user, and send them a notification if they exist
        for (String username : uniqueUsernames) {
            Page<UserDto> u = userServiceClient.getUserExact(username);
            // getUserExact either has a single result or no results,
            // it's impossible to get more than one result because it would mean
            // that there are two users who have an identical username
            if (u.getTotalElements() == 1) {
                var userToBeNotified = u.getContent().get(0);
                // only publish the notification if the user to be notified is not the
                // author of the post
                if (!userToBeNotified.getId().equals(notifyingPost.getAuthorId())) {
                    notificationPublisher.publishNotification(new NotificationDto(
                            userToBeNotified.getId(), notifyingPost.getId(), Notification.Type.MENTION
                    ));
                }
            }
        }
    }

    /**
     * Checks if a user likes a post.
     *
     * @param likingUser id of the user whose like needs to be checked
     * @param likedPost id of the post which is potentially liked by {@code likingUser}
     * @return {@code true} if the user likes the post
     */
    @Transactional
    public boolean likeExists(UUID likingUser, UUID likedPost) {
        return likeRepository.existsLike(likingUser, likedPost);
    }

    /**
     * Makes a user like a post.
     *
     * @param likingUser id of the user who wants to like a post
     * @param likedPost id of the post which will be liked
     * @return {@code true} if the user likes the post
     * @throws PostNotFoundException when post with {@code likedPost} id does not exist
     */
    @Transactional
    public boolean likePost(UUID likingUser, UUID likedPost) throws PostNotFoundException {
        throwIfPostNotFound(likedPost);

        Post post = postRepository.getReferenceById(likedPost);
        Like like = new Like(likingUser, post);
        likeRepository.save(like);

        return likeExists(likingUser, likedPost);
    }

    /**
     * Makes a user unlike a post.
     *
     * @param likingUser id of the user who wants to unlike a post
     * @param likedPost id of the post which will be unliked
     * @return {@code true} if the user no longer likes the post
     * @throws PostNotFoundException when post with {@code likedPost} id does not exist
     */
    @Transactional
    public boolean unlikePost(UUID likingUser, UUID likedPost) throws PostNotFoundException {
        throwIfPostNotFound(likedPost);
        likeRepository.deleteLike(likingUser, likedPost);
        return !likeExists(likingUser, likedPost);
    }
}
