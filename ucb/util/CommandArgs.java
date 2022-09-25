/* Standard package for processing command-line options. */

package ucb.util;

import java.util.ArrayList;
import java.util.List;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.NoSuchElementException;

import java.util.regex.Pattern;
import java.util.regex.Matcher;

/** A CommandArgs object is a mapping from option keys to values that
 *  interprets the command-line options to a main program.  It
 *  expects such arguments to conform to Sun's standard guidelines,
 *  according to which, a command (issued to a shell) has the
 *  following general form:
 *  <pre>
 *     COMMAND [ OPTION ... ] [ -- ] [ OTHER_ARGUMENT ... ]
 *  </pre>
 *  ([]'s indicate optional parts; ... indicates one or more).  Each
 *  OPTION has one of the following forms (x, y, etc. denote
 *  non-blank characters):
 *  <pre>
 *  Single parameterless short option:
 *     -x
 *
 *  Several parameterless short options:
 *     -xyz...
 *
 *  Single short option with parameter:
 *     -x OPTARG
     or
 *     -xOPTARG
 *
 *  Long parameterless option:
 *     --opt
 *
 *  Long option with parameter:
 *     --opt=foo
 *  </pre>
 *  If a short option takes an additional argument, that argument is
 *  always required to follow; it cannot be omitted.  When a long argument
 *  takes an argument, it is optional.
 *      <p>
 *  The '--' before the first OTHER_ARGUMENT is optional unless that
 *  OTHER_ARGUMENT starts with '-'.
 *      <p>
 *  One creates a CommandArgs object by supplying a String describing
 *  the legal options, and an array  of command-line argument strings (as
 *  sent to the main function).
 *     <p>
 *  The CommandArgs object then parses the command-line arguments according
 *  to the specification, and presents the options and other arguments
 *  as a mapping between option keys (like "--opt" or "-x") to lists of
 *  argument values supplied for that option in the command-line arguments
 *  (these are lists because in general, an option can appear several times).
 *  Options that take no arguments get the argument value "".  Trailing
 *  arguments correspond to the option key "--".
 *     <p>
 *  Any short option is considered equivalent to a one-character long
 *  option, and vice-versa.
 *     <p>
 *  For example, suppose that we have a program whose usage is
 *  <pre>
 *     foo [ -c ] [ -h ] [ -o FILE ] ARG1
 *  </pre>
 *  where []'s indicate optional arguments, and there may be at most
 *  one -o argument.  It's main program would begin
 *  <pre>
 *    import ucb.util.CommandArgs;
 *    class foo {
 *      public static void main(String[] args0) {
 *         boolean cOptionSpecified;
 *         boolean hOptionSpecified;
 *         String oOptionValue;
 *         List&lt;String&gt; arg1;
 *         CommandArgs args =
 *            new CommandArgs("-c -h -o={0,1} --={1}", args0);
 *         if (! args.ok())
 *            ERROR();
 *         cOptionSpecified = args.contains("-c");
 *         hOptionSpecified = args.contains("-h");
 *         oOptionValue = args.getLast("-o"); // null if absent.
 *         arg1 = args.getFirst("--");
 *         ...
 *   </pre>
 *     <p>
 *   For a program whose usage is
 *   <pre>
 *      bar [ -c ] [ -k COUNT ] [ --dry-run ] [ --form=NAME ] [ ARG ... ]
 *   </pre>
 *   where there may be at most one -k option (which must be an integer),
 *   any number of --form options, and zero or more trailing arguments, we
 *   could write:
 *   <pre>
 *    import ucb.util.CommandArgs;
 *    class foo {
 *      public static void main(String[] args0) {
 *         ...
 *         String options = "-c -k=(\\d+){0,1} --dry-run --form="
 *                          + "--={0,}";
;
 *         CommandArgs args = new CommandArgs(options, args0);
 *         ...
 *
 *         int count;
 *         if (args.contains("-k"))
 *            count = args.getInt("-k");
 *         List&lt;String&gt; forms = args.get("--form");
 *         List&lt;String&gt; otherArgs = args.get("--");
 *    </pre>
 *    <p>
 *    One can group options into mutually exclusive choices using a trailing
 *    ":N" label, where N is a numeral identifying the group.
 *    Here is an example in which there must be exactly one
 *    occurrence of either the option -i, -q, or -l, an optional occurrence
 *    of either of the mutually-exclusive options -n or -N, up to
 *    3 occurrences of options -a and -b in any combination, and no
 *    trailing arguments:
 *
 *    <pre>
 *    import ucb.util.CommandArgs;
 *    class foo {
 *      public static void main(String[] args0) {
 *         ...
 *         String options = "-c{1}:1 -q:1 -l:1 -n{0,1}:2 -N:2 -a={0,3}:3 -b=:3";
 *         CommandArgs args = new CommandArgs(options, args0);
 *         ...
 *    </pre>
 *
 *    <p>
 *    By default, when an option has a value (indicated by = after the option
 *    key), that value may be any string.  You may also describe argument
 *    values with general patterns in parentheses, using the
 *    regular-expression patterns provided by the
 *    {@link java.util.regex.Pattern} class.  For example, writing
 *
 *    <pre>
 *    CommandArgs args =
 *      new CommandArgs("--flavor=(van(illa)?|choc(olate)?)", args0)
 *    </pre>
 *
 *    specifies any number of --flavor parameters, each of which may be
 *    either 'vanilla' ('van' for short) or 'chocolate' ('choc' for short).
 *
 * <b>Option descriptors</b><br>
 *    The option string that describes possible options consists of a sequence
 *    of option descriptors, separated by whitespace.  The syntax of an
 *    option string is as follows:
 *
 *    <pre>
 *       &lt;option string&gt; ::= &lt;options&gt; &lt;trailing&gt;
 *             | &lt;options&gt; | &lt;trailing&gt;
 *       &lt;options&gt; ::= &lt;option&gt; | &lt;options&gt; option&gt;
 *       &lt;option&gt; ::= &lt;option pattern&gt;
 *             | &lt;option pattern&gt;&lt;repeat&gt;
 *       &lt;option pattern&gt; ::= &lt;simple pattern&gt;
 *             | (&lt;simple patterns&gt;)
 *       &lt;simple pattern&gt; ::=
 *               &lt;option key&gt;
 *             | &lt;option key&gt;=&lt;pattern&gt;
 *       &lt;option key&gt; ::=
 *                -&lt;single graphic character other than -&gt;
 *             | --&lt;graphic characters other than = not starting with -&gt;
 *       &lt;simple patterns&gt; ::=
 *               &lt;simple pattern&gt;
 *             | &lt;simple patterns&gt; `|' &lt;simple pattern&gt;
 *       &lt;repeat&gt; ::=
 *               &lt;count&gt; | &lt;count&gt; &lt;label&gt; | &lt;label&gt;
 *       &lt;count&gt; ::=
 *               {&lt;integer&gt;}
 *             | {&lt;integer&gt;,&lt;integer&gt;}
 *             | {&lt;integer&gt;,}
 *       &lt;label&gt; ::= : &lt;positive integer&gt;
 *       &lt;trailing&gt; ::= --=&lt;pattern&gt;
 *             | --=&lt;pattern&gt;&lt;repeat&gt;
 *       &lt;pattern&gt; ::= &lt;empty&gt; | (&lt;regular expression&gt;)
 *     </pre>
 *
 *   &lt;regular expression&gt; is as described in the documentation
 *   for {@link java.util.regex.Pattern}.  The default is `.+' (any
 *   non-empty string).
 *
 *   <p>
 *   A &lt;repeat&gt; clause limits the number of instances of a given
 *   option or trailing argument.  When unspecified, it is "zero or more"
 *   ({0,}).  A trailing &lt;label&gt; indicates a group of options that
 *   are mutually exclusive.  The count that appears on the first option
 *   specification of the group applies to all (subsequent options should
 *   specify just the label part, not the { } part).  At most one of the keys
 *   in any group may appear.  The count applies to whichever one does.
 *   <p>
 *   No &lt;option&gt; may contain whitespace.  Also, be careful of the
 *   usual escaping problems with representing regular expressions as
 *   java Strings.  The regular expression \d, for example, is written as
 *   the String literal "\\d".
 *   @author P. N. Hilfinger
 */

