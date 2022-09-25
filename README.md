# Tablut
<strong>FILES WORKED ON: tablut/Square.java, tablut/Move.java, tablut/Board.java, tablut/AI.java, tablut/TablutTests.java</strong>

<strong>Rules of the Game:</strong><br>
Tablut is played on a 9x9 checkerboard between a set of 9 white pieces and 16 black pieces. The middle square is called the throne (or castle). 
One of the white pieces is the king, and the others, his guards, are known as Swedes. The white side wins if the king reaches one of the edges 
of the board. The black pieces are known as Muscovites (referring to the Grand Duchy of Moscow). Their aim is to capture the king before he 
reaches the edge of the board. All pieces move like chess rooks: any number of squares orthogonally (horizontally or vertically). Pieces may not 
jump over each other or land on top of another piece. No piece other than the king may land on the throne, although any piece may pass through it 
when it is empty. The black (Muscovite) side goes first. A piece other than the king is captured when, as a result of an enemy move to an 
orthogonally adjacent square, the piece is enclosed on two opposite sides (again orthogonally) by hostile squares. A square is hostile if it 
contains an enemy piece, or if it is the throne square and is empty (that is, it is hostile to both white and black pieces). The occupied throne 
is also hostile to white pieces when three of the four squares surrounding it are occupied by black pieces. Captures result only as a result of 
enemy moves; a piece may move so as to land between two enemy pieces without being captured. A single move can capture up to three pieces.
The king is captured like other pieces except when he is on the throne square or on one of the four squares orthogonally adjacent to the throne. 
In that case, the king is captured only when surrounded on all four sides by hostile squares (of which the empty throne may be one). A side also 
loses when it has no legal moves on its turn, or if its move returns the board to a previous position (same pieces in the same places and the same 
side to move). As a result, there are no drawn games.

![image](https://user-images.githubusercontent.com/114131596/192127933-2828a829-6b75-4302-aaea-851dea0bc268.png)
![image](https://user-images.githubusercontent.com/114131596/192128019-b643e30f-ea4d-45aa-ae53-5dd54adce610.png)

<strong>Notation:</strong><br>
A square is denoted by a column letter followed by a row number (as in e4). Columns are enumerated from left to right with letters a through i. 
Rows are enumerated from the bottom to the top with numbers 1 through 9. An entire move then consists of the starting square, a hyphen, and the 
ending row (if vertical) or column (if horizontal). Thus, b3-6 means "Move from b3 to b6" and b3-f means "Move from b3 to f3."

<strong>To run the game use the command:</strong><br>
java -ea tablut.Main

<strong>Commands:</strong>
<ul>
  <li>A move in the format described in Notation.</li>
  <li><strong>new:</strong> End any game in progress, clear the board to its initial position, and set the current player to black.</li>
  <li><strong>seed N:</strong> If the AIs are using random numbers for move selection, this command seeds their random-number generator with the integer N. Given the same seed and the same opposing moves, an AI should always make the same moves. This feature makes games reproducible.</li>
  <li><strong>auto C:</strong> Make the C player an automated player. Here, C is "black" or "white", case-insensitive.</li>
  <li><strong>manual C:</strong> Make the C player a human player (entering moves as manual commands).</li>
  <li><strong>quit:</strong> Exit the program.</li>
  <li><strong>dump:</strong> Print the current state of the board in exactly the following format:</li>
</ul>

![Capture](https://user-images.githubusercontent.com/114131596/192129684-9bd42922-0025-4ded-8520-838743413053.PNG)

Here, K denotes the king, W another white piece (Swede) and B a black piece (Muscovite).
