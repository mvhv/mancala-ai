import java.util.HashMap;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Random;

/**
 * MTD-f search implementation for Mancala
 * 
 * CITS3001 Lab6
 *
 * Jesse Wyatt (20756971)
 */
public class MTDFAgent implements MancalaAgent {

  /**
   * Inner class to hold move / score pairs
   */
  static class MoveScore {
    public int move;
    public int score;

    public MoveScore(int move, int score) {
      this.move = move;
      this.score = score;
    }
  }

  /**
   * Inner class to hold move / child-state pairs
   */
  static class ChildMove {
    public int move;
    public int[] state;

    public ChildMove(int move, int[] state) {
      this.move = move;
      this.state = state;
    }
  }

  /**
   * Inner class to hold transposition table entries
   */
  static class TransEntry {
    public int depth;
    public int upperbound;
    public int lowerbound;

    public TransEntry() {
      this.depth = 0;
      this.upperbound = 200;
      this.lowerbound = -200;
    }
  }

  private static int N_SEEDS = 3 * 12;
  private static int MAX_SEARCH_DEPTH = 100;
  private static long MAX_RUN_TIME = 100; //maximum runtime in milliseconds
  private HashMap<Long, TransEntry> transTable;
  private long[][] zobristTable;
  private long searchStartTime;

  /**
   * Constructs an instance of the AI agent for gameplay.
   * 
   * Initialises the bitstring table for Zobrist hashing
   * and a hashtable to map transpositions. 
   */
  public MTDFAgent() {
    //init zobrist table
    Random prng = new Random();
    zobristTable = new long[14][N_SEEDS + 1];
    for (int i = 0; i < 14; ++i) {
      for (int j = 0; j < N_SEEDS + 1; ++j) {
        zobristTable[i][j] = prng.nextLong();
      }
    }

    //init transposition table
    transTable = new HashMap<Long, TransEntry>();
  }

  /**
   * Checks to see if the move timer is nearly up.
   * 
   * @return true if time is up, otherwise false
   */
  private boolean timeUp() {
    return((System.currentTimeMillis() - searchStartTime) >= MAX_RUN_TIME);
  }

  /**
   * Calcuates a Zobrist hash of a game state.
   * 
   * @param state the game state
   * @return the Zobrist hash of the state
   */
  private long zobristHash(int[] state) {
    long key = 0;
    for (int i = 0; i < 14; ++i) {
      key ^= zobristTable[i][state[i]];
    }
    return key;
  }

  /**
   * Checks if a game state is a terminal state.
   * 
   * @param state the game state
   * @return true is state is terminal, false otherwise
   */
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

  /**
   * Estimates the value of a game state.
   * 
   * @param state the game state
   * @return the estimated value of the state
   */
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

  /**
   * Checks move to see if valid, and if the corresponding state
   * is contained in the transposition table.
   *
   * @param move the game move to be checked
   * @param hash the Zobrist hash of the equivalent move state
   * @return true if move valid and state contained in transposition table
   */
  private boolean validEntry(ChildMove move, long hash) {
    if (move.move < 0) {
      return false;
    } else {
      return transTable.containsKey(hash);
    }
  }

  /**
   * Performs MTD-f search by calling many zero-width alpha-beta
   * searches.
   *
   * @param state the root state to search from
   * @param guess the first best guess of minimax value
   * @param depth the maximum search depth
   * @return the move corresponding to the true minimax value
   */
  private MoveScore MTDF(ChildMove state, int guess, int depth) {
    int value, upperbound, lowerbound, beta;
    MoveScore move = new MoveScore(-1, -1); //placeholder

    value = guess;
    upperbound = Integer.MAX_VALUE;
    lowerbound = Integer.MIN_VALUE;
    while (lowerbound < upperbound) {
      beta = Math.max(value, lowerbound + 1);
      move = alphaBetaWithMemory(state, beta - 1, beta, depth, Ply.MAX);
      value = move.score;
      if (value < beta) {
        upperbound = value;
      } else {
        lowerbound = value;
      }
    }
    return move;
  }

  /**
   * Performs Minimax search using alpha-beta pruning with 
   * transposition tables.
   *
   * @param move the game state to search from
   * @param alpha the maximised lowerbound
   * @param beta the minimised upperbound
   * @param depth the maximum search depth
   * @param step the current minimax step
   * @return the move corresponding to the minimax value
   */
  private MoveScore alphaBetaWithMemory(ChildMove move, int alpha, int beta, int depth, Ply step) {    
    int a, b, value, bestMove = 0;
    MoveScore searchResult;
    TransEntry trans;
    long hash = zobristHash(move.state);

    //base case
    if ((depth == 0) || terminal(move.state)) {
      return new MoveScore(move.move, evaluate(move.state));
    }

    //trans table lookup
    if (validEntry(move, hash)) {
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

    //recursive
    if (step == Ply.MAX) { //max step
      value = Integer.MIN_VALUE;
      a = alpha; //save original alpha
      for (ChildMove child : children(move, Ply.MAX, false)) {
        searchResult = alphaBetaWithMemory(child, a, beta, depth - 1, Ply.MIN);
        if (searchResult.score >= value) {
          value = searchResult.score;
          bestMove = child.move;
        }
        a = Math.max(a, value);
        if (alpha >= beta) break; //prune
      }
    } else { //min step
      value = Integer.MAX_VALUE;
      b = beta; //save original beta
      for (ChildMove child : children(move, Ply.MIN, false)) {
        searchResult = alphaBetaWithMemory(child, alpha, b, depth - 1, Ply.MAX);
        if (searchResult.score <= value) {
          value = searchResult.score;
          bestMove = child.move;
        }
        b = Math.min(b, value);
        if (alpha >= beta) break; //prune
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

  /**
   * Generates all valid child states of a move.
   *
   * @param parent the parent state
   * @param step the current minimax step (the player to generate moves)
   * @param extraTurn true if recursively calculating children for
   * an extra turn, false otherwise
   * @return a list of all valid child moves
   */
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
    int depth = 1;
    int guess = 0;
    ChildMove state = new ChildMove(-10, board); // this is leaking
    MoveScore best;

    this.searchStartTime = System.currentTimeMillis();
    best = MTDF(state, guess, depth);
    while ((depth < MAX_SEARCH_DEPTH) && (!timeUp())) {
      ++depth;
      best = MTDF(state, guess, depth);
      guess = best.score;
    }

    return best.move;
  }

  /**
   * The agents name.
   * @return a hardcoded string, the name of the agent.
   */
  public String name() {
    return "MTD-f Agent";
  }

  /**
   * A method to reset the agent for a new game.
   */
  public void reset() {
    //nuffin goes ere
  }
}


