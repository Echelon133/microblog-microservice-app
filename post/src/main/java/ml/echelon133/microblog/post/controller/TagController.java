package ml.echelon133.microblog.post.controller;

import ml.echelon133.microblog.post.service.TagService;
import ml.echelon133.microblog.shared.post.PostDto;
import ml.echelon133.microblog.shared.post.tag.TagDto;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/tags")
public class TagController {

    private TagService tagService;

    @Autowired
    public TagController(TagService tagService) {
        this.tagService = tagService;
    }

    @GetMapping("/popular")
    public List<TagDto> getPopularTags(@RequestParam(required = false, defaultValue = "1") Integer last) {
        return tagService.findFiveMostPopularInLast(last);
    }

    @GetMapping("/{name}/posts")
    public Page<PostDto> getMostRecentPostsInTag(@PageableDefault(size = 20) Pageable pageable, @PathVariable String name) {
        return tagService.findMostRecentPostsTagged(name, pageable);
    }
}
