package it.polimi.smartdesk_backend.util.validation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

/** Vincolo Bean Validation allineato a {@link it.polimi.smartdesk_backend.util.policy.PasswordPolicy}; valori {@code null} sono considerati validi. */
@Documented
@Constraint(validatedBy = StrongPasswordValidator.class)
@Target({ElementType.FIELD})
@Retention(RetentionPolicy.RUNTIME)
public @interface StrongPassword {

    String message() default "La password deve avere almeno 8 caratteri e contenere un numero e un simbolo.";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}

