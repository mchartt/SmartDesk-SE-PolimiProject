package it.polimi.smartdesk_backend.service.host;

import java.util.List;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import it.polimi.smartdesk_backend.dto.space.HostTechnicianCreateRequestDTO;
import it.polimi.smartdesk_backend.dto.space.HostTechnicianUpdateRequestDTO;
import it.polimi.smartdesk_backend.dto.space.TechnicianDTO;
import it.polimi.smartdesk_backend.exception.BusinessRuleException;
import it.polimi.smartdesk_backend.exception.ConflictException;
import it.polimi.smartdesk_backend.exception.NotFoundException;
import it.polimi.smartdesk_backend.mapper.TechnicianMapper;
import it.polimi.smartdesk_backend.util.message.AuthMessage;
import it.polimi.smartdesk_backend.util.message.ResourceMessage;
import it.polimi.smartdesk_backend.util.message.AdminMessage;
import it.polimi.smartdesk_backend.util.message.TicketMessage;
import it.polimi.smartdesk_backend.util.message.HttpMessage;
import it.polimi.smartdesk_backend.model.ticket.TicketStatus;
import it.polimi.smartdesk_backend.model.user.Technician;
import it.polimi.smartdesk_backend.repository.ticket.TicketRepository;
import it.polimi.smartdesk_backend.repository.user.HostRepository;
import it.polimi.smartdesk_backend.repository.user.TechnicianRepository;
import it.polimi.smartdesk_backend.repository.user.UserRepository;
import it.polimi.smartdesk_backend.util.policy.PasswordPolicy;
import lombok.RequiredArgsConstructor;

/** Account tecnico creati dall'host: registrazione, aggiornamento profilo con lock ottimistico, eliminazione. */
@Service
@RequiredArgsConstructor
public class HostTechnicianAccountService {

    private final HostRepository hostRepo;
    private final TechnicianRepository technicianRepository;
    private final TicketRepository ticketRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final TechnicianMapper technicianMapper;
    private final HostTechnicianAccessService hostTechnicianAccessService;
    private final HostTechnicianDashboardService hostTechnicianDashboardService;

    /**
     * Registra un nuovo tecnico con password hashata; email univoca a livello utente.
     *
     * @throws BusinessRuleException email già registrata
     * @throws NotFoundException host inesistente
     */
    @Transactional
    public TechnicianDTO createTechnician(Long hostID, HostTechnicianCreateRequestDTO request) {
        hostRepo.findById(hostID)
                .orElseThrow(() -> new NotFoundException(ResourceMessage.hostNotFound(hostID)));
        userRepository.findByEmail(request.getEmail().trim()).ifPresent(user -> {
            throw new BusinessRuleException(AuthMessage.EMAIL_ALREADY_REGISTERED.text());
        });

        Technician technician = new Technician();
        technician.setName(request.getName().trim());
        technician.setEmail(request.getEmail().trim());
        technician.setPassword(passwordEncoder.encode(request.getPassword()));
        technician.setSpecialization(request.getSpecialization().trim());
        technician.setCreatingHostId(hostID);
        return technicianMapper.toDto(technicianRepository.save(technician));
    }

    /**
     * Aggiorna l'anagrafica del tecnico; password opzionale se conforme a {@link PasswordPolicy}.
     *
     * @throws ConflictException {@code profileVersion} non allineata
     * @throws BusinessRuleException email in uso o password debole
     */
    @Transactional
    public TechnicianDTO updateTechnicianForHost(Long hostID, Long technicianID, HostTechnicianUpdateRequestDTO body) {
        Technician technician = loadManagedTechnician(hostID, technicianID);
        Long expected = body.getProfileVersion();
        Long current = technician.getVersion();
        long currentVal = current == null ? 0L : current;
        if (!expected.equals(currentVal)) {
            throw new ConflictException(HttpMessage.DATA_CONFLICT.text());
        }
        String email = body.getEmail() == null ? "" : body.getEmail().trim();
        if (!email.equalsIgnoreCase(technician.getEmail())) {
            userRepository.findByEmail(email).ifPresent(existing -> {
                if (!existing.getId().equals(technicianID)) {
                    throw new BusinessRuleException(AuthMessage.EMAIL_ALREADY_REGISTERED.text());
                }
            });
        }
        technician.setName(body.getName().trim());
        technician.setEmail(email);
        technician.setSpecialization(body.getSpecialization() == null ? null : body.getSpecialization().trim());
        String pw = body.getPassword();
        if (pw != null && !pw.isBlank()) {
            if (!PasswordPolicy.isStrong(pw)) {
                throw new BusinessRuleException(AdminMessage.NEW_PASSWORD_POLICY_VIOLATION.text());
            }
            technician.setPassword(passwordEncoder.encode(pw));
        }
        Technician saved = technicianRepository.save(technician);
        return hostTechnicianDashboardService.toTechnicianDTOForHostDashboard(saved, hostID);
    }

    /**
     * Elimina il tecnico dopo aver rimosso tutte le assegnazioni agli spazi; bloccato se ha ticket OPEN/IN_PROGRESS.
     *
     * @throws BusinessRuleException ticket attivi ancora aperti
     */
    @Transactional
    public void deleteTechnicianForHost(Long hostID, Long technicianID) {
        Technician technician = loadManagedTechnician(hostID, technicianID);
        boolean active = ticketRepository.existsByTechnicianIDAndStatusIn(
                technicianID, List.of(TicketStatus.OPEN.name(), TicketStatus.IN_PROGRESS.name()));
        if (active) {
            throw new BusinessRuleException(TicketMessage.TECHNICIAN_DELETE_ACTIVE_TICKETS.text());
        }
        technician.getSpaces().clear();
        technicianRepository.delete(technician);
    }

    private Technician loadManagedTechnician(Long hostID, Long technicianID) {
        Technician technician = technicianRepository.findById(technicianID)
                .orElseThrow(() -> new NotFoundException(ResourceMessage.technicianNotFound(technicianID)));
        hostTechnicianAccessService.assertTechnicianManagedByHost(hostID, technician);
        return technician;
    }
}

