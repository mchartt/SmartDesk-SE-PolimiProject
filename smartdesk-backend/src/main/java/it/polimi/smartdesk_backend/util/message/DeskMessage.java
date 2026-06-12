package it.polimi.smartdesk_backend.util.message;

import lombok.RequiredArgsConstructor;

/** Transizioni stato postazione: manutenzione, riserva, ispezione, dismissione. */
@RequiredArgsConstructor
public enum DeskMessage {
    DESK_TEMPORARILY_RESERVED("La postazione è temporaneamente riservata."),
    DESK_NOT_IN_MAINTENANCE("Il desk non è in manutenzione: impossibile rimuovere uno stato non attivo."),
    DESK_ALREADY_IN_MAINTENANCE("Il desk è già in manutenzione."),
    DESK_PENDING_INSPECTION("La postazione è in attesa di ispezione dopo manutenzione."),
    DESK_NOT_IN_MAINTENANCE_SHORT("La postazione non è in manutenzione."),
    DESK_DECOMMISSIONED_NOT_BOOKABLE("La postazione è dismessa e non è più prenotabile."),
    DESK_DECOMMISSION_CANNOT_MAINTENANCE("Una postazione dismessa non può essere messa in manutenzione."),
    DESK_DECOMMISSION_CANNOT_RESTORE("Una postazione dismessa non può essere ripristinata."),
    DESK_ALREADY_DECOMMISSIONED("La postazione è già dismessa."),
    DESK_OPERATION_NOT_ALLOWED("Operazione non consentita nello stato corrente."),
    DESK_DECOMMISSION_ONLY_AVAILABLE_OR_INSPECTION(
            "Solo una postazione disponibile o in attesa di ispezione può essere dismessa.");

    private final String text;

    public String text() {
        return text;
    }
}
