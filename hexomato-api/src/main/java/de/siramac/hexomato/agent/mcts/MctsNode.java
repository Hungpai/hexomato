package de.siramac.hexomato.agent.mcts;

import de.siramac.hexomato.domain.Game;
import de.siramac.hexomato.domain.Player;

public interface MctsNode {
    SelectionResult select();
    MctsNode expand(Game simulationEnv, Integer nextAction);
    Player simulate(Game simulationEnv);
    void backup(Player winner);
    double[] calculatePolicy();
    Player getWinner();
}
