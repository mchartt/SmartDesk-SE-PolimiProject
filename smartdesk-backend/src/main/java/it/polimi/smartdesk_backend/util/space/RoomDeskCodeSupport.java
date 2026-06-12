package it.polimi.smartdesk_backend.util.space;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import it.polimi.smartdesk_backend.exception.BusinessRuleException;
import it.polimi.smartdesk_backend.model.space.Desk;
import it.polimi.smartdesk_backend.repository.space.DeskRepository;
import it.polimi.smartdesk_backend.util.message.SpaceMessage;

/** Genera codici postazione come {@code <codiceStanza><n>} (es. SR1, SR2) e rinumerazione al cambio codice stanza. */
public final class RoomDeskCodeSupport {

    private static final int DESK_CODE_MAX_LEN = 16;

    private RoomDeskCodeSupport() {
    }

    /**
     * Calcola il prossimo codice {@code roomCode + n} con n = max suffisso numerico esistente + 1.
     *
     * @param roomCode prefisso (es. SR)
     * @param desksInRoom desk già nella stanza; lista vuota → {@code roomCode1}
     * @return codice entro 16 caratteri
     * @throws BusinessRuleException se il candidato supera il limite lunghezza
     */
    public static String nextDeskCode(String roomCode, List<Desk> desksInRoom) {
        String prefix = roomCode;
        int max = 0;
        Pattern suffix = suffixPattern(prefix);
        for (Desk desk : desksInRoom) {
            Matcher matcher = suffix.matcher(desk.getCode());
            if (matcher.matches()) {
                max = Math.max(max, Integer.parseInt(matcher.group(1)));
            }
        }
        return assertDeskCodeLength(prefix + (max + 1));
    }

    /**
     * Rinumera tutti i desk della stanza dopo cambio {@code roomCode}.
     *
     * @param desks modificati in place; nessuna operazione se lista vuota
     * @throws BusinessRuleException se un nuovo codice collide con un desk di altro record nello stesso spazio
     */
    public static void renumberDesksInRoom(String previousRoomCode, String newRoomCode, List<Desk> desks, Long spaceId,
            DeskRepository deskRepo) {
        if (desks.isEmpty()) {
            return;
        }
        Pattern oldSuffix = suffixPattern(previousRoomCode);
        List<Desk> ordered = desks.stream()
                .sorted(Comparator
                        .comparingInt((Desk d) -> suffixIndex(oldSuffix, d.getCode()))
                        .thenComparing(Desk::getDeskID))
                .toList();

        for (int i = 0; i < ordered.size(); i++) {
            String candidate = assertDeskCodeLength(newRoomCode + (i + 1));
            Optional<Desk> conflict = deskRepo.findBySpace_SpaceIDAndCode(spaceId, candidate);
            if (conflict.isPresent() && ordered.stream().noneMatch(d -> d.getDeskID().equals(conflict.get().getDeskID()))) {
                throw new BusinessRuleException(SpaceMessage.DESK_CODE_IN_USE.text());
            }
            ordered.get(i).setCode(candidate);
        }
    }

    private static int suffixIndex(Pattern suffix, String deskCode) {
        Matcher matcher = suffix.matcher(deskCode);
        if (!matcher.matches()) {
            return Integer.MAX_VALUE;
        }
        return Integer.parseInt(matcher.group(1));
    }

    private static Pattern suffixPattern(String roomCode) {
        return Pattern.compile("^" + Pattern.quote(roomCode) + "(\\d+)$", Pattern.CASE_INSENSITIVE);
    }

    private static String assertDeskCodeLength(String code) {
        if (code.length() > DESK_CODE_MAX_LEN) {
            throw new BusinessRuleException(SpaceMessage.DESK_CODE_FORMAT.text());
        }
        return code;
    }
}
