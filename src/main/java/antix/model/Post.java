package antix.model;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.Set;

import lombok.Data;

@Data
public abstract class Post {
    private String id;
    private ZonedDateTime createdAt;
    private String author;
    private String content;
    private String url;
    private List<String> mediaUrl;
    private int likes;
    private int numComments;
    private boolean isNSFW;
    private String rating;

    public Post(String id, ZonedDateTime createdAt, String author, String content, String url, List<String> mediaUrl, int likes, int numComments, boolean isNSFW, Set<String> EXPLICIT_WORDS) {
        this.id = id;
        this.createdAt = createdAt;
        this.author = author;
        this.content = content;
        this.url = url;
        this.mediaUrl = mediaUrl;
        this.likes = likes;
        this.numComments = numComments;
        this.isNSFW = isNSFW;
        this.rating = ratePostLevel(EXPLICIT_WORDS);
    }

    @Override
    public String toString() {
        return "Post ID: " + id + "\n" +
               "Created At: " + createdAt + "\n" +
               "Author: " + author + "\n" +
               "Content: " + content + "\n" +
               "URL: " + url + "\n" +
               "Media: " + mediaUrl + "\n";
    }
    
    public abstract String getPostType();

    public boolean fromReddit() {
        return getPostType().equals("RedditPost");
    }

    public String ratePostLevel(Set<String> EXPLICIT_WORDS) {
        if (this.isNSFW) {
            return "NSFW"; // NSFW
        }

        String content = this.content != null ? this.content : "";

        if (this instanceof RedditPost) {
            RedditPost redditPost = (RedditPost) this;
            content += " " + redditPost.getTitle();
        }

        content = content.toLowerCase();

        for (String explicitWord : EXPLICIT_WORDS) {
            if (content.contains(explicitWord)) {
                if (explicitWord.equals("nsfw")) {
                    this.setNSFW(true);
                    return "NSFW";
                }
                return "Sensible"; // Sensible
            }
        }

        return "Sûr"; // Sûr
    }
}
