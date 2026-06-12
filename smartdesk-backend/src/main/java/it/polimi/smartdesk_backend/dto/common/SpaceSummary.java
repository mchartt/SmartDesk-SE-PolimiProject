package it.polimi.smartdesk_backend.dto.common;

import lombok.experimental.Accessors;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import lombok.Value;

/** Riepilogo spazio minimale per contesto ticket. */
@Value
@Accessors(fluent = true)
@AllArgsConstructor
@NoArgsConstructor(force = true)
public class SpaceSummary {
    String name;
    String officeCode;
}

