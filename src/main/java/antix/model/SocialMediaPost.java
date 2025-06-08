package antix.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import java.time.LocalDateTime;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonTypeInfo(
    use = JsonTypeInfo.Id.NAME,
    include = JsonTypeInfo.As.PROPERTY,
    property = "platform",
    defaultImpl = MastodonPost.class
)
@JsonSubTypes({
    @JsonSubTypes.Type(value = MastodonPost.class, name = "mastodon"),
    @JsonSubTypes.Type(value = RedditPost.class, name = "reddit")
})
public abstract class SocialMediaPost {
    
    @JsonProperty("id")
    protected String id;
    
    @JsonProperty("content")
    protected String content;
    
    @JsonProperty("created_at")
    protected LocalDateTime createdAt;
    
    @JsonProperty("account")
    protected Account account;
    
    @JsonProperty("language")
    protected String language;

    // Constructeur par défaut
    public SocialMediaPost() {}

    // Méthodes abstraites que chaque plateforme doit implémenter
    public abstract Integer getRepliesCount();
    public abstract Integer getShareCount();
    public abstract Integer getLikeCount();
    public abstract String getPlatform();
    public abstract List<Tag> getTags();
    public abstract String getUrl(); 
    public abstract String getAuthor();
    public abstract String getDisplayName();
    public abstract String getPlatformInfo();
    public abstract String getEngagementText();
    public abstract String getScoreText();
    public abstract String getBadgeColor();
    public abstract String getBadgeTextColor();
    public abstract String getLogoPath();
    public abstract String getPlatformShortCode();
    public abstract String getPlatformDisplayName();
    // Getters et setters pour les champs communs
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public Account getAccount() {
        return account;
    }

    public void setAccount(Account account) {
        this.account = account;
    }

    public String getLanguage() {
        return language;
    }

    public void setLanguage(String language) {
        this.language = language;
    }

    @Override
    public String toString() {
        return String.format("%s[id=%s, platform=%s]", 
                           getClass().getSimpleName(), id, getPlatform());
    }
    public String getFormattedDate() {
    if (getCreatedAt() == null) return "Date inconnue";
    
    try {
        java.time.format.DateTimeFormatter formatter = 
            java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy à HH:mm");
        
        return getCreatedAt().format(formatter);
    } catch (Exception e) {
        return "Date invalide";
    }
}
}
