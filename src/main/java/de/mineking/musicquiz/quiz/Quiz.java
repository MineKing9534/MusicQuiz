package de.mineking.musicquiz.quiz;

import com.sedmelluq.discord.lavaplayer.player.AudioLoadResultHandler;
import com.sedmelluq.discord.lavaplayer.player.AudioPlayer;
import com.sedmelluq.discord.lavaplayer.tools.FriendlyException;
import com.sedmelluq.discord.lavaplayer.track.AudioPlaylist;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import com.sedmelluq.discord.lavaplayer.track.TrackMarker;
import de.mineking.musicquiz.commands.SaveCommand;
import de.mineking.musicquiz.main.MusicQuiz;
import de.mineking.musicquiz.quiz.remote.QuizData;
import de.mineking.musicquiz.quiz.remote.RemoteData;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.channel.concrete.VoiceChannel;
import net.dv8tion.jda.api.events.guild.voice.GuildVoiceUpdateEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.eclipse.jetty.websocket.core.CloseStatus;

import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

public class Quiz extends ListenerAdapter {
	public final static String buzzer = "https://assets.mixkit.co/sfx/preview/mixkit-game-show-wrong-answer-buzz-950.mp3";
	public final static Duration guessDuration = Duration.ofSeconds(15);

	final MusicQuiz bot;

	final List<Track> tracks;
	int position;

	final long master;
	final Map<Long, AtomicInteger> members = new HashMap<>();
	final List<Long> ignore = new ArrayList<>();

	long guesser;
	private Future<?> guesserReset;

	long guessTime = 0;

	final VoiceChannel channel;

	AudioPlayer player;

	public Quiz(MusicQuiz bot, VoiceChannel channel, SaveCommand.SaveData data) {
		this.bot = bot;

		this.tracks = data.tracks();
		this.position = data.position();
		this.master = data.master();
		this.channel = channel;

		channel.getMembers().forEach(this::addMember);
		data.points().forEach((id, score) -> {
			if(id == master) {
				return;
			}

			members.putIfAbsent(id, new AtomicInteger());
			members.get(id).set(score);
		});

		initializeVoiceConnection();
		sendUpdate();

		bot.jda.addEventListener(this);
	}

	private void initializeVoiceConnection() {
		player = bot.audioPlayerManager.createPlayer();
		player.setVolume(30);

		channel.getGuild().getAudioManager().openAudioConnection(channel);
		channel.getGuild().getAudioManager().setSelfDeafened(true);
		channel.getGuild().getAudioManager().setSendingHandler(new SendHandler(player));
	}

	public List<Track> getTracks() {
		return tracks;
	}

	public int getPosition() {
		return position;
	}

	public VoiceChannel getChannel() {
		return channel;
	}

	public Map<Long, AtomicInteger> getMembers() {
		return members;
	}

	public List<Long> getIgnored() {
		return ignore;
	}

	public Long getGuesser() {
		return guesser;
	}

	public long getMaster() {
		return master;
	}

	public void addMember(Member m) {
		if(m.getUser().isBot()) {
			return;
		}

		members.putIfAbsent(m.getIdLong(), new AtomicInteger(m.getIdLong() == master ? Integer.MAX_VALUE : 0));
	}

	public void setGuesser(Member member) {
		if(guesser != 0 || ignore.contains(member.getIdLong())) {
			return;
		}

		player.stopTrack();
		playTrack(buzzer, track -> {});

		ignore.add(member.getIdLong());

		guesser = member.getIdLong();
		guessTime = System.currentTimeMillis();

		if(guesserReset != null) {
			guesserReset.cancel(true);
		}

		guesserReset = MusicQuiz.executor.schedule(() -> {
			guesser = 0;
			guessTime = 0;

			sendUpdate();
		}, guessDuration.toMillis(), TimeUnit.MILLISECONDS);
	}

	public void setVolume(int volume) {
		player.setVolume(volume);
	}

	public void stop() {
		members.forEach((id, points) -> bot.server.gateway.getUserConnections(id).forEach(context -> context.closeSession(CloseStatus.NORMAL, "Quiz has ended!")));

		channel.getGuild().getAudioManager().closeAudioConnection();

		bot.quizzes.remove(this);
		bot.jda.removeEventListener(this);
	}

	public void stopTrack() {
		player.stopTrack();
	}

	public void playTrack(int delta) {
		if(delta > 0) {
			position += delta;
			ignore.clear();
		}

		guesser = 0;
		sendUpdate();

		playTrack();
	}

	private void playTrack() {
		guesser = 0;
		guessTime = 0;

		sendUpdate();

		Track quest = tracks.get(position);

		playTrack(quest.url, track -> {
			track.setPosition(quest.start * 1000);
			if(quest.end > 0) {
				track.setMarker(new TrackMarker((quest.start + quest.end) * 1000, state -> player.stopTrack()));
			}
		});
	}

	private void playTrack(String url, Consumer<AudioTrack> handler) {
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

	public void sendToMember(long member, RemoteData data) {
		bot.server.gateway.getUserConnections(member).forEach(context -> context.send(data));
	}

	public void sendToAll(RemoteData data) {
		members.forEach((id, points) -> {
			if(id == master) {
				return;
			}

			sendToMember(id, data);
		});
	}

	public void sendUpdate() {
		sendToAll(new QuizData(this, false));
		sendToMember(master, new QuizData(this, true));
	}

	@Override
	public void onGuildVoiceUpdate(GuildVoiceUpdateEvent event) {
		if(event.getChannelJoined() != null && event.getChannelJoined().equals(channel)) {
			addMember(event.getMember());
			sendUpdate();
		}

		if(event.getChannelLeft() != null && event.getChannelLeft().equals(channel) && event.getMember().equals(event.getGuild().getSelfMember())) {
			stop();
		}
	}
}
