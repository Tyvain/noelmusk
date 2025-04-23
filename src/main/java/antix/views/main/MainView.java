package antix.views.main;

import antix.model.MastodonPost;
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
import java.net.URISyntaxException;
import java.net.URL;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

@PageTitle("main")
@Route("")
public class MainView extends VerticalLayout {
    private final Div output;
    private List<MastodonPost> currentPosts = new ArrayList<>();
    private int currentIndex = 0;
    private final List<String> commandHistory = new ArrayList<>();
    private int historyIndex = -1;

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
            }
        }).addEventData("event.key");

        prompt.addValueChangeListener(v -> {
            if (internalChange.get()) return;
            String text = v.getValue().trim();

            if (!text.isEmpty()) {
                commandHistory.add(text);
                historyIndex = commandHistory.size();
            }

            internalChange.set(true);
            prompt.setValue("");
            internalChange.set(false);

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
                currentPosts.sort(Comparator.comparingInt(MastodonPost::getFavouritesCount).reversed());
                currentIndex = 0;
                displayPostSummary();
            } else if (text.equals("sort date")) {
                currentPosts.sort(Comparator.comparing(MastodonPost::getCreatedAt).reversed());
                currentIndex = 0;
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

                Set<MastodonPost> results = new HashSet<>();
                for (String tag : tags) {
                    results.addAll(fetchPostsFromTag(tag));
                }

                List<MastodonPost> filtered;

                if (isAnd) {
                    filtered = results.stream()
                            .filter(post -> {
                                String textContent = Jsoup.parse(post.getContent()).text().toLowerCase();
                                return tags.stream().allMatch(tag -> textContent.contains("#" + tag));
                            })
                            .collect(Collectors.toList());
                } else {
                    filtered = results.stream()
                            .filter(post -> {
                                String textContent = Jsoup.parse(post.getContent()).text().toLowerCase();
                                return tags.stream().anyMatch(tag -> textContent.contains("#" + tag));
                            })
                            .collect(Collectors.toList());
                }

                currentPosts = filtered;
                currentIndex = 0;
                displayPostSummary();
            } else {
                output.setText("Commande inconnue. Tapez 'help' pour la liste des commandes.");
            }
        });

        var container = new VerticalLayout();
        container.setSizeFull();
        container.setSpacing(false);
        container.setPadding(false);
        container.setMargin(false);
        container.add(output);
        add(container);
        add(prompt);
    }

    private void navigate(int offset) {
        if (!currentPosts.isEmpty()) {
            currentIndex = Math.max(0, Math.min(currentIndex + offset, currentPosts.size() - 1));
            renderCurrentPost();
        } else {
            output.setText("Aucun post à naviguer.");
        }
    }

    private void displayPostSummary() {
        if (currentPosts.isEmpty()) {
            output.setText("Aucun post trouvé.");
            return;
        }

        StringBuilder builder = new StringBuilder("""
            <style>
                table { border-collapse: collapse; width: 100%; font-family: monospace; }
                th, td { border: 1px solid #ccc; padding: 6px; text-align: left; }
                th { background-color: #333; color: white; }
            </style>
            <table>
                <tr><th>#</th><th>Auteur</th><th>Date</th><th>Contenu</th><th>Likes</th></tr>
        """);

        for (int i = 0; i < currentPosts.size(); i++) {
            MastodonPost post = currentPosts.get(i);
            builder.append("<tr>")
                    .append("<td>").append(i + 1).append("</td>")
                    .append("<td>").append(post.getAccount().getUsername()).append("</td>")
                    .append("<td>").append(post.getCreatedAt().format(DATE_FORMATTER)).append("</td>")
                    .append("<td>").append(StringUtils.abbreviate(Jsoup.parse(post.getContent()).text(), 50)).append("</td>")
                    .append("<td>").append(post.getFavouritesCount()).append("</td>")
                    .append("</tr>");
        }

        builder.append("</table>");
        output.getElement().setProperty("innerHTML", builder.toString());
    }

    private void renderCurrentPost() {
        if (currentPosts.isEmpty()) {
            output.setText("Aucun post trouvé.");
            return;
        }
        MastodonPost post = currentPosts.get(currentIndex);
        output.setText("Post " + (currentIndex + 1) + "/" + currentPosts.size() + "\n"
                + "Auteur : @" + post.getAccount().getUsername() + "\n"
                + "Date : " + post.getCreatedAt().format(DATE_FORMATTER) + "\n"
                + "Contenu :\n" + Jsoup.parse(post.getContent()).text() + "\n"
                + "Likes : " + post.getFavouritesCount() + "\n"
                + "URL : " + post.getUrl());
    }

    public List<MastodonPost> fetchPostsFromTag(String tag) {
        if (StringUtils.isEmpty(tag)) return List.of();
        try {
            var uri = new URIBuilder("https://mastodon.social/api/v1/timelines/tag/" + tag)
                    .addParameter("limit", "40")
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
            return Arrays.stream(mapper.readValue(response.toString(), MastodonPost[].class)).collect(Collectors.toList());
        } catch (IOException | URISyntaxException e) {
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
                <tr><td><code>goto N</code></td><td>Aller au post numéro N</td></tr>
                <tr><td><code>clear</code></td><td>Nettoyer l'affichage</td></tr>
                <tr><td><code>help</code></td><td>Afficher cette aide</td></tr>
            </table>
        """;
    }
}
