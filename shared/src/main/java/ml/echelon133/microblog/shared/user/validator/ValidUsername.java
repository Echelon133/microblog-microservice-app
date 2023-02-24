package ml.echelon133.microblog.shared.user.validator;

import javax.validation.Constraint;
import javax.validation.Payload;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.ANNOTATION_TYPE;
import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

@Target({FIELD, ANNOTATION_TYPE})
@Retention(RUNTIME)
@Constraint(validatedBy= UsernameValidator.class)
public @interface ValidUsername {
    String message() default "Username is not valid";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};
}
