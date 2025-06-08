package antix.service;

import antix.model.SocialMediaPost;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.apache.http.client.utils.URIBuilder;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Logger;

public class MastodonService implements SocialMediaService {
    
    private static final Logger logger = Logger.getLogger(MastodonService.class.getName());
    @Override
    public List<SocialMediaPost> fetchPostsFromTag(String tag, int limit) {
        try {
            var uri = new URIBuilder("https://mastodon.social/api/v1/timelines/tag/" + tag)
                    .addParameter("limit", String.valueOf(limit))
                    .build();

            URL url = uri.toURL();
            HttpURLConnection con = (HttpURLConnection) url.openConnection();
            con.setRequestMethod("GET");

            BufferedReader reader = new BufferedReader(new InputStreamReader(con.getInputStream()));
            StringBuilder response = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                response.append(line);
            }
            reader.close();

            ObjectMapper mapper = new ObjectMapper();
            mapper.registerModule(new JavaTimeModule());
            
            return Arrays.stream(mapper.readValue(response.toString(), SocialMediaPost[].class)).toList();

        } catch (Exception e) {
            logger.severe("Erreur lors de la récupération des posts: " + e.getMessage());
            return List.of();
        }
    }



    @Override
    public String getPlatformName() {  // ✅ Correct : getPlatformName (pas getServiceName)
        return "Mastodon";
    }
}