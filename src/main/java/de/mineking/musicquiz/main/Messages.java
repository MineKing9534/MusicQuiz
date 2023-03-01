package de.mineking.musicquiz.main;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.exceptions.ErrorHandler;
import net.dv8tion.jda.api.interactions.InteractionHook;
import net.dv8tion.jda.api.interactions.callbacks.IReplyCallback;
import net.dv8tion.jda.api.requests.ErrorResponse;

import java.util.ResourceBundle;
import java.util.concurrent.TimeUnit;

public class Messages {
	public static final ResourceBundle response = ResourceBundle.getBundle("Response");

	public enum Color {
		ERROR(java.awt.Color.RED),
		INFO(java.awt.Color.YELLOW),
		SUCCESS(java.awt.Color.GREEN),
		Interface(java.awt.Color.decode("#0879c2"));

		public final java.awt.Color color;

		Color(java.awt.Color color) {
			this.color = color;
		}
	}

	public static String get(String name, Object... args) {
		return response.getString(name).formatted(args);
	}

	public static MessageEmbed buildEmbed(String name, Color color, Object... args) {
		return new EmbedBuilder()
				.setColor(color.color)
				.setDescription(get(name, args))
				.build();
	}

	public static void send(IReplyCallback event, boolean delete, String name, Color color, Object... args) {
		if(event.isAcknowledged()) {
			event.getHook().editOriginalEmbeds(buildEmbed(name, color, args)).queue();
		}

		else if(delete) {
			event.replyEmbeds(buildEmbed(name, color, args)).setEphemeral(true).delay(10, TimeUnit.SECONDS).flatMap(InteractionHook::deleteOriginal).queue(null, new ErrorHandler().ignore(ErrorResponse.UNKNOWN_MESSAGE));
		}

		else {
			event.replyEmbeds(buildEmbed(name, color, args)).setEphemeral(true).queue();
		}
	}

	public static void send(IReplyCallback event, String name, Color color, Object... args) {
		send(event, true, name, color, args);
	}
}
