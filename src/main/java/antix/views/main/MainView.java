package antix.views.main;

import antix.model.MastodonPost;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.vaadin.flow.component.Key;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.Div;
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
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

@PageTitle("main")
@Route("")
public class MainView extends VerticalLayout {
    public MainView() {
        setSizeFull();
        setAlignItems(Alignment.CENTER);
        var grid = new Grid<>(MastodonPost.class, false);
        grid.setSelectionMode(Grid.SelectionMode.SINGLE);

        var contentDiv = new Div();
        contentDiv.setWidthFull();

        addRepliesColumn(grid);
        addContentColumn(grid);

        grid.setItemDetailsRenderer(new ComponentRenderer<>(post -> new Div(Jsoup.parse(post.getContent()).text())));
        grid.addSelectionListener(event -> selectItemListener(grid, contentDiv, event));
        grid.setDetailsVisibleOnClick(false);

        AtomicBoolean internalChange = new AtomicBoolean(false);
        var prompt = new TextField();
        prompt.setWidth("100%");
        prompt.setPlaceholder("Taper votre requête (? pour de l'aide)");

        prompt.addKeyPressListener(Key.ENTER, e -> {
            String text = prompt.getValue().trim();
            if (text.isEmpty()) return;

            if (text.startsWith("hashtag ") || text.startsWith("# ")) {
                String tag = text.substring(1).trim();
                grid.setItems(fetchPostsFromTag(tag));
                grid.getDataProvider().fetch(new Query<>()).findFirst().ifPresent(grid::select);
            } else if (text.startsWith("arobase ") || text.startsWith("@ ")) {
                String query = text.substring(1).trim();
                List<MastodonPost> searchResults = fetchPostsFromExternalSearch(query);
                grid.setItems(searchResults);
                grid.getDataProvider().fetch(new Query<>()).findFirst().ifPresent(grid::select);
            } else if (text.equals("?")) {
                grid.setItems(List.of());
                Div help = new Div();
                help.setText("Instructions :\n# mot — recherche par tag\n@ mot — recherche web\n? — affiche cette aide.\nFlèches ↑ ↓ : navigation dans les résultats.");
                contentDiv.removeAll();
                contentDiv.add(help);
            }

            internalChange.set(true);
            prompt.setValue("");
            internalChange.set(false);

            // 🔁 Re-focus après validation
            prompt.getElement().executeJs("this.focus();");
        });

        prompt.addKeyDownListener(Key.ARROW_DOWN, e -> {
            MastodonPost currentSelection = grid.getSelectedItems().stream().findFirst().orElse(null);
            List<MastodonPost> items = grid.getDataProvider().fetch(new Query<>()).collect(Collectors.toList());
            if (currentSelection != null) {
                int currentIndex = items.indexOf(currentSelection);
                if (currentIndex < items.size() - 1) {
                    grid.select(items.get(currentIndex + 1));
                }
            } else if (!items.isEmpty()) {
                grid.select(items.get(0));
            }
        });

        prompt.addKeyDownListener(Key.ARROW_UP, e -> {
            MastodonPost currentSelection = grid.getSelectedItems().stream().findFirst().orElse(null);
            List<MastodonPost> items = grid.getDataProvider().fetch(new Query<>()).collect(Collectors.toList());
            if (currentSelection != null) {
                int currentIndex = items.indexOf(currentSelection);
                if (currentIndex > 0) {
                    grid.select(items.get(currentIndex - 1));
                }
            }
        });

        var horizontalLayout = new HorizontalLayout();
        horizontalLayout.setSizeFull();

        grid.setHeight("100%");
        grid.setWidth("50%");

        contentDiv.setHeight("100%");
        contentDiv.setWidth("50%");

        horizontalLayout.add(grid, contentDiv);
        add(horizontalLayout);

        var promptContainer = new VerticalLayout(prompt);
        promptContainer.setWidth("100%");
        promptContainer.setPadding(false);
        promptContainer.setSpacing(false);
        promptContainer.setMargin(false);
        add(promptContainer);

        setFlexGrow(1, horizontalLayout);
        setFlexGrow(0, promptContainer);

        // 🛑 Bloque la souris partout sauf champ texte
        getElement().executeJs("""
            document.body.style.pointerEvents = 'none';
            const allowed = ['input', 'textarea', 'vaadin-text-field'];
            document.querySelectorAll('*').forEach(el => {
                if (allowed.includes(el.tagName.toLowerCase()) || el.closest('vaadin-text-field')) {
                    el.style.pointerEvents = 'auto';
                }
            });
        """);

        // 🛑 Empêche le clic et sélection dans la grille
        grid.getElement().executeJs("this.addEventListener('click', e => e.preventDefault());");
        grid.getElement().executeJs("this.addEventListener('mousedown', e => e.preventDefault());");
    }

