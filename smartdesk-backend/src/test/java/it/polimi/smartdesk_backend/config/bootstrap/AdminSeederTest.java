package it.polimi.smartdesk_backend.config.bootstrap;


import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import it.polimi.smartdesk_backend.model.admin.SysAdmin;
import it.polimi.smartdesk_backend.model.user.User;
import it.polimi.smartdesk_backend.model.user.Worker;
import it.polimi.smartdesk_backend.repository.user.UserRepository;

/** Test unitari su {@link AdminSeeder}: creazione admin iniziale e assenza di duplicati. */
@FieldDefaults(level = AccessLevel.PRIVATE)
@ExtendWith(MockitoExtension.class)
class AdminSeederTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    private AdminSeeder seeder(String email, String password) {
        AdminSeeder seeder = new AdminSeeder(userRepository, passwordEncoder);
        ReflectionTestUtils.setField(seeder, "adminSeedEmail", email);
        ReflectionTestUtils.setField(seeder, "adminSeedPassword", password);
        return seeder;
    }

    @Test
    void seedAdminIfConfigured() throws Exception {
        lenient().when(userRepository.findAll()).thenReturn(Collections.emptyList());
        lenient().when(userRepository.findByEmail(any())).thenReturn(Optional.empty());
        lenient().when(passwordEncoder.encode("AdminPass1!")).thenReturn("hashed");
        lenient().when(userRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        seeder("admin@test.local", "AdminPass1!").run();

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(captor.capture());

        User saved = captor.getValue();
        assertEquals("admin@test.local", saved.getEmail());
        assertEquals("hashed", saved.getPassword());
        assertEquals(SysAdmin.class, saved.getClass());
    }

    @Test
    void seedAdminSkipIfExists() throws Exception {
        SysAdmin existing = new SysAdmin();
        existing.setEmail("admin@existing.local");

        when(userRepository.findAll()).thenReturn(List.of(existing));

        seeder("admin@new.local", "SomePass1!").run();

        verify(userRepository, never()).save(any());
    }

    @Test
    void seedAdminSkipEmailEmpty() throws Exception {
        lenient().when(userRepository.findAll()).thenReturn(Collections.emptyList());

        seeder("", "SomePass1!").run();

        verify(userRepository, never()).save(any());
    }

    @Test
    void seedAdminSkipPasswordEmpty() throws Exception {
        lenient().when(userRepository.findAll()).thenReturn(Collections.emptyList());

        seeder("admin@test.local", "").run();

        verify(userRepository, never()).save(any());
    }

    @Test
    void seedAdminSkipOnlyWorker() throws Exception {
        Worker worker = new Worker();
        worker.setEmail("worker@test.local");

        lenient().when(userRepository.findAll()).thenReturn(List.of(worker));
        lenient().when(userRepository.findByEmail(any())).thenReturn(Optional.empty());
        lenient().when(passwordEncoder.encode("AdminPass1!")).thenReturn("hashed");
        lenient().when(userRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        seeder("admin@test.local", "AdminPass1!").run();

        verify(userRepository, times(1)).save(any(SysAdmin.class));
    }

    @Test
    void seedAdminSkipEmailNotAdmin() throws Exception {
        Worker worker = new Worker();
        worker.setEmail("admin@test.local");

        when(userRepository.findAll()).thenReturn(List.of(worker));
        when(userRepository.findByEmail("admin@test.local")).thenReturn(Optional.of(worker));

        seeder("admin@test.local", "AdminPass1!").run();

        verify(userRepository, never()).save(any());
    }
}
