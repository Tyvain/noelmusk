package antix.views.main;

import antix.factory.CommandFactory;
import antix.model.MastodonPost;
import antix.views.main.commands.Command;
import antix.views.main.commands.PlayCommand;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.client.utils.URIBuilder;
import org.jsoup.Jsoup;
import com.vaadin.flow.data.provider.Query;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URISyntaxException;
import java.net.URL;
import java.time.format.DateTimeFormatter;
import java.util.*;

@PageTitle("NoelMusk - Test pour la bande")
@Route("")
public class MainView extends VerticalLayout {
    private final Grid<MastodonPost> grid;
    private final Div contentDiv;
    private final List<String> commandesTapees = new ArrayList<>();


    public MainView() {
        var prompt = new TextField();
        prompt.setId("prompt-field");
        prompt.setWidth("100%");
        
        UI.getCurrent().getPage().executeJs("""
                    const overlay = document.createElement('div');
                    overlay.id = 'click-blocker';
                    overlay.style.position = 'fixed';
                    overlay.style.top = 0;
                    overlay.style.left = 0;
                    overlay.style.width = '100vw';
                    overlay.style.height = '100vh';
                    overlay.style.zIndex = '9999';
                    overlay.style.pointerEvents = 'all';
                    overlay.style.backgroundColor = 'transparent';
                    document.body.appendChild(overlay);

                    setTimeout(() => {
                        const promptField = document.querySelector('#prompt-field');
                        if (promptField && promptField.offsetParent !== null) {
                            const rect = promptField.getBoundingClientRect();
                            const hole = document.createElement('div');
                            hole.style.position = 'fixed';
                            hole.style.left = rect.left + 'px';
                            hole.style.top = rect.top + 'px';
                            hole.style.width = rect.width + 'px';
                            hole.style.height = rect.height + 'px';
                            hole.style.pointerEvents = 'none';
                            hole.style.zIndex = '10000';
                            document.body.appendChild(hole);
                            promptField.style.zIndex = '10001';
                            promptField.style.position = 'relative';
                            promptField.style.pointerEvents = 'auto';
                        }
                    }, 100);
                """);

        setSizeFull();
        setAlignItems(FlexComponent.Alignment.CENTER);
        Div backgroundWaves = new Div();
backgroundWaves.getElement().setProperty("innerHTML", """
    <svg class="waves" xmlns="http://www.w3.org/2000/svg" xmlns:xlink="http://www.w3.org/1999/xlink"
        viewBox="0 0 150 100" preserveAspectRatio="none" shape-rendering="auto">
      <defs>
        <path id="gentle-wave" d="M-160 44c30 0 58-18 88-18s 58 18 88 18
          58-18 88-18 58 18 88 18 v44h-352z" />
      </defs>
      <g class="parallax">
        <use xlink:href="#gentle-wave" x="48" y="0" fill="rgba(37, 40, 57,0.2)" />
        <use xlink:href="#gentle-wave" x="48" y="2" fill="rgba(37, 40, 57,0.5)" />
        <use xlink:href="#gentle-wave" x="48" y="-1" fill="rgba(37, 40, 57,0.3)" />
        <use xlink:href="#gentle-wave" x="48" y="0" fill="#252839" />
      </g>
    </svg>
""");
backgroundWaves.addClassName("background-waves");


getElement().insertChild(0, backgroundWaves.getElement());

        this.grid = new Grid<>(MastodonPost.class);
        this.grid.addClassName("custom-grid");
        grid.setSelectionMode(Grid.SelectionMode.SINGLE);
        grid.setColumns(); // Supprime toutes les colonnes par défaut

        this.contentDiv = new Div();
        contentDiv.setWidthFull();

        addIndexColumn(grid);
        addPostInfoColumn(grid); // Nouvelle méthode pour afficher toutes les infos dans une seule colonne

        List<MastodonPost> favoris = new ArrayList<>();
        PostSelector selector = this::selectAndDisplay;

        Map<String, Command> commandMap = CommandFactory.build(
                grid,
                contentDiv,
                selector,
                favoris,
                () -> fetchPostsFromTag("info"),
                this::fetchPostsFromTag,
                commandesTapees);

        PlayCommand playCmd = (PlayCommand) commandMap.get("play");
        
        prompt.addValueChangeListener(v -> {
            String text = v.getValue().trim();
            if (text.isEmpty())
                return;

            playCmd.stop();
            commandesTapees.add(text);

            String commandKey = text.split(" ")[0];
            Command command = commandMap.getOrDefault(commandKey, null);

            if (command != null) {
                command.execute(text);
            } else {
                Notification notification = Notification.show("Commande inconnue : \"" + commandKey + "\"", 1000, Notification.Position.TOP_END);
                notification.addThemeVariants(NotificationVariant.LUMO_ERROR);
            }
            prompt.setValue("");
        });

        // Créer un HorizontalLayout pour contenir la grid et le contenu
        var horizontalLayout = new HorizontalLayout();
        horizontalLayout.setSizeFull();
        horizontalLayout.add(backgroundWaves, grid, contentDiv);
        horizontalLayout.getStyle().set("position", "relative");

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

        grid.addSelectionListener(event -> selectItemListener(grid, contentDiv, event));

    }

