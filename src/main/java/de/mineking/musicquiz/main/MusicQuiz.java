package de.mineking.musicquiz.main;

import com.github.topisenpai.lavasrc.spotify.SpotifySourceManager;
import com.sedmelluq.discord.lavaplayer.player.AudioPlayerManager;
import com.sedmelluq.discord.lavaplayer.player.DefaultAudioPlayerManager;
import com.sedmelluq.discord.lavaplayer.source.http.HttpAudioSourceManager;
import com.sedmelluq.discord.lavaplayer.source.youtube.YoutubeAudioSourceManager;
import de.mineking.discord.commands.CommandManager;
import de.mineking.discord.commands.CommandManagerBuilder;
import de.mineking.discord.localization.DefaultLocalizationMapper;
import de.mineking.musicquiz.commands.*;
import de.mineking.musicquiz.main.remote.RemoteServer;
import de.mineking.musicquiz.quiz.Quiz;
import de.mineking.musicquiz.quiz.commands.types.CommandHandler;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.OnlineStatus;
import net.dv8tion.jda.api.entities.Activity;
import net.dv8tion.jda.api.interactions.DiscordLocale;
import net.dv8tion.jda.api.requests.GatewayIntent;
import net.dv8tion.jda.api.utils.MemberCachePolicy;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.ResourceBundle;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

public class MusicQuiz {
	public final static ScheduledExecutorService executor = Executors.newScheduledThreadPool(30);

	public final Config config;

	public final List<Quiz> quizzes = new ArrayList<>();

	public final ResourceBundle commandInfo = ResourceBundle.getBundle("CommandInfo");

	public JDA jda;
	public CommandManager cmdMan;
	public final AudioPlayerManager audioPlayerManager = new DefaultAudioPlayerManager();

	public final RemoteServer server = new RemoteServer(this);
	public final CommandHandler commands = new CommandHandler(this);

	public final SpotifyManager spotify;

	public static void main(String[] args) throws Exception {
		new MusicQuiz(args.length == 1 ? args[0] : "config");
	}

	public MusicQuiz(String config) throws Exception {
		this.config = Config.readFromFile(config);
		this.spotify = new SpotifyManager(this);

		cmdMan = CommandManagerBuilder.createDefault()
				.setAutoUpdate(false)
				.registerGlobalCommand("create", new CreateCommand(this))
				.registerGlobalCommand("save", new SaveCommand(this))
				.registerGlobalCommand("login", new LoginCommand(this))
				.registerGlobalCommand("volume", new VolumeCommand(this))
				.registerGlobalCommand("generate", new GenerateCommand(this))
				.setLocaleMapper(
						new DefaultLocalizationMapper(
								Collections.singletonList(DiscordLocale.GERMAN), (lang, key) ->
								commandInfo.getString(key)
						)
				)
				.build();

		jda = JDABuilder.createDefault(this.config.token)
				.enableIntents(GatewayIntent.GUILD_MEMBERS)
				.setStatus(OnlineStatus.ONLINE)
				.setActivity(Activity.playing("MusicQuiz"))
				.setMemberCachePolicy(MemberCachePolicy.ALL)
				.addEventListeners(cmdMan)
				.build()
				.awaitReady();

		audioPlayerManager.registerSourceManager(new YoutubeAudioSourceManager());
		audioPlayerManager.registerSourceManager(
				new SpotifySourceManager(
						new String[]{
								"ytmsearch:\"%ISRC%\"",
								"ytsearch:\"%ISRC%\"",
								"ytmsearch:%QUERY%",
								"ytsearch:%QUERY%"
						},
						this.config.spotifyClientId,
						this.config.spotifyClientSecret,
						"US",
						audioPlayerManager
				)
		);
		audioPlayerManager.registerSourceManager(new HttpAudioSourceManager());

		server.start();
	}

	public Quiz getQuizByUser(long user, boolean master) {
		for(Quiz quiz : quizzes) {
			if(master && user != quiz.getMaster()) {
				continue;
			}

			for(var entry : quiz.getMembers().entrySet()) {
				if(entry.getKey() == user) {
					return quiz;
				}
			}
		}

		return null;
	}
}
