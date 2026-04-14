package at.qe.skeleton.tests.background;

import at.qe.skeleton.background.DequeConsumer;
import at.qe.skeleton.commands.CommandDeque;
import at.qe.skeleton.commands.NotifyRaspberryCommand;
import at.qe.skeleton.dtos.StateChangeNotificationDTO;
import at.qe.skeleton.dtos.UpdateType;
import at.qe.skeleton.model.NotifyDeadLetter;
import at.qe.skeleton.repositories.NotifyDeadLetterRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class DequeConsumerUnitTests {

    @Mock
    private NotifyDeadLetterRepository repository;

    @InjectMocks
    private DequeConsumer dequeConsumer;
    private NotifyRaspberryCommand notifyCommand;

    @Test
    void testThatHandleFailureRequeuesCommandWhenUnderMaxAttempts() {
        try (MockedStatic<CommandDeque> deque = mockStatic(CommandDeque.class)) {
            dequeConsumer.handleFailure(notifyCommand);

            deque.verify(() -> CommandDeque.addFirst(notifyCommand));
            verify(repository, never()).save(any());
        }
    }

    @Test
    void testThatHandleFailurePersistsDeadLetterAfterMaxAttempts() {
        StateChangeNotificationDTO dto = new StateChangeNotificationDTO(UpdateType.SENSORS, LocalDateTime.now());
        notifyCommand = mock(NotifyRaspberryCommand.class);
        when(notifyCommand.getDto()).thenReturn(dto);
        try (MockedStatic<CommandDeque> deque = mockStatic(CommandDeque.class)) {
            // exhaust all attempts
            for (int i = 0; i <= DequeConsumer.MAX_ATTEMPTS; i++) {
                dequeConsumer.handleFailure(notifyCommand);
            }
            verify(repository, atLeastOnce()).save(any(NotifyDeadLetter.class));
        }
    }

    @Test
    void testThatHandleFailureResetsAttemptsAfterDeadLetter() {
        try (MockedStatic<CommandDeque> deque = mockStatic(CommandDeque.class)) {
            // exhaust attempts to trigger dead letter + reset
            for (int i = 1; i <= DequeConsumer.MAX_ATTEMPTS; i++) {
                dequeConsumer.handleFailure(notifyCommand);
            }

            // after reset, next failure should re-queue instead of persisting again
            dequeConsumer.handleFailure(notifyCommand);

            deque.verify(() -> CommandDeque.addFirst(notifyCommand), times(5));
        }
    }
}