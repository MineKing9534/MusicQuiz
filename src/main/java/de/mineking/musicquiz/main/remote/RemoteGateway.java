package de.mineking.musicquiz.main.remote;

import de.mineking.musicquiz.main.MusicQuiz;
import de.mineking.musicquiz.quiz.remote.EventData;
import io.javalin.http.HttpResponseException;
import io.javalin.websocket.WsConfig;
import io.javalin.websocket.WsContext;
import io.javalin.websocket.WsMessageContext;
import org.eclipse.jetty.websocket.core.CloseStatus;

import java.util.HashMap;
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

	public static class UserData {
		public final long user;

		public MusicQuiz.QuizData quiz;

		public UserData(long user, MusicQuiz.QuizData quiz) {
			this.user = user;
			this.quiz = quiz;
		}
	}

	private final MusicQuiz bot;

	public final Map<String, Long> tokens = new HashMap<>();
	public final Map<WsContext, UserData> data = new ConcurrentHashMap<>();

	public RemoteGateway(MusicQuiz bot) {
		this.bot = bot;
	}

	@Override
	public void accept(WsConfig wsConfig) {
		wsConfig.onConnect(context -> {
			if(!tokens.containsKey(context.header("Sec-WebSocket-Protocol"))) {
				context.closeSession(CloseStatus.NORMAL, "Invalid authentication");

				return;
			}

			Long user = tokens.get(context.header("Sec-WebSocket-Protocol"));

			if(user == null) {
				context.closeSession(CloseStatus.NORMAL, "Invalid authentication");

				return;
			}

			MusicQuiz.QuizData quiz = bot.getQuizByUser(user, false);

			context.sendAsClass(new EventData(EventData.Action.LOGIN).put("id", String.valueOf(user)).put("quiz", quiz != null), EventData.class);

			if(quiz != null) {
				quiz.member().remote = context;

				quiz.quiz().sendUpdate();
			}

			context.enableAutomaticPings(500, TimeUnit.MILLISECONDS);
			data.put(context, new UserData(user, quiz));
		});

		wsConfig.onClose(context -> {
			if(data.containsKey(context)) {
				UserData temp = data.get(context);

				if(temp.quiz != null) {
					temp.quiz.member().remote = null;
				}

				data.remove(context);
			}
		});

		wsConfig.onMessage(context -> {
			if(data.containsKey(context)) {
				UserData user = data.get(context);

				RemoteCommand cmd = context.messageAsClass(RemoteCommand.class);
				cmd.context = context;

				try {
					bot.commands.performCommand(
							cmd,
							user.quiz.quiz(),
							user.user
					);
				} catch(HttpResponseException e) {
					context.sendAsClass(new EventData(EventData.Action.ERROR).put("message", e.getMessage()).put("status", e.getStatus()), EventData.class);
				}
			}
		});
	}
}
