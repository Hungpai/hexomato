package de.siramac.hexomato.agent.mcts;

import de.siramac.hexomato.agent.Agent;
import de.siramac.hexomato.agent.mcts.node.NodeType;
import de.siramac.hexomato.domain.Game;
import de.siramac.hexomato.domain.Node;
import de.siramac.hexomato.domain.Player;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import static de.siramac.hexomato.agent.mcts.Util.getArgMax;

@Slf4j
@AllArgsConstructor
public class MctsAgent implements Agent {

    private NodeType nodeType;
    private Player player;
    private Game simulationEnv;
    private long simulationTime; // in milliseconds

    public MctsAgent(NodeType nodeType, Player player, Game simulationEnv) {
        // call AllArgsConstructor
        this(nodeType, player, simulationEnv, 2000);
    }

    @Override
    public Node getMove(Game game) {
        simulationEnv.reset(game.getBoard(), player, game.getWinner()); // update simulation environment to current env
        MctsNode rootNode = monteCarloTreeSearch();
        double[] policy = rootNode.calculatePolicy();
        int argmax = getArgMax(policy);
        return game.getValidActions()[argmax];
    }

    private MctsNode monteCarloTreeSearch() {
        MctsNode rootNode = MctsNodeFactory.createMctsNode(
            nodeType,
            simulationEnv.getBoard(),
            simulationEnv.getTurn(),
            simulationEnv.getValidActions(),
            simulationEnv.getWinner(),
            null,
            null
        );

        int numSimulations = 0;
        long startTime = System.currentTimeMillis();
        while (System.currentTimeMillis() - startTime < simulationTime) {

            // Selection
            SelectionResult selectionResult = rootNode.select();
            MctsNode selectedNode = selectionResult.node();
            Integer nextAction = selectionResult.action();

            // Expansion: skip expansion phase if selected node
            // 1. has no valid actions (board is full)
            // 2. or selected node is an end state (player win)
            MctsNode leafNode;
            if (nextAction != null) {
                leafNode = selectedNode.expand(simulationEnv, nextAction);
            } else {
                leafNode = selectedNode;
            }

            // Simulation
            Player winner = leafNode.getWinner();
            if (winner == null) {
                winner = leafNode.simulate(simulationEnv);
            }

            // Backup
            leafNode.backup(winner);
            numSimulations++;
        }
        log.info("Number of simulations: {}", numSimulations);
        return rootNode;
    }
}
