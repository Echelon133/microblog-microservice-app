package ml.echelon133.microblog.shared.user.validator;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class PasswordValidator implements ConstraintValidator<ValidPassword, String> {

    // at least one a-z, A-Z and 0-9
    // at least one special character from the list ' !"#$%&'()*+,-./:;<=>?@[\]^_`{|}~'
    private final Pattern passwordPattern = Pattern
            .compile("(?=.*[a-z])(?=.*[A-Z])(?=.*[0-9])(?=.*[\\x20-\\x2F\\x3A-\\x40\\x5B-\\x60\\x7B-\\x7E])");

    @Override
    public void initialize(ValidPassword constraintAnnotation) {
        ConstraintValidator.super.initialize(constraintAnnotation);
    }

    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        if (value == null) return false;
        Matcher m = passwordPattern.matcher(value);
        return m.find();
    }
}
