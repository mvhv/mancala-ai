import java.util.HashMap;
import java.util.ArrayList;
import java.util.Arrays;

/**
 * Basic Minimax search for Mancala
 * 
 * OOP version for passing results
 */
public class MMAgent implements MancalaAgent {

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

  private MoveScore minimax(ChildMove move, int depth, Ply step){
    //base case
    if ((depth == 0) || terminal(move.state)) {
      return new MoveScore(move.move, evaluate(move.state));
    }

    int value, bestMove = 0;
    MoveScore searchResult;
    //recursive
    if (step == Ply.MAX) { //max step
      value = Integer.MIN_VALUE;
      for (ChildMove child : children(move, Ply.MAX, false)) {
        searchResult = minimax(child, depth - 1, Ply.MIN);
        if (searchResult.score >= value) {
          value = searchResult.score;
          bestMove = child.move;
        }
      }
    } else { //min step
      value = Integer.MAX_VALUE;
      for (ChildMove child : children(move, Ply.MIN, false)) {
        searchResult = minimax(child, depth - 1, Ply.MAX);
        if (searchResult.score <= value) {
          value = searchResult.score;
          bestMove = child.move;
        }
      }
    }

    return new MoveScore(bestMove, value);
  }

  public ArrayList<ChildMove> children(ChildMove parent, Ply step, boolean extraTurn) {
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
          if (j == 13) { //extra turn rule
            //recursively find children of this state
            if (terminal(child.state)) { //if move ends the game it can't have extra children
              childmoves.add(child);
            } else {
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
    MoveScore best = minimax(new ChildMove(-1, board), 7, Ply.MAX);
    return best.move;
  }

  /**
   * The agents name.
   * @return a hardcoded string, the name of the agent.
   */
  public String name() {
    return "Minimax Agent";
  }

  /**
   * A method to reset the agent for a new game.
   */
  public void reset() {}
}


