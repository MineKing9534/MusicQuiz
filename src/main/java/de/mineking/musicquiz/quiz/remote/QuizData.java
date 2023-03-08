package de.mineking.musicquiz.quiz.remote;

import de.mineking.musicquiz.quiz.Quiz;
import de.mineking.musicquiz.quiz.Track;

import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class QuizData extends RemoteData {
	public record RemoteMemberData(String name, Integer points) {
	}

	public final String channel;
	public final Map<String, RemoteMemberData> members;
	public final List<String> ignored;
	public final String guesser;
	public final String master;

	public List<Track> tracks;
	public int position;

	public QuizData(Quiz quiz, boolean master) {
		super(Type.UPDATE);

		this.channel = quiz.getChannel().getName();
		this.members = quiz.getMembers().entrySet().stream()
				.sorted(Comparator.comparing(e -> e.getValue().get(), Comparator.reverseOrder()))
				.collect(Collectors.toMap(
						e -> String.valueOf(e.getKey()),
						e -> new RemoteMemberData(
								quiz.getChannel().getGuild().getMemberById(e.getKey()).getEffectiveName(),
								e.getValue().get()
						),
						(k1, k2) -> k1,
						LinkedHashMap::new
				));

		this.ignored = quiz.getIgnored().stream().map(String::valueOf).toList();
		this.guesser = quiz.getGuesser() != 0 ? String.valueOf(quiz.getGuesser()) : null;
		this.master = String.valueOf(quiz.getMaster());

		this.position = quiz.getPosition();
		this.tracks = master ? quiz.getTracks() : quiz.getTracks().subList(0, position);
	}
}
