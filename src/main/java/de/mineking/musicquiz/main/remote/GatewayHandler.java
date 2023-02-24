package de.mineking.musicquiz.main.remote;

import de.mineking.musicquiz.main.Main;
import de.mineking.musicquiz.quiz.Quiz;
import io.javalin.websocket.WsConfig;
import io.javalin.websocket.WsContext;
import net.dv8tion.jda.api.entities.User;
import org.eclipse.jetty.websocket.core.CloseStatus;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

public class GatewayHandler implements Consumer<WsConfig> {
	private record Command(long time, String command) {
	}

	private record CommandData(long time, Quiz quiz, User user, Future<?> task) {
	}

	private static class RemoteData {
		public final User user;
		public final Quiz quiz;
		public final Quiz.MemberData member;

		public RemoteData(User user, Quiz quiz, Quiz.MemberData member) {
			this.user = user;
			this.quiz = quiz;
			this.member = member;
		}
	}

	public final static Map<String, User> tokens = new HashMap<>();
	public final static Map<WsContext, RemoteData> data = new ConcurrentHashMap<>();

	public final static Map<String, CommandData> commandBuffer = new HashMap<>();

	private static void handleCommand(WsContext context, RemoteData data, Command command) {
		CommandData current = commandBuffer.get(command.command);

		if(current == null || current.time > command.time) {
			if(current != null) {
				current.task.cancel(true);
			}

			Future<?> task = Main.executor.schedule(() -> {
				switch(command.command) {
					case "guess":
						data.quiz.setGuesser(data.quiz.getMaster().getGuild().getMember(data.user));
						data.quiz.updateMessages(null, null);

						break;
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

			User user = tokens.get(context.header("Sec-WebSocket-Protocol"));

			Quiz quiz = null;
			Quiz.MemberData member = null;

			for(Quiz q : Main.quizzes) {
				for(var entry : q.getMembers().entrySet()) {
					if(entry.getKey().getUser().equals(user)) {
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

			context.send(user.getIdLong());

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
