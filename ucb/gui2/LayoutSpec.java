package ucb.gui2;

import java.awt.GridBagConstraints;
import java.awt.Insets;
import static java.awt.GridBagConstraints.*;

import java.util.HashSet;
import java.util.HashMap;
import static java.util.Collections.*;

/** A LayoutSpec specifies how an item is to be laid out in a
 *  TopLevel or other graphical container (here, we'll refer to such things
 *  collectively as <dfn>containers</dfn>).  It corresponds to
 *  the class {@link java.awt.GridBagConstraints} from the usual Java AWT
 *  framework.  That is, the container is considered to be divided
 *  into rows of equal numbers of grid cells.  The cells in any given
 *  column have the same width and those in any given row have the same height.
 *  The system is responsible for figuring out what these widths and heights
 *  should be, based on the sizes of the components and the layout parameters
 *  supplied by this class.  For any component that you add to a container,
 *  you supply a LayoutSpec that indicates what grid cells the component is
 *  to occupy, any
 *  padding added to the component, and the <dfn>insets</dfn>&mdash;the
 *  space left between the
 *  component and the sides of its grid cells.
 *
 *  <p> You supply parameters to a
 *  LayoutSpec are specified as arrays
 *  of values:  NAME1, VALUE1, NAME2, VALUE2, where the NAMEs denote
 *  particular parameters, and the VALUEs give their values.
 *  Unmentioned parameters have default values.  Here are the possible
 *  NAMES and the expected values:
 *  <table border="1" align="center">
 *  <tr> <th width=100> Parameter name </th> <th width=100> Parameter type </th>
 *       <th width=500> Description </th> </tr>
 *  <tr> <td> width </td> <td> Integer </td>
 *       <td> The number of grid cells (>=1) occupied by this
 *            component horizontally.
 *            May also have the string value "rest" or "remainder", which
 *            indicates that it occupies all remaining grid cells in its
 *            row(s), thus completing the row(s). </td>
 *  </tr>
 *  <tr> <td> height </td> <td> Integer </td>
 *       <td> The number of grid cells occupied by this component vertically.
 *            May also have the string value "rest" or "remainder", which
 *            indicates that it occupies all remaining grid cells in its
 *            columns(s), thus completing the column(s). </td>
 *  </tr>
 *  <tr> <td colspan=3> By default, a component gets placed in the rightmost
 *                      grid cell of the topmost incomplete row.  By specifying
 *                      x and y grid coordinates (top, leftmost cell is x=0,
 *                      y=0), you can place components at arbitrary places.
 *        </td>
 *  </tr>
 *  <tr> <td> x </td> <td> Integer </td>
 *       <td> The column number of the upper-left corner of the component,
 *            where column 0 is leftmost. </td>
 *  </tr>
 *  <tr> <td> y </td> <td> Integer </td>
 *       <td> The row number of the upper-left corner of the component,
 *            where row 0 is topmost. </td>
 *  </tr>
 *  <tr> <td colspan=3> If a component's preferred size does not fill the area
 *            provided in the grid (which is determined by the sizes of other
 *            components in the grid and the size of the entire container),
 *            the component may be expanded to fill the area, or placed at a
 *            particular position within the area, as determined by the
 *            next two parameters. </td>
 </tr>
 *  <tr> <td> anchor </td> <td> String </td>
 *       <td> If the component does not fill the grid cells it occupies, this
 *            parameter indicates where it should go.  Possible values are
 *            "center" (default), "north" (top), "south", "west", "east",
 *            "southwest", "southeast", "northwest", and "northeast". </td>
 *  </tr>
 *  <tr> <td> fill </td> <td> String </td>
 *       <td>  If the component does not fill the grid cells it occupies, this
 *            parameter indicates how it should be expanded.  The default
 *            is no expansion.  Possible values are
 *            "horizontal" (or "horiz"), "vertical" (or "vert"), and
 *            "both", indicating whether the component is to expand in the
 *            horizontal direction, vertical direction, or both. </td>
 *  </tr>
 *  <tr> <td colspan=3> The following parameters specify for minimal space
 *            between a component and the edges of its grid cell. </td>
 *  </tr>
 *  <tr> <td> ileft </td> <td> Integer </td>
 *       <td> Space (in pixels) to be left between component's left side and
 *            its grid cell. </td>
 *  </tr>
 *  <tr> <td> iright </td> <td> Integer </td>
 *       <td> Space (in pixels) to be left between component's right side and
 *            its grid cell. </td>
 *  </tr>
 *  <tr> <td> itop </td> <td> Integer </td>
 *       <td> Space (in pixels) to be left between component's top side and
 *            its grid cell. </td>
 *  </tr>
 *  <tr> <td> ibottom </td> <td> Integer </td>
 *       <td> Space (in pixels) to be left between component's bottom side and
 *            its grid cell. </td>
 *  </tr>
 *  <tr> <td colspan=3> The following parameters specify the relative weights
 *            given to components for distribution of extra (or reduced)
 *            space resulting from resizing.   When set to 0, the component
 *            requests no extra space on being resized.  Default is 1.0. </td>
 *  </tr>
 *  <tr> <td> weightx </td> <td> Double </td>
 *        <td> Weight for receiving more (less) space from resizing in the
 *             horizontal direction. </td>
 *  </tr>
 *  <tr> <td> weighty </td> <td> Double </td>
 *        <td> Weight for receiving more (less) space from resizing in the
 *             horizontal direction. </td>
 *  </tr>
 *  </table>
 *  @author P. N. Hilfinger
 */

