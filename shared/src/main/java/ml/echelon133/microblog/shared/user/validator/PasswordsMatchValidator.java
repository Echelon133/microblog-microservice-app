package ml.echelon133.microblog.shared.user.validator;

import ml.echelon133.microblog.shared.user.UserCreationDto;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;

public class PasswordsMatchValidator implements ConstraintValidator<PasswordsMatch, UserCreationDto> {

    @Override
    public boolean isValid(UserCreationDto newUserDto, ConstraintValidatorContext constraintValidatorContext) {
        if (newUserDto.getPassword() == null) return false;
        return newUserDto.getPassword().equals(newUserDto.getPassword2());
    }

    @Override
    public void initialize(PasswordsMatch constraintAnnotation) {
    }
}