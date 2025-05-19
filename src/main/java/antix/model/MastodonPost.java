package antix.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.Data;

import java.time.ZonedDateTime;
import java.util.List;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class MastodonPost extends Post {
    private String visibility;

    public MastodonPost(JsonNode postNode) {
        super(
            postNode.path("id").asText(),
            ZonedDateTime.parse(postNode.path("created_at").asText()), // Conversion en ZonedDateTime
            postNode.path("account").path("username").asText(),
            postNode.path("content").asText(),
            postNode.path("url").asText(),
            getAttachmentsURL(postNode),
            postNode.path("favourites_count").asInt(), // Likes
            postNode.path("replies_count").asInt() // Commentaires
        );
        this.visibility = postNode.path("visibility").asText();
    }

    private static List<String> getAttachmentsURL(JsonNode postNode) {
        return postNode.path("media_attachments").isArray()
            ? postNode.path("media_attachments").findValuesAsText("url")
            : List.of("No media");
    }

    public String getPostType() {
        return "MastodonPost";
    }
}
