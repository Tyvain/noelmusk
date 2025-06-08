package antix.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;
import java.time.Instant;
import java.util.List;
import java.util.ArrayList;
import java.util.regex.Pattern;
import java.util.regex.Matcher;

@Data
@EqualsAndHashCode(callSuper = true)
@JsonIgnoreProperties(ignoreUnknown = true)
public class RedditPost extends SocialMediaPost {

    @JsonProperty("title")
    private String title;

    @JsonProperty("selftext")
    private String selftext;

    @JsonProperty("author")
    private String author; // ✅ Ajouter ce champ

    @JsonProperty("subreddit")
    private String subreddit;

    @JsonProperty("subreddit_name_prefixed")
    private String subredditPrefixed; // ✅ Ajouter ce champ

    @JsonProperty("created_utc")
    private Long createdUtc; // ✅ Ajouter ce champ

    @JsonProperty("ups")
    private Integer upvotes; // ✅ Renommer pour éviter confusion

    @JsonProperty("downs")
    private Integer downvotes; // ✅ Renommer pour éviter confusion

    @JsonProperty("score")
    private Integer score;

    @JsonProperty("num_comments")
    private Integer numComments;

    @JsonProperty("permalink")
    private String permalink;

    @JsonProperty("url")
    private String postUrl;

    @JsonProperty("domain")
    private String domain; // ✅ Ajouter ce champ

    @JsonProperty("is_video")
    private Boolean isVideo; // ✅ Ajouter ce champ

    @JsonProperty("over_18")
    private Boolean nsfw; // ✅ Ajouter ce champ

    // Constructeur par défaut
    public RedditPost() {
    }

    // ✅ Méthodes pour gérer la conversion de timestamp
    public void setCreatedUtc(Long createdUtc) {
        this.createdUtc = createdUtc;
        if (createdUtc != null) {
            this.createdAt = LocalDateTime.ofInstant(
                    Instant.ofEpochSecond(createdUtc),
                    java.time.ZoneOffset.UTC);
        }
    }

    // ✅ Créer un Account à partir de l'auteur Reddit
    public void setAuthor(String author) {
        this.author = author;
        if (author != null) {
            Account redditAccount = new Account();
            redditAccount.setUsername(author);
            redditAccount.setDisplayName(author);
            // Reddit n'a pas d'avatar par défaut, utiliser une URL générique
            redditAccount.setAvatar("https://www.redditstatic.com/avatars/defaults/v2/avatar_default_0.png");
            super.setAccount(redditAccount);
        }
    }

    @Override
    public Integer getRepliesCount() {
        return numComments != null ? numComments : 0;
    }

    @Override
    public Integer getShareCount() {
        return 0; // Reddit n'a pas de partages directs
    }

    @Override
    public Integer getLikeCount() {
        return score != null ? score : 0;
    }

    @Override
    public String getPlatform() {
        return "reddit";
    }

    @Override
    public String getDisplayName() {
        return this.author != null ? this.author : "Inconnu";
    }

    @Override
    public String getPlatformInfo() {
        return "r/" + (this.subreddit != null ? this.subreddit : "unknown");
    }

    @Override
    public String getEngagementText() {
        return String.format("💬 %d   ⬆ %d",
                this.numComments != null ? this.numComments : 0,
                this.score != null ? this.score : 0);
    }

    @Override
    public String getScoreText() {
        return "Score: " + (this.score != null ? this.score : 0);
    }

    @Override
    public List<Tag> getTags() {
        List<Tag> extractedTags = new ArrayList<>();

        // Extraire les hashtags du titre
        if (title != null) {
            extractedTags.addAll(extractHashtagsFromText(title));
        }

        // Extraire les hashtags du contenu
        if (selftext != null) {
            extractedTags.addAll(extractHashtagsFromText(selftext));
        }

        // Ajouter le subreddit comme tag
        if (subreddit != null) {
            Tag subredditTag = new Tag();
            subredditTag.setName(subreddit);
            extractedTags.add(subredditTag);
        }

        return extractedTags;
    }

    @Override
    public String getUrl() {
        if (permalink != null) {
            return "https://reddit.com" + permalink;
        }
        if (postUrl != null && postUrl.startsWith("http")) {
            return postUrl;
        }
        return "https://reddit.com/r/" + (subreddit != null ? subreddit : "unknown");
    }

    @Override
    public String getContent() {
        StringBuilder content = new StringBuilder();
        if (title != null) {
            content.append(title);
        }
        if (selftext != null && !selftext.trim().isEmpty()) {
            if (content.length() > 0) {
                content.append("\n\n");
            }
            content.append(selftext);
        }
        super.setContent(content.toString());
        return content.toString();
    }

    /**
     * Extrait les hashtags d'un texte
     */
    private List<Tag> extractHashtagsFromText(String text) {
        List<Tag> tags = new ArrayList<>();
        Pattern pattern = Pattern.compile("#\\w+");
        Matcher matcher = pattern.matcher(text);

        while (matcher.find()) {
            String hashtag = matcher.group().substring(1);
            Tag tag = new Tag();
            tag.setName(hashtag);
            tags.add(tag);
        }

        return tags;
    }

    @Override
    public String getBadgeColor() {
        return "#FF4500";
    }

    @Override
    public String getBadgeTextColor() {
        return "white";
    }

    @Override
    public String getLogoPath() {
        return "images/reddit-logo.svg";
    }
    @Override
    public String getPlatformShortCode() {
        return "R";
    }

    @Override
    public String getPlatformDisplayName() {
        return "Reddit";
    }
}
