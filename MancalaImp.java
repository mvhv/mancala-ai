import java.util.ArrayList;
import java.util.Arrays;
import java.util.Random;
import java.util.HashMap;

/**
 * CITS3001 - Lab 6
 * Mancala Agent
 * 
 * Jesse Wyatt (20756971)
 */
public class MancalaImp implements MancalaAgent {

  /**
   * Subclass for holding transposition table entries
   */
  class TransEntry {
    public int depth;
    public int alpha;
    public int beta;

    public TransEntry(int depth, int alpha, int beta) {
      this.depth = depth;
      this.alpha = alpha;
      this.beta = beta;
    }
  }

  private static enum MMStep {MAX, MIN};
  private static int nSeeds = 12*3;
  private long[][] zobristTable;
  private HashMap<Long, TransEntry> transTable;

  /**
   * Creates an instance of the Mancala Agent
   */
  public MancalaImp(){
    //gen bitstrings for zobrist hashing
    Random randomGen = new Random();
    this.zobristTable = new long[14][nSeeds + 1];
    for (int i = 0; i < 14; ++i) {
      for (int j = 0; j < nSeeds + 1; ++j) {
        this.zobristTable[i][j] = randomGen.nextLong();
      }
    }

    this.transTable = new HashMap<Long, TransEntry>();
  }

  /**
   * Calculates the zobrist hash key for the current game state
   * @param state the current state of the board
   * @return the zobrist hash of the current game state
   */
  private long zobristKey(int[] state) {
    long key = 0;
    for (int i = 0; i < 14; ++i) {
      key ^= zobristTable[i][state[i]];
    }
    return key;
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
    int depth = 10;
    ArrayList<Integer> state = new ArrayList<Integer>();
    for(int i = 0; i < 14; ++i) state.add(board[i]);
    int best = 0;
    int value = Integer.MIN_VALUE;
    int curr;

    for (int i = 0; i < 6; ++i) { //for each house
      if (state.get(i) > 0) {
        //valid move
        ArrayList<Integer> newState = new ArrayList<Integer>(state);
        //sow seeds from i
        int j = i;
        int seeds = state.get(i);
        newState.set(i, 0);
        while(seeds > 0) {
          ++j;
          j %= 14;
          if (j < 13) { //don't place in opponent silo
            --seeds;
            newState.set(j, newState.get(j) + 1);
          }
        }
        if (j == 6) { //extra turn
          //recursively find children of this state
          curr = extraTurnValue(newState, depth);
        } else {
          if ((j >= 0) && (j <= 5) && (newState.get(j) == 1) && (newState.get(12-j) > 0)) { //empty house rule
            newState.set(6, newState.get(6) + newState.get(12-j) + 1);
            newState.set(j, 0);
            newState.set(12-j, 0);
          }
          curr = alphabeta(newState, depth, Integer.MIN_VALUE, Integer.MAX_VALUE, MMStep.MAX);
        }
        if (curr >= value) {
          value = curr;
          best = i;
        }
      }
    }
    return best;
  }

  /**
   * calculates the best value from any extra turn
   * @param state extra turn state
   * @return the optimal result
   */
  public int extraTurnValue(ArrayList<Integer> state, int depth) {
    int value = Integer.MIN_VALUE;
    int curr;

    for (int i = 0; i < 6; ++i) { //for each house
      if (state.get(i) > 0) {
        //valid move
        ArrayList<Integer> newState = new ArrayList<Integer>(state);
        //sow seeds from i
        int j = i;
        int seeds = state.get(i);
        newState.set(i, 0);
        while(seeds > 0) {
          ++j;
          j %= 14;
          if (j < 13) { //don't place in opponent silo
            --seeds;
            newState.set(j, newState.get(j) + 1);
          }
        }
        if (j == 6) { //extra turn
          //recursively find children of this state
          curr = extraTurnValue(newState, depth);
        } else {
          if ((j >= 0) && (j <= 5) && (newState.get(j) == 1) && (newState.get(12-j)) > 0) { //empty house rule
            newState.set(6, newState.get(6) + newState.get(12-j) + 1);
            newState.set(j, 0);
            newState.set(12-j, 0);
          }
          curr = alphabeta(newState, depth - 1, Integer.MIN_VALUE, Integer.MAX_VALUE, MMStep.MAX);
        }
        if (curr >= value) {
          value = curr;
        }
      }
    }
    return value;
  }


  /**
   * The agents name.
   * @return a hardcoded string, the name of the agent.
   */
  public String name() {
    return "Jesse Wyatt";
  }

  /**
   * A method to reset the agent for a new game.
   */
  public void reset() {}

