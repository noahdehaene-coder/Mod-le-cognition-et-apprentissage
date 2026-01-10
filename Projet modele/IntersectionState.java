import java.awt.Point;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.Random;

public class IntersectionState {
    private final Point position;
    private final List<Direction> choices = new ArrayList<>();
    private final EnumSet<Direction> tried = EnumSet.noneOf(Direction.class);

    public IntersectionState(Point position, List<Direction> available, Random rng) {
        this.position = new Point(position);
        refresh(available, rng);
    }

    public void refresh(List<Direction> available, Random rng) {
        choices.clear();
        choices.addAll(available);
        Collections.shuffle(choices, rng);
        tried.retainAll(choices);
    }

    public Direction next(Direction backDirection, double forgetProbability, Random rng) {
        if (rng.nextDouble() < forgetProbability) {
            tried.clear();
            Collections.shuffle(choices, rng);
        }

        for (Direction direction : choices) {
            if (direction == backDirection && choices.size() > 1 && tried.size() < choices.size()) {
                continue;
            }
            if (!tried.contains(direction)) {
                tried.add(direction);
                return direction;
            }
        }
        return null;
    }

    public Point getPosition() {
        return new Point(position);
    }
}
