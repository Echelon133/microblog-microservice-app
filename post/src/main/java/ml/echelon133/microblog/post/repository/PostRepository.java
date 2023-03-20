package ml.echelon133.microblog.post.repository;

import ml.echelon133.microblog.shared.post.Post;
import org.springframework.data.repository.PagingAndSortingRepository;

import java.util.UUID;

public interface PostRepository extends PagingAndSortingRepository<Post, UUID> {}
