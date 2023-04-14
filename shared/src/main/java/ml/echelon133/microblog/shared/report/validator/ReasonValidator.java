package ml.echelon133.microblog.shared.report.validator;

import ml.echelon133.microblog.shared.report.Report;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;

public class ReasonValidator implements ConstraintValidator<ValidReason, String> {

    @Override
    public void initialize(ValidReason constraintAnnotation) {}

    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        if (value == null) return false;
        try {
            Report.Reason.valueOf(value.toUpperCase());
            return true;
        } catch (IllegalArgumentException ignore) {
            return false;
        }
    }
}
