package antix.service;

import antix.model.RedditPost;
import antix.model.SocialMediaPost;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;
import java.util.logging.Level;

@Service
public class RedditService implements SocialMediaService {
    
    private static final Logger logger = Logger.getLogger(RedditService.class.getName());
    private static final String REDDIT_BASE_URL = "https://www.reddit.com";
    private static final String USER_AGENT = "Antix/1.0 by /u/youruser";
    
    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();
    
    @Override
    public List<SocialMediaPost> fetchPostsFromTag(String tag, int limit) {
        System.out.println("🔍 [REDDIT] Recherche pour le tag: '" + tag + "', limite: " + limit);
        
        try {
            // ✅ AMÉLIORATION 1: Essayer plusieurs stratégies de recherche
            List<SocialMediaPost> allPosts = new ArrayList<>();
            
            // Stratégie 1: Recherche dans un subreddit dédié (si c'est un subreddit)
            allPosts.addAll(searchInSubreddit(tag, limit / 2));
            
            // Stratégie 2: Recherche globale
            allPosts.addAll(searchGlobally(tag, limit / 2));
            
            System.out.println("✅ [REDDIT] Total posts trouvés: " + allPosts.size());
            return allPosts.subList(0, Math.min(allPosts.size(), limit));
            
        } catch (Exception e) {
            System.out.println("❌ [REDDIT] ERREUR GLOBALE: " + e.getMessage());
            logger.log(Level.SEVERE, "Erreur Reddit API: " + e.getMessage(), e);
            return new ArrayList<>();
        }
    }
    
    // ✅ NOUVELLE MÉTHODE: Recherche dans un subreddit
    private List<SocialMediaPost> searchInSubreddit(String tag, int limit) {
        try {
            String subredditUrl = UriComponentsBuilder
                .fromHttpUrl(REDDIT_BASE_URL + "/r/" + tag + "/new.json")
                .queryParam("limit", Math.min(limit, 25))
                .build()
                .toUriString();
                
            System.out.println("🎯 [REDDIT] Test subreddit URL: " + subredditUrl);
            return makeRequest(subredditUrl);
            
        } catch (Exception e) {
            System.out.println("⚠️ [REDDIT] Subreddit '" + tag + "' non trouvé ou erreur: " + e.getMessage());
            return new ArrayList<>();
        }
    }
    
    // ✅ NOUVELLE MÉTHODE: Recherche globale améliorée
    private List<SocialMediaPost> searchGlobally(String tag, int limit) {
        try {
            String searchUrl = UriComponentsBuilder
                .fromHttpUrl(REDDIT_BASE_URL + "/search.json")
                .queryParam("q", "title:" + tag + " OR selftext:" + tag)  // ✅ Recherche plus précise
                .queryParam("sort", "new")
                .queryParam("type", "link")                                // ✅ Seulement les posts
                .queryParam("limit", Math.min(limit, 25))
                .build()
                .toUriString();
                
            System.out.println("🌐 [REDDIT] Recherche globale URL: " + searchUrl);
            return makeRequest(searchUrl);
            
        } catch (Exception e) {
            System.out.println("❌ [REDDIT] Erreur recherche globale: " + e.getMessage());
            return new ArrayList<>();
        }
    }
    
    // ✅ MÉTHODE COMMUNE pour les requêtes
    private List<SocialMediaPost> makeRequest(String url) throws Exception {
        HttpHeaders headers = new HttpHeaders();
        headers.set("User-Agent", USER_AGENT);
        HttpEntity<String> entity = new HttpEntity<>(headers);
        
        ResponseEntity<String> response = restTemplate.exchange(
            url, HttpMethod.GET, entity, String.class
        );
        
        System.out.println("📡 [REDDIT] Status: " + response.getStatusCode());
        
        List<SocialMediaPost> posts = parseResponse(response.getBody());
        System.out.println("📄 [REDDIT] Posts parsés: " + posts.size());
        
        return posts;
    }
    
    @Override
    public String getPlatformName() {
        return "reddit";
    }
    
    private List<SocialMediaPost> parseResponse(String jsonBody) {
        List<SocialMediaPost> posts = new ArrayList<>();
        
        try {
            JsonNode root = objectMapper.readTree(jsonBody);
            JsonNode children = root.path("data").path("children");
            
            System.out.println("🔍 [REDDIT] Enfants trouvés dans JSON: " + children.size());
            
            for (JsonNode child : children) {
                JsonNode data = child.path("data");
                RedditPost post = createPost(data);
                if (post != null) {
                    posts.add(post);
                    System.out.println("✅ [REDDIT] Post créé: " + post.getTitle().substring(0, Math.min(50, post.getTitle().length())));
                }
            }
            
        } catch (Exception e) {
            System.out.println("❌ [REDDIT] Erreur parsing JSON: " + e.getMessage());
            logger.log(Level.WARNING, "Erreur parsing: " + e.getMessage(), e);
        }
        
        return posts;
    }
    
    private RedditPost createPost(JsonNode data) {
        try {
            RedditPost post = new RedditPost();
            
            post.setId(data.path("id").asText());
            post.setTitle(data.path("title").asText());
            post.setAuthor(data.path("author").asText());
            post.setSubreddit(data.path("subreddit").asText());
            post.setScore(data.path("score").asInt());
            post.setNumComments(data.path("num_comments").asInt());
            post.setPermalink(data.path("permalink").asText());
            post.setPostUrl(data.path("url").asText());
            long createdUtc = data.path("created_utc").asLong();
            post.setCreatedUtc(createdUtc);
            
            // Contenu = titre + description
            String content = data.path("title").asText();
            String selftext = data.path("selftext").asText();
            if (!selftext.isEmpty()) {
                content += "\n\n" + selftext;
            }
            post.setContent(content);
            
            return post;
            
        } catch (Exception e) {
            System.out.println("❌ [REDDIT] Erreur création post: " + e.getMessage());
            logger.log(Level.WARNING, "Erreur création post: " + e.getMessage());
            return null;
        }
    }
}
