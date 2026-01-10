import javax.swing.*;
import java.awt.*;
import java.awt.event.*;

public class ParticipantInterface extends JPanel implements KeyListener {
    private int[][] maze = {
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

    private int playerX = 0;
    private int playerY = 1;
    private int exitX = 20;
    private int exitY = 19;
    private int moves = 0;
    private boolean won = false;

    private long startTime;
    private long elapsedTime = 0;
    private Timer gameTimer;
    private boolean timerRunning = false;
    private JFrame parentFrame;
    private boolean isFullScreen = false;
    
    // Nouveaux compteurs
    private int backtrackCount = 0;
    private int intersectionRevisits = 0;
    private int cellRevisits = 0;  // Nouveau : compteur de cases revisitées
    private int[][] visitCount;
    private int lastX = 0;
    private int lastY = 1;

    private static final int CELL_SIZE = 20;
    private static final Color WALL_COLOR = new Color(30, 41, 59);
    private static final Color PATH_COLOR = new Color(241, 245, 249);
    private static final Color PLAYER_COLOR = new Color(59, 130, 246);
    private static final Color EXIT_COLOR = new Color(34, 197, 94);

    private JLabel movesLabel;
    private JLabel timeLabel;
    private JLabel statusLabel;
    private JLabel backtrackLabel;
    private JLabel intersectionLabel;
    private JLabel cellRevisitsLabel;

    public ParticipantInterface() {
        setPreferredSize(new Dimension(
            maze[0].length * CELL_SIZE + 20,
            maze.length * CELL_SIZE + 100
        ));
        setBackground(new Color(51, 65, 85));
        setFocusable(true);
        addKeyListener(this);
        
        // Initialiser la matrice de visites
        visitCount = new int[maze.length][maze[0].length];

        gameTimer = new Timer(100, e -> {
            if (timerRunning) {
                elapsedTime = System.currentTimeMillis() - startTime;
                updateTimeLabel();
            }
        });
    }

    public void setParentFrame(JFrame frame) {
        this.parentFrame = frame;
    }

    private void toggleFullScreen() {
        if (parentFrame == null) return;

        GraphicsDevice device = GraphicsEnvironment.getLocalGraphicsEnvironment().getDefaultScreenDevice();

        if (!isFullScreen) {
            if (device.isFullScreenSupported()) {
                parentFrame.dispose();
                parentFrame.setUndecorated(true);
                parentFrame.setVisible(true);
                device.setFullScreenWindow(parentFrame);
                isFullScreen = true;
            }
        } else {
            device.setFullScreenWindow(null);
            parentFrame.dispose();
            parentFrame.setUndecorated(false);
            parentFrame.setVisible(true);
            parentFrame.setExtendedState(JFrame.MAXIMIZED_BOTH);
            isFullScreen = false;
        }
        requestFocusInWindow();
    }

    public void setMovesLabel(JLabel label) {
        this.movesLabel = label;
        updateMovesLabel();
    }

    public void setTimeLabel(JLabel label) {
        this.timeLabel = label;
        updateTimeLabel();
    }

    public void setStatusLabel(JLabel label) {
        this.statusLabel = label;
    }
    
    public void setBacktrackLabel(JLabel label) {
        this.backtrackLabel = label;
        updateBacktrackLabel();
    }
    
    public void setIntersectionLabel(JLabel label) {
        this.intersectionLabel = label;
        updateIntersectionLabel();
    }
    
    public void setCellRevisitsLabel(JLabel label) {
        this.cellRevisitsLabel = label;
        updateCellRevisitsLabel();
    }

    private void updateMovesLabel() {
        if (movesLabel != null) {
            movesLabel.setText("Mouvements: " + moves);
        }
    }

    private void updateTimeLabel() {
        if (timeLabel != null) {
            long seconds = elapsedTime / 1000;
            long minutes = seconds / 60;
            seconds = seconds % 60;
            long millis = (elapsedTime % 1000) / 100;
            timeLabel.setText(String.format("Temps: %02d:%02d.%d", minutes, seconds, millis));
        }
    }
    
    private void updateBacktrackLabel() {
        if (backtrackLabel != null) {
            backtrackLabel.setText("Retours: " + backtrackCount);
        }
    }
    
    private void updateIntersectionLabel() {
        if (intersectionLabel != null) {
            intersectionLabel.setText("Intersections revisitées: " + intersectionRevisits);
        }
    }
    
    private void updateCellRevisitsLabel() {
        if (cellRevisitsLabel != null) {
            cellRevisitsLabel.setText("Cases revisitées: " + cellRevisits);
        }
    }

    private void startTimer() {
        if (!timerRunning) {
            startTime = System.currentTimeMillis();
            timerRunning = true;
            gameTimer.start();
        }
    }

    private void stopTimer() {
        timerRunning = false;
        gameTimer.stop();
    }

    // Test de ligne de vue (Bresenham) pour ne pas voir à travers les murs
    private boolean hasLineOfSight(int x1, int y1) {
        int x0 = playerX, y0 = playerY;
        if (x0 == x1 && y0 == y1) return true;

        int dx = Math.abs(x1 - x0);
        int dy = Math.abs(y1 - y0);
        int sx = x0 < x1 ? 1 : -1;
        int sy = y0 < y1 ? 1 : -1;
        int err = dx - dy;

        int cx = x0, cy = y0;
        while (!(cx == x1 && cy == y1)) {
            int e2 = err << 1;
            if (e2 > -dy) { err -= dy; cx += sx; }
            if (e2 <  dx) { err += dx; cy += sy; }

            // si on n'est pas encore sur la case cible, un mur coupe la vue ?
            if (!(cx == x1 && cy == y1)) {
                if (maze[cy][cx] == 1) return false;
            }
        }
        return true;
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2d = (Graphics2D) g;
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        int offsetX = 10;
        int offsetY = 40;

        // Dessiner le labyrinthe avec brouillard + ligne de vue
        for (int y = 0; y < maze.length; y++) {
            for (int x = 0; x < maze[y].length; x++) {
                int dx = Math.abs(x - playerX);
                int dy = Math.abs(y - playerY);
                boolean isDiagonal = (dx == 1 && dy == 1);

                // rayon + ligne de vue
                boolean inRadius = (dx <= 2 && dy <= 2 && !isDiagonal) || (dx <= 1 && dy <= 1);
                boolean isVisible = inRadius && hasLineOfSight(x, y);

                if (isVisible) {
                    if (maze[y][x] == 1) {
                        g2d.setColor(WALL_COLOR);
                    } else {
                        g2d.setColor(PATH_COLOR);
                    }
                    g2d.fillRect(offsetX + x * CELL_SIZE, offsetY + y * CELL_SIZE,
                                 CELL_SIZE, CELL_SIZE);

                    g2d.setColor(new Color(203, 213, 225));
                    g2d.drawRect(offsetX + x * CELL_SIZE, offsetY + y * CELL_SIZE,
                                 CELL_SIZE, CELL_SIZE);
                } else {
                    // Zone cachée (brouillard)
                    g2d.setColor(new Color(30, 30, 40));
                    g2d.fillRect(offsetX + x * CELL_SIZE, offsetY + y * CELL_SIZE,
                                 CELL_SIZE, CELL_SIZE);
                }
            }
        }

        // La sortie est toujours visible (dessinée au-dessus du brouillard)
        g2d.setColor(EXIT_COLOR);
        g2d.fillOval(offsetX + exitX * CELL_SIZE + 5, offsetY + exitY * CELL_SIZE + 5,
                     CELL_SIZE - 10, CELL_SIZE - 10);
        g2d.setColor(Color.WHITE);
        g2d.setFont(new Font("Arial", Font.BOLD, 12));
        g2d.drawString("⚑", offsetX + exitX * CELL_SIZE + 5, offsetY + exitY * CELL_SIZE + 15);

        // Joueur (toujours visible)
        g2d.setColor(PLAYER_COLOR);
        g2d.fillOval(offsetX + playerX * CELL_SIZE + 2, offsetY + playerY * CELL_SIZE + 2,
                     CELL_SIZE - 4, CELL_SIZE - 4);
        g2d.setColor(Color.WHITE);
        g2d.setFont(new Font("Arial", Font.BOLD, 10));
        g2d.drawString("☺", offsetX + playerX * CELL_SIZE + 5, offsetY + playerY * CELL_SIZE + 14);
    }
    
    // Vérifie si une position est une intersection (3 ou 4 chemins possibles)
    private boolean isIntersection(int x, int y) {
        if (maze[y][x] == 1) return false;
        
        int pathCount = 0;
        if (y > 0 && maze[y-1][x] == 0) pathCount++;
        if (y < maze.length-1 && maze[y+1][x] == 0) pathCount++;
        if (x > 0 && maze[y][x-1] == 0) pathCount++;
        if (x < maze[0].length-1 && maze[y][x+1] == 0) pathCount++;
        
        return pathCount >= 3;
    }

    private void movePlayer(int dx, int dy) {
        if (won) return;

        startTimer();

        int newX = playerX + dx;
        int newY = playerY + dy;

        if (newY >= 0 && newY < maze.length &&
            newX >= 0 && newX < maze[0].length &&
            maze[newY][newX] == 0) {

            // Détecter le retour en arrière
            if (newX == lastX && newY == lastY) {
                backtrackCount++;
                updateBacktrackLabel();
            }
            
            // Sauvegarder l'ancienne position
            lastX = playerX;
            lastY = playerY;
            
            // Déplacer le joueur
            playerX = newX;
            playerY = newY;
            moves++;
            updateMovesLabel();
            
            // Vérifier si la case a déjà été visitée AVANT d'incrémenter
            if (visitCount[newY][newX] > 0) {
                cellRevisits++;
                updateCellRevisitsLabel();
            }
            
            // Incrémenter le compteur de visite
            visitCount[newY][newX]++;
            
            // Vérifier si c'est une intersection revisitée
            if (isIntersection(newX, newY) && visitCount[newY][newX] > 1) {
                intersectionRevisits++;
                updateIntersectionLabel();
            }

            if (playerX == exitX && playerY == exitY) {
                won = true;
                stopTimer();

                long seconds = elapsedTime / 1000;
                long minutes = seconds / 60;
                seconds = seconds % 60;

                if (statusLabel != null) {
                    statusLabel.setText(String.format("🎉 Félicitations ! Terminé en %d mouvements et %02d:%02d !",
                        moves, minutes, seconds));
                    statusLabel.setForeground(new Color(22, 163, 74));
                }
                JOptionPane.showMessageDialog(this,
                    String.format("Félicitations !\n\nMouvements: %d\nTemps: %02d:%02d\nRetours en arrière: %d\nCases revisitées: %d\nIntersections revisitées: %d",
                        moves, minutes, seconds, backtrackCount, cellRevisits, intersectionRevisits),
                    "Victoire !", JOptionPane.INFORMATION_MESSAGE);
            }

            repaint();
        }
    }

    public void resetGame() {
        playerX = 0;
        playerY = 1;
        lastX = 0;
        lastY = 1;
        moves = 0;
        won = false;
        elapsedTime = 0;
        timerRunning = false;
        backtrackCount = 0;
        intersectionRevisits = 0;
        cellRevisits = 0;
        
        // Réinitialiser la matrice de visites
        for (int i = 0; i < visitCount.length; i++) {
            for (int j = 0; j < visitCount[i].length; j++) {
                visitCount[i][j] = 0;
            }
        }
        
        stopTimer();
        updateMovesLabel();
        updateTimeLabel();
        updateBacktrackLabel();
        updateIntersectionLabel();
        updateCellRevisitsLabel();
        if (statusLabel != null) {
            statusLabel.setText("Utilisez les flèches du clavier pour vous déplacer");
            statusLabel.setForeground(Color.BLACK);
        }
        repaint();
        requestFocusInWindow();
    }

    @Override
    public void keyPressed(KeyEvent e) {
        switch (e.getKeyCode()) {
            case KeyEvent.VK_UP:
                movePlayer(0, -1);
                break;
            case KeyEvent.VK_DOWN:
                movePlayer(0, 1);
                break;
            case KeyEvent.VK_LEFT:
                movePlayer(-1, 0);
                break;
            case KeyEvent.VK_RIGHT:
                movePlayer(1, 0);
                break;
            case KeyEvent.VK_F11:
                toggleFullScreen();
                break;
            case KeyEvent.VK_ESCAPE:
                if (isFullScreen) {
                    toggleFullScreen();
                }
                break;
        }
    }

    @Override
    public void keyTyped(KeyEvent e) {}

    @Override
    public void keyReleased(KeyEvent e) {}

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            JFrame frame = new JFrame("🎮 Jeu de Labyrinthe");
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            frame.setLayout(new BorderLayout());
            frame.setExtendedState(JFrame.MAXIMIZED_BOTH);

            ParticipantInterface game = new ParticipantInterface();

            JPanel topPanel = new JPanel(new BorderLayout());
            topPanel.setBackground(Color.WHITE);
            topPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

            JLabel titleLabel = new JLabel("🎮 Jeu de Labyrinthe", SwingConstants.CENTER);
            titleLabel.setFont(new Font("Arial", Font.BOLD, 24));
            topPanel.add(titleLabel, BorderLayout.NORTH);

            JPanel infoPanel = new JPanel(new GridLayout(2, 3, 10, 5));
            infoPanel.setBackground(Color.WHITE);
            infoPanel.setBorder(BorderFactory.createEmptyBorder(5, 0, 5, 0));

            JLabel movesLabel = new JLabel("Mouvements: 0");
            movesLabel.setFont(new Font("Arial", Font.BOLD, 16));
            game.setMovesLabel(movesLabel);

            JLabel timeLabel = new JLabel("Temps: 00:00.0");
            timeLabel.setFont(new Font("Arial", Font.BOLD, 16));
            timeLabel.setHorizontalAlignment(SwingConstants.CENTER);
            game.setTimeLabel(timeLabel);

            JButton resetButton = new JButton("Recommencer");
            resetButton.setFont(new Font("Arial", Font.BOLD, 14));
            resetButton.setBackground(new Color(59, 130, 246));
            resetButton.setForeground(Color.WHITE);
            resetButton.setFocusPainted(false);
            resetButton.addActionListener(e -> game.resetGame());
            
            JLabel backtrackLabel = new JLabel("Retours: 0");
            backtrackLabel.setFont(new Font("Arial", Font.BOLD, 14));
            game.setBacktrackLabel(backtrackLabel);
            
            JLabel cellRevisitsLabel = new JLabel("Cases revisitées: 0");
            cellRevisitsLabel.setFont(new Font("Arial", Font.BOLD, 14));
            cellRevisitsLabel.setHorizontalAlignment(SwingConstants.CENTER);
            game.setCellRevisitsLabel(cellRevisitsLabel);
            
            JLabel intersectionLabel = new JLabel("Intersections revisitées: 0");
            intersectionLabel.setFont(new Font("Arial", Font.BOLD, 14));
            intersectionLabel.setHorizontalAlignment(SwingConstants.CENTER);
            game.setIntersectionLabel(intersectionLabel);

            infoPanel.add(movesLabel);
            infoPanel.add(timeLabel);
            infoPanel.add(resetButton);
            infoPanel.add(backtrackLabel);
            infoPanel.add(cellRevisitsLabel);
            infoPanel.add(intersectionLabel);

            topPanel.add(infoPanel, BorderLayout.SOUTH);

            JPanel bottomPanel = new JPanel();
            bottomPanel.setBackground(Color.WHITE);
            bottomPanel.setBorder(BorderFactory.createEmptyBorder(5, 10, 10, 10));
            JLabel statusLabel = new JLabel("Utilisez les flèches du clavier pour vous déplacer • F11: Plein écran");
            statusLabel.setFont(new Font("Arial", Font.PLAIN, 14));
            game.setStatusLabel(statusLabel);
            bottomPanel.add(statusLabel);

            frame.add(topPanel, BorderLayout.NORTH);
            frame.add(game, BorderLayout.CENTER);
            frame.add(bottomPanel, BorderLayout.SOUTH);

            game.setParentFrame(frame);

            frame.pack();
            frame.setLocationRelativeTo(null);
            frame.setResizable(true);
            frame.setVisible(true);

            game.requestFocusInWindow();
        });
    }
}