    private void selectItemListener(Grid<MastodonPost> grid, Div contentDiv,
                                    SelectionEvent<Grid<MastodonPost>, MastodonPost> event) {
        closeAll(grid);
        event.getFirstSelectedItem().ifPresent(post -> {
            VerticalLayout container = new VerticalLayout();

            Div postContent = new Div();
            postContent.getElement().setProperty("innerHTML", post.getContent());
            container.add(postContent);

            if (post.getRepliesCount() > 0) {
                Div repliesHeader = new Div();
                repliesHeader.setText("Réponses (" + post.getRepliesCount() + ")");

                try {
                    var uri = new URIBuilder("https://mastodon.social/api/v1/statuses/" + post.getId() + "/context").build();

                    Div uriDiv = new Div();
                    uriDiv.setText(uri.toString());
                    uriDiv.getStyle().set("color", "var(--lumo-primary-color)");
                    uriDiv.getStyle().set("cursor", "pointer");
                    container.add(uriDiv);

                    Div postLinkDiv = new Div();
                    String postUrl = "https://mastodon.social/@" + post.getAccount().getUsername() + "/" + post.getId();
                    postLinkDiv.setText(postUrl);
                    postLinkDiv.getStyle().set("color", "var(--lumo-primary-color)");
                    postLinkDiv.getStyle().set("cursor", "pointer");
                    postLinkDiv.getStyle().set("margin-bottom", "1em");
                    container.add(postLinkDiv);

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
                    var context = mapper.readTree(response.toString());
                    var descendants = context.get("descendants");

                    VerticalLayout repliesContainer = new VerticalLayout();
                    repliesContainer.setSpacing(true);
                    repliesContainer.setPadding(true);
                    repliesContainer.getStyle().set("background", "var(--lumo-contrast-5pct)");
                    repliesContainer.getStyle().set("border-radius", "var(--lumo-border-radius-m)");

                    for (var reply : descendants) {
                        Div replyDiv = new Div();
                        replyDiv.getElement().setProperty("innerHTML", reply.get("content").asText());
                        replyDiv.getStyle().set("margin-bottom", "0.5em");
                        repliesContainer.add(replyDiv);
                    }

                    container.add(repliesContainer);
                } catch (IOException | URISyntaxException e) {
                    Div errorDiv = new Div();
                    errorDiv.setText("Erreur lors de la récupération des réponses: " + e.getMessage());
                    errorDiv.getStyle().set("color", "var(--lumo-error-text-color)");
                    container.add(errorDiv);
                }
                repliesHeader.getStyle().set("margin-top", "1em");
                repliesHeader.getStyle().set("font-weight", "bold");
                container.add(repliesHeader);
            }

            contentDiv.removeAll();
            contentDiv.add(container);

            grid.setDetailsVisible(post, true);
        });
    }

    private void addRepliesColumn(Grid<MastodonPost> grid) {
        grid.addColumn(MastodonPost::getRepliesCount)
            .setHeader("Réponses")
            .setAutoWidth(true);
    }

    private void addContentColumn(Grid<MastodonPost> grid) {
        grid.addColumn(post -> StringUtils.left(Jsoup.parse(post.getContent()).text(), 150))
            .setHeader("Contenu")
            .setAutoWidth(true);
    }

    private void closeAll(Grid<MastodonPost> grid) {
        grid.getDataProvider().fetch(new Query<>())
            .forEach(post -> grid.setDetailsVisible(post, false));
    }

    public List<MastodonPost> fetchPostsFromTag(String tag) {
        if (StringUtils.isEmpty(tag)) return List.of();
        try {
            var uri = new URIBuilder("https://mastodon.social/api/v1/timelines/tag/" + tag)
                .addParameter("limit", "10")
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

            return Arrays.stream(mapper.readValue(response.toString(), MastodonPost[].class)).toList();

        } catch (IOException | URISyntaxException e) {
            throw new RuntimeException(e);
        }
    }

    private List<MastodonPost> fetchPostsFromExternalSearch(String query) {
        try {
            String apiKey = "c6bf6dd8df155ed34ea810c2c707582f29c0ee95243afe91aabede792ef2987e";
            var uri = new URIBuilder("https://serpapi.com/search.json")
                    .addParameter("q", query)
                    .addParameter("api_key", apiKey)
                    .addParameter("num", "10")
                    .addParameter("engine", "google")
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
            var root = mapper.readTree(response.toString());
            var results = root.get("organic_results");

            if (results == null) return List.of();

            return StreamSupport.stream(results.spliterator(), false)
                    .map(result -> {
                        MastodonPost post = new MastodonPost();
                        post.setContent("<b>" + result.path("title").asText("") + "</b><br><i>" +
                                result.path("snippet").asText("") + "</i><br><a href='" +
                                result.path("link").asText("") + "' target='_blank'>" +
                                result.path("link").asText("") + "</a>");
                        post.setRepliesCount(0);
                        return post;
                    })
                    .collect(Collectors.toList());
        } catch (IOException | URISyntaxException e) {
            throw new RuntimeException("Erreur lors de l'appel à SerpAPI : " + e.getMessage(), e);
        }
    }
}
