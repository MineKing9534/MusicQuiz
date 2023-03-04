package de.mineking.musicquiz.quiz;

import de.mineking.musicquiz.main.Messages;
import de.mineking.musicquiz.quiz.remote.EventData;
import net.dv8tion.jda.api.events.guild.voice.GuildVoiceUpdateEvent;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.StringSelectInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;

public class EventHandler extends ListenerAdapter {
	private final Quiz quiz;

	public EventHandler(Quiz quiz) {
		this.quiz = quiz;
	}

	@Override
	public void onGuildVoiceUpdate(GuildVoiceUpdateEvent event) {
		if(event.getChannelJoined() != null && event.getChannelJoined().equals(quiz.channel)) {
			quiz.addMember(event.getMember());

			if(quiz.isStarted()) {
				quiz.getMessages().updatePublicMessage(null);
			}
		}

		if(event.getChannelLeft() != null && event.getChannelLeft().equals(quiz.channel) && event.getMember().equals(event.getGuild().getSelfMember())) {
			quiz.stop();
		}
	}

	@Override
	public void onButtonInteraction(ButtonInteractionEvent event) {
		String[] temp = event.getComponentId().split(":");

		if(!temp[0].equals(quiz.channel.getId())) {
			return;
		}

		switch(temp[1]) {
			case "start":
				if(event.getMember().getIdLong() != quiz.master) {
					Messages.send(event, "start.invalid", Messages.Color.ERROR);

					return;
				}

				quiz.start(event);

				break;

			case "control":
				if(event.getMember().getIdLong() != quiz.master) {
					Messages.send(event, "start.invalid", Messages.Color.ERROR);

					return;
				}

				quiz.getMessages().deletePrivateMessage();
				quiz.getMessages().sendPrivateMessage(event);

				break;

			case "guess":
				if(quiz.guesser != 0) {
					Messages.send(event, "guess.invalid.given", Messages.Color.ERROR);

					return;
				}

				if(quiz.ignore.contains(event.getMember().getIdLong())) {
					Messages.send(event, "guess.invalid.ignore", Messages.Color.ERROR);

					return;
				}

				quiz.setGuesser(event.getMember());
				quiz.getMessages().updateMessages(event, MessageManager.Mode.PUBLIC);

				break;

			case "correct":
				Track track = quiz.tracks.get(quiz.position);

				quiz.sendToAll(new EventData(EventData.Action.SOLUTION)
						.put("url", track.url + "?t=" + track.start)
						.put("title", track.title)
						.put("author", track.author)
				);

				quiz.messages.updatePrivateMessage(event);

				break;

			case "freeuser":
				quiz.ignore.remove(quiz.selected);

				quiz.getMessages().updateMessages(event, MessageManager.Mode.PRIVATE);

				break;

			case "freeall":
				quiz.ignore.clear();

				quiz.getMessages().updateMessages(event, MessageManager.Mode.PRIVATE);

				break;

			case "add":
				quiz.members.get(quiz.selected).points.incrementAndGet();
				quiz.getMessages().updateMessages(event, MessageManager.Mode.PRIVATE);
				break;

			case "remove":
				quiz.members.get(quiz.selected).points.decrementAndGet();
				quiz.getMessages().updateMessages(event, MessageManager.Mode.PRIVATE);
				break;

			case "next":
				quiz.position++;
				quiz.ignore.clear();

			case "restart":
				quiz.guesser = 0;

				Messages.send(event, "track.start", Messages.Color.SUCCESS);
				quiz.getMessages().updateMessages(null, null);

				quiz.playTrack();

				break;

			case "extend":
				quiz.tracks.get(quiz.position).end++;
				quiz.getMessages().updatePrivateMessage(event);
				break;

			case "run":
				quiz.tracks.get(quiz.position).end = 0;
				quiz.playTrack();
				quiz.getMessages().updatePrivateMessage(event);
				break;

			case "cancel":
				quiz.player.stopTrack();
				quiz.getMessages().updatePrivateMessage(event);
				break;

			case "stop":
				quiz.stop();
				break;
		}
	}

	@Override
	public void onStringSelectInteraction(StringSelectInteractionEvent event) {
		String[] temp = event.getComponentId().split(":");

		if(!temp[0].equals(quiz.getChannel().getId())) {
			return;
		}

		if(temp[1].equals("select")) {
			event.deferEdit().queue();

			quiz.selected = quiz.channel.getGuild().retrieveMemberById(event.getSelectedOptions().get(0).getValue()).complete().getIdLong();

			quiz.messages.updatePrivateMessage(null);
		}
	}
}