public class CommandArgs implements Iterable<String> {

    /** Hexadecimal base. */
    private static final int HEX = 16;

    /** A set of argument values extracted from RAWARGS according to
     *  the description given in OPTIONSTRING.  OPTIONSTRING is defined
     *  in the class documentation for this class (see above).  RAWARGS
     *  is typically the array of arguments passed to the main procedure.
     *
     *  Throws IllegalArgumentException if OPTIONSTRING does not conform
     *  to the syntax above.
     *
     *  Throws PatternSyntaxException if a regular expression in OPTIONSTRING
     *  has invalid format.
     */
    public CommandArgs(String optionString, String[] rawArgs) {
        _optionString = optionString;
        _arguments = rawArgs;
        _ok = true;
        ArrayList<OptionSpec> specs = OptionSpec.parse(optionString);
        createRawOptionLists(rawArgs, specs);
        checkOptions(specs);
    }

    /** Return the option string with which THIS was created. */
    public String getOptionString() {
        return _optionString;
    }

    /** Return the argument array (not a copy) with which THIS was created. */
    public String[] getArguments() {
        return _arguments;
    }

    /** Return the number of occurrences of option key KEY. */
    public int number(String key) {
        int result;
        result = 0;
        for (String k : _keyList) {
            if (k.equals(key)) {
                result += 1;
            }
        }
        return result;
    }

