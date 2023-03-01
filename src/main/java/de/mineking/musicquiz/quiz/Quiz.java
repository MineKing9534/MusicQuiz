package de.mineking.musicquiz.quiz;

import com.sedmelluq.discord.lavaplayer.player.AudioLoadResultHandler;
import com.sedmelluq.discord.lavaplayer.player.AudioPlayer;
import com.sedmelluq.discord.lavaplayer.tools.FriendlyException;
import com.sedmelluq.discord.lavaplayer.track.AudioPlaylist;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import com.sedmelluq.discord.lavaplayer.track.TrackMarker;
import de.mineking.musicquiz.main.MusicQuiz;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.channel.concrete.VoiceChannel;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.eclipse.jetty.websocket.core.CloseStatus;

import java.util.*;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.stream.Collectors;

public class Quiz extends ListenerAdapter {
	public final static String buzzer = "https://assets.mixkit.co/sfx/preview/mixkit-game-show-wrong-answer-buzz-950.mp3";

	final MusicQuiz bot;
	boolean started = false;

	final EventHandler events;
	final MessageManager messages;

	final List<Track> tracks;
	int position = 0;

	final long master;
	final Map<Long, MemberData> members = new HashMap<>();
	long selected;
	final List<Long> ignore = new ArrayList<>();
	long guesser;

	long guessTime = 0;

	final VoiceChannel channel;

	AudioPlayer player;

	public Quiz(MusicQuiz bot, VoiceChannel channel, List<Track> tracks, Member master) {
		this.bot = bot;

		Collections.shuffle(tracks);
		this.tracks = tracks;
		this.master = master.getIdLong();
		this.selected = channel.getMembers().get(0).getIdLong();
		this.channel = channel;
		channel.getMembers().forEach(this::addMember);

		events = new EventHandler(this);
		messages = new MessageManager(this);

		initializeVoiceConnection();

		bot.jda.addEventListener(events);
	}

	private void initializeVoiceConnection() {
		player = bot.audioPlayerManager.createPlayer();
		player.setVolume(30);

		channel.getGuild().getAudioManager().openAudioConnection(channel);
		channel.getGuild().getAudioManager().setSelfDeafened(true);
		channel.getGuild().getAudioManager().setSendingHandler(new SendHandler(player));
	}

	public boolean isStarted() {
		return started;
	}

	public MessageManager getMessages() {
		return messages;
	}

	public VoiceChannel getChannel() {
		return channel;
	}

	public Map<Long, MemberData> getMembers() {
		return members;
	}

	public long getMaster() {
		return master;
	}

	public void addMember(Member m) {
		if(m.getIdLong() == master || m.getUser().isBot()) {
			return;
		}

		members.putIfAbsent(m.getIdLong(), new MemberData());
	}

	public void setGuesser(Member member) {
		if(guesser != 0 || ignore.contains(member.getIdLong())) {
			return;
		}

		player.stopTrack();
		playTrack(buzzer, track -> {});

		ignore.add(member.getIdLong());

		guesser = member.getIdLong();
		selected = guesser;
		guessTime = System.currentTimeMillis();
	}

	public void setVolume(int volume) {
		player.setVolume(volume);
	}

	void stop() {
		members.forEach((m, data) -> {
			if(data.remote != null) {
				data.remote.closeSession(CloseStatus.NORMAL, "Quiz has ended!");
				bot.server.gateway.data.remove(data.remote);
			}
		});

		messages.cleanup();

		channel.getGuild().getAudioManager().closeAudioConnection();

		bot.quizzes.remove(this);
		bot.jda.removeEventListener(events);
	}

	public void playTrack() {
		guesser = 0;
		guessTime = 0;

		messages.updatePublicMessage(null);

		Track quest = tracks.get(position);

		playTrack(quest.url, track -> {
			track.setPosition(quest.start * 1000);
			if(quest.end > 0) {
				track.setMarker(new TrackMarker((quest.start + quest.end) * 1000, state -> player.stopTrack()));
			}
		});
	}

	public void playTrack(String url, Consumer<AudioTrack> handler) {
		bot.audioPlayerManager.loadItem(url, new AudioLoadResultHandler() {
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


	//TODO improve website access
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
													e -> String.valueOf(e.getKey()),
													e -> new RemoteData.RemoteMemberData(
															channel.getGuild().getMemberById(e.getKey()).getEffectiveName(),
															e.getValue().points.get()
													)
											)
									),
							ignore.stream().map(String::valueOf).toList(),
							guesser != 0 ? String.valueOf(guesser) : null
					),
					RemoteData.class
			);
		});
	}

	private final Map<Long, Future<?>> ignoreConnect = new HashMap<>();

	private void handleConnect(long user, String name) {
		boolean contains = false;

		if(ignoreConnect.containsKey(user)) {
			contains = true;

			ignoreConnect.remove(user).cancel(true);
		}

		if(!contains) {
			ignoreConnect.put(user, MusicQuiz.executor.schedule(() -> messages.sendConnectMessage(bot.jda.getUserById(user), name), 1, TimeUnit.SECONDS));
		}
	}

	public void onRemoteConnect(long user) {
		handleConnect(user, "remote.connect");
	}

	public void onRemoteDisconnect(long user) {
		handleConnect(user, "remote.disconnect");
	}
}
