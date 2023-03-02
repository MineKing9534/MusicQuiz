package de.mineking.musicquiz.quiz.remote;

import de.mineking.musicquiz.quiz.Quiz;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class QuizData extends RemoteData {
	public record RemoteMemberData(String name, Integer points) {
	}

	public Map<String, RemoteMemberData> members;
	public List<String> ignored;
	public String guesser;

	public QuizData(Quiz quiz) {
		super(Type.UPDATE);

		this.members = quiz.getMembers().entrySet().stream()
				.collect(
						Collectors.toMap(
								e -> String.valueOf(e.getKey()),
								e -> new RemoteMemberData(
										quiz.getChannel().getGuild().getMemberById(e.getKey()).getEffectiveName(),
										e.getValue().points.get()
								)
						)
				);

		this.ignored = quiz.getIgnored().stream().map(String::valueOf).toList();
		this.guesser = quiz.getGuesser() != 0 ? String.valueOf(quiz.getGuesser()) : null;
	}
}
