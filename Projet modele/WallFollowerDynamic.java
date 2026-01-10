import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class WallFollowerDynamic {
    
    // Le labyrinthe 21x21
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

    private static final int EXIT_X = 20;
    private static final int EXIT_Y = 19;
    
    // DIRECTIONS: 0=Haut, 1=Droite, 2=Bas, 3=Gauche
    private static int[] dx = {0, 1, 0, -1};
    private static int[] dy = {-1, 0, 1, 0};
    
    private static final int NB_SIMULATIONS = 100;

    public static void main(String[] args) {
        System.out.println("--- Simulation Suiveur de Mur à Fatigue Variable ---");
        System.out.println("ID;Mouvements;Retours;Cases_Revisitees");
        
        long totalMoves = 0;
        long totalBacktracks = 0;
        long totalRevisits = 0; // Nouveau compteur global

        for (int i = 1; i <= NB_SIMULATIONS; i++) {
            int[] res = runSimulation(i);
            totalMoves += res[0];
            totalBacktracks += res[1];
            totalRevisits += res[2];
        }
        
        System.out.println("\n--- MOYENNES GLOBALES ---");
        System.out.println("Mouvements Moyens      : " + (totalMoves / NB_SIMULATIONS));
        System.out.println("Retours Arrière Moyens : " + (totalBacktracks / NB_SIMULATIONS));
        System.out.println("Cases Revisitées Moy.  : " + (totalRevisits / NB_SIMULATIONS));
    }

    private static int[] runSimulation(int id) {
        int x = 0;
        int y = 1;
        int currentDir = 1; // Commence en regardant à Droite
        int moves = 0;
        int backtracks = 0;
        int revisits = 0; // Compteur local
        
        // Carte pour suivre les visites
        int[][] visitCount = new int[maze.length][maze[0].length];
        visitCount[y][x] = 1; // Marquer le départ
        
        int lastX = x, lastY = y;
        Random rand = new Random();

        while ((x != EXIT_X || y != EXIT_Y) && moves < 5000) { 
            
            // --- LOGIQUE DYNAMIQUE DE L'ERREUR ---
            double currentErrorRate;
            if (moves < 50) {
                currentErrorRate = 0.0;      // Concentration parfaite
            } else if (moves <= 200) {
                currentErrorRate = 0.01;     // Fatigue légère
            } else {
                currentErrorRate = 0.03;     // Confusion / Panique
            }
            // -------------------------------------

            int nextDir = -1;
            
            // DÉCISION
            if (rand.nextDouble() < currentErrorRate) {
                // Erreur : Mouvement aléatoire
                List<Integer> validDirs = new ArrayList<>();
                for(int d=0; d<4; d++) {
                    if(isValid(x + dx[d], y + dy[d])) validDirs.add(d);
                }
                if(!validDirs.isEmpty()) {
                    nextDir = validDirs.get(rand.nextInt(validDirs.size()));
                }
            } else {
                // Stratégie Main Droite
                int[] checkOrder = {
                    (currentDir + 1) % 4, // Droite
                    currentDir,           // Devant
                    (currentDir + 3) % 4, // Gauche
                    (currentDir + 2) % 4  // Derrière
                };
                
                for (int dir : checkOrder) {
                    if (isValid(x + dx[dir], y + dy[dir])) {
                        nextDir = dir;
                        break;
                    }
                }
            }

            if (nextDir != -1) {
                int newX = x + dx[nextDir];
                int newY = y + dy[nextDir];
                
                // Détection de retour en arrière
                if (newX == lastX && newY == lastY) {
                    backtracks++;
                }
                
                // Déplacement
                lastX = x;
                lastY = y;
                x = newX;
                y = newY;
                currentDir = nextDir;
                moves++;
                
                // --- COMPTAGE DES REVISITES ---
                if (visitCount[y][x] > 0) {
                    revisits++;
                }
                visitCount[y][x]++;
                // -----------------------------
                
            } else {
                break; 
            }
        }
        
        System.out.println(id + ";" + moves + ";" + backtracks + ";" + revisits);
        return new int[]{moves, backtracks, revisits};
    }
    
    private static boolean isValid(int x, int y) {
        return x >= 0 && x < maze[0].length && y >= 0 && y < maze.length && maze[y][x] == 0;
    }
}