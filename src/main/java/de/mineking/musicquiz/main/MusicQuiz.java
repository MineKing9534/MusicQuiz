package de.mineking.musicquiz.main;

import com.sedmelluq.discord.lavaplayer.player.AudioPlayerManager;
import com.sedmelluq.discord.lavaplayer.player.DefaultAudioPlayerManager;
import com.sedmelluq.discord.lavaplayer.source.http.HttpAudioSourceManager;
import com.sedmelluq.discord.lavaplayer.source.youtube.YoutubeAudioSourceManager;
import de.mineking.discord.commands.CommandManager;
import de.mineking.discord.commands.CommandManagerBuilder;
import de.mineking.discord.localization.DefaultLocalizationMapper;
import de.mineking.musicquiz.commands.CreateCommand;
import de.mineking.musicquiz.commands.TokenCommand;
import de.mineking.musicquiz.commands.VolumeCommand;
import de.mineking.musicquiz.main.remote.RemoteServer;
import de.mineking.musicquiz.quiz.Quiz;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.OnlineStatus;
import net.dv8tion.jda.api.entities.Activity;
import net.dv8tion.jda.api.interactions.DiscordLocale;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.ResourceBundle;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

public class MusicQuiz {
	public final static ScheduledExecutorService executor = Executors.newScheduledThreadPool(30);

	public final List<Quiz> quizzes = new ArrayList<>();

	public final ResourceBundle commandInfo = ResourceBundle.getBundle("CommandInfo");

	public JDA jda;
	public CommandManager cmdMan;
	public final AudioPlayerManager audioPlayerManager = new DefaultAudioPlayerManager();

	public final RemoteServer server = new RemoteServer(this);

	public static void main(String[] args) throws Exception {
		if(args.length < 1) {
			LoggerFactory.getLogger(MusicQuiz.class).error("No token specified!");
			return;
		}

		new MusicQuiz(args[0]);
	}

	public MusicQuiz(String token) throws InterruptedException {
		cmdMan = CommandManagerBuilder.createDefault()
				.setAutoUpdate(true)
				.registerGlobalCommand("create", new CreateCommand(this))
				.registerGlobalCommand("token", new TokenCommand(this))
				.registerGlobalCommand("volume", new VolumeCommand(this))
				.setLocaleMapper(
						new DefaultLocalizationMapper(
								Collections.singletonList(DiscordLocale.GERMAN), (lang, key) ->
								commandInfo.getString(key)
						)
				)
				.build();

		jda = JDABuilder.createDefault(token)
				.setStatus(OnlineStatus.ONLINE)
				.setActivity(Activity.playing("MusicQuiz"))
				.addEventListeners(cmdMan)
				.build()
				.awaitReady();

		audioPlayerManager.registerSourceManager(new YoutubeAudioSourceManager());
		audioPlayerManager.registerSourceManager(new HttpAudioSourceManager());

		server.start();
	}
}
