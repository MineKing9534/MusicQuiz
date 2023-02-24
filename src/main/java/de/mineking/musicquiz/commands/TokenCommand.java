package de.mineking.musicquiz.commands;

import de.mineking.discord.commands.commands.global.GlobalSlashCommand;
import de.mineking.discord.commands.context.global.GlobalSlashContext;
import de.mineking.musicquiz.main.Messages;
import de.mineking.musicquiz.main.remote.GatewayHandler;

import java.util.UUID;

public class TokenCommand extends GlobalSlashCommand {
	@Override
	protected void performCommand(GlobalSlashContext context) {
		String token = UUID.randomUUID().toString();

		GatewayHandler.tokens.put(token, context.user);

		Messages.send(context.event, "token.success", Messages.Color.SUCCESS, token);
	}
}
