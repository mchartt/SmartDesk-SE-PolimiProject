package it.polimi.smartdesk_backend.config.bootstrap;

import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Component;

import it.polimi.smartdesk_backend.model.space.Desk;
import it.polimi.smartdesk_backend.model.space.DeskStateCode;
import it.polimi.smartdesk_backend.model.space.Room;
import it.polimi.smartdesk_backend.model.space.Space;
import it.polimi.smartdesk_backend.model.user.Host;
import it.polimi.smartdesk_backend.repository.space.DeskRepository;
import it.polimi.smartdesk_backend.repository.space.RoomRepository;
import it.polimi.smartdesk_backend.repository.space.SpaceRepository;
import it.polimi.smartdesk_backend.repository.user.UserRepository;
import it.polimi.smartdesk_backend.support.codegen.CodeUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/** Dopo gli user: crea uffici, stanze e desk legati all'host indicato nel seed. */
@Component
@RequiredArgsConstructor
@Slf4j
public class SpaceSeeder {

    private final UserRepository users;
    private final SpaceRepository spaces;
    private final DeskRepository desks;
    private final RoomRepository rooms;

    /** Crea spazi, stanze e desk dal JSON; aggiorna solo i desk mancanti se lo spazio esiste già. */
    public void seedSpaces(List<SeedData.SpaceJson> rows) {
        if (rows == null) return;
        for (SeedData.SpaceJson row : rows) {
            var host = users.findByEmail(row.getHostEmail());
            if (host.isEmpty() || !(host.get() instanceof Host owner)) continue;

            Optional<Space> existing = spaces.findAll().stream()
                    .filter(s -> s.getName().equals(row.getName()) && s.getHostID().equals(owner.getId()))
                    .findFirst();

            if (existing.isPresent()) {
                seedDesksIfMissing(existing.get(), row.getDesks());
                continue;
            }

            Space space = new Space();
            space.setName(row.getName());
            space.setDescription(row.getDescription());
            space.setAddress(row.getAddress());
            space.setCity(row.getCity());
            space.setHostID(owner.getId());
            space.setApproved(row.isApproved());
            space.setOfficeCode(CodeUtils.allocateUniqueCode(spaces::existsByOfficeCode, 10, "OFFICE_CODE"));
            space = spaces.save(space);

            seedDesks(space, row.getDesks());
            log.info("Spazio e postazioni creati: {} per {}", row.getName(), row.getHostEmail());
        }
    }

    private void seedDesks(Space space, List<SeedData.DeskJson> rows) {
        seedDesksIfMissing(space, rows);
    }

    private int seedDesksIfMissing(Space space, List<SeedData.DeskJson> rows) {
        if (rows == null) return 0;
        int added = 0;
        for (SeedData.DeskJson row : rows) {
            if (desks.findBySpace_SpaceIDAndCode(space.getSpaceID(), row.getCode()).isPresent()) continue;
            desks.save(createDeskFromJson(space, row));
            added++;
        }
        return added;
    }

    private Desk createDeskFromJson(Space space, SeedData.DeskJson row) {
        Room room = resolveRoom(space, row);
        Desk desk = new Desk();
        desk.setBuilding(room.getName());
        desk.setRoom(room);
        desk.setCode(row.getCode());
        desk.setStateCode(parseDeskState(row.getStateCode()));
        desk.setPricePerHour(row.getPricePerHour());
        desk.setSpace(space);
        desk.setAmenities(row.getAmenities());
        return desk;
    }

    private Room resolveRoom(Space space, SeedData.DeskJson row) {
        String roomName = row.getBuilding() == null || row.getBuilding().isBlank() ? "Ambiente generale" : row.getBuilding().trim();
        for (Room room : rooms.findBySpace_SpaceIDOrderByNameAsc(space.getSpaceID())) {
            if (roomName.equals(room.getName())) return room;
        }
        Room room = new Room();
        room.setSpace(space);
        room.setName(roomName);
        String preferred = row.getRoomCode() != null ? row.getRoomCode().trim().toUpperCase() : null;
        String base = preferred != null && preferred.matches("^[A-Z0-9]{2,10}$") ? preferred : deriveRoomBaseCode(roomName);
        room.setCode(allocRoomCode(space.getSpaceID(), base));
        return rooms.save(room);
    }

    private String deriveRoomBaseCode(String roomName) {
        String alnum = roomName.replaceAll("[^A-Za-z0-9]", "").toUpperCase();
        return alnum.isEmpty() ? "RM" : alnum.substring(0, Math.min(6, alnum.length()));
    }

    private String allocRoomCode(Long spaceId, String base) {
        String cleanBase = base.length() > 10 ? base.substring(0, 10) : base;
        String candidate = cleanBase;
        int i = 2;
        while (rooms.existsBySpace_SpaceIDAndCode(spaceId, candidate)) {
            String suffix = String.valueOf(i++);
            candidate = cleanBase.substring(0, Math.min(cleanBase.length(), Math.max(1, 10 - suffix.length()))) + suffix;
        }
        return candidate;
    }

    private DeskStateCode parseDeskState(String raw) {
        if (raw == null || raw.isBlank()) return DeskStateCode.AVAILABLE;
        try {
            return DeskStateCode.valueOf(raw.trim().toUpperCase());
        } catch (IllegalArgumentException ex) {
            return DeskStateCode.AVAILABLE;
        }
    }
}
