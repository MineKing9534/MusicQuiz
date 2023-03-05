package de.mineking.musicquiz.quiz;

import de.mineking.musicquiz.main.Messages;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.entities.emoji.Emoji;
import net.dv8tion.jda.api.exceptions.ErrorHandler;
import net.dv8tion.jda.api.interactions.InteractionHook;
import net.dv8tion.jda.api.interactions.callbacks.IMessageEditCallback;
import net.dv8tion.jda.api.interactions.callbacks.IReplyCallback;
import net.dv8tion.jda.api.interactions.components.ActionRow;
import net.dv8tion.jda.api.interactions.components.buttons.Button;
import net.dv8tion.jda.api.interactions.components.selections.SelectOption;
import net.dv8tion.jda.api.interactions.components.selections.StringSelectMenu;
import net.dv8tion.jda.api.requests.ErrorResponse;
import net.dv8tion.jda.api.utils.TimeFormat;
import net.dv8tion.jda.api.utils.messages.MessageCreateBuilder;
import net.dv8tion.jda.api.utils.messages.MessageCreateData;
import net.dv8tion.jda.api.utils.messages.MessageEditBuilder;
import net.dv8tion.jda.api.utils.messages.MessageEditData;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

public class MessageManager {
	public final static int visibility = 5;

	private final Quiz quiz;

	private final long message;
	private InteractionHook hook;

	public MessageManager(Quiz quiz) {
		this.quiz = quiz;

		message = quiz.channel.sendMessage(
				new MessageCreateBuilder()
						.setEmbeds(
								new EmbedBuilder()
										.setColor(Messages.Color.Interface.color)
										.setDescription(Messages.get("start.text"))
										.build()
						)
						.setActionRow(
								Button.primary(quiz.channel.getId() + ":start", Messages.get("start.button"))
						)
						.build()
		).complete().getIdLong();
	}

	private MessageEditData buildPublicMessage() {
		EmbedBuilder builder = new EmbedBuilder()
				.setColor(Messages.Color.Interface.color)
				.setDescription(Messages.get("guess.text"))
				.setThumbnail(quiz.guesser != 0 ? quiz.channel.getGuild().getMemberById(quiz.guesser).getEffectiveAvatarUrl() : null);

		AtomicInteger i = new AtomicInteger(1);

		builder.addField(
				Messages.get("guess.points.title"),
				"```ansi\n" + (
						quiz.getMembers().isEmpty()
								? "\u001B[31m" + Messages.get("quiz.empty") + "\u001B[0m"
								: quiz.getMembers().entrySet().stream()
								.sorted(Comparator.comparing(e -> e.getValue().points.get(), Comparator.reverseOrder()))
								.map(e ->
										e.getKey() == quiz.master
												? Messages.get("guess.points.line.master", quiz.channel.getGuild().getMemberById(e.getKey()).getEffectiveName())
												: Messages.get("guess.points.line" +
														(
																e.getKey() == quiz.guesser
																? ".current"
																: (quiz.ignore.contains(e.getKey()) ? ".ignore" : "")
														),
												i.getAndIncrement(),
												quiz.channel.getGuild().getMemberById(e.getKey()).getEffectiveName(),
												e.getValue().points.get()
										)
								)
								.collect(Collectors.joining("\n"))
				) + "```",
				true
		);

		if(quiz.guesser != 0) {
			builder.setFooter(Messages.get("guess.footer", quiz.channel.getGuild().getMemberById(quiz.guesser).getEffectiveName()));
		}

		return new MessageEditBuilder()
				.setEmbeds(
						builder.build()
				)
				.setActionRow(
						Button.primary(quiz.channel.getId() + ":guess", Messages.get("guess.button")),
						Button.danger(quiz.channel.getId() + ":control", Messages.get("quiz.control"))
				)
				.build();
	}

