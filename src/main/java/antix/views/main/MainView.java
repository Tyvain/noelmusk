package antix.views.main;

import antix.factory.CommandFactory;
import antix.model.SocialMediaPost;
import antix.service.MastodonService;
import antix.service.RedditService;
import antix.utils.FeedbackUtils;
import antix.utils.GridUtils;
import antix.components.PlatformBadge;
import antix.views.main.commands.Command;
import antix.views.main.commands.PlayCommand;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import org.apache.commons.lang3.StringUtils;
import com.vaadin.flow.data.provider.Query;
import com.vaadin.flow.component.Key;
import com.vaadin.flow.component.KeyModifier;
import com.vaadin.flow.component.ComponentEventListener;
import com.vaadin.flow.component.KeyDownEvent;

import java.util.*;
import java.util.function.Supplier;
import java.util.logging.Logger;
import java.util.logging.Level;

@PageTitle("NoelMusk - Test pour la bande")
@Route("")
public class MainView extends VerticalLayout {
    
    private static final Logger logger = Logger.getLogger(MainView.class.getName());
    
    private final Grid<SocialMediaPost> grid;
    private final Div contentDiv;
    private final List<String> commandesTapees = new ArrayList<>();
    
    // ✅ Nouveaux attributs pour la navigation dans l'historique
    private int historyIndex = -1;
    private String currentInput = "";
    private TextField prompt; // Référence au champ de commande
    private boolean isNavigatingHistory = false; // Flag pour désactiver le listener
    private Map<String, Command> commandMap; // Référence aux commandes
    private PlayCommand playCmd; // Référence au PlayCommand
    
    private final RedditService redditService = new RedditService();
    private final MastodonService mastodonService = new MastodonService(); 
    