    /** Returns true iff option KEY is present.
     */
    public boolean contains(String key) {
        return _keyList.contains(key);
    }

    /** Returns true iff option KEY is present.
     *  @deprecated use {@link #contains(String)} instead.
     */
    @Deprecated
    public boolean containsKey(String key) {
        return contains(key);
    }

    /** Return the list of all argument values for option KEY, or null if
     *  the option does not appear.  If an option appears, but does
     *  not take a value, the values in the list will all be "". */
    public List<String> get(String key) {
        ArrayList<String> result = new ArrayList<>();
        for (int k = 0; k < _keyList.size(); k += 1) {
            if (_keyList.get(k).equals(key)) {
                result.add(valueList.get(k));
            }
        }
        return result;
    }

    /** The argument value of the first occurrence of option KEY, or null
     *  if there is no occurrence, or it has the wrong format. If an
     *  option appears, but does not take a value, returns "". */
    public String getFirst(String key) {
        return getFirst(key, null);
    }

    /** The argument value of the first occurrence of option KEY, or DFLT
     *  if there is no occurrence, or it has the wrong format. If an
     *  option appears, but does not take a value, returns "". */
    public String getFirst(String key, String dflt) {
        int k = _keyList.indexOf(key);
        if (k == -1) {
            return dflt;
        } else {
            return valueList.get(k);
        }
    }

    /** The argument value of the last occurrence of option KEY, or DFLT
     *  if there is no occurrence, or it has the wrong format. If an
     *  option appears, but does not take a value, returns "". */
    public String getLast(String key, String dflt) {
        int k = _keyList.lastIndexOf(key);
        if (k == -1) {
            return dflt;
        } else {
            return valueList.get(k);
        }
    }

    /** The argument value of the last occurrence of option KEY, or null
     *  if there is no occurrence, or it has the wrong format. If an
     *  option appears, but does not take a value, returns "". */
    public String getLast(String key) {
        return getLast(key, null);
    }

    /** Return The value of the last occurrence of option KEY, as a
     *  decimal integer.  Exception if there is no such option, or it
     *  does not have the format of an integer.

     *  Throws NumberFormatException if the value of option KEY
     *  is not a decimal numeral in the range of type int.
     */
    public int getInt(String key) {
        return getInt(key, 10);
    }

    /** Return The value of the last occurrence of option KEY, as an integer of
     *  given RADIX. Exception if there is no such option, or it does not
     *  have the format of an integer.  Hexadecimal integers may have
     *  a leading '0x', which is ignored.
     *
     *  Throws NumberFormatException if the value of option KEY is not
     *  a valid numeral of radix RADIX, RADIX is not a valid RADIX, or
     *  the number if out of range. */
    public int getInt(String key, int radix) {
        if (!contains(key)) {
            throw new NoSuchElementException(key);
        }
        return getInt(key, radix, 0);
    }

