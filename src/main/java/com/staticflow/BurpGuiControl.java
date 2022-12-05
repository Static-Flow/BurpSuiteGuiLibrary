package main.java.com.staticflow;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;

/**
This Library provides a set of tools for easily manipulating the Swing Components that comprise Burp Suite's UI.
 */
public final class BurpGuiControl {

    /**
     * This method returns a Component at a specified walk from a starting Component.
     * For example, take a JFrame named foo, has a JPanel child bar, which itself has two components a JButton baz and a JTextField buz.
     * The Swing structure would be:
     * <pre>
     * foo (JFrame)
     *  |
     *  |> bar (JPanel)
     *      |
     *      |> baz (JButton)
     *      |> buz (JTextField)
     *  |
     *  |> ... More Components
     * </pre>
     * Then to access it using `getComponentAtPath` would be: <pre>BurpGuiControl.getComponentAtPath(foo, new int[]{0,1})</pre>
     *
     * Note: Components are 0 indexed. The helper method {@link #printChildrenComponents(Container, int, int) printChildrenComponents} can be used
     * to print a formatted Swing tree to easily discovered the needed path to an element
     * @param startingContainer The parent Component to begin walking the user supplied path
     * @param componentChildPath The indexes to follow down the Swing tree to the desired Component
     * @return the Component referenced by the user supplied Swing path
     */
    public static Component getComponentAtPath(Container startingContainer, int[] componentChildPath) {
        Container endingContainer = (Container) startingContainer.getComponent(componentChildPath[0]);
        for(int index = 1; index < componentChildPath.length;index++) {
            endingContainer = (Container) endingContainer.getComponent(componentChildPath[index]);
        }
        return endingContainer;
    }

    /**
     * This method recursively walks the subcomponent tree of a provided Component down to a user supplied level and builds a formatted tree.
     * This allows for targeted use of the other methods in this Library, such as {@link #getComponentAtPath(Container, int[]) getComponentAtPath}
     * which requires knowledge of the direct path to a child component from a given parent.
     * @param component The parent component that will be recursively walked
     * @param level used for the recursive algorithm to track how many levels down the walk has traveled
     * @param maxDepth The lowest depth to search for children. Level 1 would be children only, level 2 would include grandchildren, and so on
     */
    public static String printChildrenComponents(Container component, int level, int maxDepth) {
        StringBuilder prettyPrintSwingTree = new StringBuilder();
        StringBuilder tabs = new StringBuilder();
        for(int i=0;i<level;i++){
            tabs.append("\t");
        }
        int count = 0;
        for (Component child : component.getComponents()){
            prettyPrintSwingTree.append(String.format("%s %d.%d|->%s%n",tabs,level,count,child));
            prettyPrintSwingTree.append(System.lineSeparator());
            count++;
            if (child instanceof Container && level != maxDepth) {
                prettyPrintSwingTree.append(printChildrenComponents((Container) child,level+1, maxDepth));
            }
        }
        return prettyPrintSwingTree.toString();
    }

    /**
     * This method returns the major Swing component of Burp Suite, the JTabbedPane containing the major tabs.
     * @return The JTabbedPane which holds all other Burp Suite components
     */
    public static JTabbedPane getMainTabbedPane(){
        for(Frame frame : Frame.getFrames()) {
            if(frame.isVisible() && frame.getTitle().startsWith(("Burp Suite")))
            {
                return (JTabbedPane) findFirstComponentOfType(frame,JTabbedPane.class);
            }
        }
        return null;
    }

    /**
     * This method recursively searches all children of a source Component for a Component matching the provided Component class or Null if one cannot be found
     * @param parentContainer Starting container to recursively search children
     * @param clazz The java.awt.Component class which a child Component must match
     * @return the first child of the parentContainer that matches the clazz java.awt.Component
     */
    public static Component findFirstComponentOfType(Container parentContainer, Class<? extends java.awt.Component> clazz) {
        for(Component comp : parentContainer.getComponents()) {
            if(clazz.isAssignableFrom(comp.getClass())){
                return comp;
            } else {
                Component result = findFirstComponentOfType((Container)comp, clazz);
                if(result != null && clazz.isAssignableFrom(result.getClass())){
                    return result;
                }
            }
        }
        return null;
    }

    /**
     * This method recursively searches all children of a source Component for all Components matching the provided Component class
     * @param parentContainer Starting container to recursively search children
     * @param clazz The java.awt.Component class which a child Component must match
     * @return all children of the parentContainer that match the clazz java.awt.Component
     */
    public static List<Component> findAllComponentsOfType(Container parentContainer, Class<? extends java.awt.Component> clazz) {
        ArrayList<Component> matchingComponents = new ArrayList<>();
        for(Component comp : parentContainer.getComponents()) {
            if(clazz.isAssignableFrom(comp.getClass())){
                matchingComponents.add(comp);
            } else {
                matchingComponents.addAll(findAllComponentsOfType((Container)comp, clazz));
            }
        }
        return matchingComponents;
    }

    /**
     * Retrieves the Tab component from BurpSuite denoted by the tab name. For instance if you wanted the Repeater tab
     * it would be <pre>getBaseBurpComponent("Repeater")</pre>. This also works for custom tabs generated by extensions.
     * @return The Burp UI tab denoted by the tab name
     */
    public static Component getBaseBurpComponent(String tabName) {
        JTabbedPane mainTabs =  getMainTabbedPane();
        if(mainTabs != null) {
            return mainTabs.getComponentAt(mainTabs.indexOfTab(tabName));
        } else {
            return null;
        }

    }

}