    public MainView() {
        // ✅ Initialisation du TextField
        this.prompt = new TextField();
        prompt.setId("prompt-field");
        prompt.setWidth("100%");
        
        // ✅ Ajout du gestionnaire pour les flèches avec JavaScript
        prompt.addKeyDownListener(this::handleKeyDown);
        
        // ✅ Ajout du gestionnaire pour Enter (exécution directe)
        prompt.addKeyPressListener(Key.ENTER, event -> {
            String command = prompt.getValue().trim();
            if (!command.isEmpty()) {
                System.out.println("🎯 Exécution via Enter: '" + command + "'");
                executeCommand(command);
            }
        });
        
        // Ajout d'un gestionnaire JavaScript pour empêcher le comportement par défaut des flèches
        prompt.getElement().addEventListener("keydown", event -> {
            // Ce code sera exécuté côté serveur, mais on peut ajouter du JS côté client
        });
        
        // JavaScript pour gérer les flèches côté client
        prompt.getElement().executeJs("""
            this.addEventListener('keydown', function(e) {
                if (e.key === 'ArrowUp' || e.key === 'ArrowDown') {
                    // Laisser Vaadin gérer l'événement côté serveur
                    // mais empêcher le déplacement du curseur
                    if (e.key === 'ArrowUp' || e.key === 'ArrowDown') {
                        setTimeout(() => {
                            this.setSelectionRange(this.value.length, this.value.length);
                        }, 0);
                    }
                }
            });
        """);
        
        // Code JavaScript existant...
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
        
        // Code des vagues existant...
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

        // Grid setup avec GridUtils
        this.grid = new Grid<>(SocialMediaPost.class);
        this.grid.addClassName("custom-grid");
        grid.setSelectionMode(Grid.SelectionMode.SINGLE);
        grid.setColumns(); // Efface les colonnes par défaut

        this.contentDiv = new Div();
        contentDiv.setWidthFull();

        // ✅ Utilisation de GridUtils
        GridUtils.addIndexColumn(grid);
        GridUtils.addPlatformColumn(grid);
        GridUtils.addPostInfoColumn(grid);

        List<SocialMediaPost> favoris = new ArrayList<>();
        
        PostSelector selector = this::selectAndDisplay;

        Supplier<List<SocialMediaPost>> defaultSupplier = () -> this.fetchPostsFromTag("programming");

        // ✅ Mise à jour pour passer la référence à l'historique
        Map<String, Command> commandMap = CommandFactory.build(
                grid,
                contentDiv,
                selector,
                favoris,
                defaultSupplier,
                this::fetchPostsFromTag,
                commandesTapees); // Utilise la même liste pour l'historique

        PlayCommand playCmd = (PlayCommand) commandMap.get("play");
        
        // ✅ Stockage des références pour executeCommand
        this.commandMap = commandMap;
        this.playCmd = playCmd;
        
        // ✅ Gestionnaire d'événements modifié pour utiliser l'historique
        prompt.addValueChangeListener(v -> {
            // ✅ Ignorer si on navigue dans l'historique
            if (isNavigatingHistory) {
                System.out.println("🚫 Navigation en cours - Événement ignoré");
                return;
            }
            
            String text = v.getValue().trim();
            if (text.isEmpty())
                return;

            playCmd.stop();
            
            // ✅ Ajouter à l'historique avec la nouvelle méthode
            addToHistory(text);

            String commandKey = text.split(" ")[0];
            Command command = commandMap.getOrDefault(commandKey, null);

            if (command != null) {
                command.execute(text);
            } else {
                FeedbackUtils.showError("Commande inconnue : \"" + commandKey + "\"");
            }
            prompt.setValue("");
        });

        // Reste du layout existant...
        var horizontalLayout = new HorizontalLayout();
        horizontalLayout.setSizeFull();
        horizontalLayout.add(backgroundWaves, grid, contentDiv);
        horizontalLayout.getStyle().set("position", "relative");

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

        grid.addSelectionListener(event -> selectItemListener(grid, contentDiv, event));
        
        // --- ONBOARDING OVERLAY ---
        Div onboardingOverlay = new Div();
        onboardingOverlay.setId("onboarding-overlay");
        onboardingOverlay.getStyle().set("position", "fixed");
        onboardingOverlay.getStyle().set("top", "0");
        onboardingOverlay.getStyle().set("left", "0");
        onboardingOverlay.getStyle().set("width", "100vw");
        onboardingOverlay.getStyle().set("height", "100vh");
        onboardingOverlay.getStyle().set("background", "rgba(37,40,57,0.98)");
        onboardingOverlay.getStyle().set("z-index", "2147483647");
        onboardingOverlay.getStyle().set("display", "flex");
        onboardingOverlay.getStyle().set("flex-direction", "column");
        onboardingOverlay.getStyle().set("align-items", "center");
        onboardingOverlay.getStyle().set("justify-content", "center");
        onboardingOverlay.getStyle().set("cursor", "pointer");
        onboardingOverlay.getStyle().set("color", "white");
        onboardingOverlay.getStyle().set("padding", "40px");

        // Génération dynamique du tableau des commandes
        StringBuilder tableHtml = new StringBuilder();
        tableHtml.append("""
        <div id='onboarding-overlay' style='
            position: fixed;
            top: 0; left: 0; right: 0; bottom: 0;
            z-index: 9999;
            background-color: rgba(0, 0, 0, 0.8);
            overflow-y: auto;
            display: flex;
            justify-content: center;
            align-items: flex-start;
            padding: 40px 20px;
        '>
            <div style='
                width: 100%;
                max-width: 1100px;
                margin: 0 auto;
                padding: 20px;
                background: #1e1e2f;
                border-radius: 12px;
                box-shadow: 0 8px 24px rgba(0,0,0,0.3);
                color: #f0f0f0;
                font-family: sans-serif;
            '>
                <h2 style='margin-top: 0; text-align: center; color: #fff'>Bienvenue 👋</h2>
                <p style='text-align: center; margin-bottom: 24px; font-size: 1.1em'>
                    Voici la liste des commandes disponibles :
                </p>
                <div style='overflow-x: auto'>
                    <table style='width: 100%; border-collapse: collapse;'>
                        <thead>
                            <tr style='background: #2a2a3b;'>
                                <th style='padding: 12px 10px; text-align: left; border-bottom: 2px solid #444;'>Nom</th>
                                <th style='padding: 12px 10px; text-align: left; border-bottom: 2px solid #444;'>Alias</th>
                                <th style='padding: 12px 10px; text-align: left; border-bottom: 2px solid #444;'>Définition</th>
                                
                            </tr>
                        </thead>
                        <tbody>
        """);

        Set<String> dejaVu = new HashSet<>();
        for (Command cmd : commandMap.values()) {
            String uniqueKey = String.join(",", cmd.getTitle());
            if (dejaVu.contains(uniqueKey)) continue;
            dejaVu.add(uniqueKey);

            String noms = String.join(", ", cmd.getTitle());
            List<String> alias = cmd.getAliases();
            String fonctionnement = cmd.getDescription() != null ? cmd.getDescription() : "";

            tableHtml.append("<tr>");
            tableHtml.append("<td style='padding:8px 8px;border-bottom:1px solid #333;'>").append(noms).append("</td>");
            tableHtml.append("<td style='padding:8px 8px;border-bottom:1px solid #333;'>").append(alias).append("</td>");
            tableHtml.append("<td style='padding:8px 8px;border-bottom:1px solid #333;'>").append(fonctionnement).append("</td>");
            tableHtml.append("</tr>");
        }

        tableHtml.append("""
                </tbody>
            </table>
            </div> <!-- fermeture du div overflow-x:auto -->
            <div style='margin-top:2em; font-size:1em; color:#aaa; text-align:center'>
                <b>Astuce :</b> Utilisez les flèches haut/bas pour naviguer dans l'historique des commandes.<br>
                <b>Astuce :</b> Cliquez n'importe où sur cette fenêtre pour commencer à utiliser le site.
            </div>
            </div>
            <style>
                #onboarding-overlay table {
                    table-layout: auto;
                    min-width: 700px;
                    max-width: 1100px;
                    font-size: 0.98em;
                }
                #onboarding-overlay th, #onboarding-overlay td {
                    word-break: break-word;
                    white-space: pre-line;
                    padding: 8px 8px;
                }
                #onboarding-overlay th {
                    background: #23253a;
                }
                #onboarding-overlay {
                    overflow-y: auto;
                }
                @media (max-width: 900px) {
                    #onboarding-overlay table {
                        min-width: 500px;
                        font-size: 0.93em;
                    }
                }
            </style>
""");

        onboardingOverlay.getElement().setProperty("innerHTML", tableHtml.toString());

        UI.getCurrent().getElement().appendChild(onboardingOverlay.getElement());

        onboardingOverlay.addClickListener(e -> {
            onboardingOverlay.getElement().removeFromParent();
        });

    }

