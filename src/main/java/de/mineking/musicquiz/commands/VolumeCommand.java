package de.mineking.musicquiz.commands;

import de.mineking.discord.commands.commands.global.GlobalSlashCommand;
import de.mineking.discord.commands.context.global.GlobalSlashContext;
import de.mineking.discord.commands.option.Option;
import de.mineking.musicquiz.main.Main;
import de.mineking.musicquiz.main.Messages;
import de.mineking.musicquiz.quiz.Quiz;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.interactions.commands.OptionType;

public class VolumeCommand extends GlobalSlashCommand {
	public VolumeCommand() {
		visibility = Visibility.GUILD_ONLY;

		addOption(new Option(OptionType.INTEGER, "volume", true)
				.setRequiredRange(1, 100));
	}

	@Override
	protected void performCommand(GlobalSlashContext context) {
		for(Quiz quiz : Main.quizzes) {
			if(quiz.getMaster().equals(context.event.getMember())) {
				int volume = context.getOption("volume", OptionMapping::getAsInt);

				quiz.setVolume(volume);

				Messages.send(context.event, "volume.success", Messages.Color.SUCCESS, volume);

				return;
			}
		}

		Messages.send(context.event, "volume.missing", Messages.Color.ERROR);
	}
}
