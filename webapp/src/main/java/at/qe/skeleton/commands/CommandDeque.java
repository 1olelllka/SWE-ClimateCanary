package at.qe.skeleton.commands;

import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.util.concurrent.BlockingDeque;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.TimeUnit;

@Component
@Scope("singleton")
public class CommandDeque {

    private static final BlockingDeque<Command> commands = new LinkedBlockingDeque<>();

    public static void addLast(Command command) {
        commands.addLast(command);
    }

    public static void addFirst(Command command) {
        commands.addFirst(command);
    }

    public static Command getFirst() throws InterruptedException {
        return commands.pollFirst(1, TimeUnit.SECONDS);
    }
}