    private void handleKeyDown(KeyDownEvent event) {
        System.out.println("🔍 Touche détectée: " + event.getKey());
        System.out.println("📚 Historique actuel: " + commandesTapees.size() + " commandes");
        System.out.println("📍 Index actuel: " + historyIndex);
        
        // ✅ Correction: utiliser toString() ou comparaison directe
        String keyPressed = event.getKey().toString();
        
        if ("ArrowUp".equals(keyPressed)) {
            System.out.println("⬆️ Flèche du haut détectée - Navigation UP");
            navigateHistoryUp();
        } else if ("ArrowDown".equals(keyPressed)) {
            System.out.println("⬇️ Flèche du bas détectée - Navigation DOWN");
            navigateHistoryDown();
        } else if ("ArrowRight".equals(keyPressed)) {
            System.out.println("➡️ Flèche droite détectée - Commande NEXT");
            executeCommand("n");
        } else if ("ArrowLeft".equals(keyPressed)) {
            System.out.println("⬅️ Flèche gauche détectée - Commande PREVIOUS");
            executeCommand("p");
        } else {
            // Si l'utilisateur tape autre chose, réinitialiser l'index
            if (!isNavigationKey(event.getKey())) {
                historyIndex = commandesTapees.size();
            }
        }
    }
    
    // Vérifier si c'est une touche de navigation (à ignorer pour la réinitialisation)
    private boolean isNavigationKey(Key key) {
        String keyPressed = key.toString();
        return "ArrowUp".equals(keyPressed) || "ArrowDown".equals(keyPressed) || 
               "ArrowLeft".equals(keyPressed) || "ArrowRight".equals(keyPressed) ||
               "Shift".equals(keyPressed) || "Control".equals(keyPressed) || "Alt".equals(keyPressed) ||
               "Home".equals(keyPressed) || "End".equals(keyPressed);
    }
    
    // Ajouter une commande à l'historique
    public void addToHistory(String command) {
        if (!command.trim().isEmpty()) {
            // Éviter les doublons consécutifs
            if (commandesTapees.isEmpty() || !commandesTapees.get(commandesTapees.size() - 1).equals(command)) {
                commandesTapees.add(command);
            }
        }
        // Réinitialiser l'index après ajout
        historyIndex = commandesTapees.size();
        currentInput = "";
    }
    
