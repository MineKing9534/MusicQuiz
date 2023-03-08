package de.mineking.musicquiz.commands;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import de.mineking.discord.commands.commands.global.GlobalSlashCommand;
import de.mineking.discord.commands.context.global.GlobalSlashContext;
import de.mineking.musicquiz.main.Messages;
import de.mineking.musicquiz.main.MusicQuiz;
import de.mineking.musicquiz.quiz.Track;
import kotlin.text.Charsets;
import net.dv8tion.jda.api.utils.FileUpload;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class SaveCommand extends GlobalSlashCommand {
	public final static Gson gson = new GsonBuilder()
			.setPrettyPrinting()
			.create();
	private final MusicQuiz bot;

	public SaveCommand(MusicQuiz bot) {
		this.bot = bot;

		visibility = Visibility.GUILD_ONLY;
	}

	public record SaveData(long master, List<Track> tracks, int position, Map<Long, Integer> points) {

	}

	@Override
	protected void performCommand(GlobalSlashContext context) {
		MusicQuiz.QuizData quiz = bot.getQuizByUser(context.user.getIdLong(), true);

		if(quiz == null) {
			Messages.send(context.event, "quiz.missing", Messages.Color.ERROR);

			return;
		}

		String data = gson.toJson(new SaveData(
				quiz.quiz().getMaster(),
				quiz.quiz().getTracks(),
				quiz.quiz().getPosition(),
				quiz.quiz().getMembers().entrySet().stream()
						.collect(Collectors.toMap(
								Map.Entry::getKey,
								e -> e.getValue().points.get()
						))
		));

		context.event.replyFiles(FileUpload.fromData(data.getBytes(Charsets.UTF_8), "save.json")).setEphemeral(true).queue();
	}
}
