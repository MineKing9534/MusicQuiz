package de.mineking.musicquiz.commands;

import com.google.gson.Gson;
import com.google.gson.JsonParseException;
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

import java.io.*;
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

		VoiceChannel channel;

		try {
			channel = Objects.requireNonNull(context.event.getMember().getVoiceState().getChannel().asVoiceChannel());
		} catch(Exception e) {
			Messages.send(context.event, "create.missing-voice", Messages.Color.ERROR);

			return;
		}

		SaveCommand.SaveData data;

		try(InputStream file = context.getOption("file", OptionMapping::getAsAttachment).getProxy().download().get()) {
			ByteArrayOutputStream boas = new ByteArrayOutputStream();
			file.transferTo(boas);

			try(InputStreamReader isr = new InputStreamReader(new ByteArrayInputStream(boas.toByteArray()))) {
				data = new Gson().fromJson(isr, SaveCommand.SaveData.class);
			} catch(JsonParseException e) {
				try(InputStreamReader isr = new InputStreamReader(new ByteArrayInputStream(boas.toByteArray())); BufferedReader reader = new BufferedReader(isr)) {
					data = new SaveCommand.SaveData(context.user.getIdLong(), parseQuests(reader), 0, new HashMap<>());
					Collections.shuffle(data.tracks());
				}
			}
		} catch(Exception e) {
			e.printStackTrace();
			Messages.send(context.event, "error", Messages.Color.ERROR, e.getMessage());

			return;
		}

		bot.quizzes.add(new Quiz(bot, channel, data));

		Messages.send(context.event, "create.success", Messages.Color.SUCCESS);
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
