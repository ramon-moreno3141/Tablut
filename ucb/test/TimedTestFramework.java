package ucb.test;

import java.util.*;
import java.io.*;

/** An abstract class that provides a rudimentary testing framework.  Subtypes
 *  of TimedTestFramework should call 'check' for each individual test 
 *  to check the results, or 'failure' to indicate that a test has "blown up". 
 */
public abstract class TimedTestFramework {
    protected int testCount, wrongCount, badCount;
    protected String currentTestName;
    protected final PrintStream 
        realStandardOutput = System.out,
        realStandardError = System.err;
    protected int outputLimit = 1000;
    private boolean verbose;

    private class LimitedByteArrayOutputStream extends ByteArrayOutputStream {
        private int limit;

        LimitedByteArrayOutputStream (int limit) {
            super ();
            this.limit = limit;
        }

        public void write(byte[] b, int off, int len) {
            limit -= len;
            if (limit < 0)
                throw new IllegalArgumentException ("too much output");
            super.write (b, off, len);
        }

        public void write(int b) {
            limit -= 1;
            if (limit < 0)
                throw new IllegalArgumentException ("too much output");
            super.write (b);
        }
    }

    private LimitedByteArrayOutputStream capturedOutput, capturedError;

    protected void report () {
        System.err.printf ("Out of %d tests run:%n    %d passes.%n"
                           + "    %d wrong results.%n    %d failed executions.%n",
                           getTestCount (), getPassedCount (),
                           getWrongCount (), getBadCount ());
    }

    protected TimedTestFramework () {
        testCount = wrongCount = badCount = 0;
        verbose = false;
    }

    protected void setVerbosity (boolean verbose) {
        this.verbose = verbose;
    }

    protected void setTestName (String name) {
        currentTestName = name;
        realStandardError.printf ("<<<%s>>>%n", name);
        realStandardError.flush ();
    }

    /** Start storing up the standard output into a string.  You can later 
     *  retrieve it with getCapturedOutput () and restore the initial
     *  standard output with restoreStandardOutput ().   Throws away any
     *  previously saved output. */
    protected void captureStandardOutput () {
        capturedOutput = new LimitedByteArrayOutputStream (outputLimit);
        capturedError = new LimitedByteArrayOutputStream (outputLimit);
        System.setOut (new PrintStream (capturedOutput));
        System.setErr (new PrintStream (capturedError));
    }

    /** Stop capturing standard output, and revert to initial behavior. */
    protected void restoreStandardOutput () {
        System.out.flush ();
        System.setOut (realStandardOutput);
        System.err.flush ();
        System.setErr (realStandardError);
    }    

    /** Return the captured output from the standard output. */
    protected String getCapturedOutput () {
        System.out.flush ();
        return capturedOutput.toString ();
    }

    /** Return the captured output from the standard error output. */
    protected String getCapturedError () {
        System.err.flush ();
        return capturedError.toString ();
    }

    /** Check that TEST is true, printing PASSED or ERROR messages
     *  accordingly and updating test and error counts.  */
    protected void check (boolean test) {
        check (null, test);
    }

    /** Check that CORRECT is equal to TEST, using the "same" test below. */
    protected void check (Object correct, Object test) {
        check (null, correct, test);
    }

    /** Check that CORRECT is equal to TEST, using the "same" test below.
     *  Prints the message MSG if it is not. */
    protected void check (String msg, Object correct, Object test) {
        boolean ok = same (correct, test);
        check (msg, ok); 
        if (!ok && verbose)
            realStandardError.printf ("    Expected: %s%n    Received: %s%n%n",
                                      abbrev (toString (correct), 10000), 
                                      abbrev (toString (test), 10000));
    }

    /** Check that TEST is true, printing either a PASSED message or
     *  or an ERROR message incorporating MSG accordingly and updating
     *  test and error counts.  */
    protected void check (String msg, boolean test) {
        if (msg == null || msg.equals ("")) 
            msg = "";
        else
            msg = String.format ("(%s) ", msg);
        testCount += 1;
        if (test) 
            realStandardError.printf ("Test #%d: %s  PASSED%n", 
                                      testCount, currentTestName);
        else {
            if (verbose)
                realStandardError.println ();
            realStandardError.printf ("Test #%d: %s %s ERROR%n", 
                                      testCount, msg, currentTestName);
            wrongCount += 1;
        }
    }
     
    /** Report a "failure" (typically a test that blows up instead of producing
     *  a result).  Use MSG as the explanation. */
    protected void failure (String msg) {
        testCount += 1;
        badCount += 1;
        if (msg == null)
            realStandardError.printf ("Test #%d: %s  FAILED%n", testCount, currentTestName);
        else 
            realStandardError.printf ("Test #%d: %s  FAILED%n   (%s)%n",
                                      testCount, currentTestName, msg);
    }


    /** Report a "failure" from exception E. */
    protected void failure (Throwable e) {
        testCount += 1;
        badCount += 1;
        if (verbose)
            realStandardError.println ();
        if (e == null)
            realStandardError.printf ("Test #%d: %s  FAILED%n    (Exception)%n", 
                                      testCount, currentTestName);
        else {
            realStandardError.printf ("Test #%d: %s  FAILED%n   Exception %s%n" +
                                      "   Stack trace (most recent call on top):%n",
                                      testCount, currentTestName, e);
            if (verbose) {
                int n;
                n = 0;
                for (StackTraceElement frame: e.getStackTrace ()) {
                    n += 1;
                    if (n > 20) {
                        realStandardError.printf ("      ...%n");
                        break;
                    }
                    realStandardError.printf ("      %d. %s%n", n, frame);
                }
            }
        }
        if (verbose)
            realStandardError.println ();
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
            return Arrays.deepEquals ((Object[]) x, (Object[]) y);
        } catch (ClassCastException e) { }

        return false;
    }

    public static String toString (Object x) {
        if (x == null)
            return "null";
        try {
            return Arrays.toString ((byte[]) x);
        } catch (ClassCastException e) {}
        try {
            return Arrays.toString ((short[]) x);
        } catch (ClassCastException e) {}
        try {
            return Arrays.toString ((char[]) x);
        } catch (ClassCastException e) {}
        try {
            return Arrays.toString ((int[]) x);
        } catch (ClassCastException e) {}
        try {
            return Arrays.toString ((long[]) x);
        } catch (ClassCastException e) {}
        try {
            return Arrays.toString ((boolean[]) x);
        } catch (ClassCastException e) {}
        try {
            return Arrays.toString ((float[]) x);
        } catch (ClassCastException e) {}
        try {
            return Arrays.toString ((double[]) x);
        } catch (ClassCastException e) {}
        try {
            return Arrays.deepToString ((Object[]) x);
        } catch (ClassCastException e) {}
        return x.toString ();
    }

    static String abbrev (String s, int max) {
        if (s.length () > max) {
            return s.substring (0, max/2) + "\n...\n" + 
                s.substring (s.length () - max/2);
        } else
            return s;
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
