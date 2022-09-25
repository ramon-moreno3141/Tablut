package ucb.test;
import java.util.*;
import java.io.*;
import java.util.regex.*;

import ucb.util.CommandArgs;

public class TimeLimitedTests {

    static int testsCompleted, wrongSoFar, badSoFar;
    static boolean timeOut;
    static StringBuffer processOutput = new StringBuffer ();

    /** Usage: java ucb.test.TimeLimitedTests LIMIT TESTCLASS
     *  where LIMIT is in seconds. */
    public static void main (String[] args0) {
        CommandArgs args = new CommandArgs ("-v -t=(\\d+) --={1}", args0);

        if (! args.ok ()) {
            System.err.println ("Usage: java ucb.test.TimeLimitedTests [-v] [-t N] CLASS");
            System.exit (2);
        }

        int limit = args.containsKey ("-t") ? args.getInt ("-t") : 15;
        String testClass = args.getLast ("--");

        testsCompleted = wrongSoFar = badSoFar = 0;

        try {
            do {
                final Process tester = 
                    new ProcessBuilder ()
                    .command ("java", testClass, "" + args.containsKey ("-v"), 
                              ""  + testsCompleted, "" + wrongSoFar, "" + badSoFar)
                    .redirectErrorStream (true)
                    .start ();
      
                Thread outputCollector = new Thread () {
                        public void run () {
                            InputStream testerOutput = tester.getInputStream ();
                            try {
                                while (true) {
                                    int c = testerOutput.read ();
                                    if (c == -1)
                                        break;
                                    processOutput.append ((char) c);
                                }
                                testerOutput.close ();
                            } catch (IOException e) {
                            }
                        }
                    };

                processOutput.setLength (0);
                outputCollector.start ();
                timeOut = false;
                Timer execTimer = new Timer (true);
                execTimer.schedule
                    (new TimerTask () { 
                            public void run () {
                                timeOut = true;
                                tester.destroy ();
                            }
                        }, limit*1000);
      
                try {
                    tester.waitFor ();
                    if (tester.exitValue() != 0) {
                        testsCompleted += 1;
                        badSoFar += 1;
                    }
                } catch (InterruptedException e) {
                    tester.destroy ();
                    System.err.printf ("Testing interrupted for unknown reason.%n");
                    System.err.flush ();
                }
                try {
                    outputCollector.join ();
                } catch (InterruptedException e) { 
                }

                execTimer.cancel ();
                execTimer.purge ();

                interpretOutput (processOutput.toString (), timeOut);
            } while (timeOut);
        } catch (IOException e) {
            System.err.printf ("Unexpected I/O error starting tester: %s%n"
                               + "WARNING: Some tests may not have run.%n",
                               e);
            if (testsCompleted == 0) 
                System.exit (1);
            else
                badSoFar += 1;
        }
        if (wrongSoFar + badSoFar == 0) {
            System.err.printf ("%nAll %d tests passed.%n", testsCompleted);
            System.exit (0);
        } else {
            System.err.printf ("%n%nOut of %d tests run:%n    %d passes.%n"
                               + "    %d wrong results.%n"
                               + "    %d failed executions.%n",
                               testsCompleted, 
                               testsCompleted - wrongSoFar - badSoFar,
                               wrongSoFar, badSoFar);
            System.exit (1);
        }
    }

    static final Pattern testResult = 
        Pattern.compile ("^Test #(\\d+).*?(PASSED|FAILED|ERROR)$", 
                         Pattern.MULTILINE | Pattern.DOTALL),
        truncatedTestResult 
        = Pattern.compile ("^<<<(.*)>>>$", Pattern.MULTILINE);

    static void interpretOutput (String out, boolean timeOut) {
        System.err.print (out.replaceAll ("(?m)^<<<.*>>>\r?\n", ""));
        Scanner output = new Scanner (new StringReader (out));
        while (output.findWithinHorizon (testResult, 0) != null) {
            testsCompleted = 
                Math.max (Integer.parseInt (output.match ().group (1)),
                          testsCompleted + 1);
            String outcome = output.match ().group (2);
            if (outcome.equals ("ERROR"))
                wrongSoFar += 1;
            else if (outcome.equals ("FAILED"))
                badSoFar += 1;
        }
        if (timeOut) {
            if (output.findWithinHorizon (truncatedTestResult, 0) != null)
                System.err.printf ("%nTest #%d: %s TIME LIMIT EXCEEDED%n",
                                   testsCompleted+1, 
                                   output.match ().group (1));
            else
                System.err.printf ("%nTest #%d:  TIME LIMIT EXCEEDED%n", 
                                   testsCompleted+1);
            testsCompleted += 1;
            badSoFar += 1;
        }
    }
}

