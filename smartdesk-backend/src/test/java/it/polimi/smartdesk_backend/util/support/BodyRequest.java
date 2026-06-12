package it.polimi.smartdesk_backend.util.support;

import jakarta.validation.constraints.NotBlank;
import lombok.experimental.Accessors;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import lombok.Value;

/** Classina al volo giusto per provare se becca le stringhe vuote. */
@Value
@Accessors(fluent = true)
@AllArgsConstructor
@NoArgsConstructor(force = true)
public class BodyRequest {
    @NotBlank(message = "value is required")
    String value;
}

