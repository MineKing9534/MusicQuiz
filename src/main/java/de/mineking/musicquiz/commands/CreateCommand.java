package de.mineking.musicquiz.commands;

import de.mineking.discord.commands.commands.global.GlobalSlashCommand;
import de.mineking.discord.commands.context.global.GlobalSlashContext;
import de.mineking.discord.commands.option.Option;
import de.mineking.musicquiz.main.Main;
import de.mineking.musicquiz.main.Messages;
import de.mineking.musicquiz.quiz.Quest;
import de.mineking.musicquiz.quiz.Quiz;
import net.dv8tion.jda.api.entities.channel.concrete.VoiceChannel;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.interactions.commands.OptionType;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class CreateCommand extends GlobalSlashCommand {
	public CreateCommand() {
		visibility = Visibility.GUILD_ONLY;

		addOption(new Option(OptionType.ATTACHMENT, "file", true));
	}

	@Override
	protected void performCommand(GlobalSlashContext context) {
		context.event.deferReply(true).queue();

		try {
			List<Quest> quests = parseQuests(context.getOption("file", OptionMapping::getAsAttachment).getProxy().download().get());

			VoiceChannel channel;

			try {
				channel = Objects.requireNonNull(context.event.getMember().getVoiceState().getChannel().asVoiceChannel());
			} catch(Exception e) {
				Messages.send(context.event, "create.missing-voice", Messages.Color.ERROR);

				return;
			}

			Main.quizzes.add(new Quiz(channel, quests, context.event.getMember()));
		} catch(Exception e) {
			e.printStackTrace();

			Messages.send(context.event, "error", Messages.Color.ERROR, e.getMessage());
		}
	}

	List<Quest> parseQuests(InputStream stream) throws IOException {
		List<Quest> result = new ArrayList<>();

		try(InputStreamReader isr = new InputStreamReader(stream); BufferedReader reader = new BufferedReader(isr)) {
			String line;
			while((line = reader.readLine()) != null) {
				Matcher m = Pattern.compile("(?<url>.*?)\\?t=(?<start>\\d+) \\+(?<end>\\d+)s \"(?<title>.*?)\" - \"(?<artists>.*?)\"").matcher(line);

				if(m.matches()) {
					result.add(new Quest(
							m.group("url"),
							Long.parseLong(m.group("start")),
							Long.parseLong(m.group("end")),
							m.group("title"),
							m.group("artists")
					));
				}
			}
		}

		return result;
	}
}
