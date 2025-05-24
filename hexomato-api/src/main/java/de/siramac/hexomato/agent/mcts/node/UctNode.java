package de.siramac.hexomato.agent.mcts.node;

import de.siramac.hexomato.agent.mcts.MctsNode;
import de.siramac.hexomato.agent.mcts.SelectionResult;
import de.siramac.hexomato.domain.Game;
import de.siramac.hexomato.domain.Node;
import de.siramac.hexomato.domain.Player;
import de.siramac.hexomato.pattern.BridgePattern;
import lombok.Getter;

import java.util.*;

import static de.siramac.hexomato.agent.mcts.Util.getArgMax;
import static de.siramac.hexomato.domain.Player.PLAYER_1;
import static de.siramac.hexomato.domain.Player.PLAYER_2;

public class UctNode implements MctsNode {
    protected final Node[][] state;
    protected final Player activePlayer;
    protected final Integer action; // index to availableActions
    protected final UctNode parent;
    protected final Map<Integer, UctNode> children;
    protected final Node[] validActions;

    protected final double[] childValues;
    protected final double[] childVisits;

    @Getter
    protected final Player winner;
    protected final BridgePattern bridgePattern;
    protected final Random random;

    private final double EXPLORATION_COEFFICIENT = Math.sqrt(2);

    public UctNode(
            Node[][] state,
            Player activePlayer,
            Node[] validActions,
            Player winner,
            Integer action,
            UctNode parent) {
        this.state = state;
        this.activePlayer = activePlayer;
        this.validActions = validActions;
        this.winner = winner;
        this.action = action;
        this.parent = parent;
        this.children = new HashMap<>();
        this.childValues = new double[validActions.length];
        this.childVisits = new double[validActions.length];

        this.random = new Random();
        this.bridgePattern = new BridgePattern();
    }

    public double[] calculatePolicy() {
        double sum = Arrays.stream(childVisits).sum();
        return Arrays.stream(childVisits)
                .map(child -> child / sum)
                .toArray();
    }

    /**
     * UCB_i = exploitation + c * exploration
     * <p>
     * - exploitation = W_i / N_i (win rate)
     * - W_i: number of times child_i won
     * - N_i: number of times child_i visited
     * <p>
     * - exploration = c * Math.sqrt(Math.log(N)) / N_i
     * - N: number of times parent visited
     * - c: EXPLORATION_COEFFICIENT adjusts the amount of exploration
     */
    public double[] calculateUpperConfidenceBoundForTrees(double c) {
        double[] UCB = new double[validActions.length];
        double N = Arrays.stream(childVisits).sum();

        for (int i = 0; i < validActions.length; i++) {
            double W_i = childValues[i];
            double N_i = childVisits[i];

            if (N_i == 0.0) {
                UCB[i] = Double.POSITIVE_INFINITY;
                continue;
            }

            // exploitation
            double exploitation = W_i / N_i;

            // exploration
            double exploration = c * (Math.sqrt(Math.log(N)) / N_i);

            // sum
            UCB[i] = exploitation + exploration;

        }
        return UCB;
    }

    @Override
    public SelectionResult select() {
        UctNode currentNode = this;
        Integer bestAction;

        while (true) {
            if (currentNode.validActions.length == 0  || currentNode.getWinner() != null) {
                bestAction = null;
                break;
            }

            double[] uctValues = currentNode.calculateUpperConfidenceBoundForTrees(EXPLORATION_COEFFICIENT);
            bestAction = getArgMax(uctValues);

            if (currentNode.children.containsKey(bestAction)) {
                currentNode = currentNode.children.get(bestAction);
            } else {
                break;
            }
        }
        return new SelectionResult(currentNode, bestAction);
    }

    @Override
    public UctNode expand(Game simulationEnv, Integer nextAction) {
        simulationEnv.reset(state, activePlayer, winner);
        UctNode child = this;

        if (winner == null) {
            // FIXME: nextAction can be null
            Node validAction = validActions[nextAction];
            simulationEnv.makeMoveOnBoard(validAction.getRow(), validAction.getCol(), activePlayer);
            child = new UctNode(
                    simulationEnv.getBoard(),
                    simulationEnv.getTurn(),
                    simulationEnv.getValidActions(),
                    simulationEnv.getWinner(),
                    nextAction,
                    this);
            children.put(nextAction, child);
        }
        return child;
    }

    /**
     * Game simulation until one player wins. To efficiently simulate the game:
     * - get all valid actions
     * - play bridge actions first
     * - play an available action if all bridge actions are exhausted
     * - alternate between players until all cells are occupied
     * After that, find a winning path from the first row of the board for PLAYER_1.
     * If a path was found, the winner is PLAYER_1, else PLAYER_2.
     */
    @Override
    public Player simulate(Game simulationEnv) {
        if (winner != null) return winner;
        simulationEnv.reset(state, activePlayer, null);

        List<Node> availableActions = new ArrayList<>(Arrays.asList(simulationEnv.getValidActions()));
        Collections.shuffle(availableActions);

        Set<Node> alreadyPlayedActions = new HashSet<>();
        Player player = activePlayer;
        for (Node availableAction : availableActions) {

            if (alreadyPlayedActions.contains(availableAction)) continue;

            int row = availableAction.getRow();
            int col = availableAction.getCol();
            Node node = simulationEnv.getBoard()[row][col];
            node.setPlayer(player);

            // check if current node is part of opponent bridge, then play the counter move
            Player opponent = player == PLAYER_1 ? PLAYER_2 : PLAYER_1;
            List<Node> possibleOpponentNodes = bridgePattern.getPossibleOpponentBridgeNodes(
                    opponent,
                    simulationEnv.getBoard(),
                    node);
            if (!possibleOpponentNodes.isEmpty()) {
                Node oppenentCounterNode = possibleOpponentNodes.get(random.nextInt(possibleOpponentNodes.size()));
                oppenentCounterNode.setPlayer(opponent);
                alreadyPlayedActions.add(oppenentCounterNode);
            } else {
                player = opponent;
            }
        }

        boolean isPlayer1Winner = Arrays.stream(simulationEnv.getBoard()[0])
                .filter(node -> node.getPlayer() == PLAYER_1)
                .map(node -> simulationEnv.findWinnerPath(simulationEnv.getBoard(), node, PLAYER_1))
                .anyMatch(winnerPath -> !winnerPath.isEmpty());

        return isPlayer1Winner ? PLAYER_1 : PLAYER_2;
    }

    @Override
    public void backup(Player winner) {
        UctNode currentNode = this;

        while (true) {
            Integer currentAction = currentNode.action;
            currentNode = currentNode.parent;

            if (currentNode == null) {
                break;
            }

            if (currentNode.activePlayer == winner) {
                currentNode.childValues[currentAction]++;
            }

            currentNode.childVisits[currentAction]++;
        }
    }

}
