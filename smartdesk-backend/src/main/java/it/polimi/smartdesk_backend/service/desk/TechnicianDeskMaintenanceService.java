package it.polimi.smartdesk_backend.service.desk;

import it.polimi.smartdesk_backend.exception.NotFoundException;
import it.polimi.smartdesk_backend.model.space.Desk;
import it.polimi.smartdesk_backend.service.desk.state.DeskStateMachine;
import it.polimi.smartdesk_backend.repository.space.DeskRepository;
import it.polimi.smartdesk_backend.service.ticket.TechnicianAssignmentService;
import it.polimi.smartdesk_backend.util.message.ResourceMessage;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Transizione desk AVAILABLE↔MAINTENANCE per tecnico assegnato allo spazio del desk. Separato da TechnicianAssignmentService per allineamento architetturale e documentazione. */
@Service
@RequiredArgsConstructor
public class TechnicianDeskMaintenanceService {

    private final DeskRepository deskRepository;
    private final DeskStateMachine deskStateMachine;
    private final TechnicianAssignmentService technicianAssignmentService;

    /**
     * AVAILABLE → MAINTENANCE: il tecnico avvia la riparazione del desk.
     * Lock pessimistico per evitare race con prenotazioni concorrenti.
     *
     * @param technicianUserId ID del tecnico (deve essere assegnato allo spazio del desk)
     * @param deskId ID del desk da mettere in manutenzione
     */
    @Transactional
    public void setDeskMaintenanceForTechnician(Long technicianUserId, Long deskId) {
        technicianAssignmentService.assertDeskAssignedToTechnician(technicianUserId, deskId);
        Desk desk = deskRepository.lockByDeskIdForUpdate(deskId)
                .orElseThrow(() -> new NotFoundException(ResourceMessage.deskNotFound(deskId)));
        deskStateMachine.markMaintenance(desk);
        deskRepository.save(desk);
    }

    /**
     * MAINTENANCE → PENDING_INSPECTION: intervento completato, il desk attende verifica host.
     *
     * @param technicianUserId ID del tecnico
     * @param deskId ID del desk da sbloccare
     */
    @Transactional
    public void revertDeskMaintenanceForTechnician(Long technicianUserId, Long deskId) {
        technicianAssignmentService.assertDeskAssignedToTechnician(technicianUserId, deskId);
        Desk desk = deskRepository.lockByDeskIdForUpdate(deskId)
                .orElseThrow(() -> new NotFoundException(ResourceMessage.deskNotFound(deskId)));
        deskStateMachine.makeAvailable(desk);
        deskRepository.save(desk);
    }
}

