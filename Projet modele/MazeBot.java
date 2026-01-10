import javax.swing.Timer;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Random;

public class MazeBot implements ActionListener {
    private final MazeGame game;
    private final Timer timer;
    private final Random rng = new Random();
    private final Deque<Point> trail = new ArrayDeque<>();
    private final Map<Point, IntersectionState> intersections = new HashMap<>();
    private final Deque<Point> intersectionHistory = new ArrayDeque<>();
    private double forgetProbability = 0.15; // 15% par défaut
    private boolean active = false;
    private boolean backtracking = false; // vrai quand on remonte vers une intersection
    // Réflexion / pause
    private boolean thinking = false;
    private long resumeAtMs = 0;
    private double thinkChance = 0.25; // 25% de chance de pause
    private int thinkMinMs = 500;      // 0.5 seconde
    private int thinkMaxMs = 1500;     // 1.5 seconde
    // Délai de mouvement
    private int moveMinMs = 120;       // 120 ms
    private int moveMaxMs = 250;       // 250 ms
    private long nextMoveAtMs = 0;    // quand autoriser le prochain mouvement
    // Revue périodique des intersections après un certain nombre de culs-de-sac
    private int deadEndRevisitInterval = 3; // toutes les 3 impasses, on remonte 3 intersections
    private double deadEndRevisitChance = 0.25; // probabilité de déclencher ce retour
    private int deadEndCount = 0;
    private int forcedBacktrackIntersections = 0;

    public MazeBot(MazeGame game) {
        this.game = game;
        this.timer = new Timer(120, this);
    }

    public void setForgetProbability(double probability) {
        this.forgetProbability = Math.max(0.0, Math.min(1.0, probability));
    }

    public void setThinkChance(double probability) {
        this.thinkChance = Math.max(0.0, Math.min(1.0, probability));
    }

    public void setThinkDurationRange(int minMs, int maxMs) {
        if (minMs < 0) minMs = 0;
        if (maxMs < minMs) maxMs = minMs;
        this.thinkMinMs = minMs;
        this.thinkMaxMs = maxMs;
    }

    public void setMoveDurationRange(int minMs, int maxMs) {
        if (minMs < 0) minMs = 0;
        if (maxMs < minMs) maxMs = minMs;
        this.moveMinMs = minMs;
        this.moveMaxMs = maxMs;
    }

    public void setDeadEndRevisitInterval(int intersectionsToRevisit) {
        if (intersectionsToRevisit < 1) {
            intersectionsToRevisit = 1;
        }
        this.deadEndRevisitInterval = intersectionsToRevisit;
    }

    public void setDeadEndRevisitChance(double probability) {
        if (probability < 0.0) probability = 0.0;
        if (probability > 1.0) probability = 1.0;
        this.deadEndRevisitChance = probability;
    }

    public void start(boolean resetGame) {
        stop();
        if (resetGame) {
            game.resetGame();
        }
        intersections.clear();
        trail.clear();
        intersectionHistory.clear();
        trail.add(new Point(game.getPlayerPosition()));
        active = true;
        backtracking = false;
        thinking = false;
        resumeAtMs = 0;
        nextMoveAtMs = System.currentTimeMillis();
        deadEndCount = 0;
        forcedBacktrackIntersections = 0;
        timer.start();
    }

    public void stop() {
        active = false;
        timer.stop();
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        if (!active) return;
        if (game.hasWon()) {
            stop();
            return;
        }
        step();
    }

    private void step() {
        long now = System.currentTimeMillis();

        Point current = game.getPlayerPosition();
        if (game.isAtExit(current)) {
            stop();
            return;
        }

        // Pause/réflexion gérée avant de planifier un mouvement, mais uniquement quand on est prêt à bouger
        List<Direction> optionsForPause = game.getAvailableDirections(current);
        boolean atIntersectionForPause = game.isIntersection(current);
        boolean atDeadEndForPause = optionsForPause.size() == 1;

        if (thinking) {
            if (now >= resumeAtMs) {
                thinking = false;
            } else {
                return;
            }
        }

        // Tant que le pacing mouvement n'est pas atteint, on ne re-déclenche pas une nouvelle pause
        if (now < nextMoveAtMs) {
            return; // pas encore temps de bouger
        }

        if (!thinking && (atIntersectionForPause || atDeadEndForPause) && rng.nextDouble() < thinkChance) {
            int thinkRange = Math.max(0, thinkMaxMs - thinkMinMs);
            int thinkDelay = thinkMinMs + (thinkRange == 0 ? 0 : rng.nextInt(thinkRange + 1));
            resumeAtMs = now + thinkDelay;
            thinking = true;
            return;
        }

        // Planifier le prochain mouvement après éventuelle pause
        int range = Math.max(0, moveMaxMs - moveMinMs);
        int delay = moveMinMs + (range == 0 ? 0 : rng.nextInt(range + 1));
        nextMoveAtMs = now + delay;

        Direction nextDirection = chooseDirection(current);
        if (nextDirection == null) {
            stop();
            return;
        }

        boolean moved = game.move(nextDirection);
        if (!moved) {
            intersections.remove(current);
            return;
        }

        Point afterMove = game.getPlayerPosition();
        updateTrail(afterMove);
    }

