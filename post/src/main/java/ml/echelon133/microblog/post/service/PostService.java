package ml.echelon133.microblog.post.service;

import ml.echelon133.microblog.post.exception.PostDeletionForbiddenException;
import ml.echelon133.microblog.post.exception.SelfReportException;
import ml.echelon133.microblog.post.exception.TagNotFoundException;
import ml.echelon133.microblog.post.queue.NotificationPublisher;
import ml.echelon133.microblog.post.queue.ReportPublisher;
import ml.echelon133.microblog.post.repository.LikeRepository;
import ml.echelon133.microblog.post.repository.PostRepository;
import ml.echelon133.microblog.post.web.UserServiceClient;
import ml.echelon133.microblog.shared.exception.ResourceNotFoundException;
import ml.echelon133.microblog.shared.notification.Notification;
import ml.echelon133.microblog.shared.notification.NotificationCreationDto;
import ml.echelon133.microblog.shared.post.Post;
import ml.echelon133.microblog.shared.post.PostCountersDto;
import ml.echelon133.microblog.shared.post.PostCreationDto;
import ml.echelon133.microblog.shared.post.PostDto;
import ml.echelon133.microblog.shared.post.like.Like;
import ml.echelon133.microblog.shared.post.tag.Tag;
import ml.echelon133.microblog.shared.report.Report;
import ml.echelon133.microblog.shared.report.ReportBodyDto;
import ml.echelon133.microblog.shared.report.ReportCreationDto;
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
@Transactional
public class PostService {

    private Pattern usernamePattern = Pattern.compile("@([A-Za-z0-9]{1,30})");
    private PostRepository postRepository;
    private LikeRepository likeRepository;
    private TagService tagService;
    private Clock clock;
    private NotificationPublisher notificationPublisher;
    private UserServiceClient userServiceClient;
    private ReportPublisher reportPublisher;

    @Autowired
    public PostService(PostRepository postRepository,
                       LikeRepository likeRepository,
                       TagService tagService,
                       Clock clock,
                       NotificationPublisher notificationPublisher,
                       UserServiceClient userServiceClient,
                       ReportPublisher reportPublisher) {
        this.postRepository = postRepository;
        this.likeRepository = likeRepository;
        this.tagService = tagService;
        this.clock = clock;
        this.notificationPublisher = notificationPublisher;
        this.userServiceClient = userServiceClient;
        this.reportPublisher = reportPublisher;
    }

    private void throwIfPostNotFound(UUID id) throws ResourceNotFoundException {
        if (!postRepository.existsPostByIdAndDeletedFalse(id)) {
            throw new ResourceNotFoundException(Post.class, id);
        }
    }

