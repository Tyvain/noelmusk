package antix.components;

import com.vaadin.flow.component.dependency.CssImport;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.FlexLayout;
import antix.views.main.commands.Command;

@CssImport("./themes/antix/command-card.css")
public class CommandCard extends Div {
    
    // Conteneur statique pour organiser les cartes
    private static FlexLayout container;
    
    public CommandCard(Command command) {
        // Ajout des classes CSS
        addClassName("command-card");
        
        // Titre de la commande
        H3 title = new H3(command.getTitle());
        title.addClassName("command-card-title");
        
        // Aliases de la commande
        Span aliases = new Span("[" + String.join(" / ", command.getAliases()) + "]");
        aliases.addClassName("command-card-aliases");
        
        // Description de la commande (apparaît au survol)
        Paragraph description = new Paragraph(command.getDescription());
        description.addClassName("command-card-description");
        
        // Ajout des composants à la carte
        add(title, aliases, description);
    }
    
    /**
     * Méthode statique pour obtenir ou créer le conteneur de cartes de commandes
     * Le conteneur s'adapte automatiquement à la taille de l'écran grâce au CSS
     * 
     * @return un FlexLayout configuré pour contenir des CommandCard de façon responsive
     */
    public static FlexLayout getContainer() {
        if (container == null) {
            container = new FlexLayout();
            container.addClassName("command-cards-container");
            container.setWidthFull();
            container.setFlexWrap(FlexLayout.FlexWrap.WRAP);
        }
        return container;
    }
}