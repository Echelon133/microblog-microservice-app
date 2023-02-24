package ml.echelon133.microblog.shared.user.validator;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class UsernameValidator implements ConstraintValidator<ValidUsername, String> {

    private final Pattern usernamePattern = Pattern.compile("^([A-Za-z0-9]{1,30})$");

    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        if (value == null) return false;
        Matcher m = usernamePattern.matcher(value);
        return m.find();
    }

    @Override
    public void initialize(ValidUsername constraintAnnotation) {
    }
}