  /**
   * Calculate all child states of the current state
   * @param state the current state
   * @return the list of child states
   */
  public ArrayList<ArrayList<Integer>> children(ArrayList<Integer> state, MMStep step) {
    ArrayList<ArrayList<Integer>> childstates = new ArrayList<ArrayList<Integer>>();

    if (step == MMStep.MAX) { //our moves
      for (int i = 0; i < 6; ++i) { //for each house
        if (state.get(i) > 0) {
          //valid move
          ArrayList<Integer> newState = new ArrayList<Integer>(state);
          //sow seeds from i
          int j = i;
          int seeds = state.get(i);
          newState.set(i, 0);
          while(seeds > 0) {
            ++j;
            j %= 14;
            if (j < 13) { //don't place in opponent silo
              --seeds;
              newState.set(j, newState.get(j) + 1);
            }
          }
          if (j == 6) { //extra turn
            //recursively find children of this state
            childstates.addAll(children(newState, MMStep.MAX));
          } else {
            if ((j >= 0) && (j <= 5) && (newState.get(j) == 1) && (newState.get(12-j) > 0)) { //empty house rule
              newState.set(6, newState.get(6) + newState.get(12-j) + 1);
              newState.set(j, 0);
              newState.set(12-j, 0);
            }
            childstates.add(newState);
          }
        }
      }
    } else { //enemy moves
      for (int i = 7; i < 13; ++i) { //for each house
        if (state.get(i) > 0) {
          //valid move
          ArrayList<Integer> newState = new ArrayList<Integer>(state);
          //sow seeds from i
          int j = i;
          int seeds = state.get(i);
          newState.set(i, 0);
          while(seeds > 0) {
            ++j;
            j %= 14;
            if (j != 6) { //don't place in our silo
              --seeds;
              newState.set(j, newState.get(j) + 1);
            }
          }
          if (j == 13) { //extra turn
            //recursively find children of this state
            childstates.addAll(children(newState, MMStep.MIN));
          } else {
            if ((j >= 7) && (j <= 12) && (newState.get(j) == 1) && (newState.get(12-j)) > 0) { //empty house rule
              newState.set(13, newState.get(13) + newState.get(12-j) + 1);
              newState.set(j, 0);
              newState.set(12-j, 0);
            }
            childstates.add(newState);
          }
        }
      }
    }
    return childstates;
  }

  /**
   * Caclulate the potential value for a move
   * @param state the move to make
   * @return the potential score
   */
  public int utility(ArrayList<Integer> state) {
    int score = 0;
    for (int i = 0; i < 7; ++i) score += state.get(i);
    for (int i = 7; i < 14; ++i) score -= state.get(i);

    if (terminal(state)) {
      if (score > 0) {
        return Integer.MAX_VALUE;
      } else if (score < 0) {
        return Integer.MIN_VALUE;
      }
    }
    return score;
  }

  /**
   * Check if state is a terminal state
   * @param state the current state
   * @return true if state is terminal, false otherwise
   */
  public boolean terminal(ArrayList<Integer> state) {
    //if south empty - terminal
    int count = 0;
    for (int i = 0; i < 6; ++i) count += state.get(i);
    if (count == 0) {
      return true;
    }
    //if north empty - terminal
    count = 0;
    for (int i = 7; i < 13; ++i) count += state.get(i);
    if (count == 0) {
      return true;
    }
    //else - not terminal
    return false;
  }

  /**
   * Negamax with ab pruning.
   * @param node the index of the current node
   * @param depth the number of ply to search forward
   * @param alpha minimum gain assured
   * @param beta maximum loss assured
   * @param step enum indicating if on max or min step of search
   * @return the value of the node
   */
  public int alphabeta(ArrayList<Integer> state, int depth, int alpha, int beta, MMStep step) {
    if ((terminal(state)) || (depth == 0)) {
      //base case
      return utility(state);
    } else {
      //recursive case
      int value;
      if (step == MMStep.MAX) {
        //maxstep
        value = Integer.MIN_VALUE;
        for (ArrayList<Integer> child : children(state, MMStep.MAX)) {
          value = Math.max(value, alphabeta(child, depth - 1, alpha, beta, MMStep.MIN));
          if (alpha == Integer.MIN_VALUE) {
            alpha = Math.max(alpha, value);
          } else {
            alpha = Math.max(alpha, value);
            if (alpha >= beta) {
              break;
            }
          }
        }
      } else {
        //minstep
        value = Integer.MAX_VALUE;
        for (ArrayList<Integer> child : children(state, MMStep.MIN)) {
          value = Math.min(value, alphabeta(child, depth - 1, alpha, beta, MMStep.MAX));
          if (beta == Integer.MAX_VALUE) {
            beta = Math.min(beta, value);
          } else {
            beta = Math.min(beta, value);
            if (alpha >= beta) {
              break;
            }
          }
        }
      }
      return value;
    }
  }
}
