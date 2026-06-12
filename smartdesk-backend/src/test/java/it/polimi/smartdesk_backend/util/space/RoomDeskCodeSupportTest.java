package it.polimi.smartdesk_backend.util.space;

import it.polimi.smartdesk_backend.util.space.RoomDeskCodeSupport;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;

import it.polimi.smartdesk_backend.model.space.Desk;
import it.polimi.smartdesk_backend.repository.space.DeskRepository;

/** Test generazione/rinumera codici desk per stanza ({@link RoomDeskCodeSupport}). */
class RoomDeskCodeSupportTest {

    @Test
    void nextDeskCode_startsAtOneWhenRoomEmpty() {
        assertEquals("SR1", RoomDeskCodeSupport.nextDeskCode("SR", List.of()));
    }

    @Test
    void nextDeskCode_incrementsFromHighestSuffix() {
        Desk d1 = desk(1L, "SR1");
        Desk d2 = desk(2L, "SR2");
        Desk d3 = desk(3L, "SR7");
        assertEquals("SR8", RoomDeskCodeSupport.nextDeskCode("SR", List.of(d1, d2, d3)));
    }

    @Test
    void renumberDesksInRoom_appliesNewPrefixInOrder() {
        Desk d1 = desk(10L, "TA3");
        Desk d2 = desk(11L, "TA1");
        Desk d3 = desk(12L, "TA2");
        List<Desk> desks = new ArrayList<>(List.of(d1, d2, d3));

        DeskRepository repo = mock(DeskRepository.class);
        when(repo.findBySpace_SpaceIDAndCode(eq(99L), any())).thenReturn(Optional.empty());

        RoomDeskCodeSupport.renumberDesksInRoom("TA", "SR", desks, 99L, repo);

        assertEquals("SR1", d2.getCode());
        assertEquals("SR2", d3.getCode());
        assertEquals("SR3", d1.getCode());
    }

    private static Desk desk(Long id, String code) {
        Desk desk = new Desk();
        desk.setDeskID(id);
        desk.setCode(code);
        return desk;
    }
}
