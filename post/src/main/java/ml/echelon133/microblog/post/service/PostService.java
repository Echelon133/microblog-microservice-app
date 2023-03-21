package ml.echelon133.microblog.post.service;

import ml.echelon133.microblog.post.exception.PostNotFoundException;
import ml.echelon133.microblog.post.exception.TagNotFoundException;
import ml.echelon133.microblog.post.repository.PostRepository;
import ml.echelon133.microblog.shared.post.Post;
import ml.echelon133.microblog.shared.post.PostCreationDto;
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
    private TagService tagService;

    @Autowired
    public PostService(PostRepository postRepository, TagService tagService) {
        this.postRepository = postRepository;
        this.tagService = tagService;
    }

    private void throwIfPostNotFound(UUID id) throws PostNotFoundException {
        if (!postRepository.existsById(id)) {
            throw new PostNotFoundException(id);
        }
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
        if (!quotedPost.isDeleted()) {
            Post quotingPost = new Post(quoteAuthorId, dto.getContent(), Set.of());
            quotingPost.setQuotedPost(quotedPost);

            return processPostAndSave(quotingPost);
        }
        throw new PostNotFoundException(quotedPostId);
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
}
