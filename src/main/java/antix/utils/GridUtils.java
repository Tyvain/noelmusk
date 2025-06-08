package antix.utils;

import antix.model.SocialMediaPost;
import antix.components.PlatformBadge;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.data.provider.Query;
import org.jsoup.Jsoup;
import com.vaadin.flow.component.html.Image;

import java.util.List;
import java.util.stream.Collectors;

public final class GridUtils {
    
    private GridUtils() {}

    public static List<SocialMediaPost> fetchAll(Grid<SocialMediaPost> grid) {
        return grid.getDataProvider()
                .fetch(new Query<>())
                .collect(Collectors.toList());
    }

public static void addIndexColumn(Grid<SocialMediaPost> grid) {
    grid.addColumn(post -> {
        // Solution simple : utiliser fetchAll que tu as déjà défini
        List<SocialMediaPost> items = fetchAll(grid);
        return items.indexOf(post) + 1;
    }).setWidth("50px").setHeader("#").setFlexGrow(0);
}

public static void addPlatformColumn(Grid<SocialMediaPost> grid) {
    grid.addComponentColumn(post -> {
        // Container pour centrer parfaitement
        Div container = new Div();
        container.getStyle()
            .set("display", "flex")
            .set("justify-content", "center")
            .set("align-items", "center")
            .set("height", "100%");
        
        Image logo = new Image(post.getLogoPath(), post.getPlatformDisplayName());
        logo.setWidth("24px");   // ✅ Plus grand (était 20px)
        logo.setHeight("24px");  // ✅ Plus grand (était 20px)
        logo.getStyle()
            .set("border-radius", "4px")      // Coins légèrement plus arrondis
            .set("object-fit", "contain")     // Garde les proportions
            .set("display", "block");         // Assure un affichage proper
        logo.setTitle(post.getPlatformDisplayName()); // Tooltip
        
        container.add(logo);
        return container;
    })
    .setWidth("60px")        // ✅ Largeur fixe pour la colonne
    .setFlexGrow(0)         // ✅ Empêche l'expansion automatique
    .setHeader("P");
}

    public static void addPostInfoColumn(Grid<SocialMediaPost> grid) {
        grid.addComponentColumn(post -> {
            Div container = new Div();
            container.addClassName("post-info-row");
            container.setWidthFull();

            // Badge de plateforme utilisant les nouvelles méthodes du modèle
            PlatformBadge platformBadge = new PlatformBadge(post, PlatformBadge.Size.SMALL, PlatformBadge.Style.BADGE);

            Span authorName = new Span(post.getDisplayName());
            authorName.addClassName("author-name");
            authorName.getStyle().set("font-weight", "bold").set("margin-right", "0.5em");

            Span platformInfo = new Span(post.getPlatformInfo());
            platformInfo.addClassName("username");
            platformInfo.getStyle().setColor("gray").setFontSize("small").set("margin-right", "1em");

            // Contenu du post
            Div contentPreview = new Div();
            String textContent = Jsoup.parse(post.getContent()).text();
            String previewText = textContent.length() > 150 ? textContent.substring(0, 150) + "..." : textContent;
            contentPreview.setText(previewText);
            contentPreview.addClassName("post-content");
            contentPreview.setWidthFull();
            contentPreview.getStyle().set("white-space", "normal").set("word-break", "break-word");

            Span scoreSpan = new Span(post.getEngagementText());
            scoreSpan.addClassName("post-date");
            scoreSpan.getStyle().setColor("gray").setFontSize("small");

            // Header avec badge
            HorizontalLayout header = new HorizontalLayout(platformBadge, authorName, platformInfo, scoreSpan);
            header.setAlignItems(FlexComponent.Alignment.CENTER);
            header.setWidthFull();

            VerticalLayout postLayout = new VerticalLayout(header, contentPreview);
            postLayout.setSpacing(false);
            postLayout.setPadding(false);
            postLayout.setWidthFull();

            container.add(postLayout);
            return container;
        }).setAutoWidth(true);
    }
}
