package at.qe.skeleton.tests.background;

import at.qe.skeleton.background.DequeConsumer;
import at.qe.skeleton.commands.CommandDeque;
import at.qe.skeleton.commands.NotifyRaspberryCommand;
import at.qe.skeleton.model.DeviceStatus;
import at.qe.skeleton.model.RaspberryPi;
import at.qe.skeleton.repositories.RaspberryPiRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DequeConsumerUnitTests {

    @Mock
    RaspberryPiRepository piRepository;
    @Mock
    private NotifyRaspberryCommand notifyCommand;
    @InjectMocks
    private DequeConsumer dequeConsumer;

    @BeforeEach
    void setUp() {
        when(notifyCommand.getAttempts()).thenReturn(0);
    }

    @Test
    void testThatHandleFailureRequeuesCommandWhenUnderMaxAttempts() {
        when(notifyCommand.getAttempts()).thenReturn(0); // under MAX_ATTEMPTS

        try (MockedStatic<CommandDeque> deque = mockStatic(CommandDeque.class)) {
            dequeConsumer.handleFailure(notifyCommand, new RaspberryPi());

            deque.verify(() -> CommandDeque.addFirst(notifyCommand));
            verify(piRepository, never()).save(any());
        }
    }

    @Test
    void testThatHandleFailureMarksRaspberryOfflineAfterMaxAttempts() {
        when(notifyCommand.getAttempts()).thenReturn(DequeConsumer.MAX_ATTEMPTS); // at limit

        RaspberryPi pi = RaspberryPi.builder()
                .status(DeviceStatus.ONLINE)
                .ip("localhost")
                .port(8000)
                .build();

        try (MockedStatic<CommandDeque> deque = mockStatic(CommandDeque.class)) {
            dequeConsumer.handleFailure(notifyCommand, pi);

            verify(piRepository).save(pi);
            assertEquals(DeviceStatus.OFFLINE, pi.getStatus());
            deque.verify(() -> CommandDeque.addFirst(any()), never());
        }
    }

    @Test
    void testThatHandleFailureResetsAttemptsAfterMaxAttempts() {
        when(notifyCommand.getAttempts()).thenReturn(DequeConsumer.MAX_ATTEMPTS); // at limit

        RaspberryPi pi = RaspberryPi.builder()
                .status(DeviceStatus.ONLINE)
                .ip("localhost")
                .port(8000)
                .build();

        try (MockedStatic<CommandDeque> deque = mockStatic(CommandDeque.class)) {
            dequeConsumer.handleFailure(notifyCommand, pi);

            verify(notifyCommand).resetAttempts();
        }
    }

    @Test
    void testThatHandleFailureDoesNotSetOfflineWhenAlreadyOffline() {
        when(notifyCommand.getAttempts()).thenReturn(DequeConsumer.MAX_ATTEMPTS);

        RaspberryPi pi = RaspberryPi.builder()
                .status(DeviceStatus.OFFLINE)
                .ip("localhost")
                .port(8000)
                .build();

        try (MockedStatic<CommandDeque> deque = mockStatic(CommandDeque.class)) {
            dequeConsumer.handleFailure(notifyCommand, pi);

            verify(piRepository, never()).save(any());
        }
    }
}