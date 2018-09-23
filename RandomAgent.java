/**
 * Interface for A MancalaAgent.
 *From CITS3001 at the University of Western Australia.
 *Must be implemented using a no-argument constructor.
 */
public class RandomAgent implements MancalaAgent{

  /**
   * Allows the agent to nominate the house the agent would like to move seeds from. 
   * The agent will allways have control of houses 0-5 with store at 6. 
   * Any move other than 0-5 will result in a forfeit. 
   * An move from an empty house will result in a forfeit.
   * A legal move will always be available. 
   * @param board the current state of the game. 
   * The board is an int array of length 14, indicating the 12 houses and 2 stores. 
   * The agent's house are 0-5 and their store is 6. The opponents houses are 7-12 and their store is 13. Board[i] is the number of seeds in house (store) i.
   * board[(i+1}%14] is the next house (store) anticlockwise from board[i].  
   * This will be consistent between moves of a normal game so the agent can maintain a strategy space.
   * @return the house the agent would like to move the seeds from this turn.
   */
  public int move(int[] board){
    int x = (int) (Math.random()*1999);
    while(board[x%6]==0){
      x= (int)(Math.random()*1999);
    }
    return x%6;
  } 

  /**
   * The agents name.
   * @return a hardcoded string, the name of the agent.
   */
  public String name(){return "RandomAgent";}

  /**
   * A method to reset the agent for a new game.
   */
  public void reset(){}
}


