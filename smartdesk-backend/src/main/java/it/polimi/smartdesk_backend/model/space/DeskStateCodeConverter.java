package it.polimi.smartdesk_backend.model.space;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

/** JPA {@link DeskStateCode} ↔ colonna stringa. Mappatura diretta tra costante enum e valore stringa nel DB. Default a {@link DeskStateCode#AVAILABLE} per valori null o non riconosciuti. */
@Converter(autoApply = true)
public class DeskStateCodeConverter implements AttributeConverter<DeskStateCode, String> {

    /** Enum → stringa colonna; null diventa AVAILABLE. */
    @Override
    public String convertToDatabaseColumn(DeskStateCode attribute) {
        return attribute == null ? DeskStateCode.AVAILABLE.name() : attribute.name();
    }

    /** Stringa colonna → enum; valori vuoti o sconosciuti → AVAILABLE. */
    @Override
    public DeskStateCode convertToEntityAttribute(String dbData) {
        if (dbData == null || dbData.isBlank()) {
            return DeskStateCode.AVAILABLE;
        }
        try {
            return DeskStateCode.valueOf(dbData.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            return DeskStateCode.AVAILABLE;
        }
    }
}
