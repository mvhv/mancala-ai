import java.io.StringWriter;

/**
 * A static class for executing a game of Mancala.
 * Mancala Agents are supplied to play the game.
 * The Mancala class coordinate one game between the two agents and reports the result.
 * Used for CITS301 at the university of Western Australia.
 **/
public class Mancala{
  //board[6] is the store of agent 1
  //board[13] is the store of agent 2
  //board[i+1 % 14] is the next house anticlockwise from board[i]
  private static int[] board;
  //The agents playing thge game, implementation of Mancala Agent
  private static MancalaAgent agent1;
  private static MancalaAgent agent2;
  //A flag to indicate which players move it is.
  private static boolean onesTurn;

  /**
   * Conducts a play between agent 1 andagent 2 and reports 
   * the final score (agent1's score- agent2's score).
   * If at any stage, players attempt an illegal move they forfeit the game 36-0.
   * @param report a printstream to record the moves of the game
   * @param a1 an implementation of MancalaAgent
   * @param a2 an implementation of MancalaAgent
   * @return the difference,x, in score. Agent 1 will have 18+x/2 points. Agent 2 will have 18-x/2 points.
   **/ 
  public static int play(MancalaAgent a1, MancalaAgent a2, StringWriter report){
    agent1 = a1; agent1.reset();
    agent2 = a2; agent2.reset();
    board = new int[14];
    for(int i = 0; i<6; i++) board[i] = 3;
    for(int i = 7; i<13; i++) board[i] = 3;
    boolean oneTurn = true;
    while(!gameOver()){
     if(onesTurn){
      int mv = agent1.move(board.clone());
      report.write(agent1.name()+" plays move "+mv+"\n");
      if(board[mv]==0 || mv<0 || mv>5){
        forfeit(1);
        report.write("Illegal move! Forfeit!\n");
      }
      else{ 
       int i = mv;
       while(board[mv]>0){
        i=i==12?0:i+1;
        board[i]++; board[mv]--;
       }
       if(i<6 && board[i]==1 && board[12-i]>0){
         board[6]+=board[12-i]; 
         board[12-i]=0;
         board[6]+=board[i];
         board[i]=0;
       }
       if(i!=6) onesTurn = false;
      }
     }
     else{
      int mv = 7+agent2.move(invertBoard());
      report.write(agent2.name()+" plays move "+mv+"\n");
      if(board[mv]==0 || mv<7 || mv>12){
        forfeit(2);
        report.write("Illegal move! Forfeit!\n");
      }
      else{ 
       int i = mv;
       while(board[mv]>0){
        i=i==5?7:i==13?0:i+1;
        board[i]++; board[mv]--;
       }
       if(i<13 && i>6 && board[i]==1 && board[12-i]>0){
         board[13]+=board[12-i]; 
         board[12-i]=0;
         board[13]+=board[i];
         board[i] = 0;
       }
       if(i!=13) onesTurn = true;
      }
     }
     report.write(boardString());
    }
    gameOver();
    report.write("Game Over!\nFinal Board\n");
    if(board[6]>board[13])
      report.write(agent1.name()+" wins: "+board[6]+" to "+board[13]+"\n");
    else if(board[6]<board[13])
      report.write(agent2.name()+" wins: "+board[13]+" to "+board[6]+"\n");
    else report.write("Match drawn: 18 all\n");
    return board[6]-board[13];
  }



  /**
   * Conducts a play between agent 1 and agent 2 and reports 
   * the final score (agent1's score- agent2's score).
   * The Game log is printed to stdout
   * @return the difference,x, in score. Agent 1 will have 18+x/2 points. Agent 2 will have 18-x/2 points.
   **/ 
  public static int play(MancalaAgent a1, MancalaAgent a2){
    StringWriter sw = new StringWriter();
    int res = play(a1, a2,sw);
    System.out.println(sw.toString());
    return res;
  }  

  /**
   * Produces a string representation of the game in the form:
   *            Agent2
   *    12  11  10  9   8   7
   *13                          6
   *    0   1   2   3   4   5
   *            Agent1
   */            

  public static String boardString(){
    StringBuffer sb = new StringBuffer();
    sb.append("\t\t\t"+agent2.name()+"\n");
    for(int i = 12; i>6; i--) sb.append("\t"+board[i]);
    sb.append("\n"+board[13]+"\t\t\t\t\t\t\t"+board[6]+"\n");
    for(int i = 0; i<6; i++) sb.append("\t"+board[i]);
    sb.append("\n\t\t\t"+agent1.name()+"\n");
    return sb.toString();
  }

  //Tests to see if the game is over
  private static boolean gameOver(){
    boolean go = true;
    for(int i = 0; i<6; i++) go = go && board[i] == 0;
    if(go){ 
      for(int i = 7; i<13; i++){ 
        board[13]+=board[i];
        board[i] = 0;
      }
      return true;
    }
    go = true;
    for(int i = 7; i<13; i++) go = go && board[i] == 0;
    if(go){ 
      for(int i = 0; i<6; i++){ 
        board[6]+=board[i];
        board[i] = 0;
      }
      return true;
    }
    return false;
  }
  
  //moves all seeds to opponents store.
  private static void forfeit(int agent){
    if(agent==1){
      for(int i = 0; i<13; i++){
        board[13]+=board[i]; board[i]=0;
      }
    }
    if(agent==2){
      for(int i = 0; i<14; i=i==5?7:i+1){
        board[6]+=board[i]; board[i]=0;
      }
    }
  }

  //inverts the board so agent 2 sees it as if playing as agent 1
  private static int[] invertBoard(){
    int[] bd = new int[14];
    for(int i = 0; i<7;i++){
      bd[i] = board[7+i];
      bd[7+i] = board[i];
    }
    return bd; 
  }


  //runs a basic game between two random players
  public static void main(String[] args){
    play(new MancalaImp(), new RandomAgent());
  }

}


    


