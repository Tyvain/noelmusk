package antix.views.main;

import antix.model.MastodonPost;
import antix.model.Post;

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
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
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
    private final List<String> commandHistory = new ArrayList<>();
    private int historyIndex = -1;

    private int currentPage = 0;
    private static final int PAGE_SIZE = 10;

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yy 'à' HH:mm");

    public MainView() {
        setSizeFull();

        output = new Div();
        output.getStyle().set("white-space", "pre-wrap");
        output.getStyle().set("overflow-y", "auto");
        output.setHeight("80vh");
        output.setWidthFull();

        var prompt = new TextField();
        prompt.setPlaceholder("Entrez une commande...");
        prompt.setWidthFull();

        AtomicBoolean internalChange = new AtomicBoolean(false);

        output.setText("");
        output.getElement().setProperty("innerHTML", getHelpTableHtml());

        prompt.getElement().addEventListener("keydown", e -> {
            try {
               String key = e.getEventData().getString("event.key");

                if ("ArrowUp".equals(key)) {
                    if (!commandHistory.isEmpty()) {
                        historyIndex = Math.max(0, historyIndex - 1);
                        internalChange.set(true);
                        prompt.setValue(commandHistory.get(historyIndex));
                        internalChange.set(false);
                    }
                } else if ("ArrowDown".equals(key)) {
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
                } else if ("ArrowLeft".equals(key)) {
                    navigatePage(-1);
                } else if ("ArrowRight".equals(key)) {
                    navigatePage(1);
                } else if ("Enter".equals(key)) {
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
        } else if (text.startsWith("s ")) {
            String query = text.substring(2).trim();

            boolean isAnd = query.contains("&");
            String[] rawTags = isAnd ? query.split("&") : query.split(" ");

            List<String> tags = Arrays.stream(rawTags)
                    .map(String::trim)
                    .filter(s -> !s.isEmpty())
                    .map(String::toLowerCase)
                    .collect(Collectors.toList());

            List<Post> results = new ArrayList<>();
            for (String tag : tags) {
                results.addAll(fetchMastodonPostsFromTag(tag));
                results.addAll(fetchRedditPostsFromTag(tag));
            }
            List<Post> filtered = results.stream()
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
            filtered.sort(Comparator.comparing(Post::getCreatedAt).reversed());

            currentPosts = filtered;
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
            </style>
        """);

        builder.append("<div style='font-family:monospace;margin-bottom:5px;'>")
               .append("Page ").append(currentPage + 1).append("/").append(totalPages)
               .append(" — ").append(currentPosts.size()).append(" posts")
               .append("</div>");

        builder.append("""
            <table>
                <tr><th>#</th><th>Auteur</th><th>Date</th><th>Contenu</th><th>Likes</th></tr>
        """);

        for (int i = start; i < end; i++) {
            Post post = currentPosts.get(i);
            builder.append("<tr>")
                    .append("<td>").append(i + 1).append("</td>")
                    .append("<td>").append(post.getAuthor()).append("</td>")
                    .append("<td>").append(post.getCreatedAt().format(DATE_FORMATTER)).append("</td>")
                    .append("<td>").append(post.fromReddit() ? ((RedditPost) post).getTitle() : post.getContent()).append("</td>")
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


        output.getElement().setProperty("innerHTML",
            "<p style='font-family:monospace;'>" +
            "<b>Post :</b> " + (currentIndex + 1) + "/" + currentPosts.size() + "<br>" +
            "<b>Auteur :</b> @" + post.getAuthor() + "<br>" +
            "<b>Date :</b> " + post.getCreatedAt().format(DATE_FORMATTER) + "<br>" +
            titre +
            contenu +
            "<b>Likes :</b> " + post.getLikes() + "<br>" +
            "<b>URL :</b> " + post.getUrl() + "<br>" +
            mediaURL +
            "</p>"
        );
    }

    public List<MastodonPost> fetchMastodonPostsFromTag(String tag) {
        if (StringUtils.isEmpty(tag)) return List.of();
        try {
            var uri = new URIBuilder("https://mastodon.social/api/v1/timelines/tag/" + tag)
                    .addParameter("limit", "50")
                    .build();
            URL url = uri.toURL();
            
            JsonNode postsNode = getURLResponse(url);
            List<MastodonPost> posts = new ArrayList<>();

            for (JsonNode postNode : postsNode) {
                MastodonPost mastodonPost = new MastodonPost(postNode);
                posts.add(mastodonPost);
            }

            posts.sort(Comparator.comparing(MastodonPost::getLikes).reversed());
            return posts.subList(0, posts.size() < 15 ? posts.size() : 15);

        } catch (Exception e) {
            output.setText("Erreur de récupération : " + e.getMessage());
            return List.of();
        }
    }

    public List<RedditPost> fetchRedditPostsFromTag(String tag) {
        if (StringUtils.isEmpty(tag)) return List.of();
        try {
            // Construire l'URL avec URIBuilder
            URI uri = new URIBuilder("https://www.reddit.com/search.json")
                    .addParameter("q", tag)
                    .addParameter("limit", String.valueOf(50))
                    .build();

            URL url = uri.toURL();
            
            JsonNode rootNode = getURLResponse(url);
            JsonNode postsNode = rootNode.path("data").path("children");

            List<RedditPost> posts = new ArrayList<>();

            for (JsonNode postNode : postsNode) {
                RedditPost redditPost = new RedditPost(postNode);
                posts.add(redditPost);
            }
            posts.sort(Comparator.comparing(RedditPost::getLikes).reversed());
            return posts.subList(0, posts.size() < 15 ? posts.size() : 15);

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
                <tr><td><code>help</code></td><td>Afficher cette aide</td></tr>
            </table>
        """;
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
            e.printStackTrace();
            return null;
        }
    }
}
