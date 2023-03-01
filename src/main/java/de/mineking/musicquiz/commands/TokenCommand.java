package de.mineking.musicquiz.commands;

import de.mineking.discord.commands.commands.global.GlobalSlashCommand;
import de.mineking.discord.commands.context.global.GlobalSlashContext;
import de.mineking.musicquiz.main.Messages;
import de.mineking.musicquiz.main.MusicQuiz;

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

		Messages.send(context.event, "token.success", Messages.Color.SUCCESS, token);
	}
}
