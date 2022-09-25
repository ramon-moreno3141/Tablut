/* Java Test Constructor.
 *
 * This is a program that constructs programs.  Specifically, when invoked
 * with
 *
 *  java MakeTests [ --java-template=<TEMPL1> ] [ --test-template=<TEMPL2> ] \
 *	  [ --output=<JAVAFILE> ] \
 * 	  <TESTNAME>.<ANYTHING>
 * 
 * where <TESTNAME>.<ANYTHING> is the name of a file (<TESTNAME> is a simple
 * name; .<ANYTHING> is an arbitrary extension), MakeTests creates a 
 * program named <JAVAFILE> (default <TESTNAME>.java) from the information 
 * in <TESTNAME>.<ANYTHING> and some template files named 
 * <TEMPL1> (default <TESTNAME>.java.template) and <TEMPL2> (default
 * <TESTNAME>.test.template).
 */

package ucb.test;

import java.util.*;
import java.util.regex.*;
import java.io.*;
import ucb.util.CommandArgs;

public class MakeTests {

  public static void main (String args0[]) {
    CommandArgs args = 
      new CommandArgs ("--output={0,1} " 
		       + "--java-template={0,1} " 
		       + "--test-template={0,1} "
		       + "--={1}",
		       args0);
    if (! args.ok ()) 
      Usage ();

    String testNameFile = args.getLast ("--");
    Matcher m = Pattern.compile ("(.*?)(\\..*)?").matcher (testNameFile);
    m.matches ();
    String testName = m.group (1);

    String outputFileName = 
      args.containsKey ("--output") ? args.getLast ("--output") 
      : testName + ".java";
    String fileTemplateName = 
      args.containsKey ("--java-template") ? args.getLast ("--java-template") 
      : testName + ".java.template";
    String testTemplateName =
      args.containsKey ("--test-template") ? args.getLast ("--test-template") 
      : testName + ".test.template";

    String testBodies = getFileContents (testNameFile);
    String fileTemplate = 
      getFileContents (fileTemplateName)
      .replace ("%% CLASS %%", testName);
    String testTemplate = stripComments (getFileContents (testTemplateName));

    Matcher matCommands = 
      Pattern.compile ("(?sm)(.*?)((?:^\\s*TEST:.*)?)").matcher (testBodies);
    
    matCommands.matches ();

    String decls = matCommands.group (1);
    String body = makeTestBody (testTemplate, matCommands.group (2));

    Writer out;
    try {
      out = new FileWriter (outputFileName);
      out.write (makeTestFile (fileTemplate, body, decls));
      System.err.printf ("Wrote %s.java%n", testName);
      out.close ();
    } catch (IOException e) {
      System.err.printf ("Error: Could not write %s%n", outputFileName);
      System.exit (1);
    }
  }

  static final Pattern EVERYTHING = Pattern.compile ("(?s).*");

  /** The entire contents of the file named FILENAME.  Terminates the
   *  program if the file does not exist. */
  static String getFileContents (String fileName) {
    try {
      Scanner inp = new Scanner (new FileReader (fileName));
      String result = inp.findWithinHorizon (EVERYTHING, 0);
      inp.close ();
      return result;
    } catch (FileNotFoundException e) {
      System.err.printf ("Error: could not find %s%n", fileName);
      System.exit (1);
    }
    return null;  // Not reached
  }

  /** Returns S with all lines containing nothing but a '//'-style
   *  comment removed. */
  static String stripComments (String s) {
    return s.replaceAll ("(?m)^\\s*//.*\n", "");
  }
  
  /** A string constructed by concatenating a modified copy of TEMPLATE
   *  for each test described in BODIES.  BODIES consists of a sequence of
   *  test groups, each starting with a line of the form
   *     TEST: <description>
   *     <body>
   *  (with TEST at the left margin).  Lines after TEST that start with
   *  whitespace constitute the "body" of the test.  <description> is
   *  substituted for instances of '%% NAME %%' in TEMPLATE, and <body>
   *  is substituted for '%% STATEMENTS %%'. */
  static String makeTestBody (String template, String bodies) {
    StringBuffer result = new StringBuffer ();
    Matcher testMatcher = 
      Pattern.compile ("(?m:^TEST):\\s*(.*)\n(((\\p{Blank}.*)?\n)*)").matcher (bodies);

    while (testMatcher.find ()) {
      result.append (template.replace ("%% DESCRIPTION %%", 
				       testMatcher.group (1)
				       .replace ("\"","\\\""))
		     .replace ("%% STATEMENTS %%", 
			       testMatcher.group (2)));
    }

    return result.toString ();
  }

  /** The string TEMPLATE with BODY substituted for '%% STATEMENTS %%' 
   *  and DECLS substituted for '%% DECLS %%' */
  static String makeTestFile (String template, String body, String decls) {
    return template
      .replace ("%% STATEMENTS %%", body)
      .replace ("%% DECLS %%", decls);
  }

  static void Usage () {
    System.out.printf ("Usage:%n"
		       + "    java MakeTests [ --java-template=<TEMPL1> ] [ --test-template=<TEMPL2> ] \\%n"
		       + "                   [ --output=<JAVAFILE> ] \\%n"
		       + "                   <TESTNAME>.<ANYTHING>%n");
    System.exit (1);
  }
}

