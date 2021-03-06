import java.util.HashMap;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Random;

/**
 * Alpha-Beta Pruning search with memory for Mancala
 * 
 * OOP version for passing results
 */
public class ABWMAgent implements MancalaAgent {

  static enum Ply {MAX, MIN};

  static class MoveScore {
    public int move;
    public int score;

    public MoveScore(int move, int score) {
      this.move = move;
      this.score = score;
    }
  }

  static class ChildMove {
    public int move;
    public int[] state;

    public ChildMove(int move, int[] state) {
      this.move = move;
      this.state = state;
    }
  }

  static class TransEntry {
    public int depth;
    public int upperbound;
    public int lowerbound;

    public TransEntry() {
      this.depth = 0;
      this.upperbound = Integer.MAX_VALUE;
      this.lowerbound = Integer.MIN_VALUE;
    }
  }

  private HashMap<Long, TransEntry> transTable;
  private long[][] zobristTable;
  private int nSeeds = 3 * 12;

  public ABWMAgent() {
    //init zobrist table
    Random prng = new Random();
    zobristTable = new long[14][nSeeds + 1];
    for (int i = 0; i < 14; ++i) {
      for (int j = 0; j < nSeeds + 1; ++j) {
        zobristTable[i][j] = prng.nextLong();
      }
    }

    //init transposition table
    transTable = new HashMap<Long, TransEntry>();
  }

  private long zobristHash(int[] state) {
    long key = 0;
    for (int i = 0; i < 14; ++i) {
      key ^= zobristTable[i][state[i]];
    }
    return key;
  }

  private boolean terminal(int[] state) {
    //if south empty then state is terminal
    int count = 0;
    for (int i = 0; i < 6; ++i) count += state[i];
    if (count == 0) return true;

    //if north empty then state is terminal
    count = 0;
    for (int i = 7; i < 13; ++i) count += state[i];
    if (count == 0) return true;

    //else state not terminal
    return false;
  }

  private int evaluate(int[] state) {
    int score = 0;

    //check endgame conditions
    if (terminal(state)) {
      for (int i = 0; i < 7; ++i) score += state[i];
      for (int i = 7; i < 14; ++i) score -= state[i];

      if (score > 0) {
        return 100; //victory
      } else if (score < 0) {
        return -100; //loss
      } else {
        return 0; //draw
      }
    } else

    //calculate board value
    for (int i = 0; i < 6; ++i) {
      if ((state[i] == 0) && (state[12-i] > 0)) { //empty house rule
        score += state[12-i] / 2 + 1; //potential empty house captures are worth half
      } else {
        score += state[i]; //house seeds have standard value
      }
    }
    score += 2*state[6]; //siloed seeds are double

    for (int i = 7; i < 13; ++i) {
      if ((state[i] == 0) && (state[12-i] > 0)) {
        score -= state[12-i] / 2 + 1;
      } else {
        score -= state[i];
      }
    }
    score -= 2*state[13];

    return score;
  }

  private MoveScore alphaBetaWithMemory(ChildMove move, int alpha, int beta, int depth, Ply step) {
    int value, bestMove = 0;
    MoveScore searchResult;
    TransEntry trans;
    long hash = zobristHash(move.state);
    
    //trans table lookup
    if (transTable.containsKey(hash)) {
      trans = transTable.get(hash);
      if (trans.depth >= depth) {
        if (trans.lowerbound >= beta) {
          return new MoveScore(move.move, trans.lowerbound);
        }
        if (trans.upperbound <= alpha) {
          return new MoveScore(move.move, trans.upperbound);
        }
        alpha = Math.max(alpha, trans.lowerbound);
        beta = Math.min(beta, trans.upperbound);
      }
    }
    
    //base case
    if ((depth == 0) || terminal(move.state)) {
      return new MoveScore(move.move, evaluate(move.state));
    }

    //recursive
    if (step == Ply.MAX) { //max step
      value = Integer.MIN_VALUE;
      for (ChildMove child : children(move, Ply.MAX, false)) {
        searchResult = alphaBetaWithMemory(child, alpha, beta, depth - 1, Ply.MIN);
        if (searchResult.score >= value) {
          value = searchResult.score;
          bestMove = child.move;
        }
        alpha = Math.max(alpha, value);
        if (alpha >= beta) break; //pruning
      }
    } else { //min step
      value = Integer.MAX_VALUE;
      for (ChildMove child : children(move, Ply.MIN, false)) {
        searchResult = alphaBetaWithMemory(child, alpha, beta, depth - 1, Ply.MAX);
        if (searchResult.score <= value) {
          value = searchResult.score;
          bestMove = child.move;
        }
        beta = Math.min(beta, value);
        if (alpha >= beta) break;
      }
    }

    //store trans table values
    trans = transTable.getOrDefault(hash, new TransEntry());

    if (trans.depth <= depth) {
      //fail low implies an upper bound
      if (value <= alpha) {
        trans.upperbound = value;
      }
      //fail high implies a lower bound
      else if (value >= beta) {
        trans.lowerbound = value;
      }
      //accurate minimax value
      else {
        trans.lowerbound = value;
        trans.upperbound = value;
      }
      trans.depth = depth;
      transTable.put(hash, trans);
    }

    return new MoveScore(bestMove, value);
  }

