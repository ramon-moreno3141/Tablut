/** Provides the classes needed to implement a simple graphical user
 *  interface, containing buttons, a menu bar, simple canvases
 *  allowing arbitrary graphics and responding to the mouse or
 *  keyboard, and modal dialogs and messages.
 *
 *  The class TopLevel provides top-level displayable
 *  windows. Typically, you will extend this class and use its
 *  constructor for your extension to set up menus, buttons, etc., and
 *  to arrange the necessary callbacks that happen when these items
 *  are clicked.   Your main program creates a new instance of the
 *  extended class, and uses its display method to make it appear.
 *
 *  The class TopLevel allows you to add Widgets (or rather, objects
 *  that extend this class).  The only Widget currently provided is
 *  the Pad, which is by default a blank area the is sensitive to
 *  mouse and keyboard actions.  Typically, you will extend Pad to
 *  provide whatever functionality you want.
 *
 *  The class LayoutSpec encapsulates information needed to place
 *  items into a TopLevel.   You provide either a LayoutSpec or a set of
 *  arguments for its constructor whenever you add something to the TopLevel.
 */
package ucb.gui;
