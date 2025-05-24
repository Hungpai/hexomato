package de.siramac.hexomato;

import de.siramac.hexomato.agent.Agent;
import de.siramac.hexomato.agent.mcts.MctsAgent;
import de.siramac.hexomato.agent.mcts.node.NodeType;
import de.siramac.hexomato.domain.Game;
import de.siramac.hexomato.domain.Node;
import de.siramac.hexomato.domain.Player;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.ArrayList;
import java.util.List;

import static de.siramac.hexomato.domain.Player.PLAYER_1;
import static de.siramac.hexomato.domain.Player.PLAYER_2;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
public class MctsTest {

    @Test
    void mctsTest() {
        Game game = new Game(Player.PLAYER_1, false, "TestMcts");
        Agent agent = new MctsAgent(
            NodeType.UCT,
                Player.PLAYER_2,
                new Game(Player.PLAYER_2, false, "TestMcts"));
        int row = 4;
        int col = 4;
        Node node = game.getBoard()[row][col];
        game.makeMoveOnBoard(node.getRow(), node.getCol(), PLAYER_1);

        Node aiMove = agent.getMove(game);
        assertThat(aiMove).isNotNull();
        assertThat(aiMove.getClass()).isEqualTo(Node.class);
    }

    /**
     * Make two AIs play against each other :D
     */
    @Test
    void mctsExperiment() {
        Game game = new Game(PLAYER_1, false, "");

        Agent agent1 = new MctsAgent(
                NodeType.RAVE,
                PLAYER_1,
                new Game(PLAYER_1, false, "AI I"), 2000);
        Agent agent2 = new MctsAgent(
                NodeType.UCT,
                PLAYER_2,
                new Game(PLAYER_2, false, "AI II"), 2000);

        Game simulationGame = new Game(PLAYER_1, false, "");
        List<Player> stats = new ArrayList<>();
        int numberOfGames = 5;
        for (int i = 0; i < numberOfGames; i++) {
            Player winner = simulateGame(simulationGame, agent1, agent2);
            stats.add(winner);
            System.out.println("The winner of Game " + (i + 1) + " is: " + winner);
            simulationGame.reset(game.getBoard(), game.getTurn(), game.getWinner());
        }
        double player1Winrate = (double) stats.stream().filter(p -> p == PLAYER_1).count() / numberOfGames;
        double player2Winrate = (double) stats.stream().filter(p -> p == PLAYER_2).count() / numberOfGames;
        System.out.println("Player 1 winrate: " + player1Winrate);
        System.out.println("Player 2 winrate: " + player2Winrate);
    }

    Player simulateGame(Game simulationGame, Agent player1, Agent player2) {
        Node node;
        Player player;
        while (simulationGame.getWinner() == null) {
            if (simulationGame.getTurn() == PLAYER_1) {
                node = player1.getMove(simulationGame);
                player = PLAYER_1;
            } else {
                node = player2.getMove(simulationGame);
                player = PLAYER_2;
            }
            simulationGame.makeMoveOnBoard(node.getRow(), node.getCol(), player);
        }
        return simulationGame.getWinner();
    }

    @Test
    void expand_nextActionNullPointerException() {
        // ARRANGE: Create a game state with already 110 played moves
        Game game = new Game(PLAYER_1, false, "");
        Node[][] board = game.getBoard();
        int totalMoves = 110;
        int k = 0;
        for (Node[] nodes : board) {
            for (Node node : nodes) {
                if (k % 2 == 0) {
                    node.setPlayer(PLAYER_1);
                } else {
                    node.setPlayer(PLAYER_2);
                }
                k++;

                if (k == totalMoves) break;
            }
            if (k == totalMoves) break;
        }

        // ACT & ASSERT
        Agent agent1 = new MctsAgent(NodeType.UCT, PLAYER_1, new Game(PLAYER_1, false, "AI I"));
        assertDoesNotThrow(() -> agent1.getMove(game));
    }

}
