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
    public MainView() {
        setSizeFull();
        setAlignItems(FlexComponent.Alignment.CENTER);
        var grid = new Grid<>(MastodonPost.class, false);
        grid.setSelectionMode(Grid.SelectionMode.SINGLE);
        
        // Créer le composant HTML pour afficher le contenu
        var contentDiv = new Div();
        contentDiv.setWidthFull();

        //addLineNumberColumn(grid);
       // addTagsColumn(grid);
        
        addContentColumn(grid);
        addRepliesColumn(grid);
        
        grid.addSelectionListener(event -> selectItemListener(grid, contentDiv, event));
        grid.setDetailsVisibleOnClick(false);

        AtomicBoolean internalChange = new AtomicBoolean(false);
        var prompt = new TextField();
        prompt.setWidth("100%");
        prompt.addValueChangeListener(v -> {
            if (internalChange.get()) {
                return;
            }
            String text = v.getValue().trim();
            // if (text.startsWith("goto ")) {
            //     closeAll(grid);
            //     String id = text.substring(3).trim();
            //     if (!id.isEmpty()) {
            //         var post = findPostById(grid, Integer.parseInt(id));
            //         grid.setDetailsVisible(post, true);
            //     }
            // } else
             if (text.startsWith("hashtag ") || text.startsWith("h ")) {
                String tags = text.substring(1).trim();
                Set<MastodonPost> uniqueResearches = new HashSet<>(); // Utilisation d'un Set pour éviter les doublons
            
                for (String tag : tags.split(" ")) {
                    uniqueResearches.addAll(fetchPostsFromTag(tag));
                }
            
                List<MastodonPost> researches = new ArrayList<>(uniqueResearches); // Conversion en liste si nécessaire
                grid.setItems(researches);
                
                grid.getDataProvider().fetch(new Query<>()).findFirst().ifPresent(grid::select);
            } else if (text.equals("next") || text.equals("n")) {
                change_post(true, grid);
            } else if (text.equals("previous") || text.equals("p")) {
                change_post(false, grid);
            }
            internalChange.set(true);
            prompt.setValue("");
            internalChange.set(false);
        });

        // Créer un HorizontalLayout pour contenir la grid et le contenu
        var horizontalLayout = new HorizontalLayout();
        horizontalLayout.setSizeFull();
        
        // Ajuster la taille de la grid
        grid.setHeight("100%");
        grid.setWidth("50%");
        
        // Ajuster le contentDiv
        contentDiv.setHeight("100%");
        contentDiv.setWidth("50%");
        
        // Ajouter les composants au layout horizontal
        horizontalLayout.add(grid, contentDiv);
        
        // Remplacer les add() individuels par l'ajout du layout horizontal
        add(horizontalLayout);
        
        // Ajouter le prompt en bas
        var promptContainer = new VerticalLayout(prompt);
        promptContainer.setWidth("100%");
        promptContainer.setPadding(false);
        promptContainer.setSpacing(false);
        promptContainer.setMargin(false);
        add(promptContainer);
        
        // Ajuster les flex grow
        setFlexGrow(1, horizontalLayout);
        setFlexGrow(0, promptContainer);
    }

    private void change_post(boolean previous, Grid<MastodonPost> grid) {
        MastodonPost currentSelection = grid.getSelectedItems().stream().findFirst().orElse(null);
        List<MastodonPost> items = grid.getDataProvider().fetch(new Query<>()).collect(Collectors.toList());
        if (currentSelection != null) {
            // Sélectionne la ligne suivante si la requete est next sinon la ligne précédente
            int currentIndex = items.indexOf(currentSelection);
            int newIndex = currentIndex + 1;
            if (previous) {
                newIndex = currentIndex - 1;
            }
            if ((!previous && currentIndex < items.size() - 1) ||  (previous && currentIndex > 0)) {
                grid.select(items.get(newIndex));
            }
        } else if (!items.isEmpty()) {
            grid.select(items.get(0));
        }
    }

    private void addLineNumberColumn(Grid<MastodonPost> grid) {
        grid.addColumn(post -> getLineNumber(post, grid))
                .setWidth("4em")
                .setFlexGrow(0);
    }

    private void addTagsColumn(Grid<MastodonPost> grid) {
        grid.addColumn(new ComponentRenderer<>(post -> {
            var tagContainer = new FlexLayout();
            tagContainer.setFlexWrap(FlexLayout.FlexWrap.WRAP);
            tagContainer.setAlignItems(FlexComponent.Alignment.CENTER);
            
            if (post.getTags() != null) {
                var tags = post.getTags();
                var displayCount = Math.min(3, tags.size());
                
                // Affiche les 3 premiers tags
                for (int i = 0; i < displayCount; i++) {
                    var badge = new Span(tags.get(i).getName());
                    badge.getElement().getThemeList().add("badge contrast");
                    badge.getStyle().set("margin", "2px");
                    tagContainer.add(badge);
                }
                
                // Affiche le badge +X si il y a plus de 3 tags
                if (tags.size() > 3) {
                    var remainingCount = tags.size() - 3;
                    var moreBadge = new Span("+" + remainingCount);
                    moreBadge.getElement().getThemeList().add("badge");
                    moreBadge.getStyle().set("margin", "2px");
                    tagContainer.add(moreBadge);
                }
            }
            return tagContainer;
        })).setAutoWidth(true);
    }

    private void addRepliesColumn(Grid<MastodonPost> grid) {
        grid.addColumn(post -> post.getRepliesCount())
                .setHeader("Commentaires")
                .setAutoWidth(true);
    }

    private void addContentColumn(Grid<MastodonPost> grid) {
        grid.addColumn(p -> StringUtils.left(Jsoup.parse(p.getContent()).text(), 150))
                .setHeader("Post")
                .setAutoWidth(true);
    }

    private int getLineNumber(MastodonPost post, Grid<MastodonPost> grid) {
        List<MastodonPost> currentItems = grid.getDataProvider()
                .fetch(new Query<>())
                .toList();
        return currentItems.indexOf(post) + 1;
    }

    private void closeAll(Grid<MastodonPost> grid) {
        grid.getDataProvider().fetch(new Query<>())
                .forEach(post -> grid.setDetailsVisible(post, false));
    }

    private MastodonPost findPostById(Grid<MastodonPost> grid, int id) {
        List<MastodonPost> items = grid.getDataProvider().fetch(new Query<>())
                .toList();

        if (id >= 0 && id < items.size()) {
            return items.get(id - 1);
        }
        return null;
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
            return Arrays.stream(mapper.readValue(response.toString(), MastodonPost[].class))
                    .collect(Collectors.toList());
        } catch (IOException | URISyntaxException e) {
            throw new RuntimeException(e);
        }
    }

    private void selectItemListener(Grid<MastodonPost> grid, Div contentDiv,
            SelectionEvent<Grid<MastodonPost>, MastodonPost> event) {
        // Ferme tous les détails
        closeAll(grid);
        // Met à jour le contenu HTML et expande la ligne sélectionnée
        event.getFirstSelectedItem().ifPresent(post -> {
            // Créer un conteneur vertical pour le contenu et les réponses
            VerticalLayout container = new VerticalLayout();
            
            // Ajouter le contenu du post
            Div postContent = new Div();
            postContent.getElement().setProperty("innerHTML", post.getContent());

            container.add(post.getUrl());
            
            container.add(postContent);
            post.getCreatedAt();
            // Ajouter les réponses si elles existent
            if (post.getRepliesCount() > 0) {
                Div repliesHeader = new Div();
                repliesHeader.setText("Réponses (" + post.getRepliesCount() + ")");
                // Récupérer les réponses via l'API
                try {
                    var uri = new URIBuilder("https://mastodon.social/api/v1/statuses/" + post.getId() + "/context")
                            .build();

                    // Afficher l'URI pour pouvoir cliquer dessus
                    Div uriDiv = new Div();
                    uriDiv.setText(uri.toString());
                    uriDiv.getStyle().set("color", "var(--lumo-primary-color)");
                    uriDiv.getStyle().set("cursor", "pointer");
                    container.add(uriDiv);
                    // Afficher le lien du post
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

                    // Créer un conteneur pour les réponses
                    VerticalLayout repliesContainer = new VerticalLayout();
                    repliesContainer.setSpacing(true);
                    repliesContainer.setPadding(true);
                    repliesContainer.getStyle().set("background", "var(--lumo-contrast-5pct)");
                    repliesContainer.getStyle().set("border-radius", "var(--lumo-border-radius-m)");

                    // Ajouter chaque réponse
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
            
            // Remplacer le contenu du contentDiv
            contentDiv.removeAll();
            contentDiv.add(container);
            
            grid.setDetailsVisible(post, true);
        });
    }
}