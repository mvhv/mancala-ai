# mancala-ai
A variety of search-based implementations of an AI agent for the board game mancala (kalah).

The following files are taken from course content provided by Tim French from the University of Western Australia:
* *Mancala.java* - Game harness
* *MancalaAgent.java* - Interface for the AI agent API
* *RandomAgent.java* - An implementation of a simple random choice agent

All other files are implementations of the AI agent API by Jesse Wyatt. Listed in order of complexity:
* *MMAgent.java* - Simple minimax agent without pruning
* *ABAgent.java* - Minimax search with alpha-beta pruning
* *ABWMAgent.java* - Minimax search with alpha-beta pruning and transposition tables
* *ABIDAgent.java* - Minimax search with alpha-beta pruning and transposition tables, iterative deepening with a 0.5s turn time limit
* *MTDFAgent.java* - MTD-f ("zero-width" iterative deepening) extension of *ABWMAgent*

Significant code is duplicated between agents because of practical academic restrictions and this may result in additional bugs in less complex agents. Code was initially written with OpenJDK 1.8 as a target, but later agents are modified to maintain compatibility with Java 1.5 and avoid the use of System library calls.

A game may be run by compiling all files and running ```java Mancala```. Agent selection is hardcoded within Mancala.java with a default configuration of *MTDFAgent* vs *RandomAgent*. Other agents can be tested by modifying their types at instantiation in ```Mancala.main()``` before/during the call to ```Mancala.play()```.

Repository was shifted to public visibility as of 20/10/2018.
