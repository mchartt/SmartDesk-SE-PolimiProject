package it.polimi.smartdesk_backend.service.host;


import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import it.polimi.smartdesk_backend.dto.space.HostTechnicianCreateRequestDTO;
import it.polimi.smartdesk_backend.dto.space.HostTechnicianUpdateRequestDTO;
import it.polimi.smartdesk_backend.dto.space.TechnicianDTO;
import it.polimi.smartdesk_backend.exception.BusinessRuleException;
import it.polimi.smartdesk_backend.exception.ConflictException;
import it.polimi.smartdesk_backend.mapper.TechnicianMapper;
import it.polimi.smartdesk_backend.model.user.Host;
import it.polimi.smartdesk_backend.model.user.Technician;
import it.polimi.smartdesk_backend.repository.ticket.TicketRepository;
import it.polimi.smartdesk_backend.repository.user.HostRepository;
import it.polimi.smartdesk_backend.repository.user.TechnicianRepository;
import it.polimi.smartdesk_backend.repository.user.UserRepository;

@FieldDefaults(level = AccessLevel.PRIVATE)
@ExtendWith(MockitoExtension.class)
class HostTechnicianAccountServiceTest {

    @Mock
    private HostRepository hostRepo;

    @Mock
    private TechnicianRepository technicianRepository;

    @Mock
    private TicketRepository ticketRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private TechnicianMapper technicianMapper;

    @Mock
    private HostTechnicianAccessService hostTechnicianAccessService;

    @Mock
    private HostTechnicianDashboardService hostTechnicianDashboardService;

    @InjectMocks
    private HostTechnicianAccountService hostTechnicianAccountService;

    @Test
    void createTechnicianForHost() {
        HostTechnicianCreateRequestDTO request = new HostTechnicianCreateRequestDTO(
                "Mario", "tech@sd.it", "Secret123!", "Hardware");
        Host host = new Host();
        host.setId(4L);
        Technician saved = new Technician();
        saved.setId(22L);
        TechnicianDTO dto = new TechnicianDTO();
        dto.setTechnicianID(22L);

        when(hostRepo.findById(4L)).thenReturn(Optional.of(host));
        when(userRepository.findByEmail("tech@sd.it")).thenReturn(Optional.empty());
        when(passwordEncoder.encode("Secret123!")).thenReturn("hash");
        when(technicianRepository.save(any(Technician.class))).thenReturn(saved);
        when(technicianMapper.toDto(saved)).thenReturn(dto);

        TechnicianDTO result = hostTechnicianAccountService.createTechnician(4L, request);

        assertEquals(22L, result.getTechnicianID());
        verify(technicianRepository).save(any(Technician.class));
    }

    @Test
    void createBlocksAlreadyRegisteredEmail() {
        HostTechnicianCreateRequestDTO request = new HostTechnicianCreateRequestDTO(
                "Mario", "tech@sd.it", "Secret123!", "Hardware");
        when(hostRepo.findById(4L)).thenReturn(Optional.of(new Host()));
        when(userRepository.findByEmail("tech@sd.it")).thenReturn(Optional.of(new Technician()));

        assertThrows(BusinessRuleException.class, () -> hostTechnicianAccountService.createTechnician(4L, request));

        verify(technicianRepository, never()).save(any());
    }

    @Test
    void updateBlocksOldVersion() {
        Technician technician = new Technician();
        technician.setId(22L);
        technician.setVersion(3L);
        HostTechnicianUpdateRequestDTO request = new HostTechnicianUpdateRequestDTO(
                "Mario", "tech@sd.it", "Hardware", "", 2L);
        when(technicianRepository.findById(22L)).thenReturn(Optional.of(technician));

        assertThrows(ConflictException.class,
                () -> hostTechnicianAccountService.updateTechnicianForHost(4L, 22L, request));
    }

    @Test
    void updateBlocksWeakPassword() {
        Technician technician = new Technician();
        technician.setId(22L);
        technician.setEmail("old@sd.it");
        technician.setVersion(2L);
        HostTechnicianUpdateRequestDTO request = new HostTechnicianUpdateRequestDTO(
                "Mario", "tech@sd.it", "Hardware", "debole", 2L);
        when(technicianRepository.findById(22L)).thenReturn(Optional.of(technician));
        when(userRepository.findByEmail("tech@sd.it")).thenReturn(Optional.empty());

        assertThrows(BusinessRuleException.class,
                () -> hostTechnicianAccountService.updateTechnicianForHost(4L, 22L, request));
    }

    @Test
    void deleteBlocksTechnicianWithActiveTickets() {
        Technician technician = new Technician();
        technician.setId(22L);
        when(technicianRepository.findById(22L)).thenReturn(Optional.of(technician));
        when(ticketRepository.existsByTechnicianIDAndStatusIn(eq(22L), any()))
                .thenReturn(true);

        assertThrows(BusinessRuleException.class,
                () -> hostTechnicianAccountService.deleteTechnicianForHost(4L, 22L));
    }
}
