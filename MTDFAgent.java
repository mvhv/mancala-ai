import java.util.HashMap;
import java.util.Random;
import java.util.ArrayList;
import java.util.Arrays;

/**
 * Implementation of MTD-f for mancala
 * Based on: http://people.csail.mit.edu/plaat/mtdf.html
 */
public class MTDFAgent implements MancalaAgent{

  /**
   * Subclass for holding transposition table entries
   */
  class TransEntry {
    public int depth;
    public int upperbound;
    public int lowerbound;

    public TransEntry() {
      this.depth = 0;
      this.upperbound = Integer.MAX_VALUE;
      this.lowerbound = Integer.MIN_VALUE;
    }

    public TransEntry(int depth, int upperbound, int lowerbound) {
      this.depth = depth;
      this.upperbound = upperbound;
      this.lowerbound = lowerbound;
    }
  }

  /**
   * Subclass to hold a move with its minimax score
   */
  class MoveScore {
    public int move;
    public int score;

    public MoveScore(int move, int score) {
      this.move = move;
      this.score = score;
    }
  }

  /**
   * Subclass to hold a move with its corresponding child state
   */
  class MoveState {
    public int move;
    public int[] state;

    public MoveState(int move, int child) {
      this.move = move;
      this.child = state;
    }
  }

  private static enum Ply {MAX, MIN};
  private static int MAX_SEARCH_DEPTH = 10;
  private HashMap<Long, TransEntry> transTable;
  private long[][] zobristTable;
  private int nSeeds = 3 * 12;