    /** Return the value of the last occurrence of option KEY, as an integer of
     *  given RADIX, or DFLT if KEY is not present. Exception if value does not
     *  have the format of an integer.  Hexadecimal integers may have
     *  a leading '0x', which is ignored.
     *  @throws NumberFormatException if the value of option KEY is not
     *  a valid numeral of radix RADIX, RADIX is not a valid RADIX, or
     *  the number if out of range. */
    public int getInt(String key, int radix, int dflt) {
        String val = getLast(key);
        if (val == null) {
            return dflt;
        }
        if (radix == HEX && val != null && val.startsWith("0x")) {
            return Integer.parseInt(val.substring(2), HEX);
        } else if (radix == HEX && val != null && val.startsWith("-0x")) {
            return Integer.parseInt("-" + val.substring(3), HEX);
        } else {
            return Integer.parseInt(val, radix);
        }
    }

    /** Return the value of the last occurrence of option KEY, as a
     *  decimal integer. Exception if there is no such option, or it
     *  does not have the format of an integer.
     *
     *  Throws NumberFormatException if the value of option KEY is
     *  not a decimal numeral in the range of long. */
    public long getLong(String key) {
        return getLong(key, 10);
    }

    /** Return the value of the last occurrence of option KEY, as an integer of
     *  given RADIX. Exception if there is no such option, or it does not
     *  have the format of an integer. Hexadecimal integers may have
     *  a leading '0x', which is ignored.
     *  Throws NumberFormatException if the value of option KEY is not
     *  a valid numeral of radix RADIX, RADIX is not a valid RADIX, or
     *  the result is not in the range of long. */
    public long getLong(String key, int radix) {
        if (!contains(key)) {
            throw new NoSuchElementException(key);
        }
        return getLong(key, radix, 0L);
    }

    /** Return the value of the last occurrence of option KEY, as an integer of
     *  given RADIX, or DFLT if KEY is not present. Exception if value
     *  does not have the format of a long integer. Hexadecimal integers may
     *  have a leading '0x', which is ignored.
     *  @throws NumberFormatException if the value of option KEY is not
     *  a valid numeral of radix RADIX, RADIX is not a valid RADIX, or
     *  the result is not in the range of long. */
    public long getLong(String key, int radix, long dflt) {
        String val = getLast(key);
        if (val == null) {
            return dflt;
        }
        if (radix == HEX && val != null && val.startsWith("0x")) {
            return Long.parseLong(val.substring(2), HEX);
        } else if (radix == HEX && val != null && val.startsWith("-0x")) {
            return Long.parseLong("-" + val.substring(3), HEX);
        } else {
            return Long.parseLong(val, radix);
        }
    }

    /** Return the value of the last occurrence of option KEY, as
     *  a floating-point value.  Exception if there is no such option,
     *  or it does not have the proper format.
     *
     *  Throws NumberFormatException if the value of option KEY
     *  is not a proper floating-point numeral. */
    public double getDouble(String key) {
        return Double.parseDouble(getLast(key));
    }

    /** REturn the value of the last occurrence of option KEY, as
     *  a floating-point value, or DFLT if KEY is not present.  Exception
     *  if value does not have the proper format.
     *
     *  Throws NumberFormatException if the value of option KEY
     *  is not a proper floating-point numeral. */
    public double getDouble(String key, double dflt) {
        String val = getLast(key);
        if (val == null) {
            return dflt;
        }
        return Double.parseDouble(val);
    }

    /** Return true iff all arguments were correct. */
    public boolean ok() {
        return _ok;
    }

    /** Return a list of all keys that appeared in the arguments, in order of
     *  appearance.  Trailing arguments are marked with the key "--".
     *  Invalid keys are not represented. */
    public List<String> optionKeys() {
        return _keyList;
    }

    /** Return A list of all option values that appeared in the arguments,
     *  in order of appearance.  Trailing arguments appear at the end.  Options
     *  that don't take values or are given a value of "", as are some
     *  options that are supplied incorrectly.  The order and number of the
     *  elements corresponds to the result of optionKeys(). */
    public List<String> optionValues() {
        return valueList;
    }

