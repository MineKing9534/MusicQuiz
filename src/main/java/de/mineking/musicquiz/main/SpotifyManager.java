package de.mineking.musicquiz.main;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.InstanceCreator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import se.michaelthelin.spotify.SpotifyApi;
import se.michaelthelin.spotify.SpotifyHttpManager;
import se.michaelthelin.spotify.enums.AuthorizationScope;
import se.michaelthelin.spotify.model_objects.specification.User;

import java.io.FileReader;
import java.io.FileWriter;
import java.util.function.Consumer;

public class SpotifyManager {
	public static final Logger logger = LoggerFactory.getLogger(SpotifyManager.class);
	public static final Gson gson = new GsonBuilder()
			.setPrettyPrinting()
			.create();

	public static final String path = "spotify";
	private transient final MusicQuiz bot;

	public String code;
	public String accessToken;
	public String refreshToken;
	public long expiresAt;

	private final transient SpotifyApi api;
	public transient User user;

	public SpotifyManager(MusicQuiz bot) {
		this.bot = bot;
		this.api = new SpotifyApi.Builder()
				.setClientId(bot.config.spotifyClientId)
				.setClientSecret(bot.config.spotifyClientSecret)
				.setRedirectUri(SpotifyHttpManager.makeUri("https://localhost"))
				.build();

		try(FileReader r = new FileReader(path)) {
			new GsonBuilder()
					.registerTypeAdapter(getClass(), (InstanceCreator<SpotifyManager>) type -> SpotifyManager.this)
					.create()
					.fromJson(r, SpotifyManager.class);
		} catch(Exception e) {
			logger.error("Spotify credentials error", e);
			System.exit(1);
			return;
		}

		if(code != null) {
			api.authorizationCode(code).build().executeAsync()
					.exceptionally(e -> {
						logger.error("Spotify credentials error", e);
						System.exit(1);
						return null;
					})
					.thenAccept(token -> {
						this.code = null;

						this.accessToken = token.getAccessToken();
						this.refreshToken = token.getRefreshToken();
						this.expiresAt = System.currentTimeMillis() + token.getExpiresIn() * 1000;

						save();
						finish();
					});
		}

		else {
			if(accessToken == null || refreshToken == null) {
				logger.error("No spotify oauth2 credentials found!");

				String url = api.authorizationCodeUri()
						.scope(AuthorizationScope.USER_FOLLOW_READ, AuthorizationScope.USER_FOLLOW_READ, AuthorizationScope.USER_READ_PRIVATE, AuthorizationScope.USER_READ_EMAIL)
						.build().execute().toString();
				logger.info("Authorization-URL: {}. Provide the code query param in the spotify file!", url);

				System.exit(1);
			}

			finish();
		}
	}

	private void finish() {
		api.setAccessToken(accessToken);
		api.setRefreshToken(refreshToken);

		api(api ->
				api.getCurrentUsersProfile().build().executeAsync()
						.thenAccept(user -> this.user = user)
		);
	}

	public void api(Consumer<SpotifyApi> api) {
		if(System.currentTimeMillis() > expiresAt - 5000) {
			this.api.authorizationCodeRefresh().build().executeAsync()
					.thenAccept(token -> {
						this.accessToken = token.getAccessToken();
						this.expiresAt = System.currentTimeMillis() + token.getExpiresIn() * 1000;
						this.api.setAccessToken(accessToken);

						logger.info("New spotify access token: {}; Expires in {}", accessToken, token.getExpiresIn());

						save();

						api.accept(this.api);
					});
		}

		else {
			api.accept(this.api);
		}
	}

	public void save() {
		try(FileWriter w = new FileWriter(path)) {
			gson.toJson(this, w);
		} catch(Exception e) {
			throw new RuntimeException(e);
		}
	}
}
