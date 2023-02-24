package de.mineking.musicquiz.main.remote;

import com.google.gson.Gson;
import io.javalin.Javalin;
import io.javalin.json.JsonMapper;
import io.javalin.plugin.bundled.CorsPluginConfig;
import static io.javalin.apibuilder.ApiBuilder.*;

import java.lang.reflect.Type;

public class RemoteServer {
	private final static Gson gson = new Gson();

	public static void start() {
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
			ws("/gateway", new GatewayHandler());
		});
	}
}
