package de.mineking.musicquiz.main.remote;

import de.mineking.musicquiz.main.MusicQuiz;
import de.mineking.musicquiz.quiz.Quiz;
import de.mineking.musicquiz.quiz.remote.EventData;
import de.mineking.musicquiz.quiz.remote.QuizData;
import io.javalin.http.HttpResponseException;
import io.javalin.websocket.WsConfig;
import io.javalin.websocket.WsContext;
import io.javalin.websocket.WsMessageContext;
import org.eclipse.jetty.websocket.core.CloseStatus;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

public class RemoteGateway implements Consumer<WsConfig> {
	public static class RemoteCommand {
		public long time;
		public String command;
		public Map<String, Object> args;

		public transient WsMessageContext context;
	}

	private final MusicQuiz bot;

	public final Map<String, Long> tokens = new HashMap<>();
	public final Map<WsContext, Long> users = new ConcurrentHashMap<>();

	public RemoteGateway(MusicQuiz bot) {
		this.bot = bot;
	}

	public List<WsContext> getUserConnections(long user) {
		List<WsContext> result = new ArrayList<>();

		users.forEach((context, u) -> {
			if(u == user) {
				result.add(context);
			}
		});

		return result;
	}

	@Override
	public void accept(WsConfig wsConfig) {
		wsConfig.onMessage(context -> {
			if(!users.containsKey(context)) {
				Long user = tokens.get(context.message());

				if(user == null) {
					context.closeSession(CloseStatus.NORMAL, "Invalid authentication");

					return;
				}

				Quiz quiz = bot.getQuizByUser(user, false);
				context.send(new EventData(EventData.Action.LOGIN).put("id", String.valueOf(user)).put("quiz", quiz != null));

				if(quiz != null) {
					context.send(new QuizData(quiz, quiz.getMaster() == user));
				}

				context.enableAutomaticPings(500, TimeUnit.MILLISECONDS);
				users.put(context, user);
			}

			else {
				long user = users.get(context);

				RemoteCommand cmd = context.messageAsClass(RemoteCommand.class);
				cmd.context = context;

				try {
					bot.commands.performCommand(
							cmd,
							bot.getQuizByUser(user, false),
							user
					);
				} catch(HttpResponseException e) {
					context.send(new EventData(EventData.Action.ERROR).put("message", e.getMessage()).put("status", e.getStatus()));
				}
			}
		});

		wsConfig.onClose(users::remove);
	}
}