    private Direction chooseDirection(Point current) {
        List<Direction> options = game.getAvailableDirections(current);
        if (options.isEmpty()) return null;

        Point previous = getPreviousPosition();
        Direction backDir = null;
        if (previous != null) {
            backDir = Direction.fromDelta(previous.x - current.x, previous.y - current.y);
        }

        boolean atIntersection = game.isIntersection(current);
        if (atIntersection) {
            // Mettre à jour l'historique des intersections pour la pondération d'oubli
            Point lastInter = intersectionHistory.peekLast();
            if (lastInter == null || !lastInter.equals(current)) {
                intersectionHistory.addLast(new Point(current));
            }
            IntersectionState state = intersections.computeIfAbsent(new Point(current),
                p -> new IntersectionState(p, options, rng));
            state.refresh(options, rng);

            // Si on est en phase de backtracking forcé, continuer à remonter d'intersection en intersection
            if (forcedBacktrackIntersections > 0 && backDir != null) {
                forcedBacktrackIntersections--;
                backtracking = true;
                return backDir;
            }

            double effectiveForget = computeEffectiveForgetProbability(current);
            Direction choice = state.next(backDir, effectiveForget, rng);
            if (choice != null) {
                backtracking = false; // on tente une nouvelle branche
                return choice;
            }
            // tout essayé: enclencher le backtracking (retour vers l'intersection précédente)
            backtracking = true;
            return backDir;
        }

        // cul-de-sac: on enclenche le backtracking et on retourne en arrière
        if (options.size() == 1) {
            deadEndCount++;
            // Toutes les deadEndRevisitInterval impasses, on force à remonter ce nombre d'intersections
            if (deadEndRevisitInterval > 0 && deadEndCount % deadEndRevisitInterval == 0) {
                if (rng.nextDouble() < deadEndRevisitChance) {
                    forcedBacktrackIntersections = deadEndRevisitInterval;
                }
            }
            backtracking = true;
            return options.get(0);
        }


        if (options.size() == 2 && backDir != null) {
            // Dans un couloir, si on est en phase de backtracking, continuer à reculer
            if (backtracking) {
                return backDir;
            } else {
                for (Direction direction : options) {
                    if (direction != backDir) {
                        return direction;
                    }
                }
                return backDir;
            }
        }

        List<Direction> nonBack = new ArrayList<>();
        for (Direction dir : options) {
            if (dir != backDir) {
                nonBack.add(dir);
            }
        }
        if (!nonBack.isEmpty()) {
            return nonBack.get(rng.nextInt(nonBack.size()));
        }
        return options.get(0);
    }

    // Récence: 0 pour la dernière intersection rencontrée, 1 pour l'avant-dernière, etc.
    private int getIntersectionAge(Point intersection) {
        int idx = 0;
        Iterator<Point> it = intersectionHistory.descendingIterator();
        while (it.hasNext()) {
            Point p = it.next();
            if (p.equals(intersection)) {
                return idx;
            }
            idx++;
        }
        return 0; // première fois: considérer comme la plus récente
    }

    // Plus l'intersection est ancienne, plus la probabilité d'oubli augmente
    private double computeEffectiveForgetProbability(Point intersection) {
        int age = getIntersectionAge(intersection);
        double slope = 0.2; // +20% relatif par rang d'ancienneté
        double scaled = forgetProbability * (1.0 + age * slope);
        if (scaled < 0.0) scaled = 0.0;
        if (scaled > 1.0) scaled = 1.0;
        return scaled;
    }

    private Point getPreviousPosition() {
        if (trail.size() < 2) return null;
        Iterator<Point> iterator = trail.descendingIterator();
        iterator.next();
        return iterator.next();
    }

    private void updateTrail(Point afterMove) {
        if (trail.isEmpty()) {
            trail.add(new Point(afterMove));
            return;
        }

        Point previous = getPreviousPosition();
        if (previous != null && afterMove.equals(previous)) {
            trail.removeLast();
        } else {
            Point last = trail.peekLast();
            if (last == null || !last.equals(afterMove)) {
                trail.addLast(new Point(afterMove));
            }
        }
    }
}
