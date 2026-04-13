package at.qe.skeleton.background;

import at.qe.skeleton.commands.Command;
import at.qe.skeleton.commands.CommandDeque;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

@Component
public class ClientObserver {

    @EventListener
    @Async
    public void handleNewCommand(Command command) { // data
        CommandDeque.addLast(command);
    }

}
