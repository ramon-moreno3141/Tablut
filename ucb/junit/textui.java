package ucb.junit;

import org.junit.runner.Result;
import org.junit.runner.notification.Failure;
import org.junit.runner.JUnitCore;
import org.junit.Test;

import java.util.List;
import java.util.ArrayList;

import java.lang.reflect.Method;

/** An alternative unit test runner with text output.
 *  @author P. N. Hilfinger
 */
public class textui {

    /** Verbosity levels. */
    public static final int
        SILENT = 0,
        SUMMARY = 1,
        TESTNAMES = 2,
        MESSAGES = 3;

    /** Milliseconds in one second. */
    private static final double MILLI = 0.001;

    /** Run all tests in CLASSES, reporting results on the standard error
     *  output, depending on VERBOSITY, which determines what is reported.
     *  if VERBOSITY is
     *  <ul>
     *    <li> <code>Silent</code>, prints nothing.
     *    <li> <code>Summary</code>, prints the total test time and
     *         numbers of tests run and failed.
     *    <li> <code>TestNames</code>, as for Summary, plus print names
     *         of failing test methods.
     *    <li> <code>Messages</code>, as for TestNames, and print descriptive
     *         message notating the error, plus its location in the test
     *         routines.
     *  </ul>
     * Returns the number of failed tests. */
    public static int runClasses(int verbosity, Class<?>... classes) {
        Result r = JUnitCore.runClasses(classes);
        int count = r.getRunCount();
        int numFailed = r.getFailureCount();
        if (verbosity <= SILENT) {
            return numFailed;
        }
        System.err.printf("Time: %.3f%n", r.getRunTime() * MILLI);
        if (numFailed > 0 && verbosity > SUMMARY) {
            System.err.printf("There were %d failures:%n%n", numFailed);
            int n;
            n = 1;
            for (Failure f : r.getFailures()) {
                System.err.printf("%d) %s%n", n, f.getTestHeader());
                n += 1;
                if (verbosity <= TESTNAMES) {
                    continue;
                }
                Throwable e;
                e = f.getException();
                if (e instanceof AssertionError) {
                    if (e.getMessage() == null) {
                        System.err.println("    Assertion failed");
                    } else {
                        System.err.printf("    %s%n", e.getMessage());
                        if (e.getMessage().startsWith("Expected exception:")) {
                            break;
                        }
                    }
                } else {
                    if (e.getCause() != null) {
                        e = e.getCause();
                    }
                    System.err.printf("    %s%n", e);
                }
                for (StackTraceElement frame : e.getStackTrace()) {
                    if (frame.getClassName().startsWith("org.junit.")) {
                        continue;
                    }
                    printPosition(frame);
                    if (isStoppingFrame(frame)) {
                        break;
                    }
                }
                System.err.println();
            }
        }
        System.err.printf("Ran %d tests.", count);
        if (numFailed == 0) {
            System.err.println(" All passed.");
        } else {
            System.err.printf(" %d failed.%n", numFailed);
        }
        return numFailed;
    }

    /** Return
     *  {@link #runClasses(int,Class...) runClasses}(Messages, CLASSES). */
    public static int runClasses(Class<?>... classes) {
        return runClasses(MESSAGES, classes);
    }

    /** Return as for
     *  {@link #runClasses(int,Class...) runClasses} (VERBOSITY, CLASSES),
     *  but with the class list stored in a list rather than an array. */
    public static int runClasses(int verbosity, List<Class<?>> classes) {
        return runClasses(verbosity,
                           classes.toArray(new Class<?>[classes.size()]));
    }

    /** Return {@link #runClasses(int,List) runClasses}(MESSAGES, CLASSES). */
    public static int runClasses(List<Class<?>> classes) {
        return runClasses(MESSAGES, classes);
    }

    /** Produces the report described for runClasses, on the classes named in
     *  ARGS.  An initial "--level=NAME" determines the verbosity, which may
     *  be Silent, Summary, TestNames, or Messages.  Default is Messages. */
    public static void main(String... args) {
        int verbosity;
        int k;
        verbosity = MESSAGES;
        k = 0;
        if (args[0].startsWith("--level=")) {
            if (args[0].endsWith("=Silent")) {
                verbosity = SILENT;
            } else if (args[0].endsWith("=Summary")) {
                verbosity = SUMMARY;
            } else if (args[0].endsWith("=TestNames")) {
                verbosity = TESTNAMES;
            } else if (args[0].endsWith("=Messages")) {
                verbosity = MESSAGES;
            } else {
                System.err.printf("Unknown verbosity level: %s%n", args[0]);
                System.exit(1);
            }
            k = 1;
        } else if (args[0].startsWith("--")) {
            System.err.printf("Unknown option: %s%n", args[0]);
            System.exit(1);
        }
        List<Class<?>> classes = new ArrayList<>();
        while (k < args.length) {
            try {
                classes.add(Class.forName(args[k]));
                k += 1;
            } catch (ClassNotFoundException e) {
                System.err.printf("Unknown class: %s%n", args[k]);
                System.exit(1);
            }
        }
        System.exit(runClasses(verbosity, classes));
    }

    /** Print a representation of the source position indicated by FRAME. */
    private static void printPosition(StackTraceElement frame) {
        if (frame.isNativeMethod()) {
            System.err.printf("    at %s.%s (native method)%n",
                               frame.getClassName(),
                               frame.getMethodName());
        } else {
            System.err.printf("    at %s.%s:%d (%s)%n",
                               frame.getClassName(),
                               frame.getMethodName(),
                               frame.getLineNumber(),
                               frame.getFileName());
        }
    }

    /** Return true iff FRAME is positioned on a method with a junit @Test
     *  annotation. */
    private static boolean isStoppingFrame(StackTraceElement frame) {
        if (frame.isNativeMethod()) {
            return false;
        }
        try {
            Class<?> cls = Class.forName(frame.getClassName());
            Method mthd = cls.getMethod(frame.getMethodName());
            return mthd.getAnnotation(Test.class) != null;
        } catch (ClassNotFoundException e) {
            return false;
        } catch (NoSuchMethodException e) {
            return false;
        }
    }
}