    /** Return An iterator over the set of keys in optionKeys(). That is,
     *  each key appears exactly once in some order.  Use {@link #get(String)},
     *  {@link #getFirst(String)}, etc. to retrieve the value(s)
     *  of an option. */
    @Override
    public Iterator<String> iterator() {
        return new HashSet<String>(_keyList).iterator();
    }

    /* Private section */

    /** Saved constructor arguments. */
    private final String _optionString;
    /** Original arguments supplied to the constructor. */
    private final String[] _arguments;

    /** Flag indicating whether any error has been found. */
    private boolean _ok;

    /** Value of optionKeys(). */
    private ArrayList<String> _keyList = new ArrayList<String>();
    /** Value of optionValues(). */
    private ArrayList<String> valueList = new ArrayList<String>();

    /** Patterns for parts of option string. */
    private static final String
        SHORT_OPTION = "-[a-zA-Z0-9_#@+%]",
        LONG_OPTION = "--[-_a-zA-Z0-9]*",
        VALUE = "(?:=(\\(.*\\)|))?",
        REPEAT = "(?:\\{(\\d+)(,)?(\\d*)\\}(?::([1-9]\\d*))?|:([1-9]\\d*))?",
        OPTION = "(" + SHORT_OPTION + "|" + LONG_OPTION + ")" + VALUE + REPEAT;
    /** Compiled version of OPTION. */
    private static final Pattern
        /* Group
           1 = <option key>
           2 = <value>
           3,4,5 = <repeat spec>  6 = <label definition>  7 = <label use>
        */
        OPTION_PATTERN = Pattern.compile(OPTION);

    /** Encapsulates a single option specification (e.g., "-o=", "--foo{0,1}",
     *  etc.), plus a list of values supplied for that option.  */
    private static class OptionSpec {
        /** Return an OptionSpec that parses the option described by OPT. */
        static ArrayList<OptionSpec> parse(String opt) {
            ArrayList<OptionSpec> result = new ArrayList<>();
            HashMap<String, OptionSpec> labels = new HashMap<>();
            String[] opts = opt.trim().split("\\s+");
            for (int i = 0; i < opts.length; i += 1) {
                if (opts[i].equals("")) {
                    continue;
                }
                Matcher m = OPTION_PATTERN.matcher(opts[i]);
                if (m.matches()) {
                    OptionSpec spec = new OptionSpec();
                    spec._key = m.group(1);
                    spec._valuePattern =
                        ("").equals(m.group(2)) ? ".+" : m.group(2);
                    spec._primary = spec;
                    if (m.group(7) != null) {
                        spec._primary = labels.get(m.group(7));
                        if (spec._primary == null) {
                            String msg = "undefined label: " + m.group(7);
                            throw new IllegalArgumentException(msg);
                        }
                    } else if (m.group(6) != null) {
                        if (labels.containsKey(m.group(6))) {
                            String msg  = "multiply defined label: "
                                + m.group(6);
                            throw new IllegalArgumentException(msg);
                        } else {
                            labels.put(m.group(6), spec);
                        }
                    }
                    if (m.group(3) != null) {
                        spec._min = Integer.parseInt(m.group(3));
                        if (m.group(4) == null) {
                            spec._max = spec._min;
                        } else if (m.group(5).equals("")) {
                            spec._max = Integer.MAX_VALUE;
                        } else {
                            spec._max = Integer.parseInt(m.group(5));
                        }
                    } else {
                        spec._min = 0;
                        spec._max = Integer.MAX_VALUE;
                    }
                    spec._count = 0;
                    result.add(spec);
                    if (spec._key.equals("--") && i != opts.length - 1) {
                        String msg = "junk at end of option string";
                        throw new IllegalArgumentException(msg);
                    }
                } else {
                    String msg = "bad option specifier: " + opts[i];
                    throw new IllegalArgumentException(msg);
                }
            }
            return result;
        }

        /** Return true iff this OptionSpec handles options whose key is KEY. */
        boolean matches(String key) {
            return key.equals(_key);
        }