  private ArrayList<ChildMove> children(ChildMove parent, Ply step, boolean extraTurn) {
    ArrayList<ChildMove> childmoves = new ArrayList<ChildMove>();
    ChildMove child;

    if (step == Ply.MAX) { //our moves
      for (int i = 0; i < 6; ++i) {
        if (parent.state[i] > 0) {
          //move is valid
          if (extraTurn) { // if extra turn, treat as same move as parent
            child = new ChildMove(parent.move, Arrays.copyOf(parent.state, 14));
          } else {
            child = new ChildMove(i, Arrays.copyOf(parent.state, 14));
          }
          //sow seeds from i
          int j = i;
          int seeds = parent.state[i];
          child.state[i] = 0;
          while(seeds > 0) {
            ++j;
            j %= 14;
            if (j < 13) { //don't place in opponent store
              --seeds;
              child.state[j] += 1;
            }
          }
          if (j == 6) { //extra turn
            if (terminal(child.state)) { //if move ends the game it can't give an extra turn
              childmoves.add(child);
            } else { //recursively find extra move children of this state
              childmoves.addAll(children(child, step, true));
            }
          } else {
            if ((j >= 0) && (j <= 5) && (child.state[j] == 1) && (child.state[12-j] > 0)) { //empty house rule
              child.state[6] = child.state[6] + child.state[12-j] + 1;
              child.state[j] = 0;
              child.state[12-j] = 0;
            }
            childmoves.add(child);
          }
        }
      }
    } else { //enemy moves
      for (int i = 7; i < 13; ++i) {
        if (parent.state[i] > 0) {
          //move is valid
          if (extraTurn) {
            child = new ChildMove(parent.move, Arrays.copyOf(parent.state, 14));
          } else {
            child = new ChildMove(i, Arrays.copyOf(parent.state, 14));
          }
          //sow seeds from i
          int j = i;
          int seeds = parent.state[i];
          child.state[i] = 0;
          while(seeds > 0) {
            ++j;
            j %= 14;
            if (j < 6) { //don't place in our store
              --seeds;
              child.state[j] += 1;
            }
          }
          if (j == 13) { //extra turn
            if (terminal(child.state)) { //if move ends the game it can't give an extra turn
              childmoves.add(child);
            } else { //recursively find extra move children of this state
              childmoves.addAll(children(child, step, true));
            }
          } else {
            if ((j >= 7) && (j <= 12) && (child.state[j] == 1) && (child.state[12-j] > 0)) { //empty house rule
              child.state[13] = child.state[13] + child.state[12-j] + 1;
              child.state[j] = 0;
              child.state[12-j] = 0;
            }
            childmoves.add(child);
          }
        }
      }
    }
    return childmoves;
  }

  /**
   * Allows the agent to nominate the house the agent would like to move seeds from. 
   * The agent will allways have control of houses 0-5 with store at 6. 
   * Any move other than 0-5 will result in a forfeit. 
   * An move from an empty house will result in a forfeit.
   * A legal move will always be available.
   * Assume your agent has 0.5 seconds to make a move. 
   * @param board the current state of the game. 
   * The board is an int array of length 14, indicating the 12 houses and 2 stores. 
   * The agent's house are 0-5 and their store is 6. The opponents houses are 7-12 and their store is 13. Board[i] is the number of seeds in house (store) i.
   * board[(i+1}%14] is the next house (store) anticlockwise from board[i].  
   * This will be consistent between moves of a normal game so the agent can maintain a strategy space.
   * @return the house the agent would like to move the seeds from this turn.
   */
  public int move(int[] board) {
    int alpha = Integer.MIN_VALUE;
    int beta = Integer.MAX_VALUE;
    int depth = 12;
    ChildMove state = new ChildMove(-1, board);
    MoveScore best = alphaBetaWithMemory(state, alpha, beta, depth, Ply.MAX);
    return best.move;
  }

  /**
   * The agents name.
   * @return a hardcoded string, the name of the agent.
   */
  public String name() {
    return "Alpha-Beta Memory Agent";
  }

  /**
   * A method to reset the agent for a new game.
   */
  public void reset() {}
}


