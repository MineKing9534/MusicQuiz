package de.mineking.musicquiz.quiz.commands.types;

import de.mineking.musicquiz.main.MusicQuiz;
import de.mineking.musicquiz.main.remote.RemoteGateway;
import de.mineking.musicquiz.quiz.Quiz;
import de.mineking.musicquiz.quiz.commands.*;
import de.mineking.musicquiz.quiz.remote.EventData;
import io.javalin.http.ForbiddenResponse;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

public class CommandHandler {
	private record CacheData(long time, Quiz quiz, long user, Future<?> task) {
	}

	public final Map<String, Command> commands = new HashMap<>();
	public final Map<String, CacheData> commandBuffer = new HashMap<>();

	public CommandHandler(MusicQuiz bot) {
		commands.put("guess", new GuessCommand(bot));

		commands.put("play", new PlayCommand(bot));
		commands.put("extend", new ExtendCommand(bot));
		commands.put("stop", new StopCommand(bot));

		commands.put("confirm", new ConfirmCommand(bot));
		commands.put("free", new FreeCommand(bot));
		commands.put("score", new ScoreCommand(bot));

		commands.put("end", new EndCommand(bot));
	}

	public void performCommand(RemoteGateway.RemoteCommand command, Quiz quiz, long user) {
		if(command.args == null) {
			command.args = new HashMap<>();
		}

		Command cmd = commands.get(command.command);

		if(cmd == null) {
			return;
		}

		if(cmd instanceof MasterCommand && user != quiz.getMaster()) {
			throw new ForbiddenResponse("This command is master-only");
		}

		if(!(cmd instanceof CachingCommand cc)) {
			cmd.performCommand(quiz, user, command.args);
		}

		else {
			command.context.sendAsClass(new EventData(EventData.Action.WAIT), EventData.class);
			CacheData current = commandBuffer.get(command.command);

			if(current == null || current.time > command.time) {
				if(current != null) {
					current.task.cancel(true);
				}

				Future<?> task = MusicQuiz.executor.schedule(() -> {
					cmd.performCommand(quiz, user, command.args);

					commandBuffer.remove(command.command);
				}, cc.getCacheDuration().toMillis(), TimeUnit.MILLISECONDS);

				commandBuffer.put(command.command, new CacheData(command.time, quiz, user, task));
			}
		}
	}
}