	private MessageEditData buildPrivateMessage() {
		EmbedBuilder builder = new EmbedBuilder()
				.setColor(Messages.Color.Interface.color);

		StringBuilder contents = new StringBuilder();
		for(int i = Math.max(0, quiz.position - visibility); i < Math.min(quiz.tracks.size(), quiz.position + visibility + 1); i++) {
			Track quest = quiz.tracks.get(i);

			if(i == quiz.position) {
				contents.append(Messages.get("quiz.message.current", quest.title, quest.author, quest.url, quest.start, quest.end > 0 ? quest.end : "♾"));
			}

			else {
				contents.append(Messages.get("quiz.message", i + 1, quest.title, quest.author, quest.url, quest.start, quest.end > 0 ? quest.end : "♾"));
			}

			contents.append("\n");
		}

		builder.addField(
				Messages.get("quiz.content.title"),
				contents.toString(),
				false
		);

		if(quiz.guesser != 0) {
			builder.addField(
					Messages.get("quiz.guesser.title"),
					Messages.get("quiz.guesser.content", quiz.channel.getGuild().getMemberById(quiz.guesser).getEffectiveName(), TimeFormat.RELATIVE.atTimestamp(quiz.guessTime)),
					false
			);
		}

		List<SelectOption> options = new ArrayList<>();

		quiz.getMembers().forEach((user, score) -> {
			SelectOption option = SelectOption.of(quiz.channel.getGuild().getMemberById(user).getEffectiveName(), String.valueOf(user))
					.withDescription(Messages.get("select.description", score.points));

			if(user.equals(quiz.selected)) {
				option = option
						.withDefault(true)
						.withEmoji(Emoji.fromFormatted(quiz.ignore.contains(user) ? "▶" : "➡"));
			}

			else if(quiz.ignore.contains(user)) {
				option = option
						.withEmoji(Emoji.fromFormatted("❌"));
			}

			options.add(option);
		});

		boolean empty = options.isEmpty();

		if(empty) {
			options.add(
					SelectOption.of(Messages.get("quiz.empty"), "-")
							.withDefault(true)
			);
		}

		return new MessageEditBuilder()
				.setEmbeds(builder.build())
				.setComponents(
						ActionRow.of(
								StringSelectMenu.create(quiz.channel.getId() + ":select")
										.addOptions(options)
										.build()
										.withDisabled(empty)
						),
						ActionRow.of(
								Button.primary(quiz.channel.getId() + ":correct", Messages.get("quiz.correct")),
								Button.primary(quiz.channel.getId() + ":freeuser", Messages.get("quiz.freeuser")),
								Button.primary(quiz.channel.getId() + ":freeall", Messages.get("quiz.freeall")),
								Button.primary(quiz.channel.getId() + ":add", Messages.get("quiz.add")),
								Button.primary(quiz.channel.getId() + ":remove", Messages.get("quiz.remove"))
						),
						ActionRow.of(
								Button.primary(quiz.channel.getId() + ":restart", Messages.get("quiz.restart")),
								Button.primary(quiz.channel.getId() + ":next", Messages.get("quiz.next")),
								Button.primary(quiz.channel.getId() + ":extend", Messages.get("quiz.extend")),
								Button.primary(quiz.channel.getId() + ":run", Messages.get("quiz.run")),
								Button.primary(quiz.channel.getId() + ":cancel", Messages.get("quiz.cancel"))
						),
						ActionRow.of(
								Button.danger(quiz.channel.getId() + ":stop", Messages.get("quiz.stop"))
						)
				)
				.build();
	}

	public void updatePublicMessage(IMessageEditCallback event) {
		if(event == null) {
			quiz.channel.editMessageById(message, buildPublicMessage()).queue(null, new ErrorHandler().ignore(ErrorResponse.UNKNOWN_MESSAGE));
		}

		else {
			event.editMessage(buildPublicMessage()).queue();
		}

		quiz.sendUpdate();
	}

	public void updatePrivateMessage(IMessageEditCallback event) {
		if(event == null) {
			if(hook != null) {
				hook.editOriginal(buildPrivateMessage()).queue(null, new ErrorHandler().ignore(ErrorResponse.UNKNOWN_MESSAGE));
			}
		}

		else {
			event.editMessage(buildPrivateMessage()).queue();
		}
	}

	public enum Mode {
		PUBLIC, PRIVATE
	}

	public void updateMessages(IMessageEditCallback event, Mode mode) {
		updatePrivateMessage(mode == Mode.PRIVATE ? event : null);
		updatePublicMessage(mode == Mode.PUBLIC ? event : null);
	}

	public void sendPrivateMessage(IReplyCallback event) {
		event.reply(MessageCreateData.fromEditData(buildPrivateMessage())).setEphemeral(true).queue(hook -> {
			this.hook = hook;

			hook.deleteOriginal().queueAfter(10, TimeUnit.MINUTES, null, new ErrorHandler().ignore(ErrorResponse.UNKNOWN_MESSAGE));
		});
	}

	public void deletePrivateMessage() {
		if(hook != null) {
			hook.deleteOriginal().queue(null, new ErrorHandler().ignore(ErrorResponse.UNKNOWN_MESSAGE));
		}
	}

	public void sendConnectMessage(User user, String name) {
		if(hook == null || hook.isExpired()) {
			return;
		}

		hook.sendMessageEmbeds(
				Messages.buildEmbed(
						name,
						Messages.Color.INFO,
						user.getAsTag()
				)
		).setEphemeral(true).delay(10, TimeUnit.SECONDS).flatMap(mes -> hook.deleteMessageById(mes.getIdLong())).queue();
	}

	public void cleanup() {
		deletePrivateMessage();

		quiz.channel.deleteMessageById(message).queue(null, new ErrorHandler().ignore(ErrorResponse.UNKNOWN_MESSAGE));
	}
}