  public MTDFAgent() {
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

  private int evaluate(int[] state) {
    int score = 0;
    for (int i = 0; i < 7; ++i) score += state[i];
    for (int i = 7; i < 14; ++i) score -= state[i];

    if (terminal(state)) {
      if (score > 0) {
        return Integer.MAX_VALUE;
      } else if (score < 0) {
        return Integer.MIN_VALUE;
      }
    }

    return score;
  }

  private ArrayList<MoveState> children(int[] state, Ply step) {
    ArrayList<MoveState> childstates = new ArrayList<MoveState>();

    if (step == Ply.MAX) { //our moves
      for (int i = 0; i < 6; ++i) {
        if (state[i] > 0) {
          //move is valid
          MoveState newState = MoveState(i, Arrays.copyOf(state, 14));
          //sow seeds from i
          int j = i;
          int seeds = state[i];
          newState.state[i] = 0;
          while(seeds > 0) {
            ++j;
            j %= 14;
            if (j < 13) { //don't place in opponent store
              --seeds;
              newState.state[j] += 1;
            }
          }
          if (j == 6) { //extra turn
            //recursively find children of this state
            childstates.addAll(children(newState.state, Ply.MAX));
          } else {
            if ((j >= 0) && (j <= 5) && (newState.state[j] == 1) && (newState.state[12-j] > 0)) { //empty house rule
              newState.state[6] = newState.state[6] + newState.state[12-j] + 1;
              newState.state[j] = 0;
              newState.state[12-j] = 0;
            }
            childstates.add(newState);
          }
        }
      }
    } else { //enemy moves
      for (int i = 7; i < 13; ++i) {
        if (state[i] > 0) {
          //move is valid
          MoveState newState = new MoveState(i, Arrays.copyOf(state, 14));
          //sow seeds from i
          int j = i;
          int seeds = state[i];
          newState.state[i] = 0;
          while(seeds > 0) {
            ++j;
            j %= 14;
            if (j < 6) { //don't place in our store
              --seeds;
              newState.state[j] += 1;
            }
          }
          if (j == 13) { //extra turn rule
            //recursively find children of this state
            childstates.addAll(children(newState.state, Ply.MAX));
          } else {
            if ((j >= 7) && (j <= 12) && (newState.state[j] == 1) && (newState.state[12-j] > 0)) { //empty house rule
              newState.state[13] = newState.state[13] + newState.state[12-j] + 1;
              newState.state[j] = 0;
              newState.state[12-j] = 0;
            }
            childstates.add(newState);
          }
        }
      }
    }
    return childstates;
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

  private long hash(int[] state) {
    long key = 0;
    for (int i = 0; i < 14; ++i) {
      key ^= zobristTable[i][state[i]];
    }
    return key;
  }

  private boolean timesUp() {
    return false;
  }

  private int MTDF(int[] root, int f, int d) {
    int g, upperbound, lowerbound, beta;
    
    g = f;
    upperbound = Integer.MAX_VALUE;
    lowerbound = Integer.MIN_VALUE;

    while (lowerbound < upperbound) {
      if (g == lowerbound) {
        beta = g + 1;
      } else {
        beta = g;
      }

      g = alphaBetaWithMemory(root, beta - 1, beta, d, Ply.MAX);

      if (g < beta) {
        upperbound = g;
      } else {
        lowerbound = g;
      }
    }

    return g;
  }

  private int iterativeDeepening(int[] root) {
    int firstguess = 0;

    for (int d = 1; d < MAX_SEARCH_DEPTH; ++d) {
      firstguess = MTDF(root, firstguess, d);
      if (timesUp()) {
        break;
      }
    }
    return firstguess;
  }

  private int alphaBetaWithMemory(int[] state, int alpha, int beta, int d, Ply step) {
    int g, a, b;
    TransEntry transState;
    long stateHash = hash(state);

    if (transTable.containsKey(stateHash)) { //trans table lookup
      transState = transTable.get(stateHash);
      if (transState.depth >= d) {
        if (transState.lowerbound >= beta) {
          return transState.lowerbound;
        }
        if (transState.upperbound <= alpha) {
          return transState.upperbound;
        }
        alpha = Math.max(alpha, transState.lowerbound);
        beta = Math.min(beta, transState.upperbound);
      }
    }

    if ((d == 0) || terminal(state)) { // leaf node
      g = evaluate(state);
    } else if (step == Ply.MAX) { //MAX STEP
      g = Integer.MIN_VALUE;
      a = alpha; //save original alpha value

      for (MoveState child : children(state, Ply.MAX)) {
        if (g >= beta) break;
        g = Math.max(g, alphaBetaWithMemory(child, a, beta, d - 1, Ply.MIN)); ////////// FIX THIS, MUST RETURN MoveScore -----------------------------------
        a = Math.max(a, g);
      }

    } else { // step == Ply.MIN //MIN STEP
      g = Integer.MAX_VALUE;
      b = beta; //save original beta value

      for (MoveState child : children(state, Ply.MIN)) {
        if (g <= alpha) break;
        g = Math.min(g, alphaBetaWithMemory(child, alpha, b, d - 1, Ply.MAX));
        b = Math.min(b, g);
      }
    }
    //traditional transposition table storing of bounds
    transState = transTable.getOrDefault(stateHash, new TransEntry());
    
    if (transState.depth >= d) {
      //fail low result implies an upper bound
      if (g <= alpha) {
        transState.upperbound = g;
      }
      //found an accurate minimax value - will not occur if called with zero window
      if ((g > alpha) && (g < beta)) {
        transState.lowerbound = g;
        transState.upperbound = g;
      }
      //fail high result implies a lower bound
      if (g >= beta) {
        transState.lowerbound = g;
      }
      transState.depth = d;
      transTable.put(stateHash, transState);
    }
    return g;
  }

  /**
   * Allows the agent to nominate the house the agent would like to move seeds from. 
   * The agent will allways have control of houses 0-5 with store at 6. 
   * Any move other than 0-5 will result in a forfeit. 
   * An move from an empty house will result in a forfeit.
   * A legal move will always be available.
   * Assume your agent has 0.5 seconds to make a move. 
   * @param state the current state of the game. 
   * The board is an int array of length 14, indicating the 12 houses and 2 stores. 
   * The agent's house are 0-5 and their store is 6. The opponents houses are 7-12 and their store is 13. Board[i] is the number of seeds in house (store) i.
   * board[(i+1}%14] is the next house (store) anticlockwise from board[i].  
   * This will be consistent between moves of a normal game so the agent can maintain a strategy space.
   * @return the house the agent would like to move the seeds from this turn.
   */
  public int move(int[] state) {

  }

  private int evalExtraTurns(int[] state) {
    return Integer.MIN_VALUE;
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
  public void reset() {}
}


