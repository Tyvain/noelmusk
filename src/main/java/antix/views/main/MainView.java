package antix.views.main;

import antix.model.MastodonPost;
import antix.model.Post;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.client.utils.URIBuilder;
import org.jsoup.Jsoup;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import antix.model.RedditPost;

@PageTitle("main")
@Route("")
public class MainView extends VerticalLayout {
    private final Div output;
    private List<Post> currentPosts = new ArrayList<>();
    private int currentIndex = 0;
    private boolean allowNsfw = false;

    private final List<String> commandHistory = new ArrayList<>();
    private int historyIndex = -1;

    private int currentPage = 0;
    private static final int MAX_LENGTH = 75;
    private static final int PAGE_SIZE = 10;
    private static final int maxPostPerMedia = 20;
    private static final String PostPerMedia = "60";

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yy 'à' HH:mm");

    private static final Set<String> EXPLICIT_WORDS = new HashSet<>();

    static {
        ObjectMapper mapper = new ObjectMapper();
        try (InputStream is = Post.class.getClassLoader().getResourceAsStream("explicit_words.json")) {
            if (is == null) {
                System.err.println("Erreur : explicit_words.json introuvable dans le classpath !");
            } else {
                java.util.Map<String, List<String>> data = mapper.readValue(is, new TypeReference<java.util.Map<String, List<String>>>() {});
                if (data != null && data.containsKey("EXPLICIT_WORDS")) {
                    EXPLICIT_WORDS.addAll(data.get("EXPLICIT_WORDS"));
                } else {
                    System.err.println("Erreur : La clé 'EXPLICIT_WORDS' est manquante ou vide dans le fichier JSON.");
                }
            }
        } catch (IOException e) {
            System.err.println("Erreur lors du chargement de explicit_words.json: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    public MainView() {
        setSizeFull();
        output = new Div();
        output.getStyle().set("white-space", "pre-wrap");
        output.getStyle().set("overflow-y", "auto");
        output.setHeight("80vh");
        output.setWidthFull();

        output.setEnabled(false);

        var prompt = new TextField();
        prompt.setPlaceholder("Entrez une commande...");
        prompt.setWidthFull();

        AtomicBoolean internalChange = new AtomicBoolean(false);

        output.setText("");
        output.getElement().setProperty("innerHTML", getHelpTableHtml());

        prompt.getElement().addEventListener("keydown", e -> {
            try {
               String key = e.getEventData().getString("event.key");

                if (keyIsEqual(key, "ArrowUp")) {
                    if (!commandHistory.isEmpty()) {
                        historyIndex = Math.max(0, historyIndex - 1);
                        internalChange.set(true);
                        prompt.setValue(commandHistory.get(historyIndex));
                        internalChange.set(false);
                    }
                } else if (keyIsEqual(key, "ArrowDown")) {
                    if (!commandHistory.isEmpty()) {
                        historyIndex = Math.min(commandHistory.size(), historyIndex + 1);
                        internalChange.set(true);
                        if (historyIndex < commandHistory.size()) {
                            prompt.setValue(commandHistory.get(historyIndex));
                        } else {
                            prompt.setValue("");
                        }
                        internalChange.set(false);
                    }
                } else if (keyIsEqual(key,"ArrowLeft")) {
                    navigatePage(-1);
                } else if (keyIsEqual(key, "ArrowRight")) {
                    navigatePage(1);
                } else if (keyIsEqual(key,"Enter")) {
                    String text = prompt.getValue().trim();

                    if (!text.isEmpty()) {
                        commandHistory.add(text);
                        historyIndex = commandHistory.size();
                    }

                    internalChange.set(true);
                    prompt.setValue("");
                    internalChange.set(false);

                    handleCommand(text);
                } 
            } catch (NullPointerException Ne) {
                Logger.getLogger(MainView.class.getName()).log(Level.WARNING, "NullPointerException ignorée", e);
            }
        }).addEventData("event.key");

        var container = new VerticalLayout();
        container.setSizeFull();
        container.setSpacing(false);
        container.setPadding(false);
        container.setMargin(false);
        container.add(output);
        add(container);
        add(prompt);
    }

    private void handleCommand(String text) {
        if (text.equals("n") || text.equals("next")) {
            navigate(1);
        } else if (text.equals("p") || text.equals("previous")) {
            navigate(-1);
        } else if (text.equals("list") || text.equals("l")) {
            displayPostSummary();
        } else if (text.startsWith("goto ")) {
            try {
                int index = Integer.parseInt(text.substring(5).trim()) - 1;
                if (index >= 0 && index < currentPosts.size()) {
                    currentIndex = index;
                    renderCurrentPost();
                } else {
                    output.setText("Index hors limites.");
                }
            } catch (NumberFormatException e) {
                output.setText("Format invalide pour goto.");
            }
        } else if (text.equals("sort like")) {
            currentPosts.sort(Comparator.comparingInt(Post::getLikes).reversed());
            currentIndex = 0;
            currentPage = 0;
            displayPostSummary();
        } else if (text.equals("sort date")) {
            currentPosts.sort(Comparator.comparing(Post::getCreatedAt).reversed());
            currentIndex = 0;
            currentPage = 0;
            displayPostSummary();
        } else if (text.equals("help")) {
            output.getElement().setProperty("innerHTML", getHelpTableHtml());
        } else if (text.equals("clear")) {
            output.setText("");
        } else if (text.equals("allow nsfw")) {
            toggle_nsfw(true);
        } else if (text.equals("unallow nsfw")) {
            toggle_nsfw(false);
        } else if (text.startsWith("s ")) {
            String query = text.substring(2).trim();
            boolean isAnd = query.contains("&");
            
            List<String> tags = getTagsFromQuery(query, isAnd);
            currentPosts = new ArrayList<>();
            addPostsFromSocialMedia(currentPosts, tags, isAnd);

            currentIndex = 0;
            currentPage = 0;            
            displayPostSummary();
        } else if (text.startsWith("view")) {
            try {
                if (text.equals("view")) {
                    if (currentIndex >= 0 && currentIndex < currentPosts.size()) {
                        getUI().ifPresent(ui -> ui.getPage().open(currentPosts.get(currentIndex).getUrl()));
                    } else {
                        output.setText("Aucun post sélectionné.");
                    }
                } else {
                    int index = Integer.parseInt(text.substring(5).trim()) - 1;
                    if (index >= 0 && index < currentPosts.size()) {
                        getUI().ifPresent(ui -> ui.getPage().open(currentPosts.get(index).getUrl()));
                    } else {
                        output.setText("Index hors limites.");
                    }
                }
            } catch (NumberFormatException e) {
                output.setText("Format invalide pour view.");
            }
        } else {
            output.setText("Commande inconnue. Tapez 'help' pour la liste des commandes.");
        }
    }

    private void toggle_nsfw(boolean allow) {
        allowNsfw = allow;
        output.setText("Mode NSFW " + (!allowNsfw ? "désactivé." : "activé."));
    }

    private void navigate(int offset) {
        if (!currentPosts.isEmpty()) {
            currentIndex = Math.max(0, Math.min(currentIndex + offset, currentPosts.size() - 1));
            renderCurrentPost();
        } else {
            output.setText("Aucun post à naviguer.");
        }
    }

    

    private void navigatePage(int offset) {
        int maxPage = (int) Math.ceil(currentPosts.size() / (double) PAGE_SIZE);
        currentPage = Math.max(0, Math.min(currentPage + offset, maxPage - 1));
        displayPostSummary();
    }

    private void displayPostSummary() {
        if (currentPosts.isEmpty()) {
            output.setText("Aucun post trouvé.");
            return;
        }

        int totalPages = (int) Math.ceil(currentPosts.size() / (double) PAGE_SIZE);
        int start = currentPage * PAGE_SIZE;
        int end = Math.min(start + PAGE_SIZE, currentPosts.size());

        StringBuilder builder = new StringBuilder("""
            <style>
                table { border-collapse: collapse; width: 100%; font-family: monospace; }
                th, td { border: 1px solid #ccc; padding: 6px; text-align: left; }
                th { background-color: #333; color: white; }
                td.icon-cell { text-align: center; vertical-align: middle; }
                img.icon { width: 20px; height: 20px; display: block; margin: auto; }
                .rating-Sûr { color: green; font-weight: bold; }
                .rating-Sensible { color: orange; font-weight: bold; } /* Changed from yellow for better visibility */
                .rating-NSFW { color: red; font-weight: bold; }
            </style>
        """);

        builder.append("<div style='font-family:monospace;margin-bottom:5px;'>")
            .append("Page ").append(currentPage + 1).append("/").append(totalPages)
            .append(" — ").append(currentPosts.size()).append(" posts")
            .append("</div>");

        builder.append("""
            <table>
                <tr><th>#</th><th>Réseau</th><th>Auteur</th><th>Date</th><th>Contenu</th><th>Rating</th><th>Comments</th><th>Likes</th></tr>
        """);

        for (int i = start; i < end; i++) {
            Post post = currentPosts.get(i);

            String iconUrl = post.fromReddit()
                    ? "assets/reddit-icon.png"
                    : "assets/mastodon-icon.svg";

            String contenu = Jsoup.parse(post.getContent()).text();
            if (post.fromReddit()) {
                String titre = ((RedditPost) post).getTitle();

                if (contenu != null && !contenu.trim().isEmpty()) {
                    contenu = titre + " | " + contenu;
                } else {
                    contenu = titre;
                }
            }

            // on retire les sauts de lignes
            contenu.replaceAll("[\\n\\r]+", " ");
            if (contenu.length() > MAX_LENGTH) {
                contenu = contenu.substring(0, MAX_LENGTH - 3).trim() + "...";
            }

            String ratingClass = "";
            switch (post.getRating()) {
                case "Sûr":
                    ratingClass = "rating-Sûr";
                    break;
                case "Sensible":
                    ratingClass = "rating-Sensible";
                    break;
                case "NSFW":
                    ratingClass = "rating-NSFW";
                    break;
            }

            builder.append("<tr>")
                    .append("<td>").append(i + 1).append("</td>")
                    .append("<td class='icon-cell'><img class='icon' src='").append(iconUrl).append("'/></td>")
                    .append("<td>").append(post.getAuthor()).append("</td>")
                    .append("<td>").append(formatterDate(post.getCreatedAt())).append("</td>")
                    .append("<td>").append(contenu).append("</td>")
                    .append("<td class='").append(ratingClass).append("'>").append(post.getRating()).append("</td>")
                    .append("<td>").append(post.getNumComments()).append("</td>")
                    .append("<td>").append(post.getLikes()).append("</td>")
                    .append("</tr>");
        }

        builder.append("</table>");
        output.getElement().setProperty("innerHTML", builder.toString());
    }


    private void renderCurrentPost() {
        if (currentPosts.isEmpty()) {
            output.setText("<p style='font-family:monospace;'>Aucun post trouvé.</p>");
            return;
        }


        Post post = currentPosts.get(currentIndex);
        String titre = post instanceof RedditPost ? "<b>Titre :</b> " + ((RedditPost) post).getTitle() + "<br>" : "";
        String contenu = post.getContent().isEmpty() ? "" : "<b>Contenu :</b><br>" + Jsoup.parse(post.getContent()).text() + "<br>";
        
        StringBuilder mediaURL = new StringBuilder(post.getMediaUrl().isEmpty() ? "" : "<br><b>Liens des contenus :</b><br>");
        for (String url : post.getMediaUrl()) {
            if (!post.fromReddit() || url.contains("preview")) mediaURL.append(url).append("<br>");
        }

        String subreddit = post instanceof RedditPost ? "<b>Subreddit :</b> r/" + ((RedditPost) post).getSubreddit() + "<br>" : "";


        output.getElement().setProperty("innerHTML",
            """
            <style>
                .rating-Sûr { color: green; font-weight: bold; }
                .rating-Sensible { color: orange; font-weight: bold; }
                .rating-NSFW { color: red; font-weight: bold; }
            </style>
            """ +
            "<p style='font-family:monospace;'>" +
            "<b>Post :</b> " + (currentIndex + 1) + "/" + currentPosts.size() + "<br>" +
            "<b>Auteur :</b> @" + post.getAuthor() + "<br>" +
            subreddit +
            "<b>Date :</b> " + formatterDate(post.getCreatedAt()) + "<br><br>" +
            titre +
            contenu +
            "<b>Rating :</b><span class='rating-" + post.getRating() + "'> " + post.getRating() + "</span><br>" +
            "<b>Likes :</b> " + post.getLikes() + "<br>" +
            "<b>Replies :</b> " + post.getNumComments() + "<br>" +
            "<b>URL :</b> " + post.getUrl() + "<br>" +
            mediaURL +
            "</p>"
        );
    }

    private List<String> getTagsFromQuery(String query, boolean isAnd) {
        String[] rawTags = isAnd ? query.split("&") : query.split(" ");
        return Arrays.stream(rawTags)
            .map(String::trim)
            .filter(s -> !s.isEmpty())
            .map(String::toLowerCase)
            .collect(Collectors.toList());

    }
    private void addPostsFromSocialMedia(List<Post> posts, List<String> tags, boolean isAnd) {
        currentPosts.addAll(fetchPostsFromMastodon(tags, isAnd));
        currentPosts.addAll(fetchPostsFromReddit(tags, isAnd));
        currentPosts.sort(Comparator.comparing(Post::getCreatedAt).reversed());
    }
    private List<Post> fetchPostsFromMastodon(List<String> tags, boolean isAnd) {
        List<Post> mastodonResults = new ArrayList<>();
        for (String tag : tags) {
            mastodonResults.addAll(fetchMastodonPostsFromTag(tag, allowNsfw));
        }
        return filterPost(mastodonResults, tags, isAnd);
    }

    private List<Post> fetchPostsFromReddit(List<String> tags, boolean isAnd) {
        List<Post> redditResults = new ArrayList<>();
        for (String tag : tags) {
            redditResults.addAll(fetchRedditPostsFromTag(tag, allowNsfw));
        }
        return filterPost(redditResults, tags, isAnd);
    }

    private List<Post> filterPost(List<Post> posts, List<String> tags, boolean isAnd) {
        posts = posts.stream()
                .filter(post -> {
                    final String[] textContent = {Jsoup.parse(post.getContent()).text().toLowerCase().trim()};

                    if (post.fromReddit()) {
                        textContent[0] += " " + ((RedditPost) post).getTitle().toLowerCase().trim();
                    }

                    return isAnd
                            ? tags.stream().allMatch(tag -> textContent[0].contains(tag))
                            : tags.stream().anyMatch(tag -> textContent[0].contains(tag));
                })
                .collect(Collectors.toList());
        posts.sort(Comparator.comparing(Post::getCreatedAt).reversed());
        return posts.subList(0, posts.size() < maxPostPerMedia * tags.size() ? posts.size() : maxPostPerMedia * tags.size());
    }

    public List<MastodonPost> fetchMastodonPostsFromTag(String tag, boolean allowNsfw) {
        if (StringUtils.isEmpty(tag)) return List.of();
        try {
            var uri = new URIBuilder("https://mastodon.social/api/v1/timelines/tag/" + tag)
                    .addParameter("limit", PostPerMedia)
                    .build();
            URL url = uri.toURL();
            
            JsonNode postsNode = getURLResponse(url);
            List<MastodonPost> posts = new ArrayList<>();

            for (JsonNode postNode : postsNode) {
                boolean isSensitive = postNode.path("sensitive").asBoolean(false);
                if (!allowNsfw && isSensitive) continue;

                MastodonPost mastodonPost = new MastodonPost(postNode, EXPLICIT_WORDS);
                posts.add(mastodonPost);
            }

            posts.sort(Comparator.comparing(MastodonPost::getCreatedAt).reversed());
            return posts.subList(0, posts.size() < maxPostPerMedia ? posts.size() : maxPostPerMedia);

        } catch (Exception e) {
            output.setText("Erreur de récupération : " + e.getMessage());
            return List.of();
        }
    }

    public List<RedditPost> fetchRedditPostsFromTag(String tag, boolean allowNsfw) {
        if (StringUtils.isEmpty(tag)) return List.of();
        try {
            // Construire l'URL avec URIBuilder
            URI uri = new URIBuilder("https://www.reddit.com/search.json")
                    .addParameter("q", tag)
                    .addParameter("limit", PostPerMedia)
                    .build();

            URL url = uri.toURL();
            
            JsonNode postsNode = getURLResponse(url)
                .path("data")
                .path("children");
                
            List<RedditPost> posts = new ArrayList<>();
            for (JsonNode postNode : postsNode) {
                boolean isNsfw = postNode.path("over_18").asBoolean(false);
                if (!allowNsfw && isNsfw) continue;

                RedditPost redditPost = new RedditPost(postNode, EXPLICIT_WORDS);
                posts.add(redditPost);
            }
            posts.sort(Comparator.comparing(RedditPost::getCreatedAt).reversed());
            return posts.subList(0, posts.size() < maxPostPerMedia ? posts.size() : maxPostPerMedia);

        } catch (Exception e) {
            output.setText("Erreur de récupération : " + e.getMessage());
            return List.of();
        }
    }


    private String getHelpTableHtml() {
        return """
            <style>
                table { border-collapse: collapse; width: 100%; font-family: monospace; }
                th, td { border: 1px solid #ccc; padding: 8px; text-align: left; }
                th { background-color:rgb(24, 1, 1); color: white; }
            </style>
            <table>
                <tr><th>Commande</th><th>Description</th></tr>
                <tr><td><code>s tag1 & tag2</code></td><td>Recherche avec tous les tags (ET)</td></tr>
                <tr><td><code>s tag1 tag2</code></td><td>Recherche avec au moins un tag (OU)</td></tr>
                <tr><td><code>next</code> / <code>n</code></td><td>Post suivant</td></tr>
                <tr><td><code>previous</code> / <code>p</code></td><td>Post précédent</td></tr>
                <tr><td><code>list</code> / <code>l</code></td><td>Affiche un sommaire des posts</td></tr>
                <tr><td><code>sort like</code></td><td>Tri décroissant par likes</td></tr>
                <tr><td><code>sort date</code></td><td>Tri décroissant par date</td></tr>
                <tr><td><code>goto N</code></td><td>Aller au post numéro N (affichage détaillé)</td></tr>
                <tr><td><code>view N</code></td><td>Ouvrir le post N dans un nouvel onglet</td></tr>
                <tr><td><code>view</code></td><td>Ouvrir le post affiché actuellement</td></tr>
                <tr><td><code>clear</code></td><td>Nettoyer l'affichage</td></tr>
                <tr><td><code>allow nsfw</code></td><td>Afficher les contenus sensibles</td></tr>
                <tr><td><code>unallow nsfw</code></td><td>Ne pas afficher les contenus sensibles</td></tr>
                <tr><td><code>help</code></td><td>Afficher cette aide</td></tr>
            </table>
        """;
    }

    private boolean keyIsEqual(String key, String input) {
        return key.equals(input);
    }

    private JsonNode getURLResponse(URL url) {
        try {
            HttpURLConnection con = (HttpURLConnection) url.openConnection();
            con.setRequestMethod("GET");
            con.setRequestProperty("User-Agent", "Mozilla/5.0");

            // Lire la réponse JSON
            BufferedReader in = new BufferedReader(new InputStreamReader(con.getInputStream()));
            StringBuilder response = new StringBuilder();
            String inputLine;

            while ((inputLine = in.readLine()) != null) {
                response.append(inputLine);
            }
            in.close();

            // Parser le JSON avec Jackson
            ObjectMapper objectMapper = new ObjectMapper();
            return objectMapper.readTree(response.toString());
        } catch (Exception e) {
            System.err.println("Erreur lors de la récupération des données : ".concat(e.getMessage()));
            return null;
        }
    }

    private String formatterDate(ZonedDateTime time) {
        return time.format(DATE_FORMATTER);
    }
}
