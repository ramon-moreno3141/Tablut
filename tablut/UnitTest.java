package tablut;

import org.junit.Test;
import static org.junit.Assert.*;
import ucb.junit.textui;

import static tablut.Piece.*;
import static tablut.Board.*;
import static tablut.Square.*;
import static tablut.Move.mv;

/** The suite of all JUnit tests for the enigma package.
 *  @author Ramon Moreno
 */
public class UnitTest {

    /** Run the JUnit tests in this package. Add xxxTest.class entries to
     *  the arguments of runClasses to run other JUnit tests. */
    public static void main(String[] ignored) {
        textui.runClasses(UnitTest.class, TablutTests.class);
    }

    /** A dummy test as a placeholder for real ones. */
    @Test
    public void myTest() {
        Board board = new Board();
        board.makeMove(mv(sq("f", "1"), sq("i", "1")));
        board.makeMove(mv(sq("e", "3"), sq("i", "3")));
        board.undo();
        assertEquals(Piece.WHITE, board.turn());
    }

}


