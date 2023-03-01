package de.mineking.musicquiz.quiz;

import com.sedmelluq.discord.lavaplayer.player.AudioLoadResultHandler;
import com.sedmelluq.discord.lavaplayer.player.AudioPlayer;
import com.sedmelluq.discord.lavaplayer.tools.FriendlyException;
import com.sedmelluq.discord.lavaplayer.track.AudioPlaylist;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import com.sedmelluq.discord.lavaplayer.track.TrackMarker;
import de.mineking.musicquiz.main.Main;
import de.mineking.musicquiz.main.Messages;
import de.mineking.musicquiz.main.remote.GatewayHandler;
import io.javalin.websocket.WsContext;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.entities.channel.concrete.VoiceChannel;
import net.dv8tion.jda.api.entities.emoji.Emoji;
import net.dv8tion.jda.api.events.guild.voice.GuildVoiceUpdateEvent;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.StringSelectInteractionEvent;
import net.dv8tion.jda.api.exceptions.ErrorHandler;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
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
import org.eclipse.jetty.websocket.core.CloseStatus;

import java.time.Instant;
import java.util.*;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import java.util.stream.Collectors;

public class Quiz extends ListenerAdapter {
	public final static String buzzer = "https://assets.mixkit.co/sfx/preview/mixkit-game-show-wrong-answer-buzz-950.mp3";

	public static class MemberData {
		public final AtomicInteger points = new AtomicInteger();
		public WsContext remote;
	}

	public final static int visibility = 5;

	private final List<Quest> quests;
	private int position = 0;

	private final Member master;
	private final Map<Member, MemberData> members = new HashMap<>();
	private Member selected;
	private final List<Member> ignore = new ArrayList<>();
	private Member guesser;

	private long guessTime = 0;

	private final VoiceChannel channel;
	private final long message;
	private InteractionHook hook;

	private boolean started = false;

	private final AudioPlayer player = Main.audioPlayerManager.createPlayer();

	public Quiz(VoiceChannel channel, List<Quest> quests, Member master) {
		Collections.shuffle(quests);

		this.quests = quests;
		this.master = master;
		this.selected = channel.getMembers().get(0);

		player.setVolume(30);

		channel.getMembers().forEach(this::addMember);

		message = channel.sendMessage(
				new MessageCreateBuilder()
						.setEmbeds(
								new EmbedBuilder()
										.setColor(Messages.Color.Interface.color)
										.setDescription(Messages.get("start.text"))
										.build()
						)
						.setActionRow(
								Button.primary(channel.getId() + ":start", Messages.get("start.button"))
						)
						.build()
		).complete().getIdLong();

		this.channel = channel;

		channel.getGuild().getAudioManager().openAudioConnection(channel);
		channel.getGuild().getAudioManager().setSelfDeafened(true);

		channel.getGuild().getAudioManager().setSendingHandler(new SendHandler(player));

		Main.jda.addEventListener(this);
	}

	public Map<Member, MemberData> getMembers() {
		return members;
	}

	public Member getMaster() {
		return master;
	}

	private MessageEditData buildPublicMessage() {
		EmbedBuilder builder = new EmbedBuilder()
				.setColor(Messages.Color.Interface.color)
				.setDescription(Messages.get("guess.text"))
				.setThumbnail(guesser != null ? guesser.getEffectiveAvatarUrl() : null);

		AtomicInteger i = new AtomicInteger(1);

		builder.addField(
				Messages.get("guess.points.title"),
				"```ansi\n" + (
						members.isEmpty()
								? "\u001B[31m" + Messages.get("quiz.empty") + "\u001B[0m"
								: members.entrySet().stream()
								.sorted(Comparator.comparing(e -> e.getValue().points.get(), Comparator.reverseOrder()))
								.map(e -> Messages.get("guess.points.line" + (e.getKey().equals(guesser) ? ".current" : (ignore.contains(e.getKey()) ? ".ignore" : "")), i.getAndIncrement(), e.getKey().getEffectiveName(), e.getValue().points.get()))
								.collect(Collectors.joining("\n"))
				) +
						"```",
				true
		);

		if(guesser != null) {
			builder.setFooter(Messages.get("guess.footer", guesser.getEffectiveName()));
			builder.setTimestamp(Instant.ofEpochMilli(guessTime));
		}

		return new MessageEditBuilder()
				.setEmbeds(
						builder.build()
				)
				.setActionRow(
						Button.primary(channel.getId() + ":guess", Messages.get("guess.button")),
						Button.danger(channel.getId() + ":control", Messages.get("quiz.control"))
				)
				.build();
	}

