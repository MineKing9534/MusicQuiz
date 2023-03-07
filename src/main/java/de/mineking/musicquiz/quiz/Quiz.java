package de.mineking.musicquiz.quiz;

import com.sedmelluq.discord.lavaplayer.player.AudioLoadResultHandler;
import com.sedmelluq.discord.lavaplayer.player.AudioPlayer;
import com.sedmelluq.discord.lavaplayer.tools.FriendlyException;
import com.sedmelluq.discord.lavaplayer.track.AudioPlaylist;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import com.sedmelluq.discord.lavaplayer.track.TrackMarker;
import de.mineking.musicquiz.main.MusicQuiz;
import de.mineking.musicquiz.quiz.remote.QuizData;
import de.mineking.musicquiz.quiz.remote.RemoteData;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.channel.concrete.VoiceChannel;
import net.dv8tion.jda.api.events.guild.voice.GuildVoiceUpdateEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.eclipse.jetty.websocket.core.CloseStatus;

import java.time.Duration;
import java.util.*;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

public class Quiz extends ListenerAdapter {
	public final static String buzzer = "https://assets.mixkit.co/sfx/preview/mixkit-game-show-wrong-answer-buzz-950.mp3";
	public final static Duration guessDuration = Duration.ofSeconds(15);

	final MusicQuiz bot;

	final List<Track> tracks;
	int position = 0;

	final long master;
	final Map<Long, MemberData> members = new HashMap<>();
	final List<Long> ignore = new ArrayList<>();

	long guesser;
	private Future<?> guesserReset;

	long guessTime = 0;

	final VoiceChannel channel;

	AudioPlayer player;

	public Quiz(MusicQuiz bot, VoiceChannel channel, List<Track> tracks, Member master) {
		this.bot = bot;

		this.tracks = tracks;
		this.master = master.getIdLong();
		this.channel = channel;
		channel.getMembers().forEach(this::addMember);

		initializeVoiceConnection();

		bot.server.gateway.data.forEach((context, user) -> {
			if(members.containsKey(user.user)) {
				user.quiz = this;
				user.member = members.get(user.user);

				user.member.remote = context;
			}
		});

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

	public Map<Long, MemberData> getMembers() {
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

		members.putIfAbsent(m.getIdLong(), new MemberData(m.getIdLong() == master ? Integer.MAX_VALUE : 0));
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
		members.forEach((m, data) -> {
			if(data.remote != null) {
				data.remote.closeSession(CloseStatus.NORMAL, "Quiz has ended!");
				bot.server.gateway.data.remove(data.remote);
			}
		});

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
		members.get(member).send(data);
	}

	public void sendToAll(RemoteData data) {
		members.forEach((id, m) -> {
			if(id == master) {
				return;
			}

			m.send(data);
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
