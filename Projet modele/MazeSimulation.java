import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class MazeSimulation {

    // =================================================================
    // PARAMÈTRE D'AJUSTEMENT (FITTING)
    // 0.0 = Aléatoire complet (~690 mouv)
    // 1.0 = Mémoire Parfaite (~200 mouv)
    // Essayez de changer ceci pour trouver 429 (ex: 0.3, 0.4, 0.5...)
    private static final double MEMORY_RATE = 0.42; 
    // =================================================================

    private static int[][] maze = {
        {1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1},
        {0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,1,0,0,0,1},
        {1,0,1,1,1,1,1,1,1,1,1,1,1,1,1,0,1,0,1,0,1},
        {1,0,0,0,1,0,0,0,0,0,0,0,0,0,1,0,0,0,1,0,1},
        {1,1,1,0,1,0,1,1,1,1,1,0,1,1,1,1,1,1,1,0,1},
        {1,0,1,0,1,0,0,0,1,0,1,0,0,0,0,0,0,0,0,0,1},
        {1,0,1,0,1,1,1,0,1,0,1,0,1,1,1,1,1,1,1,1,1},
        {1,0,1,0,1,0,1,0,1,0,0,0,1,0,1,0,0,0,0,0,1},
        {1,0,1,0,1,0,1,0,1,1,1,1,1,0,1,0,1,1,1,0,1},
        {1,0,0,0,1,0,1,0,0,0,1,0,0,0,0,0,0,0,1,0,1},
        {1,0,1,1,1,0,1,1,1,0,1,0,1,1,1,1,1,0,1,0,1},
        {1,0,0,0,0,0,1,0,1,0,1,0,0,0,1,0,1,0,1,0,1},
        {1,0,1,1,1,1,1,0,1,0,1,1,1,0,1,0,1,0,1,0,1},
        {1,0,0,0,0,0,1,0,0,0,1,0,0,0,1,0,0,0,1,0,1},
        {1,1,1,1,1,0,1,0,1,1,1,0,1,1,1,0,1,1,1,0,1},
        {1,0,0,0,1,0,1,0,1,0,0,0,1,0,0,0,0,0,1,0,1},
        {1,0,1,0,1,0,1,0,1,0,1,1,1,0,1,1,1,1,1,0,1},
        {1,0,1,0,1,0,1,0,0,0,1,0,1,0,0,0,0,0,1,0,1},
        {1,0,1,0,1,0,1,0,1,1,1,0,1,0,1,1,1,0,1,0,1},
        {1,0,1,0,0,0,1,0,0,0,0,0,1,0,1,0,0,0,1,0,0},
        {1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1}
    };

    private static final int START_X = 0;
    private static final int START_Y = 1;
    private static final int EXIT_X = 20;
    private static final int EXIT_Y = 19;
    private static final int NB_SIMULATIONS = 100;

    public static void main(String[] args) {
        System.out.println("--- SIMULATION : Modèle à Mémoire Imparfaite (" + (MEMORY_RATE * 100) + "%) ---");
        System.out.println("CIBLES HUMAINES -> Mouvements: 429 | Retours: 22 | Revisites: 245");
        System.out.println("---------------------------------------------------------------------");
        System.out.println("Essai;Mouvements;Retours;Cases_Revisitees");

        long totalMoves = 0;
        long totalBacktracks = 0;
        long totalRevisits = 0;

        for (int i = 1; i <= NB_SIMULATIONS; i++) {
            int[] result = runSimulation();
            System.out.println(i + ";" + result[0] + ";" + result[1] + ";" + result[2]);
            totalMoves += result[0];
            totalBacktracks += result[1];
            totalRevisits += result[2];
        }

        double avgMoves = (double) totalMoves / NB_SIMULATIONS;
        double avgBacktracks = (double) totalBacktracks / NB_SIMULATIONS;
        double avgRevisits = (double) totalRevisits / NB_SIMULATIONS;

        System.out.println("\n--- RÉSULTATS MOYENS ---");
        System.out.println("Mouvements      : " + String.format("%.2f", avgMoves));
        System.out.println("Retours Arrière : " + String.format("%.2f", avgBacktracks));
        System.out.println("Cases Revisitées: " + String.format("%.2f", avgRevisits));
        
    }

    private static int[] runSimulation() {
        int currentX = START_X;
        int currentY = START_Y;
        int prevX = currentX;
        int prevY = currentY;

        int moves = 0;
        int backtracks = 0;
        int revisits = 0;

        // Carte mémoire (Combien de fois on est passé ?)
        int[][] visitCount = new int[maze.length][maze[0].length];
        visitCount[currentY][currentX] = 1;

        Random rand = new Random();
        boolean won = false;

        while (!won && moves < 10000) {
            List<int[]> candidates = new ArrayList<>();
            int[][] directions = {{0, -1}, {0, 1}, {-1, 0}, {1, 0}};

            // 1. Lister les voisins (sans le retour immédiat)
            for (int[] dir : directions) {
                int nx = currentX + dir[0];
                int ny = currentY + dir[1];

                if (ny >= 0 && ny < maze.length && nx >= 0 && nx < maze[0].length && maze[ny][nx] == 0) {
                    if (!(nx == prevX && ny == prevY)) {
                        candidates.add(new int[]{nx, ny});
                    }
                }
            }

            int nextX, nextY;

            if (candidates.isEmpty()) {
                // Cul-de-sac
                nextX = prevX;
                nextY = prevY;
                backtracks++;
            } else {
                // STRATÉGIE MIXTE (Mémoire vs Hasard)
                
                // On cherche s'il y a des cases "neuves" (jamais visitées)
                List<int[]> freshCandidates = new ArrayList<>();
                for (int[] cand : candidates) {
                    if (visitCount[cand[1]][cand[0]] == 0) {
                        freshCandidates.add(cand);
                    }
                }

                // DÉCISION
                boolean useMemory = rand.nextDouble() < MEMORY_RATE;

                if (useMemory && !freshCandidates.isEmpty()) {
                    // MODE INTELLIGENT : On choisit une case neuve
                    int choice = rand.nextInt(freshCandidates.size());
                    nextX = freshCandidates.get(choice)[0];
                    nextY = freshCandidates.get(choice)[1];
                } else {
                    // MODE DISTRAIT (ou pas de case neuve dispo) : On choisit au hasard parmi TOUT ce qui est possible
                    int choice = rand.nextInt(candidates.size());
                    nextX = candidates.get(choice)[0];
                    nextY = candidates.get(choice)[1];
                }
            }

            moves++;
            if (visitCount[nextY][nextX] > 0) revisits++;
            visitCount[nextY][nextX]++;

            prevX = currentX;
            prevY = currentY;
            currentX = nextX;
            currentY = nextY;

            if (currentX == EXIT_X && currentY == EXIT_Y) won = true;
        }
        
        return new int[]{moves, backtracks, revisits};
    }
}