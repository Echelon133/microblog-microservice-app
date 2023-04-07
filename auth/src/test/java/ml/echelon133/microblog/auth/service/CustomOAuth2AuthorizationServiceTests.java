package ml.echelon133.microblog.auth.service;

import ml.echelon133.microblog.auth.repository.OAuth2AuthorizationRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.oauth2.server.authorization.OAuth2TokenType;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClientRepository;

import java.util.*;

import static ml.echelon133.microblog.auth.service.AuthTestData.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
@DisplayName("Tests of CustomOAuth2AuthorizationService")
public class CustomOAuth2AuthorizationServiceTests {

    @Mock
    private RegisteredClientRepository registeredClientRepository;

    @Mock
    private OAuth2AuthorizationRepository authorizationRepository;

    @InjectMocks
    private CustomOAuth2AuthorizationService authorizationService;

    @Test
    @DisplayName("findById returns null when authorization not found")
    public void findById_AuthorizationNotFound_ReturnsNull() {
        // given
        given(authorizationRepository.findById("test")).willReturn(Optional.empty());

        // when
        var auth = authorizationService.findById("test");

        // then
        assertNull(auth);
    }

    @Test
    @DisplayName("findById throws when id empty")
    public void findById_IdEmpty_ThrowsException() {
        // when
        String message = assertThrows(IllegalArgumentException.class, () -> {
            authorizationService.findById("");
        }).getMessage();

        // then
        assertEquals("Id cannot be empty", message);
    }

    @Test
    @DisplayName("findById non null when authorization found")
    public void findById_AuthorizationFound_ReturnsNonNull() {
        // given
        given(registeredClientRepository.findById(Client.REGISTERED_CLIENT_ID))
                .willReturn(Client.createTestRegisteredClient());
        given(authorizationRepository.findById(Redis.AUTH_ID)).willReturn(
                Optional.of(Redis.createValidRedisOAuth2Authorization())
        );

        // when
        var result = authorizationService.findById(Redis.AUTH_ID);

        // then
        assertNotNull(result);
    }

    @Test
    @DisplayName("save calls the repository")
    public void save_WhenProvidedAuthorization_CallsRepository() {
        var auth = Auth.createValidOAuth2Authorization();

        // when
        authorizationService.save(auth);

        // then
        verify(authorizationRepository, times(1)).save(any());
    }

    @Test
    @DisplayName("remove calls the repository")
    public void remove_WhenProvidedAuthorization_CallsRepository() {
        var auth = Auth.createValidOAuth2Authorization();

        // when
        authorizationService.remove(auth);

        // then
        verify(authorizationRepository, times(1)).deleteById(auth.getId());
    }

    @Test
    @DisplayName("findByToken throws when token empty")
    public void findByToken_TokenEmpty_ThrowsException() {
        // when
        String message = assertThrows(IllegalArgumentException.class, () -> {
            authorizationService.findByToken("", null);
        }).getMessage();

        // then
        assertEquals("Token cannot be empty", message);
    }

    @Test
    @DisplayName("findByToken searches among both authorization codes and access tokens when token type null")
    public void findByToken_TokenTypeNull_SearchesAmongBothTokenTypes() {
        var token = "test-token-value";

        // given
        given(authorizationRepository.findByAuthorizationCodeValue(token)).willReturn(Optional.empty());
        given(authorizationRepository.findByAccessTokenValue(token)).willReturn(Optional.empty());

        // when
        var auth = authorizationService.findByToken(token, null);

        // then
        assertNull(auth);
    }

    @Test
    @DisplayName("findByToken searches among authorization codes when token type 'code'")
    public void findByToken_TokenTypeCode_SearchesAmongAuthorizationCodes() {
        var token = "test-token-value";

        // given
        given(authorizationRepository.findByAuthorizationCodeValue(token)).willReturn(Optional.empty());

        // when
        var auth = authorizationService.findByToken(token, new OAuth2TokenType("code"));

        // then
        assertNull(auth);
    }

    @Test
    @DisplayName("findByToken searches among access tokens when token type 'access_token'")
    public void findByToken_TokenTypeAccessToken_SearchesAmongAccessTokens() {
        var token = "test-token-value";

        // given
        given(authorizationRepository.findByAccessTokenValue(token)).willReturn(Optional.empty());

        // when
        var auth = authorizationService.findByToken(token, OAuth2TokenType.ACCESS_TOKEN);

        // then
        assertNull(auth);
    }

    @Test
    @DisplayName("findByToken does not search repository when token type unknown")
    public void findByToken_TokenTypeUnknown_DoesNotSearchRepository() {
        var token = "test-token-value";

        // when
        var auth = authorizationService.findByToken(token, new OAuth2TokenType("some-unknown-type"));

        // then
        assertNull(auth);
    }
}
