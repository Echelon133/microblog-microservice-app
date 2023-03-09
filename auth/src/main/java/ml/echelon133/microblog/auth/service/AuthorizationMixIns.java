package ml.echelon133.microblog.auth.service;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.deser.std.DateDeserializers;
import com.fasterxml.jackson.databind.deser.std.UUIDDeserializer;

/**
 * MixIns which need to be added to the {@link com.fasterxml.jackson.databind.ObjectMapper} to enable
 * deserialization of our custom classes contained within {@link org.springframework.security.oauth2.server.authorization.OAuth2Authorization}
 * which - by default - are not considered safe to deserialize.
 * Without these mixins, an exception mentioning
 * <a href="https://github.com/spring-projects/spring-security/issues/4370">Issue 4370</a> is thrown.
 */
public class AuthorizationMixIns {

    @JsonTypeInfo(use = JsonTypeInfo.Id.CLASS)
    @JsonAutoDetect(
            fieldVisibility = JsonAutoDetect.Visibility.NONE,
            getterVisibility = JsonAutoDetect.Visibility.NON_PRIVATE,
            isGetterVisibility = JsonAutoDetect.Visibility.NON_PRIVATE)
    @JsonIgnoreProperties(
            value = {"displayedName", "aviURL", "description", "password", "dateCreated", "email", "authorities"},
            ignoreUnknown = true
    )
    static abstract class UserMixIn {}

    @JsonTypeInfo(use = JsonTypeInfo.Id.CLASS)
    @JsonAutoDetect(
            fieldVisibility = JsonAutoDetect.Visibility.NONE,
            getterVisibility = JsonAutoDetect.Visibility.NON_PRIVATE,
            isGetterVisibility = JsonAutoDetect.Visibility.NON_PRIVATE)
    @JsonIgnoreProperties(value = {"dateCreated"}, ignoreUnknown = true)
    static abstract class RoleMixIn {}

    @JsonTypeInfo(use = JsonTypeInfo.Id.CLASS)
    @JsonDeserialize(using = DateDeserializers.TimestampDeserializer.class)
    @JsonAutoDetect(
            fieldVisibility = JsonAutoDetect.Visibility.ANY,
            getterVisibility = JsonAutoDetect.Visibility.NONE,
            isGetterVisibility = JsonAutoDetect.Visibility.NONE)
    @JsonIgnoreProperties(ignoreUnknown = true)
    static abstract class TimestampMixIn {}

    @JsonTypeInfo(use = JsonTypeInfo.Id.CLASS)
    @JsonDeserialize(using = UUIDDeserializer.class)
    @JsonAutoDetect(
            fieldVisibility = JsonAutoDetect.Visibility.ANY,
            getterVisibility = JsonAutoDetect.Visibility.NONE,
            isGetterVisibility = JsonAutoDetect.Visibility.NONE)
    @JsonIgnoreProperties(ignoreUnknown = true)
    static abstract class UUIDMixIn {}
}
