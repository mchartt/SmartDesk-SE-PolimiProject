package it.polimi.smartdesk_backend.dto.ticket;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Payload per aggiungere un commento worker su un ticket esistente. */
@Data
@NoArgsConstructor
public class TicketCommentRequestDTO {

    public static final String BODY_REQUIRED = "Il commento non può essere vuoto.";
    public static final String BODY_TOO_LONG = "Il commento non può superare {max} caratteri.";

    @NotBlank(message = BODY_REQUIRED)
    @Size(max = TicketDTO.DESCRIPTION_MAX_LENGTH, message = BODY_TOO_LONG)
    private String body;
}
