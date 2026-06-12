package it.polimi.smartdesk_backend.mapper;

import it.polimi.smartdesk_backend.dto.space.DeskDTO;
import it.polimi.smartdesk_backend.model.space.Desk;
import it.polimi.smartdesk_backend.model.space.DeskStateCode;

/** Converte le postazioni nelle viste desk per catalogo, ricerca e gestione host. */
public final class DeskMapper {

    private DeskMapper() {
    }

    /** Prepara il desk per le API: se manca l'edificio lo prende dalla stanza. */
    public static DeskDTO fromDesk(Desk desk) {
        String building = desk.getBuilding();
        if ((building == null || building.isBlank()) && desk.getRoom() != null) {
            building = desk.getRoom().getName();
        }
        if (building == null) {
            building = "";
        }
        DeskDTO dto = new DeskDTO(desk.getDeskID(), desk.getCode(), building, desk.getAmenities());
        dto.setCurrentState(desk.getStateCode().name());
        dto.setPricePerHour(desk.getPricePerHour());
        dto.setBookable(desk.getStateCode() != DeskStateCode.MAINTENANCE);
        if (desk.getSpace() != null) {
            dto.setSpaceID(desk.getSpace().getSpaceID());
        }
        if (desk.getRoom() != null) {
            dto.setRoomID(desk.getRoom().getRoomID());
            dto.setRoomName(desk.getRoom().getName());
            dto.setRoomCode(desk.getRoom().getCode());
        }
        return dto;
    }

    /** Variante per i risultati di ricerca: aggiunge spazio, media voti e stato/prenotabilità calcolati. */
    public static DeskDTO searchResult(Desk desk, Long spaceId, Double spaceAverageRating,
                                      DeskStateCode currentState, boolean bookable) {
        DeskDTO dto = fromDesk(desk);
        dto.setSpaceID(spaceId);
        dto.setSpaceAverageRating(spaceAverageRating);
        dto.setCurrentState(currentState.name());
        dto.setBookable(bookable);
        return dto;
    }
}
