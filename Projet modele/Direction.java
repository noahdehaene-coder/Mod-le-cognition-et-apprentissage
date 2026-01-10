public enum Direction {
    UP(0, -1),
    DOWN(0, 1),
    LEFT(-1, 0),
    RIGHT(1, 0);

    public final int dx;
    public final int dy;

    Direction(int dx, int dy) {
        this.dx = dx;
        this.dy = dy;
    }

    public Direction opposite() {
        switch (this) {
            case UP: return DOWN;
            case DOWN: return UP;
            case LEFT: return RIGHT;
            case RIGHT: return LEFT;
            default: return this;
        }
    }

    public static Direction fromDelta(int dx, int dy) {
        for (Direction dir : values()) {
            if (dir.dx == dx && dir.dy == dy) {
                return dir;
            }
        }
        return null;
    }
}
