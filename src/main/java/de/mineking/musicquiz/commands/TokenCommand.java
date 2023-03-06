package de.mineking.musicquiz.commands;

import de.mineking.discord.commands.commands.global.GlobalSlashCommand;
import de.mineking.discord.commands.context.global.GlobalSlashContext;
import de.mineking.musicquiz.main.Messages;
import de.mineking.musicquiz.main.MusicQuiz;
import net.dv8tion.jda.api.interactions.components.buttons.Button;
import net.dv8tion.jda.api.utils.messages.MessageCreateBuilder;

import java.util.UUID;

public class TokenCommand extends GlobalSlashCommand {
	private final MusicQuiz bot;

	public TokenCommand(MusicQuiz bot) {
		this.bot = bot;
	}

	@Override
	protected void performCommand(GlobalSlashContext context) {
		String token = UUID.randomUUID().toString();

		bot.server.gateway.tokens.put(token, context.user.getIdLong());

		context.event.reply(
				new MessageCreateBuilder()
						.setEmbeds(Messages.buildEmbed("token.success", Messages.Color.SUCCESS, token))
						.setActionRow(Button.link(bot.config.url + "?authentication=" + token, Messages.get("token.button")))
						.build()
		).setEphemeral(true).queue();
		Messages.send(context.event, "token.success", Messages.Color.SUCCESS, token);
	}
}
