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
public class DequeConsumerUnitTests {

    @Mock
    RaspberryPiRepository piRepository;

    @InjectMocks
    private DequeConsumer dequeConsumer;

    private NotifyRaspberryCommand notifyCommand;

    @BeforeEach
    void setUp() {
        notifyCommand = mock(NotifyRaspberryCommand.class);
    }

    @Test
    void testThatHandleFailureRequeuesCommandWhenUnderMaxAttempts() {
        try (MockedStatic<CommandDeque> deque = mockStatic(CommandDeque.class)) {
            dequeConsumer.handleFailure(notifyCommand);

            deque.verify(() -> CommandDeque.addFirst(notifyCommand));
            verify(piRepository, never()).save(any());
        }
    }

    @Test
    void testThatHandleFailureMarksRaspberryOfflineAfterMaxAttempts() {
        RaspberryPi pi = new RaspberryPi();
        when(notifyCommand.getRaspberry()).thenReturn(pi);

        try (MockedStatic<CommandDeque> deque = mockStatic(CommandDeque.class)) {
            // first MAX_ATTEMPTS calls re-queue, the next one triggers offline
            for (int i = 0; i <= DequeConsumer.MAX_ATTEMPTS; i++) {
                dequeConsumer.handleFailure(notifyCommand);
            }

            verify(piRepository, atLeastOnce()).save(pi);
            assertEquals(DeviceStatus.OFFLINE, pi.getStatus());
        }
    }

    @Test
    void testThatHandleFailureResetsAttemptsAfterMaxAttempts() {
        RaspberryPi pi = new RaspberryPi();
        when(notifyCommand.getRaspberry()).thenReturn(pi);

        try (MockedStatic<CommandDeque> deque = mockStatic(CommandDeque.class)) {
            // exhaust attempts to trigger offline + reset
            for (int i = 0; i <= DequeConsumer.MAX_ATTEMPTS; i++) {
                dequeConsumer.handleFailure(notifyCommand);
            }

            // after reset, next failure should re-queue again, not persist
            dequeConsumer.handleFailure(notifyCommand);

            // MAX_ATTEMPTS re-queues before offline, plus 1 after reset
            deque.verify(() -> CommandDeque.addFirst(notifyCommand), times(DequeConsumer.MAX_ATTEMPTS + 1));
        }
    }
}