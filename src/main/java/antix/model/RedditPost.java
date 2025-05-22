package antix.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.Data;

import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class RedditPost extends Post {
    private String subreddit;
    private String title;

    public RedditPost(JsonNode postNode, Set<String> EXPLICIT_WORDS) {
        super(
            postNode.path("data").path("id").asText(),
            ZonedDateTime.ofInstant(Instant.ofEpochSecond(postNode.path("data").path("created_utc").asLong()), ZoneId.of("UTC")),
            postNode.path("data").path("author").asText(),
            postNode.path("data").path("selftext").asText(),
            "https://www.reddit.com" + postNode.path("data").path("permalink").asText(),
            getAttachmentsURL(postNode),
            postNode.path("data").path("ups").asInt(),
            postNode.path("data").path("num_comments").asInt(),
            postNode.path("over_18").asBoolean(false),
            EXPLICIT_WORDS
        );
        this.subreddit = postNode.path("data").path("subreddit").asText();
        this.title = postNode.path("data").path("title").asText();
        this.setRating(ratePostLevel(EXPLICIT_WORDS));
    }

    private static List<String> getAttachmentsURL(JsonNode postNode) {
        List<String> attachments = new ArrayList<>();

        // Vérifier si le post contient une URL principale
        if (postNode.path("data").has("url_overridden_by_dest")) {
            attachments.add(postNode.path("data").path("url_overridden_by_dest").asText());
        }

        // Vérifier si le post contient des previews d'images
        if (postNode.path("data").has("preview")) {
            JsonNode previewNode = postNode.path("data").path("preview").path("images");
            for (JsonNode imageNode : previewNode) {
                attachments.add(imageNode.path("source").path("url").asText());
            }
        }

        // Vérifier si le post contient des médias (GIFs, vidéos)
        if (postNode.path("data").has("media_metadata")) {
            JsonNode mediaNode = postNode.path("data").path("media_metadata");
            mediaNode.fields().forEachRemaining(entry -> {
                JsonNode media = entry.getValue();
                if (media.has("s")) {
                    attachments.add(media.path("s").path("u").asText());
                }
            });
        }

        return attachments.isEmpty() ? List.of("No media") : attachments;
    }

    public String getPostType() {
        return "RedditPost";
    }
}
