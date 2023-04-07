package ml.echelon133.microblog.auth.service;

import ml.echelon133.microblog.auth.model.RedisOAuth2Authorization;
import ml.echelon133.microblog.auth.repository.OAuth2AuthorizationRepository;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.oauth2.core.endpoint.OAuth2ParameterNames;
import org.springframework.security.oauth2.server.authorization.OAuth2Authorization;
import org.springframework.security.oauth2.server.authorization.OAuth2AuthorizationService;
import org.springframework.security.oauth2.server.authorization.OAuth2TokenType;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;

import java.util.*;

/**
 * {@link OAuth2AuthorizationService} implementation which enables saving and loading {@link OAuth2Authorization}
 * objects to and from Redis.
 */
@Service
public class CustomOAuth2AuthorizationService implements OAuth2AuthorizationService {

    private static final Logger LOGGER = LogManager.getLogger(CustomOAuth2AuthorizationService.class);

    private final OAuth2AuthorizationRepository authorizationRepository;
    private final AuthorizationMapper authorizationMapper;

    @Autowired
    public CustomOAuth2AuthorizationService(OAuth2AuthorizationRepository authorizationRepository,
                                            AuthorizationMapper authorizationMapper) {
        this.authorizationRepository = authorizationRepository;
        this.authorizationMapper = authorizationMapper;
    }

    @Override
    public void save(OAuth2Authorization authorization) {
        LOGGER.debug(String.format(
                "Saving authorization '%s' owned by principal '%s'", authorization.getId(), authorization.getPrincipalName()
        ));
        this.authorizationRepository.save(authorizationMapper.flatten(authorization));
    }

    @Override
    public void remove(OAuth2Authorization authorization) {
        LOGGER.debug(String.format(
                "Removing authorization '%s' owned by principal '%s'", authorization.getId(), authorization.getPrincipalName()
        ));
        this.authorizationRepository.deleteById(authorization.getId());
    }

    @Override
    public OAuth2Authorization findById(String id) {
        Assert.hasText(id, "Id cannot be empty");
        var foundAuth = this.authorizationRepository.findById(id);

        if (foundAuth.isEmpty()) {
            LOGGER.debug("Could not find auth with id " + id);
            return null;
        } else {
            var unwrappedAuth = foundAuth.get();
            LOGGER.debug(String.format(
                    "Found auth with id '%s', that belongs to principal '%s'", id, unwrappedAuth.getPrincipalName()
            ));
            return authorizationMapper.unflatten(unwrappedAuth);
        }
    }

    @Override
    public OAuth2Authorization findByToken(String token, OAuth2TokenType tokenType) {
        Assert.hasText(token, "Token cannot be empty");

        Optional<RedisOAuth2Authorization> foundAuth = Optional.empty();

        if (tokenType == null) {
            LOGGER.debug(String.format(
                    "Token type is null, try to find token '%s' among authorization codes or access tokens", token
            ));
            // try to get either authorization_code or access_token and
            // ignore state and refresh_token because they are not used in this
            // implementation of an authorization server
            foundAuth =
                    this.authorizationRepository.findByAuthorizationCodeValue(token);
            if (foundAuth.isEmpty()) {
                foundAuth =
                        this.authorizationRepository.findByAccessTokenValue(token);
            }
        } else if (OAuth2ParameterNames.CODE.equals(tokenType.getValue())) {
            LOGGER.debug(String.format(
                    "Token type is CODE, try to find token '%s' among authorization codes", token
            ));
            foundAuth = this.authorizationRepository.findByAuthorizationCodeValue(token);
        } else if (OAuth2TokenType.ACCESS_TOKEN.equals(tokenType)) {
            LOGGER.debug(String.format(
                    "Token type is ACCESS_TOKEN, try to find token '%s' among access tokens", token
            ));
            foundAuth = this.authorizationRepository.findByAccessTokenValue(token);
        } else {
            LOGGER.warn(String.format(
                    "Server does not support tokens of type '%s'. Only CODE and ACCESS_TOKEN are supported", tokenType.getValue()
            ));
        }

        if (foundAuth.isPresent()) {
            LOGGER.debug("Found auth containing token " + token);
        } else {
            LOGGER.debug("Did not find auth containing token " + token);
        }
        return foundAuth.map(authorizationMapper::unflatten).orElse(null);
    }
}
