package ucb.test;

import java.util.*;
import java.io.*;

/** An abstract class that provides a rudimentary testing framework.  Subtypes
 *  of TestFramework should override the runTests method to perform 
 *  all desired tests, calling 'check' for each individual test to check 
 *  the results, or 'failure' to indicate that a test has "blown up". 
 */
public abstract class TestFramework {
    protected int testCount, wrongCount, badCount;
    protected String currentTestName;
    protected final PrintStream realStandardOutput = System.out;
    private ByteArrayOutputStream capturedOutput;

    protected void report () {
        System.err.printf ("Out of %d tests run:%n    %d passes.%n"
                           + "    %d wrong results.%n    %d failed executions.%n",
                           getTestCount (), getPassedCount (),
                           getWrongCount (), getBadCount ());
    }

    /** Execute all tests.  Should be overridden in subclasses. */
    protected abstract void runTests ();

    protected TestFramework () {
        testCount = wrongCount = badCount = 0;
    }

    protected void setTestName (String name) {
        currentTestName = name;
    }

    /** Start storing up the standard output into a string.  You can later 
     *  retrieve it with getCapturedOutput () and restore the initial
     *  standard output with restoreStandardOutput ().   Throws away any
     *  previously saved output. */
    protected void captureStandardOutput () {
        capturedOutput = new ByteArrayOutputStream ();
        System.setOut (new PrintStream (capturedOutput));
    }

    /** Stop capturing standard output, and revert to initial behavior. */
    protected void restoreStandardOutput () {
        System.setOut (realStandardOutput);
    }    

    /** Return the captured output from the standard output. */
    protected String getCapturedOutput () {
        System.out.flush ();
        return capturedOutput.toString ();
    }

    /** If TEST is tree, print "PASSED" message.  Otherwise indicate
     *  error. */
    protected void check (boolean test) {
        testCount += 1;
        if (test)
            System.err.printf ("PASSED: %s%n", currentTestName);
        else {
            System.err.printf ("ERROR: %s%n", currentTestName);
            wrongCount += 1;
        }
    }

    /** If TEST is tree, print "PASSED" message.  Otherwise indicate
     *  error, printing MSG. */
    protected void check (String msg, boolean test) {
        testCount += 1;
        if (test) 
            System.err.printf ("PASSED: %s%n", currentTestName);
        else {
            System.err.printf ("ERROR: %s (%s)%n", currentTestName, msg);
            wrongCount += 1;
        }
    }

    /** Check that CORRECT is equal to TEST, using the "same" test below. */
    protected void check (Object correct, Object test) {
        check (same (correct, test));
    }
     
    /** Check that CORRECT is equal to TEST, using the "same" test below. 
     *  Print MSG on error. */
    protected void check (String msg, Object correct, Object test) {
        check (msg, same (correct, test));
    }
     
    /** Report a "failure" (typically a test that blows up instead of producing
     *  a result).  Use MSG as the explanation. */
    protected void failure (String msg) {
        testCount += 1;
        badCount += 1;
        if (msg == null)
            System.err.printf ("FAILED: %s%n", currentTestName);
        else 
            System.err.printf ("FAILED: %s (%s)%n", currentTestName, msg);
    }


    /** True iff X equals Y in the sense that either
     *  1) X and Y are arrays of the same length and their respective
     *     elements are == (if primitive) and (recursively) the same otherwise;
     *  or
     *  2) X and Y are both null.
     *  or
     *  3) X.equals(Y).  */
    public static boolean same (Object x, Object y) {
        return ((x == null) == (y == null)
                && (x == null
                    || (x.getClass ().isArray () && y.getClass ().isArray () 
                        && sameArrays (x, y))
                    || x.equals (y)));
    }

    public static boolean sameArrays (Object x, Object y) {
        Class<?> type = x.getClass ();
        try {
            return Arrays.equals ((byte[]) x, (byte[]) y);
        } catch (ClassCastException e) {}
        try {
            return Arrays.equals ((short[]) x, (short[]) y);
        } catch (ClassCastException e) {}
        try {
            return Arrays.equals ((char[]) x, (char[]) y);
        } catch (ClassCastException e) {}
        try {
            return Arrays.equals ((int[]) x, (int[]) y);
        } catch (ClassCastException e) {}
        try {
            return Arrays.equals ((long[]) x, (long[]) y);
        } catch (ClassCastException e) {}
        try {
            return Arrays.equals ((boolean[]) x, (boolean[]) y);
        } catch (ClassCastException e) {}
        try {
            return Arrays.equals ((float[]) x, (float[]) y);
        } catch (ClassCastException e) {}
        try {
            return Arrays.equals ((double[]) x, (double[]) y);
        } catch (ClassCastException e) {}
    
        try {
            Object[] xa = (Object[]) x;
            Object[] ya = (Object[]) y;

            if (xa.length != ya.length)
                return false;
            for (int i = 0; i < xa.length; i += 1)
                if (! same (xa[i], ya[i]))
                    return false;
            return true;
        } catch (ClassCastException e) { }

        return false;
    }

    /** Cumulative number of calls to check. */
    public int getTestCount () { return testCount; }
    /** Passed tests. */
    public int getPassedCount () { return testCount - getErrorCount (); }
    /** Cumulative number of calls to where the arguments were not the same. */
    public int getWrongCount () { return wrongCount; }
    /** Cumulative number of "bad" tests: those causing an exception */
    public int getBadCount () { return badCount; }
    /** Cumulative count of tests that fail due to exceptions. */
    public int getErrorCount () { return badCount + wrongCount; }

}
