package Model;

import java.util.*;

/**
 * AI dùng thuật toán Minimax + Alpha-Beta Pruning.
 * EASY: chọn ngẫu nhiên | MEDIUM: độ sâu 2 | HARD: độ sâu 6
 */
public class AIPlayer {
    private final GameModel.Diff diff;
    private final Random rng = new Random();

    public AIPlayer(GameModel.Diff diff) { this.diff = diff; }

    public Move best(Board board, boolean isRed) {
        List<Move> moves = board.validMoves(isRed);
        if (moves.isEmpty()) return null;
        if (diff == GameModel.Diff.EASY) return moves.get(rng.nextInt(moves.size()));

        int depth = (diff == GameModel.Diff.MEDIUM) ? 2 : 6;
        Move bestMove = null;
        int bestScore = Integer.MIN_VALUE;
        for (Move m : moves) {
            Board next = board.copy();
            next.applyMove(m);
            int score = minimax(next, !isRed, depth-1, Integer.MIN_VALUE, Integer.MAX_VALUE, false, isRed);
            if (score > bestScore) { bestScore = score; bestMove = m; }
        }
        return bestMove;
    }

    private int minimax(Board board, boolean cur, int depth, int a, int b, boolean max, boolean aiRed) {
        List<Move> moves = board.validMoves(cur);
        if (depth==0 || moves.isEmpty()) return board.evaluate(aiRed);
        if (cur == aiRed) { // maximizing
            int v = Integer.MIN_VALUE;
            for (Move m : moves) {
                Board next = board.copy(); next.applyMove(m);
                v = Math.max(v, minimax(next, !cur, depth-1, a, b, false, aiRed));
                a = Math.max(a, v); if (b<=a) break;
            }
            return v;
        } else {            // minimizing
            int v = Integer.MAX_VALUE;
            for (Move m : moves) {
                Board next = board.copy(); next.applyMove(m);
                v = Math.min(v, minimax(next, !cur, depth-1, a, b, true, aiRed));
                b = Math.min(b, v); if (b<=a) break;
            }
            return v;
        }
    }
}