        /** Return true iff VAL is a valid value for this option (""
         *  indicates the "value" of an option that does not take a
         *  value). */
        boolean validValue(String val) {
            if (_valuePattern == null) {
                if (!val.equals("")) {
                    return false;
                }
            } else if (!Pattern.matches(_valuePattern, val)) {
                return false;
            }
            _count += 1;
            _primary._groupCount += 1;
            if (_count != _primary._groupCount || _count > _primary._max) {
                return false;
            }
            return true;
        }

        /** Return true if have accumulated at least the minimum number of
         *  values required for this spec. */
        boolean hasMinValues() {
            return _primary._count >= _primary._min;
        }

        /** Return true iff the option described by THIS takes an argument. */
        boolean hasArgument() {
            return _valuePattern != null;
        }

        /** Return my option key. */
        String key() {
            return _key;
        }

        /** The option key. */
        private String _key;
        /** Pattern describing legal values of this option. */
        private String _valuePattern;
        /** Controlling option instance for grouped options. */
        private OptionSpec _primary;
        /** Minimum and maximum allows multiplicities of this option. */
        private int _min, _max;
        /** Number of actual option values in this group and of this specific
         *  option, respectively. */
        private int _groupCount, _count;
    }

    /** Patterns matching parts of option string. */
    private static final Pattern
        ARGUMENT = Pattern.compile("(--)|(--\\S+?)(?:=(.*))?|(-[^-].*)"),
        WS = Pattern.compile("\\s+");

    /** Fill keyList and valueList from ARGS, where OPTIONSPECS is option
     *  string supplied to the constructor, broken into individual
     *  specifications. */
    private void createRawOptionLists(String[] args,
                                      List<OptionSpec> optionSpecs) {
        _keyList.clear();
        valueList.clear();

        int i;
        i = 0;
        while (i < args.length) {
            Matcher m = ARGUMENT.matcher(args[i]);
            if (m.matches()) {
                if (m.group(1) != null) {
                    i += 1;
                    break;
                } else if (m.group(2) != null) {
                    _keyList.add(m.group(2));
                    if (m.group(3) == null) {
                        valueList.add("");
                    } else {
                        valueList.add(m.group(3));
                    }
                } else if (shortOptionWithArg(m.group(4), optionSpecs)) {
                    _keyList.add(m.group(4).substring(0, 2));
                    if (m.group(4).length() > 2) {
                        valueList.add(m.group(4).substring(2));
                    } else {
                        i += 1;
                        if (i == args.length) {
                            _ok = false;
                            valueList.add("");
                        } else {
                            valueList.add(args[i]);
                        }
                    }
                } else {
                    for (int k = 1; k < m.group(4).length(); k += 1) {
                        String key = "-" + m.group(4).charAt(k);
                        _keyList.add(key);
                        if (shortOptionWithArg(key, optionSpecs)) {
                            _ok = false;
                        }
                        valueList.add("");
                    }
                }
                i += 1;
            } else {
                break;
            }
        }
        while (i < args.length) {
            _keyList.add("--");
            valueList.add(args[i]);
            i += 1;
        }
    }

    /** Check all keys and values against SPECS, the list of parsed option
     *  specifications. Sets ok to false if problems are
     *  found. */
    private void checkOptions(ArrayList<OptionSpec> specs) {
    FindSpec:
        for (int i = 0; i < _keyList.size(); i += 1) {
            for (OptionSpec spec : specs) {
                if (spec.matches(_keyList.get(i))) {
                    _ok &= spec.validValue(valueList.get(i));
                    continue FindSpec;
                }
            }
            _ok = false;
        }

        for (OptionSpec spec : specs) {
            _ok &= spec.hasMinValues();
        }
    }

    /** Return true iff, according to SPECS, OPT is a short option that
     *  takes an argument. */
    private static boolean shortOptionWithArg(String opt,
                                              List<OptionSpec> specs) {
        if (opt.startsWith("--") || !opt.startsWith("-")
            || opt.length() < 2) {
            return false;
        }
        opt = opt.substring(0, 2);
        for (int i = 0; i < specs.size(); i += 1) {
            if (specs.get(i).matches(opt)) {
                return specs.get(i).hasArgument();
            }
        }
        return false;
    }



}
