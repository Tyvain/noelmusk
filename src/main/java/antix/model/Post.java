package antix.model;

import java.time.ZonedDateTime;
import java.util.List;

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

    public Post(String id, ZonedDateTime createdAt, String author, String content, String url, List<String> mediaUrl, int likes, int numComments) {
        this.id = id;
        this.createdAt = createdAt;
        this.author = author;
        this.content = content;
        this.url = url;
        this.mediaUrl = mediaUrl;
        this.likes = likes;
        this.numComments = numComments;
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
}
