package de.mineking.musicquiz.commands;

import com.google.gson.Gson;
import de.mineking.discord.commands.commands.global.GlobalSlashCommand;
import de.mineking.discord.commands.context.global.GlobalSlashContext;
import de.mineking.discord.commands.option.Option;
import de.mineking.musicquiz.main.Messages;
import de.mineking.musicquiz.main.MusicQuiz;
import de.mineking.musicquiz.quiz.Quiz;
import de.mineking.musicquiz.quiz.Track;
import net.dv8tion.jda.api.entities.channel.concrete.VoiceChannel;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.interactions.commands.OptionType;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class CreateCommand extends GlobalSlashCommand {
	private final MusicQuiz bot;

	public CreateCommand(MusicQuiz bot) {
		this.bot = bot;

		visibility = Visibility.GUILD_ONLY;

		addOption(new Option(OptionType.ATTACHMENT, "file", true));
	}

	@Override
	protected void performCommand(GlobalSlashContext context) {
		context.event.deferReply(true).queue();

		try(InputStreamReader isr = new InputStreamReader(context.getOption("file", OptionMapping::getAsAttachment).getProxy().download().get()); BufferedReader reader = new BufferedReader(isr)) {
			VoiceChannel channel;

			try {
				channel = Objects.requireNonNull(context.event.getMember().getVoiceState().getChannel().asVoiceChannel());
			} catch(Exception e) {
				Messages.send(context.event, "create.missing-voice", Messages.Color.ERROR);

				return;
			}

			SaveCommand.SaveData data;

			try {
				data = new Gson().fromJson(reader, SaveCommand.SaveData.class);
			} catch(Exception ignored) {
				data = new SaveCommand.SaveData(context.user.getIdLong(), parseQuests(reader), 0, new HashMap<>());
				Collections.shuffle(data.tracks());
			}

			bot.quizzes.add(new Quiz(bot, channel, data));

			Messages.send(context.event, "create.success", Messages.Color.SUCCESS);
		} catch(Exception e) {
			e.printStackTrace();

			Messages.send(context.event, "error", Messages.Color.ERROR, e.getMessage());
		}
	}

	List<Track> parseQuests(BufferedReader reader) throws IOException {
		List<Track> result = new ArrayList<>();

		String line;
		while((line = reader.readLine()) != null) {
			Matcher m = Pattern.compile("(?<url>.*?)\\?t=(?<start>\\d+) \\+(?<end>\\d+)s \"(?<title>.*?)\" - \"(?<artists>.*?)\"").matcher(line);

			if(m.matches()) {
				result.add(new Track(
						m.group("url"),
						Long.parseLong(m.group("start")),
						Long.parseLong(m.group("end")),
						m.group("title"),
						m.group("artists")
				));
			}
		}

		return result;
	}
}
