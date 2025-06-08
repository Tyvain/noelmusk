
package antix.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.ZonedDateTime;
import java.util.List;


import java.util.Collections;

@Data
@EqualsAndHashCode(callSuper = true)
@JsonIgnoreProperties(ignoreUnknown = true)
public class MastodonPost extends SocialMediaPost {

    @JsonProperty("in_reply_to_id")
    private String inReplyToId;

    @JsonProperty("in_reply_to_account_id")
    private String inReplyToAccountId;

    private boolean sensitive;

    @JsonProperty("spoiler_text")
    private String spoilerText;

    private String visibility;
    private String uri;
    private String url; // ✅ Le champ url existe déjà

    @JsonProperty("replies_count")
    private Integer repliesCount;

    @JsonProperty("reblogs_count")
    private Integer reblogsCount;

    @JsonProperty("favourites_count")
    private Integer favouritesCount;

    @JsonProperty("edited_at")
    private ZonedDateTime editedAt;

    private boolean favourited;
    private boolean reblogged;
    private boolean muted;
    private boolean bookmarked;

    private List<Tag> tags; // ✅ Le champ tags existe déjà

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

    // Constructeur par défaut
    public MastodonPost() {
    }

    // ✅ Implémentation des méthodes abstraites
    @Override
    public Integer getRepliesCount() {
        return repliesCount;
    }

    @Override
    public Integer getShareCount() {
        return reblogsCount;
    }

    @Override
    public Integer getLikeCount() {
        return favouritesCount;
    }

    @Override
    public String getPlatform() {
        return "mastodon";
    }

    @Override
    public List<Tag> getTags() {
        return tags != null ? tags : Collections.emptyList();
    }

    @Override
    public String getUrl() {
        return url; // ✅ Utilise le champ url existant
    }

    // ✅ Gestion de la conversion ZonedDateTime -> LocalDateTime
    @JsonProperty("created_at")
    public void setCreatedAtFromJson(ZonedDateTime zonedDateTime) {
        if (zonedDateTime != null) {
            this.createdAt = zonedDateTime.toLocalDateTime();
        }
    }

    @JsonProperty("created_at")
    public ZonedDateTime getCreatedAtAsZoned() {
        return createdAt != null ? createdAt.atZone(java.time.ZoneOffset.UTC) : null;
    }

    // ✅ Synchroniser les champs avec la classe parent
    @Override
    public String getId() {
        return super.getId();
    }

    @Override
    public void setId(String id) {
        super.setId(id);
    }

    @Override
    public String getContent() {
        return super.getContent();
    }

    @Override
    public void setContent(String content) {
        super.setContent(content);
    }

    @Override
    public Account getAccount() {
        return super.getAccount();
    }

    @Override
    public String getAuthor() {
        return getAccount() != null ? getAccount().getDisplayName() : "Utilisateur inconnu";
    }

    @Override
    public void setAccount(Account account) {
        super.setAccount(account);
    }

    @Override
    public String getLanguage() {
        return super.getLanguage();
    }

    @Override
    public void setLanguage(String language) {
        super.setLanguage(language);
    }

    @Override
    public String getDisplayName() {
        return this.account != null && this.account.getDisplayName() != null ? this.account.getDisplayName()
                : "Inconnu";
    }

    @Override
    public String getPlatformInfo() {
        return "@"
                + (this.account != null && this.account.getUsername() != null ? this.account.getUsername() : "inconnu");
    }

    @Override
    public String getEngagementText() {
        return String.format("💬 %d   ⭐ %d   🔄 %d",
                this.getRepliesCount(),
                this.favouritesCount != null ? this.favouritesCount : 0,
                this.reblogsCount != null ? this.reblogsCount : 0);
    }

    @Override
    public String getScoreText() {
        return "Likes: " + (this.favouritesCount != null ? this.favouritesCount : 0);
    }

    @Override
    public String getBadgeColor() {
        return "#6364FF";
    }

    @Override
    public String getBadgeTextColor() {
        return "white";
    }

    @Override
    public String getLogoPath() {
        return "images/mastodon-logo.svg";
    }

    @Override
    public String getPlatformShortCode() {
        return "M";
    }

    @Override
    public String getPlatformDisplayName() {
        return "Mastodon";
    }
}
