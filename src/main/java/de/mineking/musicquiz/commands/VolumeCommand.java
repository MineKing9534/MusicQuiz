package de.mineking.musicquiz.commands;

import de.mineking.discord.commands.commands.global.GlobalSlashCommand;
import de.mineking.discord.commands.context.global.GlobalSlashContext;
import de.mineking.discord.commands.option.Option;
import de.mineking.musicquiz.main.Messages;
import de.mineking.musicquiz.main.MusicQuiz;
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
		MusicQuiz.QuizData quiz = bot.getQuizByUser(context.user.getIdLong(), true);

		if(quiz == null) {
			Messages.send(context.event, "quiz.missing", Messages.Color.ERROR);

			return;
		}

		int volume = context.getOption("volume", OptionMapping::getAsInt);

		quiz.quiz().setVolume(volume);
		Messages.send(context.event, "volume.success", Messages.Color.SUCCESS, volume);
	}
}