    private void addIndexColumn(Grid<MastodonPost> grid) {
        grid.addColumn(post -> {
            List<MastodonPost> items = grid.getListDataView().getItems().toList();
            return items.indexOf(post) + 1;
        }).setWidth("50px");
    }

private void addPostInfoColumn(Grid<MastodonPost> grid) {
        grid.addComponentColumn(post -> {
            Div container = new Div();
            container.addClassName("post-info-row");
            container.setWidthFull();

            Span authorName = new Span(post.getAccount().getDisplayName());
            authorName.addClassName("author-name");
            authorName.getStyle().set("font-weight", "bold").set("margin-right", "0.5em");

            Span username = new Span("@" + post.getAccount().getUsername());
            username.addClassName("username");
            username.getStyle().setColor("gray").setFontSize("small").set("margin-right", "1em");

            Div contentPreview = new Div();
            String textContent = Jsoup.parse(post.getContent()).text();
            String previewText = textContent.length() > 150 ? textContent.substring(0, 150) + "..." : textContent;
            contentPreview.setText(previewText);
            contentPreview.addClassName("post-content");
            contentPreview.setWidthFull();
            contentPreview.getStyle().set("white-space", "normal").set("word-break", "break-word");

            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
            String formattedDate = post.getCreatedAt().format(formatter);
            Span dateSpan = new Span(formattedDate);
            dateSpan.addClassName("post-date");
            dateSpan.getStyle().setColor("gray").setFontSize("small");

            HorizontalLayout header = new HorizontalLayout(authorName, username, dateSpan);
            header.setAlignItems(FlexComponent.Alignment.BASELINE);
            header.setWidthFull();

            VerticalLayout postLayout = new VerticalLayout(header, contentPreview);
            postLayout.setSpacing(false);
            postLayout.setPadding(false);
            postLayout.setWidthFull();
            postLayout.setWidth("auto");
            postLayout.setMaxWidth("800px");

            container.add(postLayout);
            return container;
        }).setAutoWidth(true);
        grid.removeAllHeaderRows();
        
    }

    public List<MastodonPost> fetchPostsFromTag(String tag) {
        if (StringUtils.isEmpty(tag))
            return List.of();

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
            return Arrays.stream(mapper.readValue(response.toString(), MastodonPost[].class)).toList();
        } catch (IOException | URISyntaxException e) {
            throw new RuntimeException(e);
        }
    }

    private void selectItemListener(Grid<MastodonPost> grid, Div contentDiv,
            com.vaadin.flow.data.selection.SelectionEvent<Grid<MastodonPost>, MastodonPost> event) {
        grid.getDataProvider().fetch(new Query<>()).forEach(p -> grid.setDetailsVisible(p, false));
        event.getFirstSelectedItem().ifPresent(this::selectAndDisplay);
    }

    public void selectAndDisplay(MastodonPost post) {
        grid.select(post);
        contentDiv.removeAll();
    
        if (post != null) {
            // Créer le layout principal pour tout le post
            VerticalLayout container = new VerticalLayout();
            container.setWidthFull();
            container.setSpacing(false);

            // HEADER : HorizontalLayout
            HorizontalLayout header = new HorizontalLayout();
            header.setWidthFull();
            header.setJustifyContentMode(FlexComponent.JustifyContentMode.BETWEEN);
            header.setAlignItems(FlexComponent.Alignment.START); // Ajouté pour bien aligner en haut

            // Partie gauche (Nom + @)
            VerticalLayout userInfo = new VerticalLayout();
            userInfo.setSpacing(false);
            userInfo.setPadding(false);

            Div displayNameDiv = new Div();
            displayNameDiv.getStyle().set("font-weight", "bold");
            displayNameDiv.setText(post.getAccount().getDisplayName());

            Div usernameDiv = new Div();
            usernameDiv.getStyle().set("color", "gray");
            usernameDiv.setText("@" + post.getAccount().getUsername());

            userInfo.add(displayNameDiv, usernameDiv);

            // Partie droite (date)
            Div dateDiv = new Div();
            String formattedDate = post.getCreatedAt()
                .toLocalDate()
                .format(java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy"));
            dateDiv.getStyle().set("font-size", "0.8em");
            dateDiv.getStyle().set("color", "gray");
            dateDiv.setText(formattedDate);

            // Ajouter userInfo et dateDiv dans le header
            header.add(userInfo, dateDiv);

            // Ajouter le header dans le container
            container.add(header);
            
            Div postContent = new Div();
            postContent.getElement().setProperty("innerHTML", post.getContent());
            container.add(postContent);
            // ➔ Infos engagement (reposts, favoris...)
            Div engagementDiv = new Div();
            String engagementText = String.format(
                    "💬 %d   🔁 %d   ❤️ %d",
                    post.getRepliesCount(),
                    post.getReblogsCount(),
                    post.getFavouritesCount()
            );
            engagementDiv.setText(engagementText);
            container.add(engagementDiv);

            // Afficher tout dans la page
            contentDiv.removeAll();
            contentDiv.add(container);
            grid.setDetailsVisible(post, true);
        }
    }
    
};
