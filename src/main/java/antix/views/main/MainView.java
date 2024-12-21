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

@PageTitle("main")
@Route("")
public class MainView extends VerticalLayout {
    public MainView() {
        setSizeFull();
        setAlignItems(FlexComponent.Alignment.CENTER);
        var grid = new Grid<>(MastodonPost.class, false);
        grid.setSelectionMode(Grid.SelectionMode.SINGLE);
        grid.addThemeVariants(GridVariant.LUMO_ROW_STRIPES);
        
        grid.setHeight("50%");
        
        var contentDiv = new Div();
        contentDiv.setWidthFull();
        contentDiv.setHeight("50%");
        contentDiv.getStyle()
                .set("padding", "1em")
                .set("background-color", "var(--lumo-contrast-5pct)");

        addLineNumberColumn(grid);
        addTagsColumn(grid);
        addContentColumn(grid);
        
        grid.setItemDetailsRenderer(new ComponentRenderer<>(post -> new Div(Jsoup.parse(post.getContent()).text())));
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
            if (text.startsWith("exp")) {
                closeAll(grid);
                String id = text.substring(3).trim();
                if (!id.isEmpty()) {
                    var post = findPostById(grid, Integer.parseInt(id));
                    grid.setDetailsVisible(post, true);
                }
            } else if (text.startsWith("h")) {
                String tag = text.substring(1).trim();
                grid.setItems(fetchPostsFromTag(tag));
                grid.getDataProvider().fetch(new Query<>()).findFirst().ifPresent(firstItem -> {
                    grid.select(firstItem);
                });
            } else if (text.equals("n")) {
                // Sélectionne la ligne suivante
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
            } else if (text.equals("p")) {
                // Sélectionne la ligne précédente
                MastodonPost currentSelection = grid.getSelectedItems().stream().findFirst().orElse(null);
                List<MastodonPost> items = grid.getDataProvider().fetch(new Query<>()).collect(Collectors.toList());
                if (currentSelection != null) {
                    int currentIndex = items.indexOf(currentSelection);
                    if (currentIndex > 0) {
                        grid.select(items.get(currentIndex - 1));
                    }
                }
            }
            internalChange.set(true);
            prompt.setValue("");
            internalChange.set(false);
        });

        add(grid);
        add(contentDiv);
        
        var promptContainer = new VerticalLayout(prompt);
        promptContainer.setWidth("100%");
        promptContainer.setPadding(false);
        promptContainer.setSpacing(false);
        promptContainer.setMargin(false);
        
        add(promptContainer);
        
        setFlexGrow(0.5, grid);
        setFlexGrow(0.5, contentDiv);
        setFlexGrow(0, promptContainer);
    }

    private void selectItemListener(Grid<MastodonPost> grid, Div contentDiv,
            SelectionEvent<Grid<MastodonPost>, MastodonPost> event) {
        // Ferme tous les détails
        closeAll(grid);
        // Met à jour le contenu HTML
        event.getFirstSelectedItem().ifPresent(post -> {
            contentDiv.getElement().setProperty("innerHTML", post.getContent());
        });
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

    private void addContentColumn(Grid<MastodonPost> grid) {
        grid.addColumn(p -> StringUtils.left(Jsoup.parse(p.getContent()).text(), 150))
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
}