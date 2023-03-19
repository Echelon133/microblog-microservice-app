package ml.echelon133.microblog.post.service;

import ml.echelon133.microblog.post.exception.TagNotFoundException;
import ml.echelon133.microblog.post.repository.TagRepository;
import ml.echelon133.microblog.shared.post.tag.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class TagService {

    private TagRepository tagRepository;

    @Autowired
    public TagService(TagRepository tagRepository) {
        this.tagRepository = tagRepository;
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
}
