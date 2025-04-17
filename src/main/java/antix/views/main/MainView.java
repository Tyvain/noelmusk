package antix.views.main;

import antix.model.MastodonPost;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.grid.GridVariant;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.FlexLayout;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.provider.Query;
import com.vaadin.flow.data.renderer.ComponentRenderer;
import com.vaadin.flow.data.selection.SelectionEvent;
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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

@PageTitle("main")
@Route("")
public class MainView extends VerticalLayout {

    private final List<MastodonPost> posts = new ArrayList<>();
    private int currentIndex = 0;
    private final Div contentDiv = new Div();

    public MainView() {
        setSizeFull();
        setAlignItems(Alignment.CENTER);

        contentDiv.setWidthFull();
        contentDiv.setHeightFull();

        var prompt = new TextField();
        prompt.setWidth("100%");

        prompt.addValueChangeListener(v -> {
            String text = v.getValue().trim();
            prompt.clear();

            if (text.equals("help")) {
                showMessage("""
                        🆘 Commandes disponibles :
                        - `h tag1 tag2` : rechercher des posts par hashtags
                        - `n` ou `next` : post suivant
                        - `p` ou `previous` : post précédent
                        - `md` : exporter le post actuel en Markdown
                        - `clear` : effacer l'écran
                        - `help` : afficher cette aide
                        """);
            } else if (text.equals("clear")) {
                contentDiv.removeAll();
            } else if (text.equals("md")) {
                exportMarkdown();
            } else if (text.startsWith("h ") || text.startsWith("hashtag ")) {
                String[] parts = text.split(" ");
                Set<MastodonPost> results = new HashSet<>();
                for (int i = 1; i < parts.length; i++) {
                    results.addAll(fetchPostsFromTag(parts[i]));
                }
                posts.clear();
                posts.addAll(results);
                currentIndex = 0;
                showCurrentPost();
            } else if (text.equals("n") || text.equals("next")) {
                if (currentIndex < posts.size() - 1) currentIndex++;
                showCurrentPost();
            } else if (text.equals("p") || text.equals("previous")) {
                if (currentIndex > 0) currentIndex--;
                showCurrentPost();
            } else {
                showMessage("❓ Commande inconnue. Tape `help` pour voir les options.");
            }
        });

        var promptContainer = new VerticalLayout(prompt);
        promptContainer.setWidthFull();
        promptContainer.setPadding(false);

        add(contentDiv, promptContainer);
        setFlexGrow(1, contentDiv);
        setFlexGrow(0, promptContainer);
    }

    private void showMessage(String message) {
        contentDiv.removeAll();
        Span msg = new Span(message);
msg.getStyle().set("white-space", "pre-wrap").set("font-family", "monospace");
contentDiv.add(msg);

    }

    private void showCurrentPost() {
        contentDiv.removeAll();
        if (posts.isEmpty()) {
            contentDiv.add(new Span("Aucun post trouvé."));
            return;
        }

        MastodonPost post = posts.get(currentIndex);
        var container = new VerticalLayout();
        container.setSpacing(false);

        // Infos générales
        var header = new Span("📄 Post " + (currentIndex + 1) + " / " + posts.size());
        header.getStyle().set("font-weight", "bold");
        container.add(header);

        var author = new Span("👤 " + post.getAccount().getDisplayName() + " (@" + post.getAccount().getUsername() + ")");
        var date = new Span("🕒 " + post.getCreatedAt().toLocalDateTime().toString());
        container.add(author, date);

        // Lien
        var link = new Span("🔗 https://mastodon.social/@" + post.getAccount().getUsername() + "/" + post.getId());
        container.add(link);

        // Contenu du post
        var content = new Span(Jsoup.parse(post.getContent()).text());
        container.add(content);

        // Statistiques
        var stats = new Span("🔁 Boosts: " + post.getReblogsCount() + "   ⭐ Favoris: " + post.getFavouritesCount());
        container.add(stats);

        // Réponses
        if (post.getRepliesCount() > 0) {
            var repliesTitle = new Span("💬 Réponses (" + post.getRepliesCount() + ")");
            repliesTitle.getStyle().set("margin-top", "1em").set("font-weight", "bold");
            container.add(repliesTitle);

            try {
                var uri = new URIBuilder("https://mastodon.social/api/v1/statuses/" + post.getId() + "/context").build();
                var connection = (HttpURLConnection) uri.toURL().openConnection();
                connection.setRequestMethod("GET");
                BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
                StringBuilder response = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) response.append(line);
                reader.close();

                var mapper = new ObjectMapper().registerModule(new JavaTimeModule());
                var context = mapper.readTree(response.toString());
                var descendants = context.get("descendants");

                for (var reply : descendants) {
                    var replyContent = Jsoup.parse(reply.get("content").asText()).text();
                    var replyUser = reply.get("account").get("username").asText();
                    container.add(new Span("↳ @" + replyUser + ": " + replyContent));
                }

            } catch (IOException | URISyntaxException e) {
                container.add(new Span("Erreur lors du chargement des réponses."));
            }
        }

        contentDiv.add(container);
    }

    private void exportMarkdown() {
        if (posts.isEmpty()) {
            showMessage("Aucun post à exporter.");
            return;
        }
        MastodonPost post = posts.get(currentIndex);
        StringBuilder sb = new StringBuilder();
        sb.append("# ").append(post.getAccount().getDisplayName()).append(" (@" + post.getAccount().getUsername() + ")\n\n");
        sb.append("> ").append(Jsoup.parse(post.getContent()).text()).append("\n\n");
        sb.append("[Lien au post](https://mastodon.social/@" + post.getAccount().getUsername() + "/" + post.getId() + ")\n\n");
        sb.append("🕒 ").append(post.getCreatedAt().toLocalDateTime().toString()).append("\n");
        sb.append("🔁 ").append(post.getReblogsCount()).append("   ⭐ ").append(post.getFavouritesCount()).append("\n");

        showMessage(sb.toString());
    }

    public List<MastodonPost> fetchPostsFromTag(String tag) {
        try {
            var uri = new URIBuilder("https://mastodon.social/api/v1/timelines/tag/" + tag)
                    .addParameter("limit", "10").build();
            var con = (HttpURLConnection) uri.toURL().openConnection();
            con.setRequestMethod("GET");

            BufferedReader reader = new BufferedReader(new InputStreamReader(con.getInputStream()));
            StringBuilder response = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) response.append(line);
            reader.close();

            ObjectMapper mapper = new ObjectMapper().registerModule(new JavaTimeModule());
            return Arrays.asList(mapper.readValue(response.toString(), MastodonPost[].class));
        } catch (IOException | URISyntaxException e) {
            return List.of();
        }
    }
}