package de.siramac.hexomato.agent.mcts;

import de.siramac.hexomato.agent.mcts.node.AmafNode;
import de.siramac.hexomato.agent.mcts.node.NodeType;
import de.siramac.hexomato.agent.mcts.node.RaveNode;
import de.siramac.hexomato.agent.mcts.node.UctNode;
import de.siramac.hexomato.domain.Node;
import de.siramac.hexomato.domain.Player;

public class MctsNodeFactory {
    public static MctsNode createMctsNode(
            NodeType type,
            Node[][] state,
            Player activePlayer,
            Node[] validActions,
            Player winner,
            Integer action,
            MctsNode parent) {
        return switch (type) {
            case UCT -> new UctNode(state, activePlayer, validActions, winner, action, (UctNode) parent);
            case AMAF -> new AmafNode(state, activePlayer, validActions, winner, action, (AmafNode) parent);
            case RAVE -> new RaveNode(state, activePlayer, validActions, winner, action, (RaveNode) parent);
        };
    }
}
