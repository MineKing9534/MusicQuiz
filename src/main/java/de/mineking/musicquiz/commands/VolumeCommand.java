package de.mineking.musicquiz.commands;

import de.mineking.discord.commands.commands.global.GlobalSlashCommand;
import de.mineking.discord.commands.context.global.GlobalSlashContext;
import de.mineking.discord.commands.option.Option;
import de.mineking.musicquiz.main.Messages;
import de.mineking.musicquiz.main.MusicQuiz;
import de.mineking.musicquiz.quiz.Quiz;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.interactions.commands.OptionType;

public class VolumeCommand extends GlobalSlashCommand {
	private final MusicQuiz bot;

	public VolumeCommand(MusicQuiz bot) {
		this.bot = bot;

		visibility = Visibility.GUILD_ONLY;

		addOption(new Option(OptionType.INTEGER, "volume", true)
				.setRequiredRange(1, 100));
	}

	@Override
	protected void performCommand(GlobalSlashContext context) {
		for(Quiz quiz : bot.quizzes) {
			if(quiz.getMaster() == context.event.getMember().getIdLong()) {
				int volume = context.getOption("volume", OptionMapping::getAsInt);

				quiz.setVolume(volume);

				Messages.send(context.event, "volume.success", Messages.Color.SUCCESS, volume);

				return;
			}
		}

		Messages.send(context.event, "volume.missing", Messages.Color.ERROR);
	}
}