public class LayoutSpec {

    /** A new LayoutSpec, initialized with the parameters SPECS, as described
     *  in the class comment for LayoutSpec. */
    public LayoutSpec(Object... specs) {
        _params.weightx = 1.0;
        _params.weighty = 1.0;
        _params.insets = new Insets(0, 0, 0, 0);
        add(specs);
    }

    /** Modify THIS by setting the parameters indicated by SPECS, which has the
     *  same form as described in the class comment for LayoutSpec. */
    public void add(Object... specs) {
        if (specs.length % 2 == 1) {
            throw new IllegalArgumentException("Missing last value");
        }
        for (int i = 0; i < specs.length; i += 2) {
            if (!(specs[i] instanceof String)
                || !ALL_SPECS.contains(specs[i])) {
                throw new IllegalArgumentException("Illegal LayoutSpec key: "
                                                   + specs[i]);
            } else if (!(specs[i + 1] instanceof Integer
                         || specs[i + 1] instanceof Double
                         || specs[i + 1] instanceof String)) {
                throw new IllegalArgumentException("Illegal value for"
                                                   + specs[i] + " key");
            }
        }

        for (int i = 0; i < specs.length; i += 2) {
            Object key = specs[i];
            Object val = specs[i + 1];
            addKey(key, val);
        }
    }

    /** Return my constraints. */
    public GridBagConstraints params() {
        return _params;
    }

    /** Add KEY: VAL pair to current constraints. */
    private void addKey(Object key, Object val) {
        switch (key.toString()) {
        case "x":
            _params.gridx = toInt(val);
            break;
        case "y":
            _params.gridy = toInt(val);
            break;
        case "width":
            _params.gridwidth = toInt(val);
            break;
        case "ht":
            _params.gridheight = toInt(val);
            break;
        case "anchor":
            _params.anchor = toInt(val);
            break;
        case "ileft":
            _params.insets.left = toInt(val);
            break;
        case "iright":
            _params.insets.right = toInt(val);
            break;
        case "itop":
            _params.insets.top = toInt(val);
            break;
        case "ibottom":
            _params.insets.bottom = toInt(val);
            break;
        case "fill":
            _params.fill = toInt(val);
            break;
        case "weightx":
            _params.weightx = toDouble(val);
            break;
        case "weighty":
            _params.weighty = toDouble(val);
            break;
        default:
            break;
        }
    }

    /** Return parameter value X as an int. */
    private int toInt(Object x) {
        if (x instanceof Integer) {
            return (Integer) x;
        } else if (x instanceof Double) {
            return (int) (double) (Double) x;
        } else if (!(x instanceof String)) {
            return -1;
        }
        x = ((String) x).toLowerCase();
        if (INT_NAMES.containsKey(x)) {
            return INT_NAMES.get(x);
        } else {
            return -1;
        }
    }

    /** Return parameter value X as a double. */
    private double toDouble(Object x) {
        if (x instanceof Double) {
            return (Double) x;
        } else {
            return toInt(x);
        }
    }

    /** The set of all valid specification keys. */
    private static final HashSet<String> ALL_SPECS =
        new HashSet<String>(
            java.util.Arrays.<String>asList("x", "y",
                                            "fill",
                                            "height", "ht", "width", "wid",
                                            "anchor",
                                            "weightx", "weighty",
                                            "ileft", "iright",
                                            "itop", "ibottom"));

    /** Mapping of keys to integer values. */
    private static final HashMap<Object, Integer> INT_NAMES;

    /** Initial values for INT_NAMES. */
    private static final Object[][] INT_NAMES_INIT = {
        {"center", CENTER},
        {"north", NORTH},
        {"south", SOUTH},
        {"east", EAST},
        {"west", WEST},
        {"southwest", SOUTHWEST},
        {"southeast", SOUTHEAST},
        {"northwest", NORTHWEST},
        {"northeast", NORTHEAST},
        {"remainder", REMAINDER},
        {"rest", REMAINDER },
        {"horizontal", HORIZONTAL},
        {"horiz", HORIZONTAL},
        {"vertical", VERTICAL},
        {"vert", VERTICAL},
        {"both", BOTH}
    };

    static {
        INT_NAMES = new HashMap<Object, Integer>();
        for (Object[] pair : INT_NAMES_INIT) {
            INT_NAMES.put(pair[0], (Integer) pair[1]);
        }
    }

    /** The constraints denoted by this spec. */
    private final GridBagConstraints _params = new GridBagConstraints();

}
