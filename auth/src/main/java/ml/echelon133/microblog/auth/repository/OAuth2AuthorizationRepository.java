package ml.echelon133.microblog.auth.repository;

import ml.echelon133.microblog.auth.model.RedisOAuth2Authorization;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface OAuth2AuthorizationRepository extends CrudRepository<RedisOAuth2Authorization, String> {
    Optional<RedisOAuth2Authorization> findByAuthorizationCodeValue(String token);
    Optional<RedisOAuth2Authorization> findByAccessTokenValue(String token);
}