    // Navigation vers le haut dans l'historique (commandes plus anciennes)
    private void navigateHistoryUp() {
        System.out.println("🔄 Navigation UP - Historique: " + commandesTapees.size() + " éléments");
        
        if (commandesTapees.isEmpty()) {
            System.out.println("❌ Historique vide");
            return;
        }
        
        // Sauvegarder la saisie actuelle si on est à la fin
        if (historyIndex == commandesTapees.size()) {
            currentInput = prompt.getValue();
            System.out.println("💾 Sauvegarde input actuel: '" + currentInput + "'");
        }
        
        if (historyIndex > 0) {
            historyIndex--;
            String command = commandesTapees.get(historyIndex);
            System.out.println("✅ Navigation vers: '" + command + "' (index " + historyIndex + ")");
            
            // ✅ Désactiver temporairement le listener
            isNavigatingHistory = true;
            prompt.setValue(command);
            isNavigatingHistory = false;
            
            // Placer le curseur à la fin
            prompt.getElement().executeJs("this.setSelectionRange(this.value.length, this.value.length)");
        } else if (historyIndex == 0) {
            // Déjà à la première commande, rester dessus
            String command = commandesTapees.get(0);
            System.out.println("🔄 Reste sur première commande: '" + command + "'");
            
            // ✅ Désactiver temporairement le listener
            isNavigatingHistory = true;
            prompt.setValue(command);
            isNavigatingHistory = false;
            
            prompt.getElement().executeJs("this.setSelectionRange(this.value.length, this.value.length)");
        } else {
            // Premier accès à l'historique
            historyIndex = commandesTapees.size() - 1;
            if (historyIndex >= 0) {
                String command = commandesTapees.get(historyIndex);
                System.out.println("🎯 Premier accès à l'historique: '" + command + "' (index " + historyIndex + ")");
                
                // ✅ Désactiver temporairement le listener
                isNavigatingHistory = true;
                prompt.setValue(command);
                isNavigatingHistory = false;
                
                prompt.getElement().executeJs("this.setSelectionRange(this.value.length, this.value.length)");
            }
        }
    }
    
    // Navigation vers le bas dans l'historique (commandes plus récentes)
    private void navigateHistoryDown() {
        System.out.println("🔄 Navigation DOWN - Historique: " + commandesTapees.size() + " éléments");
        
        if (commandesTapees.isEmpty() || historyIndex < 0) {
            System.out.println("❌ Historique vide ou index invalide");
            return;
        }
        
        if (historyIndex < commandesTapees.size() - 1) {
            historyIndex++;
            String command = commandesTapees.get(historyIndex);
            System.out.println("✅ Navigation vers: '" + command + "' (index " + historyIndex + ")");
            
            // ✅ Désactiver temporairement le listener
            isNavigatingHistory = true;
            prompt.setValue(command);
            isNavigatingHistory = false;
            
            prompt.getElement().executeJs("this.setSelectionRange(this.value.length, this.value.length)");
        } else if (historyIndex == commandesTapees.size() - 1) {
            // Retour à la saisie actuelle
            historyIndex = commandesTapees.size();
            System.out.println("🔄 Retour à la saisie actuelle: '" + currentInput + "'");
            
            // ✅ Désactiver temporairement le listener
            isNavigatingHistory = true;
            prompt.setValue(currentInput);
            isNavigatingHistory = false;
            
            prompt.getElement().executeJs("this.setSelectionRange(this.value.length, this.value.length)");
        }
    }

    // ✅ Nouvelle méthode pour exécuter les commandes
    private void executeCommand(String command) {
        playCmd.stop();
        
        // ✅ Ajouter à l'historique
        addToHistory(command);

        String commandKey = command.split(" ")[0];
        Command commandObj = commandMap.getOrDefault(commandKey, null);

        if (commandObj != null) {
            commandObj.execute(command);
        } else {
            FeedbackUtils.showError("Commande inconnue : \"" + commandKey + "\"");
        }
        
        // ✅ Vider le champ après exécution
        prompt.setValue("");
    }
    public List<SocialMediaPost> fetchPostsFromTag(String tag) {
        return fetchPostsFromTag(tag, 80);
    }

