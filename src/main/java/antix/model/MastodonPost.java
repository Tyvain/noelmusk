package antix.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.time.ZonedDateTime;
import java.util.List;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class MastodonPost {
    private String id;

    @JsonProperty("created_at")
    private ZonedDateTime createdAt;

    @JsonProperty("in_reply_to_id")
    private String inReplyToId;

    @JsonProperty("in_reply_to_account_id")
    private String inReplyToAccountId;

    private boolean sensitive;

    @JsonProperty("spoiler_text")
    private String spoilerText;

    private String visibility;
    private String language;
    private String uri;
    private String url;

    @JsonProperty("replies_count")
    private int repliesCount;

    @JsonProperty("reblogs_count")
    private int reblogsCount;

    @JsonProperty("favourites_count")
    private int favouritesCount;

    @JsonProperty("edited_at")
    private ZonedDateTime editedAt;

    private boolean favourited;
    private boolean reblogged;
    private boolean muted;
    private boolean bookmarked;
    private String content;
    private List<Tag> tags;
    private Account account;

    @JsonProperty("media_attachments")
    private List<MediaAttachment> mediaAttachments;

    @JsonProperty("filtered")
    private List<Object> filtered;

    @JsonProperty("reblog")
    private MastodonPost reblog;

    @JsonProperty("hide_collections")
    private Boolean hideCollections;

    private List<Mention> mentions;
    private Application application;
    private Card card;

    // ✅ Champs ajoutés pour les besoins de l'interface
    private String source;           // "Mastodon" ou "Google"
    private Integer likes;           // Nombre de likes (pour Google, laissé à null ou 0)
    private Integer comments;        // Nombre de commentaires (optionnel)
    private Integer shares;          // Nombre de partages (optionnel)

    // ✅ Champ ajouté pour /open (Google uniquement)
    private String externalUrl;

    public String getExternalUrl() {
        return externalUrl;
    }

    public void setExternalUrl(String externalUrl) {
        this.externalUrl = externalUrl;
    }
}