    /**
     * Projects the post/quote/response with specified {@link java.util.UUID} into a DTO object.
     *
     * @param id id of the post/quote/response
     * @return DTO projection of the post
     * @throws ResourceNotFoundException thrown when the post does not exist or is marked as deleted
     */
    public PostDto findById(UUID id) throws ResourceNotFoundException {
        return postRepository.findByPostId(id).orElseThrow(() ->
                new ResourceNotFoundException(Post.class, id)
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
     * @param last how many hours old should the oldest post on the feed be, posts which are older will not show up on the feed
     * @param pageable all information about the wanted page
     * @return a {@link Page} containing posts which together create user's feed
     * @throws IllegalArgumentException if {@code hours} value is not in 1-24 range
     */
    public Page<PostDto> generateFeed(Optional<UUID> userId, boolean popular, Integer last, Pageable pageable) {
        if (last > 24 || last <= 0) {
            throw new IllegalArgumentException("values of 'last' outside the 1-24 range are not valid");
        }

        var start = Date.from(Instant.now(clock).minus(last, ChronoUnit.HOURS));
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
     * @throws ResourceNotFoundException thrown when the post with specified id does not exist
     */
    public PostCountersDto findPostCounters(UUID postId) throws ResourceNotFoundException {
        throwIfPostNotFound(postId);

        var likes = likeRepository.countByLikeIdLikedPostId(postId);
        var quotes = postRepository.countByQuotedPostIdAndDeletedFalse(postId);
        var responses = postRepository.countByParentPostIdAndDeletedFalse(postId);
        return new PostCountersDto(likes, quotes, responses);
    }

    /**
     * Finds a {@link Page} of posts of {@code userId} sorted by their recency.
     * Posts which are marked as deleted will be ignored, as the results of this
     * query are public.
     *
     * @param userId id of the user whose posts will be fetched
     * @param pageable all information about the wanted page
     * @return a page of posts sorted from the most recent to the least recent
     */
    public Page<PostDto> findMostRecentPostsOfUser(UUID userId, Pageable pageable) {
        return postRepository.findMostRecentPostsOfUser(userId, pageable);
    }

    /**
     * Finds a {@link Page} of quotes of {@code postId} sorted by their recency.
     * Quotes which are marked as deleted will be ignored, as the results of this
     * query are public.
     *
     * @param postId id of the post whose quotes will be fetched
     * @param pageable all information about the wanted page
     * @return a page of quotes of post sorted from the most recent to the least recent
     */
    public Page<PostDto> findMostRecentQuotesOfPost(UUID postId, Pageable pageable) {
        return postRepository.findMostRecentQuotesOfPost(postId, pageable);
    }

    /**
     * Finds a {@link Page} of responses to {@code postId} sorted by their recency.
     * Responses which are marked as deleted will be ignored, as the results of this
     * query are public.
     *
     * @param postId id of the post whose responses will be fetched
     * @param pageable all information about the wanted page
     * @return a page of responses to post sorted from the most recent to the least recent
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
     * @throws ResourceNotFoundException when post being quoted does not exist or is marked as deleted
     * @return saved {@link Post}
     */
    public Post createQuotePost(UUID quoteAuthorId, UUID quotedPostId, PostCreationDto dto) throws ResourceNotFoundException {
        Optional<Post> quotedPost = postRepository.findById(quotedPostId);

        if (quotedPost.isEmpty() || quotedPost.get().isDeleted()) {
            throw new ResourceNotFoundException(Post.class, quotedPostId);
        }

        var unwrappedPost = quotedPost.get();

        Post quotingPost = new Post(quoteAuthorId, dto.getContent(), Set.of());
        quotingPost.setQuotedPost(unwrappedPost);
        var savedQuotingPost = processPostAndSave(quotingPost);

        // do not notify the user if they are quoting their own post
        if (!quoteAuthorId.equals(unwrappedPost.getAuthorId())) {
            notificationPublisher.publishNotification(new NotificationCreationDto(
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
     * @throws ResourceNotFoundException when post being responded to does not exist or is marked as deleted
     * @return saved {@link Post}
     */
    public Post createResponsePost(UUID responseAuthorId, UUID parentPostId, PostCreationDto dto) throws ResourceNotFoundException {
        Optional<Post> parentPost = postRepository.findById(parentPostId);

        if (parentPost.isEmpty() || parentPost.get().isDeleted()) {
            throw new ResourceNotFoundException(Post.class, parentPostId);
        }

        var unwrappedPost = parentPost.get();

        Post responsePost = new Post(responseAuthorId, dto.getContent(), Set.of());
        responsePost.setParentPost(parentPost.get());
        var savedResponsePost = processPostAndSave(responsePost);

        // do not notify the user if they are responding to their own post
        if (!responseAuthorId.equals(unwrappedPost.getAuthorId())) {
            notificationPublisher.publishNotification(new NotificationCreationDto(
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
     * @throws ResourceNotFoundException when post with {@code postId} does not exist or is already marked as deleted
     * @throws PostDeletionForbiddenException when user requesting post's deletion is not the author of that post
     */
    public Post deletePost(UUID requestingUserId, UUID postId) throws ResourceNotFoundException,
            PostDeletionForbiddenException {

        var foundPost = postRepository
                .findById(postId)
                .filter(p -> !p.isDeleted())
                .orElseThrow(() -> new ResourceNotFoundException(Post.class, postId));

        if (!foundPost.getAuthorId().equals(requestingUserId)) {
            throw new PostDeletionForbiddenException();
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

        Set<String> uniqueTagNames = new HashSet<>();
        Set<Tag> tags = new HashSet<>();

        // find or create a Tag object for every unique tag
        while (m.find()) {
            // internally, every tag name should have all characters lower case
            var tagName = m.group(1).toLowerCase();

            // ignore the tagName if it's not unique (happens when the post's content uses the same tag many times)
            if (!uniqueTagNames.contains(tagName)) {
                try {
                    // if the tag already exists in the database - reuse it
                    Tag dbTag = tagService.findByName(tagName);
                    tags.add(dbTag);
                } catch (TagNotFoundException ex) {
                    // tag doesn't exist in the database - create a new tag
                    tags.add(new Tag(tagName));
                } finally {
                    uniqueTagNames.add(tagName);
                }
            }
        }

        return tags;
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
        // look for the username pattern in the content
        Matcher m = usernamePattern.matcher(notifyingPost.getContent());

        Set<String> uniqueUsernames = new HashSet<>();

        // try to fetch every single unique mentioned user and send them a notification if they exist
        while (m.find()) {
            var username = m.group(1);

            if (!uniqueUsernames.contains(username)) {
                Page<UserDto> u = userServiceClient.getUserExact(username);
                // getUserExact either has a single result or no results,
                // it's impossible to get more than one result because it would mean
                // that there are two users who have an identical username
                if (u.getTotalElements() == 1) {
                    var userToBeNotified = u.getContent().get(0);
                    // only publish the notification if the user to be notified is not the
                    // author of the post
                    if (!userToBeNotified.getId().equals(notifyingPost.getAuthorId())) {
                        notificationPublisher.publishNotification(new NotificationCreationDto(
                                userToBeNotified.getId(), notifyingPost.getId(), Notification.Type.MENTION
                        ));
                    }
                }
                uniqueUsernames.add(username);
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
    public boolean likeExists(UUID likingUser, UUID likedPost) {
        return likeRepository.existsLike(likingUser, likedPost);
    }

    /**
     * Makes a user like a post.
     *
     * @param likingUser id of the user who wants to like a post
     * @param likedPost id of the post which will be liked
     * @return {@code true} if the user likes the post
     * @throws ResourceNotFoundException when post with {@code likedPost} id does not exist
     */
    public boolean likePost(UUID likingUser, UUID likedPost) throws ResourceNotFoundException {
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
     * @throws ResourceNotFoundException when post with {@code likedPost} id does not exist
     */
    public boolean unlikePost(UUID likingUser, UUID likedPost) throws ResourceNotFoundException {
        throwIfPostNotFound(likedPost);
        likeRepository.deleteLike(likingUser, likedPost);
        return !likeExists(likingUser, likedPost);
    }

    /**
     * Creates a report of a post.
     *
     * <strong>This method should only be given pre-validated DTOs, because it does not run any checks
     * of the validity of the report's content.</strong>
     *
     * @param dto pre-validated dto containing a reason and context of the report
     * @param reportingUser id of the user who is reporting the post
     * @param reportedPost id of the post which is being reported
     * @throws ResourceNotFoundException when post with {@code reportedPost} id does not exist
     * @throws SelfReportException when user tries to report their own post
     */
    public void reportPost(ReportBodyDto dto, UUID reportingUser, UUID reportedPost)
            throws ResourceNotFoundException, SelfReportException {

        var foundPost = postRepository
                .findById(reportedPost)
                .filter(p -> !p.isDeleted())
                .orElseThrow(() -> new ResourceNotFoundException(Post.class, reportedPost));

        if (foundPost.getAuthorId().equals(reportingUser)) {
            throw new SelfReportException();
        }

        reportPublisher.publishReport(new ReportCreationDto(
                // this valueOf will never fail if the calling code
                // follows the invariant from this method's javadoc, which
                // states that ReportBodyDto has to be pre-validated
                Report.Reason.valueOf(dto.getReason().toUpperCase()),
                dto.getContext(),
                reportedPost,
                reportingUser
        ));
    }
}
