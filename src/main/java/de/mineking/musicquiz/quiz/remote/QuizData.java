package de.mineking.musicquiz.quiz.remote;

import de.mineking.musicquiz.quiz.MemberData;
import de.mineking.musicquiz.quiz.Quiz;

import java.util.*;
import java.util.function.BiConsumer;
import java.util.function.BinaryOperator;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collector;

public class QuizData extends RemoteData {
	public record RemoteMemberData(String name, Integer points) {
	}

	public final String channel;
	public final Map<String, RemoteMemberData> members;
	public final List<String> ignored;
	public final String guesser;

	public QuizData(Quiz quiz) {
		super(Type.UPDATE);

		this.channel = quiz.getChannel().getName();
		this.members = quiz.getMembers().entrySet().stream()
				.sorted(Comparator.comparing(e -> e.getValue().points.get(), Comparator.reverseOrder()))
				.collect(new Collector<Map.Entry<Long, MemberData>, Map<String, RemoteMemberData>, Map<String, RemoteMemberData>>() {
					@Override
					public Supplier<Map<String, RemoteMemberData>> supplier() {
						return LinkedHashMap::new;
					}

					@Override
					public BiConsumer<Map<String, RemoteMemberData>, Map.Entry<Long, MemberData>> accumulator() {
						return (m, d) -> m.putIfAbsent(String.valueOf(d.getKey()), new RemoteMemberData(
								quiz.getChannel().getGuild().getMemberById(d.getKey()).getEffectiveName(),
								d.getValue().points.get()
						));
					}

					@Override
					public BinaryOperator<Map<String, RemoteMemberData>> combiner() {
						return (m1, m2) -> {
							m2.forEach(m1::putIfAbsent);
							return m1;
						};
					}

					@Override
					public Function<Map<String, RemoteMemberData>, Map<String, RemoteMemberData>> finisher() {
						return m -> m;
					}

					@Override
					public Set<Characteristics> characteristics() {
						return Collections.singleton(Characteristics.IDENTITY_FINISH);
					}
				});

		this.ignored = quiz.getIgnored().stream().map(String::valueOf).toList();
		this.guesser = quiz.getGuesser() != 0 ? String.valueOf(quiz.getGuesser()) : null;
	}
}
