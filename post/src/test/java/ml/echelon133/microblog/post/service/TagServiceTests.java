package ml.echelon133.microblog.post.service;

import ml.echelon133.microblog.post.exception.TagNotFoundException;
import ml.echelon133.microblog.post.repository.TagRepository;
import ml.echelon133.microblog.shared.post.tag.Tag;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.context.TestPropertySource;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.BDDMockito.given;

/*
    Disable kubernetes during tests to make local execution of tests possible.
    If kubernetes is not disabled, tests won't execute at all because Spring will
    fail to configure kubernetes when run outside it.
 */
@TestPropertySource(properties = "spring.cloud.kubernetes.enabled=false")
@ExtendWith(MockitoExtension.class)
@DisplayName("Tests of TagService")
public class TagServiceTests {

    @Mock
    private TagRepository tagRepository;

    @InjectMocks
    private TagService tagService;

    @Test
    @DisplayName("findByName throws a TagNotFoundException when there is no tag")
    public void findByName_TagNotFound_ThrowsException() {
        var name = "test";

        // given
        given(tagRepository.findByName("test")).willReturn(Optional.empty());

        // when
        String message = assertThrows(TagNotFoundException.class, () -> {
            tagService.findByName(name);
        }).getMessage();

        // then
        assertEquals("Tag 'test' could not be found", message);
    }

    @Test
    @DisplayName("findByName returns a tag when found")
    public void findByName_TagFound_ReturnsCorrect() throws TagNotFoundException {
        var name = "test";
        var tag = new Tag(name);

        // given
        given(tagRepository.findByName("test")).willReturn(Optional.of(tag));

        // when
        Tag foundTag  = tagService.findByName(name);

        // then
        assertEquals(name, foundTag.getName());
    }
}
