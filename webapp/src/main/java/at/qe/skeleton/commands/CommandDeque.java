package at.qe.skeleton.commands;

import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.util.concurrent.BlockingDeque;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.TimeUnit;

/**
 * Singleton-scoped, thread-safe deque that acts as the command queue between
 * {@link at.qe.skeleton.background.ClientObserver} (producers) and
 * {@link at.qe.skeleton.background.DequeConsumer} (consumer). All methods are
 * static so callers do not need to inject the bean.
 */
@Component
@Scope("singleton")
public class CommandDeque {

    private static final BlockingDeque<Command> commands = new LinkedBlockingDeque<>();

    /**
     * Appends the given command to the tail of the deque (normal enqueue path).
     *
     * @param command the command to enqueue
     */
    public static void addLast(Command command) {
        commands.addLast(command);
    }

    /**
     * Inserts the given command at the head of the deque, giving it priority over
     * already-queued commands. Used by {@link at.qe.skeleton.background.DequeConsumer}
     * to re-queue a failed command for immediate retry.
     *
     * @param command the command to prioritize
     */
    public static void addFirst(Command command) {
        commands.addFirst(command);
    }

    /**
     * Retrieves and removes the head of the deque, waiting up to 1 second for an
     * element to become available.
     *
     * @return the head command, or {@code null} if the timeout elapses before one arrives
     * @throws InterruptedException if the waiting thread is interrupted
     */
    public static Command getFirst() throws InterruptedException {
        return commands.pollFirst(1, TimeUnit.SECONDS);
    }
}