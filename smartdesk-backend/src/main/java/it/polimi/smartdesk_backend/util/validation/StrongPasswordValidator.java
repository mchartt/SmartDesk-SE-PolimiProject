package it.polimi.smartdesk_backend.util.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

import it.polimi.smartdesk_backend.util.policy.PasswordPolicy;
import lombok.NoArgsConstructor;

/** Implementazione semplice: delega tutto a {@link PasswordPolicy#isStrong(String)}. */
@NoArgsConstructor
public class StrongPasswordValidator implements ConstraintValidator<StrongPassword, String> {

    /** {@inheritDoc} */
    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        if (value == null) {
            return true;
        }
        return PasswordPolicy.isStrong(value);
    }
}

