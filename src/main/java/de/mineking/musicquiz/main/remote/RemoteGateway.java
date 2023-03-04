package de.mineking.musicquiz.main.remote;

import de.mineking.musicquiz.main.MusicQuiz;
import de.mineking.musicquiz.quiz.MemberData;
import de.mineking.musicquiz.quiz.Quiz;
import de.mineking.musicquiz.quiz.remote.EventData;
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

	public static class UserData {
		public final long user;

		public Quiz quiz;
		public MemberData member;

		public UserData(long user, Quiz quiz, MemberData member) {
			this.user = user;
			this.quiz = quiz;
			this.member = member;
		}
	}

	private final MusicQuiz bot;

	public final Map<String, Long> tokens = new HashMap<>();
	public final Map<WsContext, UserData> data = new ConcurrentHashMap<>();

	public final Map<String, CommandData> commandBuffer = new HashMap<>();

	public RemoteGateway(MusicQuiz bot) {
		this.bot = bot;
	}

	private void handleCommand(WsContext context, UserData data, Command command) {
		context.sendAsClass(new EventData(EventData.Action.WAIT), EventData.class);

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

			if(quiz != null && quiz.isStarted()) {
				member.remote = context;

				quiz.sendUpdate();
				quiz.onRemoteConnect(user);
			}

			context.sendAsClass(new EventData(EventData.Action.LOGIN).put("id", String.valueOf(user)).put("quiz", quiz != null), EventData.class);

			context.enableAutomaticPings(500, TimeUnit.MILLISECONDS);
			data.put(context, new UserData(user, quiz, member));
		});

		wsConfig.onClose(context -> {
			if(data.containsKey(context)) {
				UserData temp = data.get(context);

				if(temp.quiz != null) {
					temp.member.remote = null;
					temp.quiz.onRemoteDisconnect(temp.user);
				}

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
