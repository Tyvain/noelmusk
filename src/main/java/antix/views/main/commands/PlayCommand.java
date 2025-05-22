package antix.views.main.commands;

import antix.model.MastodonPost;
import antix.utils.GridUtils;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.grid.Grid;

import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Commande qui permet de faire défiler automatiquement les posts à intervalle
 * régulier.
 * Ex : play 5 → défilement toutes les 5 secondes.
 */
public class PlayCommand extends Command {

    private Timer timer;
    private final Grid<MastodonPost> grid;

    /**
     * Constructeur de la commande Play.
     *
     * @param grid Grille contenant les posts.
     */
    public PlayCommand(Grid<MastodonPost> grid) {
        super(
            List.of("play"),
            "Play",
            """
            ▶️ play <n>
            
            💡 Change de post automatiquement toutes les n secondes (rotation circulaire)
            """
        );
        this.grid = grid;
    }

    /**
     * Lance la lecture automatique des posts, un par un, à intervalles réguliers.
     *
     * @param input Entrée utilisateur (ex : \"play 5\" pour 5 secondes entre chaque
     *              post).
     */
    @Override
    public void execute(String input) {
        stop(); // Arrête une éventuelle lecture déjà en cours

        int seconds = 3;
        try {
            seconds = Integer.parseInt(input.replaceFirst("play\\s*", "").trim());
        } catch (NumberFormatException ignored) {
            // Si invalide, garde 3 secondes par défaut
        }

        UI ui = UI.getCurrent();
        if (ui == null)
            return;

        List<MastodonPost> items = GridUtils.fetchAll(grid);
        if (items.isEmpty())
            return;

        MastodonPost current = grid.asSingleSelect().getValue();
        int startIndex = (current != null) ? items.indexOf(current) : 0;
        AtomicInteger index = new AtomicInteger(startIndex);

        timer = new Timer();
        timer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                ui.access(() -> {
                    List<MastodonPost> currentItems = GridUtils.fetchAll(grid); // re-fetch à chaque tick
                    if (currentItems.isEmpty())
                        return;

                    int i = index.getAndUpdate(val -> (val + 1) % currentItems.size());
                    MastodonPost post = currentItems.get(i);
                    grid.select(post);
                });
            }
        }, 0, seconds * 1000L);
    }

    /**
     * Stoppe la lecture automatique si elle est en cours.
     */
    public synchronized void stop() {
        if (timer != null) {
            timer.cancel();
            timer = null;
        }
    }
}
