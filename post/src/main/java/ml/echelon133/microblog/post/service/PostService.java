package ml.echelon133.microblog.post.service;

import ml.echelon133.microblog.post.exception.PostDeletionForbiddenException;
import ml.echelon133.microblog.post.exception.PostNotFoundException;
import ml.echelon133.microblog.post.exception.TagNotFoundException;
import ml.echelon133.microblog.post.repository.LikeRepository;
import ml.echelon133.microblog.post.repository.PostRepository;
import ml.echelon133.microblog.shared.post.Post;
import ml.echelon133.microblog.shared.post.PostCreationDto;
import ml.echelon133.microblog.shared.post.PostDto;
import ml.echelon133.microblog.shared.post.like.Like;
import ml.echelon133.microblog.shared.post.tag.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.transaction.Transactional;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Matcher;

@Service
public class PostService {

    private PostRepository postRepository;
    private LikeRepository likeRepository;
    private TagService tagService;

    @Autowired
    public PostService(PostRepository postRepository, LikeRepository likeRepository, TagService tagService) {
        this.postRepository = postRepository;
        this.likeRepository = likeRepository;
        this.tagService = tagService;
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
     * Processes the content of a new post and returns a saved {@link Post}.
     *
     * Currently, processing involves detecting all valid hashtags used in the content of a post and associating
     * the post with these hashtags.
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
        return postRepository.save(post);
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
     * @param quoteAuthorId id of the user who wants to quote another post
     * @param quotedPostId id of the post being quoted
     * @param dto pre-validated DTO containing the content of a new quote
     * @throws PostNotFoundException when post being quoted does not exist or is marked as deleted
     * @return saved {@link Post}
     */
    @Transactional
    public Post createQuotePost(UUID quoteAuthorId, UUID quotedPostId, PostCreationDto dto) throws PostNotFoundException {
        throwIfPostNotFound(quotedPostId);

        Post quotedPost = postRepository.getReferenceById(quotedPostId);
        Post quotingPost = new Post(quoteAuthorId, dto.getContent(), Set.of());
        quotingPost.setQuotedPost(quotedPost);
        return processPostAndSave(quotingPost);

    }

    /**
     * Creates a response post and returns a saved {@link Post}.
     *
     * <strong>This method should only be given pre-validated DTOs, because it does not run any checks
     * of the validity of the post's content.</strong>
     *
     * @param responseAuthorId id of the user who wants to respond to another post
     * @param parentPostId id of the post being responded to
     * @param dto pre-validated DTO containing the content of a new quote
     * @throws PostNotFoundException when post being responded to does not exist or is marked as deleted
     * @return saved {@link Post}
     */
    @Transactional
    public Post createResponsePost(UUID responseAuthorId, UUID parentPostId, PostCreationDto dto) throws PostNotFoundException {
        throwIfPostNotFound(parentPostId);

        Post parentPost = postRepository.getReferenceById(parentPostId);
        Post responsePost = new Post(responseAuthorId, dto.getContent(), Set.of());
        responsePost.setParentPost(parentPost);

        return processPostAndSave(responsePost);
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