	private MessageEditData buildPrivateMessage() {
		EmbedBuilder builder = new EmbedBuilder()
				.setColor(Messages.Color.Interface.color);

		StringBuilder contents = new StringBuilder();
		for(int i = Math.max(0, position - visibility); i < Math.min(quests.size(), position + visibility + 1); i++) {
			Quest quest = quests.get(i);

			if(i == position) {
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

		if(guesser != null) {
			builder.addField(
					Messages.get("quiz.guesser.title"),
					Messages.get("quiz.guesser.content", guesser.getEffectiveName(), TimeFormat.RELATIVE.atTimestamp(guessTime)),
					false
			);
		}

		List<SelectOption> options = new ArrayList<>();

		members.forEach((user, score) -> {
			SelectOption option = SelectOption.of(user.getEffectiveName(), user.getId())
					.withDescription(Messages.get("select.description", score.points));

			if(user.equals(selected)) {
				option = option
						.withDefault(true)
						.withEmoji(Emoji.fromFormatted(ignore.contains(user) ? "▶" : "➡"));
			}

			else if(ignore.contains(user)) {
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
								StringSelectMenu.create(channel.getId() + ":select")
										.addOptions(options)
										.build()
										.withDisabled(empty)
						),
						ActionRow.of(
								Button.primary(channel.getId() + ":free", Messages.get("quiz.free")),
								Button.primary(channel.getId() + ":freeuser", Messages.get("quiz.freeuser")),
								Button.primary(channel.getId() + ":freeall", Messages.get("quiz.freeall")),
								Button.primary(channel.getId() + ":add", Messages.get("quiz.add")),
								Button.primary(channel.getId() + ":remove", Messages.get("quiz.remove"))
						),
						ActionRow.of(
								Button.primary(channel.getId() + ":restart", Messages.get("quiz.restart")),
								Button.primary(channel.getId() + ":next", Messages.get("quiz.next")),
								Button.primary(channel.getId() + ":extend", Messages.get("quiz.extend")),
								Button.primary(channel.getId() + ":run", Messages.get("quiz.run")),
								Button.primary(channel.getId() + ":cancel", Messages.get("quiz.cancel"))
						),
						ActionRow.of(
								Button.danger(channel.getId() + ":stop", Messages.get("quiz.stop"))
						)
				)
				.build();
	}

	public void updatePublicMessage(IMessageEditCallback event) {
		if(event == null) {
			channel.editMessageById(message, buildPublicMessage()).queue();
		}

		else {
			event.editMessage(buildPublicMessage()).queue();
		}

		sendUpdate();
	}

	public void updatePrivateMessage(IMessageEditCallback event) {
		if(event == null) {
			if(hook != null) {
				hook.editOriginal(buildPrivateMessage()).queue();
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

	private void sendPrivateMessage(IReplyCallback event) {
		event.reply(MessageCreateData.fromEditData(buildPrivateMessage())).setEphemeral(true).queue(hook -> {
			this.hook = hook;

			hook.deleteOriginal().queueAfter(15, TimeUnit.MINUTES, null, new ErrorHandler().ignore(ErrorResponse.UNKNOWN_MESSAGE));
		});
	}

	@Override
	public void onButtonInteraction(ButtonInteractionEvent event) {
		String[] temp = event.getComponentId().split(":");

		if(!temp[0].equals(channel.getId())) {
			return;
		}

		switch(temp[1]) {
			case "start":
				started = true;

				if(!event.getMember().equals(master)) {
					Messages.send(event, "start.invalid", Messages.Color.ERROR);

					return;
				}

				updatePublicMessage(null);
				sendPrivateMessage(event);

				break;

			case "control":
				if(!event.getMember().equals(master)) {
					Messages.send(event, "start.invalid", Messages.Color.ERROR);

					return;
				}

				if(hook != null) {
					hook.deleteOriginal().queue(null, new ErrorHandler().ignore(ErrorResponse.UNKNOWN_MESSAGE));
				}

				sendPrivateMessage(event);

				break;

			case "guess":
				if(guesser != null) {
					Messages.send(event, "guess.invalid.given", Messages.Color.ERROR);

					return;
				}

				if(ignore.contains(event.getMember())) {
					Messages.send(event, "guess.invalid.ignore", Messages.Color.ERROR);

					return;
				}

				setGuesser(event.getMember());
				updateMessages(event, Mode.PUBLIC);

				break;

			case "free":
				guesser = null;
				guessTime = 0;

				updateMessages(event, Mode.PRIVATE);

				break;

			case "freeuser":
				ignore.remove(selected);

				updateMessages(event, Mode.PRIVATE);

				break;

			case "freeall":
				ignore.clear();

				updateMessages(event, Mode.PRIVATE);

				break;

			case "add":
				members.get(selected).points.incrementAndGet();
				updateMessages(event, Mode.PRIVATE);
				break;
			case "remove":
				members.get(selected).points.decrementAndGet();
				updateMessages(event, Mode.PRIVATE);
				break;

			case "next":
				position++;
				ignore.clear();
			case "restart":
				guesser = null;

				Messages.send(event, "track.start", Messages.Color.SUCCESS);
				updateMessages(null, null);

				playTrack();
				break;
			case "extend":
				quests.get(position).end++;
				updatePrivateMessage(event);

				break;

			case "run":
				quests.get(position).end = 0;
				playTrack();
				updatePrivateMessage(event);
				break;
			case "cancel":
				player.stopTrack();
				updatePrivateMessage(event);
				break;

			case "stop": stop(); break;
		}
	}

	@Override
	public void onStringSelectInteraction(StringSelectInteractionEvent event) {
		String[] temp = event.getComponentId().split(":");

		if(!temp[0].equals(channel.getId())) {
			return;
		}

		if(temp[1].equals("select")) {
			event.deferEdit().queue();

			selected = channel.getGuild().retrieveMemberById(event.getSelectedOptions().get(0).getValue()).complete();

			updatePrivateMessage(null);
		}
	}

	private void addMember(Member m) {
		if(m.equals(master) || m.getUser().isBot()) {
			return;
		}

		members.putIfAbsent(m, new MemberData());
	}

	private void stop() {
		members.forEach((m, data) -> {
			if(data.remote != null) {
				data.remote.closeSession(CloseStatus.NORMAL, "Quiz has ended!");
				GatewayHandler.data.remove(data.remote);
			}
		});

		if(hook != null) {
			hook.deleteOriginal().queue();
		}

		channel.deleteMessageById(message).queue(null, new ErrorHandler().ignore(ErrorResponse.UNKNOWN_MESSAGE));
		channel.getGuild().getAudioManager().closeAudioConnection();


		Main.quizzes.remove(this);
		Main.jda.removeEventListener(this);
	}

	@Override
	public void onGuildVoiceUpdate(GuildVoiceUpdateEvent event) {
		if(event.getChannelJoined() != null && event.getChannelJoined().equals(channel)) {
			addMember(event.getMember());

			if(isStarted()) {
				updatePublicMessage(null);
			}
		}

		if(event.getChannelLeft() != null && event.getChannelLeft().equals(channel) && event.getMember().equals(event.getGuild().getSelfMember())) {
			stop();
		}
	}

	private void playTrack() {
		guesser = null;
		guessTime = 0;
		channel.editMessageById(message, buildPublicMessage()).queue();

		Quest quest = quests.get(position);

		playTrack(quest.url, track -> {
			track.setPosition(quest.start * 1000);
			if(quest.end > 0) {
				track.setMarker(new TrackMarker((quest.start + quest.end) * 1000, state -> player.stopTrack()));
			}
		});
	}

	private void playTrack(String url, Consumer<AudioTrack> handler) {
		Main.audioPlayerManager.loadItem(url, new AudioLoadResultHandler() {
			@Override
			public void trackLoaded(AudioTrack track) {
				handler.accept(track);

				player.playTrack(track);
			}

			@Override
			public void playlistLoaded(AudioPlaylist playlist) {}

			@Override
			public void noMatches() {System.out.println("no match for <" + url + ">!");}

			@Override
			public void loadFailed(FriendlyException exception) {}
		});
	}

	public void setGuesser(Member member) {
		if(guesser != null || ignore.contains(member)) {
			return;
		}

		player.stopTrack();
		playTrack(buzzer, track -> {});

		ignore.add(member);

		guesser = member;
		selected = guesser;
		guessTime = System.currentTimeMillis();
	}

	public void setVolume(int volume) {
		player.setVolume(volume);
	}

	public record RemoteData(Map<String, RemoteMemberData> members, List<String> ignored, String guesser) {
		public record RemoteMemberData(String name, Integer points) {
		}
	}

	public void sendUpdate() {
		members.forEach((member, data) -> {
			if(data.remote == null) {
				return;
			}

			data.remote.sendAsClass(
					new RemoteData(
							members.entrySet().stream()
									.collect(
											Collectors.toMap(
													e -> e.getKey().getId(),
													e -> new RemoteData.RemoteMemberData(
															e.getKey().getEffectiveName(),
															e.getValue().points.get()
													)
											)
									),
							ignore.stream().map(Member::getId).toList(),
							guesser != null ? guesser.getId() : null
					),
					RemoteData.class
			);
		});
	}

	private void sendConnectMessage(User user, String name) {
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

	private final Map<Long, Future<?>> ignoreConnect = new HashMap<>();

	private void handleConnect(User user, String name) {
		boolean contains = false;

		if(ignoreConnect.containsKey(user.getIdLong())) {
			contains = true;

			ignoreConnect.remove(user.getIdLong()).cancel(true);
		}

		if(!contains) {
			ignoreConnect.put(user.getIdLong(), Main.executor.schedule(() -> sendConnectMessage(user, name), 1, TimeUnit.SECONDS));
		}
	}

	public void onRemoteConnect(User user) {
		handleConnect(user, "remote.connect");
	}

	public void onRemoteDisconnect(User user) {
		handleConnect(user, "remote.disconnect");
	}

	public boolean isStarted() {
		return started;
	}
}
