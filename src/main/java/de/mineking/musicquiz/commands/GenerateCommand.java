package de.mineking.musicquiz.commands;

import de.mineking.discord.commands.commands.global.GlobalSlashCommand;
import de.mineking.discord.commands.context.global.GlobalSlashContext;
import de.mineking.discord.commands.option.Option;
import de.mineking.musicquiz.main.Messages;
import de.mineking.musicquiz.main.MusicQuiz;
import de.mineking.musicquiz.quiz.Track;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.utils.FileUpload;
import se.michaelthelin.spotify.SpotifyApi;
import se.michaelthelin.spotify.model_objects.IPlaylistItem;
import se.michaelthelin.spotify.model_objects.specification.ArtistSimplified;
import se.michaelthelin.spotify.model_objects.specification.PlaylistSimplified;
import se.michaelthelin.spotify.model_objects.specification.PlaylistTrack;

import java.nio.charset.StandardCharsets;
import java.util.Random;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class GenerateCommand extends GlobalSlashCommand {
	public final static String format = "%s?t=%s +%ss \"%s\" - \"%s\"";
	public final static Random random = new Random();

	private final MusicQuiz bot;

	public GenerateCommand(MusicQuiz bot) {
		this.bot = bot;

		visibility = Visibility.GUILD_ONLY;

		addOption(new Option(OptionType.INTEGER, "size", true));
	}

	private Track getTrack(SpotifyApi api, PlaylistSimplified playlist) throws Exception {
		PlaylistTrack[] tracks = api.getPlaylistsItems(playlist.getId()).build().execute().getItems();

		IPlaylistItem track = tracks[random.nextInt(tracks.length)].getTrack();

		int length = 3 + random.nextInt(5);

		return new Track(
				"https://open.spotify.com/track/" + track.getId(),
				random.nextInt(track.getDurationMs() / 1000 - length),
				length,
				track.getName(),
				Stream.of(api.getTrack(track.getId()).build().execute().getArtists())
						.map(ArtistSimplified::getName)
						.collect(Collectors.joining(", "))
		);
	}

	@Override
	protected void performCommand(GlobalSlashContext context) {
		context.event.deferReply(true).queue();

		bot.spotify.api(api -> {
			try {
				PlaylistSimplified[] playlists = api.searchPlaylists("Charts").build().execute().getItems();

				StringBuilder content = new StringBuilder();

				for(int i = 0; i < context.getOption("size", OptionMapping::getAsInt); i++) {
					Track track = getTrack(api, playlists[random.nextInt(playlists.length)]);

					content.append(format.formatted(track.url, track.start, track.end, track.title, track.author)).append("\n");
				}

				context.event.getHook().editOriginalAttachments(FileUpload.fromData(content.toString().getBytes(StandardCharsets.UTF_8), "quiz.txt")).queue();
			} catch(Exception e) {
				e.printStackTrace();
				Messages.send(context.event, "error", Messages.Color.ERROR, e.getMessage());
			}
		});
	}
}
