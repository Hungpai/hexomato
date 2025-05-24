package de.siramac.hexomato.agent.mcts.node;

import de.siramac.hexomato.agent.mcts.SelectionResult;
import de.siramac.hexomato.domain.Game;
import de.siramac.hexomato.domain.Node;
import de.siramac.hexomato.domain.Player;
import lombok.extern.slf4j.Slf4j;

import java.util.*;

import static de.siramac.hexomato.agent.mcts.Util.getArgMax;
import static de.siramac.hexomato.domain.Player.PLAYER_1;
import static de.siramac.hexomato.domain.Player.PLAYER_2;

@Slf4j
public class AmafNode extends UctNode {

    private final Map<Player, List<Node>> simulationMoves;

    public AmafNode(
            Node[][] state,
            Player activePlayer,
            Node[] validActions,
            Player winner,
            Integer action,
            AmafNode parent) {
        super(state, activePlayer, validActions, winner, action, parent);
        this.simulationMoves = new HashMap<>();
        this.simulationMoves.put(PLAYER_1, new ArrayList<>());
        this.simulationMoves.put(PLAYER_2, new ArrayList<>());
    }

    @Override
    public SelectionResult select() {
        AmafNode currentNode = this;
        Integer bestAction;

        while (true) {
            if (currentNode.validActions.length == 0  || currentNode.getWinner() != null) {
                bestAction = null;
                break;
            }

            double[] uctValues = currentNode.calculateUpperConfidenceBoundForTrees(0.0);
            bestAction = getArgMax(uctValues);

            if (currentNode.children.containsKey(bestAction)) {
                currentNode = (AmafNode) currentNode.children.get(bestAction);
            } else {
                break;
            }
        }
        return new SelectionResult(currentNode, bestAction);
    }

    @Override
    public AmafNode expand(Game simulationEnv, Integer nextAction) {
        simulationEnv.reset(state, activePlayer, winner);
        AmafNode child = this;

        if (winner == null) {
            Node validAction = validActions[nextAction];
            simulationEnv.makeMoveOnBoard(validAction.getRow(), validAction.getCol(), activePlayer);
            child = new AmafNode(
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

    @Override
    public Player simulate(Game simulationEnv) {
        if (winner != null) return winner;
        simulationEnv.reset(state, activePlayer, null);
        simulationMoves.get(PLAYER_1).clear();
        simulationMoves.get(PLAYER_2).clear();

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
            simulationMoves.get(player).add(node);

            // check if current node is part of opponent bridge, then play the counter move
            Player opponent = player == PLAYER_1 ? PLAYER_2 : PLAYER_1;
            List<Node> possibleOpponentNodes = bridgePattern.getPossibleOpponentBridgeNodes(
                    opponent, simulationEnv.getBoard(), node);
            if (!possibleOpponentNodes.isEmpty()) {
                Node oppenentCounterNode = possibleOpponentNodes.get(random.nextInt(possibleOpponentNodes.size()));
                oppenentCounterNode.setPlayer(opponent);
                alreadyPlayedActions.add(oppenentCounterNode);
                simulationMoves.get(opponent).add(oppenentCounterNode);
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
        AmafNode currentNode = this;

        while (true) {
            Integer currentAction = currentNode.action;
            currentNode = (AmafNode) currentNode.parent;

            // root node
            if (currentNode == null) {
                break;
            }

            // mcts update: only update moves from the leave to the root
            currentNode.childVisits[currentAction]++;
            if (currentNode.activePlayer == winner) {
                currentNode.childValues[currentAction]++;
            }

            // amaf update: update all moves that were played during simulation
            List<Node> moves = currentNode.activePlayer == PLAYER_1
                    ? this.simulationMoves.get(PLAYER_1)
                    : this.simulationMoves.get(PLAYER_2);

            for (int i = 0; i < currentNode.validActions.length; i++) {
                if (!moves.contains(currentNode.validActions[i])) continue;

                currentNode.childVisits[i]++;
                if (currentNode.activePlayer == winner) {
                    currentNode.childValues[i]++;
                }
            }
        }
    }
}
