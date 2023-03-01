package de.mineking.musicquiz.main.remote;

import com.google.gson.Gson;
import de.mineking.musicquiz.main.MusicQuiz;
import io.javalin.Javalin;
import io.javalin.apibuilder.ApiBuilder;
import io.javalin.json.JsonMapper;
import io.javalin.plugin.bundled.CorsPluginConfig;

import java.lang.reflect.Type;

public class RemoteServer {
	private final static Gson gson = new Gson();

	private final MusicQuiz bot;
	public RemoteGateway gateway;

	public RemoteServer(MusicQuiz bot) {
		this.bot = bot;

		gateway = new RemoteGateway(bot);
	}

	public void start() {
		Javalin server = Javalin.create(config -> {
			config.http.defaultContentType = "text/json";
			config.jsonMapper(new JsonMapper() {
				@Override
				public <T> T fromJsonString(String json, Type targetType) {
					return gson.fromJson(json, targetType);
				}

				@Override
				public String toJsonString(Object obj, Type type) {
					return gson.toJson(obj, type);
				}
			});

			config.plugins.enableCors(cors -> cors.add(CorsPluginConfig::anyHost));
		});

		server.start(9536);

		server.routes(() -> {
			ApiBuilder.ws("/gateway", gateway);
		});
	}
}
