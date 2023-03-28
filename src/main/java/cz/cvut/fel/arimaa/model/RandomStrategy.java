package cz.cvut.fel.arimaa.model;

import cz.cvut.fel.arimaa.types.Color;
import cz.cvut.fel.arimaa.types.Move;
import cz.cvut.fel.arimaa.types.Step;

import java.util.*;

class RandomStrategy implements Strategy {

    private Step getRandomStep(Set<Step> steps) {
        List<Step> list = new ArrayList<>(steps);
        Collections.shuffle(list);
        return list.get(0);
    }

    @Override
    public void makeMove(Board board, Color color) {
        int numberOfSteps = new Random().nextInt(Move.MAX_LEGAL_STEPS - 1) + 1;
        for (int i = 0; i < numberOfSteps; ++i) {
            Set<Step> validSteps = board.getValidSteps(color);
            Step step = getRandomStep(validSteps);
            board.makeStep(step);
        }
    }
}
