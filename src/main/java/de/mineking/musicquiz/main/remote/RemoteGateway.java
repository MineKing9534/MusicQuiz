package de.mineking.musicquiz.main.remote;

import de.mineking.musicquiz.main.MusicQuiz;
import de.mineking.musicquiz.quiz.MemberData;
import de.mineking.musicquiz.quiz.Quiz;
import io.javalin.websocket.WsConfig;
import io.javalin.websocket.WsContext;
import org.eclipse.jetty.websocket.core.CloseStatus;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

public class RemoteGateway implements Consumer<WsConfig> {
	private record Command(long time, String command) {
	}

	private record CommandData(long time, Quiz quiz, long user, Future<?> task) {
	}

	private record RemoteData(long user, Quiz quiz, MemberData member) {
	}

	private final MusicQuiz bot;

	public final Map<String, Long> tokens = new HashMap<>();
	public final Map<WsContext, RemoteData> data = new ConcurrentHashMap<>();

	public final Map<String, CommandData> commandBuffer = new HashMap<>();

	public RemoteGateway(MusicQuiz bot) {
		this.bot = bot;
	}

	private void handleCommand(WsContext context, RemoteData data, Command command) {
		CommandData current = commandBuffer.get(command.command);

		if(current == null || current.time > command.time) {
			if(current != null) {
				current.task.cancel(true);
			}

			Future<?> task = MusicQuiz.executor.schedule(() -> {
				if(command.command.equals("guess")) {
					data.quiz.setGuesser(data.quiz.getChannel().getGuild().getMemberById(data.user));
					data.quiz.getMessages().updateMessages(null, null);
				}

				commandBuffer.remove(command.command);
			}, 1, TimeUnit.SECONDS);

			commandBuffer.put(command.command, new CommandData(command.time, data.quiz, data.user, task));
		}
	}

	@Override
	public void accept(WsConfig wsConfig) {
		wsConfig.onConnect(context -> {
			if(!tokens.containsKey(context.header("Sec-WebSocket-Protocol"))) {
				context.closeSession(CloseStatus.NORMAL, "Invalid authentication");

				return;
			}

			Long user = tokens.get(context.header("Sec-WebSocket-Protocol"));

			Quiz quiz = null;
			MemberData member = null;

			for(Quiz q : bot.quizzes) {
				for(var entry : q.getMembers().entrySet()) {
					if(Objects.equals(entry.getKey(), user)) {
						quiz = q;
						member = entry.getValue();

						break;
					}
				}
			}

			if(quiz == null) {
				context.closeSession(CloseStatus.NORMAL, "No Quiz for user");

				return;
			}

			if(!quiz.isStarted()) {
				context.closeSession(CloseStatus.NORMAL, "Quiz hasn't started yet!");

				return;
			}

			context.send(user);

			member.remote = context;
			quiz.sendUpdate();

			quiz.onRemoteConnect(user);

			context.enableAutomaticPings(500, TimeUnit.MILLISECONDS);

			data.put(context, new RemoteData(user, quiz, member));
		});

		wsConfig.onClose(context -> {
			if(data.containsKey(context)) {
				RemoteData temp = data.get(context);
				temp.member.remote = null;

				temp.quiz.onRemoteDisconnect(temp.user);

				data.remove(context);
			}
		});

		wsConfig.onMessage(context -> {
			if(data.containsKey(context)) {
				handleCommand(context, data.get(context), context.messageAsClass(Command.class));
			}
		});
	}
}
