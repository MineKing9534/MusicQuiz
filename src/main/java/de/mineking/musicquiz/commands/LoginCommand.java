package de.mineking.musicquiz.commands;

import de.mineking.discord.commands.commands.global.GlobalSlashCommand;
import de.mineking.discord.commands.context.global.GlobalSlashContext;
import de.mineking.musicquiz.main.Messages;
import de.mineking.musicquiz.main.MusicQuiz;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.interactions.components.buttons.Button;
import net.dv8tion.jda.api.utils.messages.MessageCreateBuilder;

import java.security.SecureRandom;
import java.util.Base64;

public class LoginCommand extends GlobalSlashCommand {
	public final static SecureRandom random = new SecureRandom();
	private final MusicQuiz bot;

	public LoginCommand(MusicQuiz bot) {
		this.bot = bot;
	}

	@Override
	protected void performCommand(GlobalSlashContext context) {
		byte[] bytes = new byte[32];
		random.nextBytes(bytes);
		String token = Base64.getUrlEncoder().encodeToString(bytes);

		bot.server.gateway.tokens.put(token, context.user.getIdLong());

		context.event.reply(
				new MessageCreateBuilder()
						.setEmbeds(new EmbedBuilder(Messages.buildEmbed("login.success", Messages.Color.Interface, bot.config.url + "/index.html"))
								.setTitle(Messages.get("login.title"))
								.addField(
										Messages.get("login.token"),
										"```fix\n" + token + "```",
										false
								)
								.build()
						)
						.setActionRow(Button.link(bot.config.url + "/quiz.html?authentication=" + token, Messages.get("login.button")))
						.build()
		).setEphemeral(true).queue();
	}
}
