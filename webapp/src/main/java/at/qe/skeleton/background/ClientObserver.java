package at.qe.skeleton.background;

import at.qe.skeleton.commands.Command;
import at.qe.skeleton.commands.CommandDeque;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

/**
 * Spring event listener that bridges the application event bus and the command queue.
 * Whenever a {@link Command} is published as an application event, this observer
 * appends it to the tail of {@link CommandDeque} for asynchronous processing by
 * {@link DequeConsumer}.
 */
@Component
public class ClientObserver {

    /**
     * Receives a published {@link Command} event and enqueues it at the end of the
     * command deque. Executed asynchronously so the publishing thread is not blocked.
     *
     * @param command the command event to enqueue
     */
    @EventListener
    @Async
    public void handleNewCommand(Command command) {
        CommandDeque.addLast(command);
    }

}