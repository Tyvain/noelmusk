package antix.views.main;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.Anchor;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

@PageTitle("Recherche Web")
@Route("")
public class MainView extends VerticalLayout {

    private static final String API_KEY = "62d5e580948a4c19b4e4a770e66ef6f8"; // remplace par ta clé
    private static final String CX = "VOTRE_ID_MOTEUR";   // remplace par ton ID moteur

    public static class SearchResult {
        private final String title;
        private final String snippet;
        private final String url;

        public SearchResult(String title, String snippet, String url) {
            this.title = title;
            this.snippet = snippet;
            this.url = url;
        }

        public String getTitle() {
            return title;
        }

        public String getSnippet() {
            return snippet;
        }

        public String getUrl() {
            return url;
        }
    }

    public MainView() {
        setSizeFull();

        // Grille pour afficher les résultats web
        var grid = new Grid<>(SearchResult.class, false);
        grid.setSizeFull();
        grid.addColumn(SearchResult::getTitle).setHeader("Titre").setAutoWidth(true);
        grid.addColumn(SearchResult::getSnippet).setHeader("Résumé").setAutoWidth(true);
        grid.addComponentColumn(result -> {
            Anchor link = new Anchor(result.getUrl(), "Ouvrir");
            link.setTarget("_blank");
            return link;
        }).setHeader("Lien").setAutoWidth(true);

        // Div de contenu HTML complémentaire (à droite)
        var contentDiv = new Div();
        contentDiv.setWidthFull();

        // Layout horizontal contenant la grille et le contenu
        var horizontalLayout = new HorizontalLayout(grid, contentDiv);
        horizontalLayout.setSizeFull();
        grid.setWidth("50%");
        contentDiv.setWidth("50%");

        // Champ de recherche
        var prompt = new TextField();
        prompt.setWidth("100%");
        prompt.setPlaceholder("Tapez une recherche web (ex: Voiture moteur)");

        prompt.addValueChangeListener(e -> {
            String query = e.getValue().trim();
            if (!query.isEmpty()) {
                List<SearchResult> results = fetchSearchResults(query);
                grid.setItems(results);
                if (!results.isEmpty()) {
                    grid.select(results.get(0));
                }
                prompt.clear();
            }
        });

        // Ajouter listener de sélection (affiche détails à droite)
        grid.addSelectionListener(event -> {
            event.getFirstSelectedItem().ifPresent(result -> {
                contentDiv.removeAll();
                Div html = new Div();
                html.getElement().setProperty("innerHTML", "<h3>" + result.getTitle() + "</h3><p>" + result.getSnippet() + "</p><a href='" + result.getUrl() + "' target='_blank'>Voir le lien</a>");
                contentDiv.add(html);
            });
        });

        // Conteneur pour le champ prompt
        var promptContainer = new VerticalLayout(prompt);
        promptContainer.setWidth("100%");
        promptContainer.setPadding(false);
        promptContainer.setSpacing(false);
        promptContainer.setMargin(false);

        // Ajout à la vue
        add(horizontalLayout, promptContainer);
        setFlexGrow(1, horizontalLayout);
        setFlexGrow(0, promptContainer);
    }

    private List<SearchResult> fetchSearchResults(String query) {
        List<SearchResult> results = new ArrayList<>();
        try {
            String apiKey = "62d5e580948a4c19b4e4a770e66ef6f8"; // Remplace ici
            String url = "https://newsapi.org/v2/everything?q=" + URLEncoder.encode(query, StandardCharsets.UTF_8) +
                         "&apiKey=" + apiKey + "&pageSize=10&language=fr";
    
            HttpClient client = HttpClient.newHttpClient();
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .GET()
                    .build();
    
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
    
            ObjectMapper mapper = new ObjectMapper();
            JsonNode root = mapper.readTree(response.body());
    
            if (root.has("articles")) {
                for (JsonNode article : root.get("articles")) {
                    String title = article.path("title").asText();
                    String description = article.path("description").asText();
                    String link = article.path("url").asText();
                    results.add(new SearchResult(title, description, link));
                }
            }
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
        }
    
        return results;
    }
    
}