    public List<SocialMediaPost> fetchPostsFromTag(String tag, int maxPerService) {
        if (StringUtils.isEmpty(tag)) {
            return List.of();
        }

        List<SocialMediaPost> allPosts = new ArrayList<>();
        
        try {
            System.out.println("🔍 Tentative de récupération des posts Reddit pour le tag: " + tag);
            List<SocialMediaPost> redditPosts = redditService.fetchPostsFromTag(tag, maxPerService);
            System.out.println("✅ Posts Reddit récupérés: " + redditPosts.size() + "/" + maxPerService + " max");
            allPosts.addAll(redditPosts);
            
            System.out.println("🔍 Tentative de récupération des posts Mastodon pour le tag: " + tag);
            List<SocialMediaPost> mastodonPosts = mastodonService.fetchPostsFromTag(tag, maxPerService);
            System.out.println("✅ Posts Mastodon récupérés: " + mastodonPosts.size() + "/" + maxPerService + " max");
            allPosts.addAll(mastodonPosts);
            
            allPosts.sort((post1, post2) -> post2.getCreatedAt().compareTo(post1.getCreatedAt()));
            
            System.out.println("🎯 TOTAL FINAL: " + allPosts.size() + " posts");
            System.out.println("📊 Répartition: " + redditPosts.size() + " Reddit + " + mastodonPosts.size() + " Mastodon");
            
            updateGrid(allPosts);
            
            return allPosts;
            
        } catch (Exception e) {
            System.out.println("❌ ERREUR lors de la récupération des posts: " + e.getMessage());
            logger.log(Level.SEVERE, "Erreur lors de la récupération des posts", e);
            FeedbackUtils.showError("Erreur lors de la récupération des posts : " + e.getMessage());
            
            return new ArrayList<>();
        }
    }

    public void updateGrid(List<SocialMediaPost> posts) {
        System.out.println("🔄 Mise à jour du grid avec " + posts.size() + " posts");
        grid.setItems(posts);
        grid.getDataProvider().refreshAll();
        
        if (!posts.isEmpty()) {
            System.out.println("✅ Grid mis à jour avec succès");
        } else {
            System.out.println("⚠️ Aucun post à afficher dans le grid");
        }
    }

    private void selectItemListener(Grid<SocialMediaPost> grid, Div contentDiv,
            com.vaadin.flow.data.selection.SelectionEvent<Grid<SocialMediaPost>, SocialMediaPost> event) {
        grid.getDataProvider().fetch(new Query<>()).forEach(p -> grid.setDetailsVisible(p, false));
        event.getFirstSelectedItem().ifPresent(this::selectAndDisplay);
    }

    public void selectAndDisplay(SocialMediaPost post) {
        grid.select(post);
        contentDiv.removeAll();

        if (post != null) {
            VerticalLayout container = new VerticalLayout();
            container.setWidthFull();
            container.setSpacing(false);

            PlatformBadge largeBadge = new PlatformBadge(post, PlatformBadge.Size.LARGE, PlatformBadge.Style.BADGE);
            HorizontalLayout platformHeader = new HorizontalLayout(largeBadge);
            container.add(platformHeader);

            HorizontalLayout header = new HorizontalLayout();
            header.setWidthFull();
            header.setJustifyContentMode(FlexComponent.JustifyContentMode.BETWEEN);
            header.setAlignItems(FlexComponent.Alignment.START);

            VerticalLayout userInfo = new VerticalLayout();
            userInfo.setSpacing(false);
            userInfo.setPadding(false);

            Div displayNameDiv = new Div();
            displayNameDiv.getStyle().set("font-weight", "bold");
            displayNameDiv.setText(post.getDisplayName());

            Div platformDiv = new Div();
            platformDiv.getStyle().set("color", "orange");
            platformDiv.setText(post.getPlatformInfo());

            Div dateDiv = new Div();
            dateDiv.getStyle().set("color", "gray");
            dateDiv.getStyle().set("font-size", "0.9em");
            dateDiv.getStyle().set("margin-top", "4px");
            dateDiv.setText("📅 " + post.getFormattedDate());

            userInfo.add(displayNameDiv, platformDiv, dateDiv); 

            Div scoreDiv = new Div();
            scoreDiv.getStyle().set("font-size", "0.8em");
            scoreDiv.getStyle().set("color", "gray");
            scoreDiv.setText(post.getScoreText());

            header.add(userInfo, scoreDiv);
            container.add(header);
            
            Div postContent = new Div();
            postContent.getElement().setProperty("innerHTML", post.getContent());
            container.add(postContent);
            
            Div engagementDiv = new Div();
            engagementDiv.setText(post.getEngagementText());
            container.add(engagementDiv);

            contentDiv.removeAll();
            contentDiv.add(container);
            grid.setDetailsVisible(post, true);
        }
    }
}