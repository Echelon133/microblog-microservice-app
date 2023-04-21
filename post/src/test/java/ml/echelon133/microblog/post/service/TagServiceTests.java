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
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.time.Clock;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
@DisplayName("Tests of TagService")
public class TagServiceTests {

    @Mock
    private TagRepository tagRepository;

    @Mock
    private Clock clock;

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
        assertEquals("tag #test could not be found", message);
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

    @Test
    @DisplayName("findFiveMostPopularInLast throws an IllegalArgumentException when hours are not valid")
    public void findFiveMostPopularInLast_InvalidHours_ThrowsException() {
        // test range -100 to 100, without 1-24 (which wouldn't throw)
        // given
        var testRange = IntStream.range(-100, 100).filter(h -> h > 24 || h <= 0);

        testRange.forEach(hour -> {
            // when
            String message = assertThrows(IllegalArgumentException.class, () -> {
               tagService.findFiveMostPopularInLast(hour);
            }).getMessage();

            // then
            assertEquals("values of 'last' outside the 1-24 range are not valid", message);
        });
    }

    @Test
    @DisplayName("findFiveMostPopularInLast does not throw when hours are valid")
    public void findFiveMostPopularInLast_ValidHours_DoesNotThrow() {
        // test range 1 to 24
        // given
        var testRange = IntStream.range(1, 24 + 1);
        given(clock.instant()).willReturn(Instant.now());
        given(tagRepository.findPopularTags(any(), any(), any()))
                .willReturn(new PageImpl<>(List.of()));

        testRange.forEach(hour -> {
            // when
            tagService.findFiveMostPopularInLast(hour);
        });

        // then
        verify(tagRepository, times(24)).findPopularTags(any(), any(), any());
    }

    @Test
    @DisplayName("findFiveMostPopularInLast correctly calculates start and end dates based on given hours")
    public void findFiveMostPopularInLast_ValidHours_CorrectlyCalculatesStartAndEndDates() {
        // test range 1 to 24
        // given
        var fixedInstant = Instant.now();
        given(clock.instant()).willReturn(fixedInstant);
        given(tagRepository.findPopularTags(any(), any(), any())).willReturn(new PageImpl<>(List.of()));
        var testRange = IntStream.range(1, 24 + 1);

        testRange.forEach(hour -> {
            // when
            tagService.findFiveMostPopularInLast(hour);

            // then
            var expectedStartDate = Date.from(fixedInstant.minus(hour, ChronoUnit.HOURS));
            var expectedEndDate = Date.from(fixedInstant);

            verify(tagRepository).findPopularTags(
                    eq(expectedStartDate),
                    eq(expectedEndDate),
                    argThat(p -> p.getPageSize() == 5)
            );
        });
    }

    @Test
    @DisplayName("findMostRecentPostsTagged calls the repository method")
    public void findMostRecentPostsTagged_ProvidedArguments_CallsRepository() {
        var tagName = "test";
        var pageable = Pageable.ofSize(10);

        // when
        tagService.findMostRecentPostsTagged(tagName, pageable);

        // then
        verify(tagRepository, times(1)).findMostRecentPostsTagged(eq(tagName), eq(pageable));
    }
}
