package main.java.com.staticflow;

import javax.swing.*;
import javax.swing.event.TreeModelListener;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.MutableTreeNode;
import java.awt.*;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.Optional;

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

    /**
     * Retrieves the top level frame for the Settings pop-out menu. This is used by {@link BurpGuiControl#addMenuToSettingsTree} to inject custom menu options
     * @return Frame containing the Settings menu
     */
    public static Frame getSettingsFrame() {
        for(Frame frame : Frame.getFrames()) {
            if(frame.getTitle().startsWith(("Settings")))
            {
               return frame;
            }
        }
        return null;
    }

    /**
     * Internally called by {@link BurpGuiControl#addMenuToSettingsTree} to initialize the top level menu for holding user defined menu options.
     * It checks for the existence of a custom JTree model listener {@link SettingsTreeChangeListener} and if it does not exists, creates it.
     * @param model The {@link DefaultTreeModel Model} used internally within Burp Suite to represent the Settings menus
     * @param name The node name for the user defined menu component
     * @param settingsTreeComponent The user defined {@link Component} that will be injected into the Burp Suite settings tree
     * @return The top level custom DefaultMutableTreeNode where extensions custom settings nodes are placed
     */
    private static DefaultMutableTreeNode initializeCustomSettingsTree(DefaultTreeModel model, String name, Component settingsTreeComponent) {
        // Loop through the listeners attached to the Burp Suite Settings Tree Model
        for(TreeModelListener listener : model.getTreeModelListeners()) {
            //Find our custom one
            if(listener.getClass().getName().equals(SettingsTreeChangeListener.class.getName())) {
                //If it exists, return
                System.out.println("Settings Tree Listener already exists, returning.");
                return (DefaultMutableTreeNode) model.getChild(model.getRoot(),model.getChildCount(model.getRoot())-1);
            }
        }
        //If it does not exist, create it, then attach it to the JTree model
        System.out.println("Settings Tree Listener does not exist, creating.");
        SettingsTreeChangeListener settingsTreeChangeListener = new SettingsTreeChangeListener(model,name,settingsTreeComponent);
        model.addTreeModelListener(settingsTreeChangeListener);
        DefaultMutableTreeNode rootNode = (DefaultMutableTreeNode) model.getRoot();
        DefaultMutableTreeNode baseCustomSettingsNode = new DefaultMutableTreeNode("Custom Extension Settings");
        rootNode.insert(baseCustomSettingsNode,rootNode.getChildCount());
        return baseCustomSettingsNode;
    }

    /**
     * Called by Extensions, typically when being removed or on Burp Suite close, to remove any custom settings menus. If this is not called, the custom menu
     * will persist after the extension is gone.
     * @param name The name of the menu to remove.
     */
    public static void removeCustomSettingsTree(String name) {
        System.out.println("Removing: "+name);
        //Retrieve the top level frame
        Frame treeFrame = getSettingsFrame();
        //Get the JTree reference from it
        JTree tree = (JTree) findFirstComponentOfType(treeFrame,JTree.class);
        //Get the model from the JTree
        DefaultTreeModel treeModel = (DefaultTreeModel) tree.getModel();
        //Get the root JTree node
        DefaultMutableTreeNode rootNode = (DefaultMutableTreeNode) treeModel.getRoot();
        //List all children of the "Custom Extension Settings" node
        Enumeration customSettingsChildren = rootNode.getChildAt(rootNode.getChildCount() - 1).children();
        //For each child
        while(customSettingsChildren.hasMoreElements()) {
            DefaultMutableTreeNode nextChild = (DefaultMutableTreeNode) customSettingsChildren.nextElement();
            System.out.println("Checking node: "+nextChild.toString()+" For removal");
            //if it matches the name to be deleted, remove it
            if(nextChild.toString().equals(name)) {
                treeModel.removeNodeFromParent(nextChild);
                break;
            }
        }
        //if there are no more children, remove the top level custom node as well and remove the custom model change listener
        if(rootNode.getChildAt(rootNode.getChildCount() - 1).getChildCount() == 0) {
            System.out.println("No more custom nodes exists, removing root custom node.");
            treeModel.removeNodeFromParent((MutableTreeNode) rootNode.getChildAt(rootNode.getChildCount() - 1));
            for(TreeModelListener listener : treeModel.getTreeModelListeners()) {
                if(listener instanceof SettingsTreeChangeListener) {
                    treeModel.removeTreeModelListener(listener);
                    return;
                }
            }
        }
        treeModel.reload();
    }

    /**
     * Here there be dragons! Burp Suite does not provide a means of accessing the Settings Tree to add custom options. This library achieves this by using
     * reflection to access the classes controlling the Settings menu and injecting the user supplied menu component. At a high level the process goes like this:
     *      1. Get a reference to the JTree and its DefaultTreeModel
     *      2. Install a custom TreeModelChangeListener to capture updates to the Tree (For why, see {@link SettingsTreeChangeListener}
     *      3. Get a reference to the root node and its child
     *      4. Create a new instance of the Burp Suite internal class representing a Menu Tree Node
     *      5. Call an internal method of the Class from step 4 to generate the default Component shown when clicking on the Menu Node
     *      6. Cast the internal Burp Suite class returned from the step 5 method call to JPanel and add the users custom Component to it
     *      7. Get a reference to the global JPanel which holds a reference to all possible Settings panels, and add the users custom Component to it
     *      8. Create a new DefaultMutableTreeNode, and set the Object from step 4 as it's UserObject
     *      9. Add the new Tree Node from step 8 to the new "Custom Extension Settings" Tree Node
     *      10. Add the "Custom Extension Settings" Tree Node to the Root Node and reload the model
     * @param name The name for the custom setting node
     * @param settingsTree The {@link Component} to be shown when clicking on the custom setting node
     * @throws ClassNotFoundException Thrown if the internal Burp Suite Class cannot be found
     * @throws NoSuchMethodException Thrown if the required constructor is not found on the internal Burp Suite Class
     * @throws InvocationTargetException Thrown if the internal Burp Suite Class constructor throws an exception
     * @throws InstantiationException Thrown if the internal Burp Suite Class cannot be instantiated
     * @throws IllegalAccessException Thrown if the internal Burp Suite Class cannot be accessed
     */
    public static void addMenuToSettingsTree(String name, Component settingsTree) throws ClassNotFoundException, NoSuchMethodException, InvocationTargetException, InstantiationException, IllegalAccessException {
        //Retrieve the top level Settings JFrame
        Frame treeFrame = getSettingsFrame();
        //Retrieve the JTree component from the JFrame
        assert treeFrame != null;
        JTree tree = (JTree) findFirstComponentOfType(treeFrame,JTree.class);
        //Retrieve the Model from the JTree
        assert tree != null;
        DefaultTreeModel treeModel = (DefaultTreeModel) tree.getModel();
        //Check if the top level "Custom Extension Settings" node needs to be created
        DefaultMutableTreeNode baseCustomSettingsNode = initializeCustomSettingsTree(treeModel,name,settingsTree);
        //Retrieve the root JTree node
        DefaultMutableTreeNode rootNode = (DefaultMutableTreeNode) treeModel.getRoot();
        //Retrieve the first child of the root JTree node
        DefaultMutableTreeNode toolsMenuNode = (DefaultMutableTreeNode) ((DefaultMutableTreeNode) treeModel.getRoot()).getChildAt(0);
        //Retrieve the Class name of the internal Burp Suite Class that represents Settings Menu Nodes
        String toolsMenuNodeUserObjectClassName  = toolsMenuNode.getUserObject().getClass().getName();
        //Load the Class using Burp Suite's ClassLoader
        Class<?> clazz = toolsMenuNode.getUserObject().getClass().getClassLoader().loadClass(toolsMenuNodeUserObjectClassName);
        //Retrieve the parameterized constructor which takes the name of the Menu Node as a parameter
        Constructor<?> clazzConstructor = clazz.getDeclaredConstructor(String.class);
        //Make the constructor publicly accessible
        clazzConstructor.setAccessible(true);
        //Instantiate a new Menu Node Object using the user supplied name
        Object customSettings = clazzConstructor.newInstance(name);

        //Reference to the internal Burp Suite object which represents the Componet rendered when a Menu Node is clicked
        Object panelResultClass = null;
        //Loop over all methods within the internal Burp Suite Class that represents Settings Menu Nodes
        for(Method method : customSettings.getClass().getDeclaredMethods()) {
            /*
                If the method takes one parameter and that parameter is an internal Burp Suite class, it is likely to be the method we need.
                This is leaky but really the only way to do it. You can't call it by name because the obfuscation on the Burp Suite class files will
                continue to change. This is the only method on this class which has both 1 parameter and that parameter is a Burp Suite internal class so it works.
             */
            if(method.getParameterCount() == 1 && method.getParameterTypes()[0].getName().startsWith("burp")) {
                //make it publicly accessible
                method.setAccessible(true);
                //invoke the method and receive the Object representing the default blank Component which is shown when clicking on a Menu Node
                Optional panelResultClassOptional = (Optional) method.invoke(customSettings,new Object[]{ null });
                //Retrieve the Object from the Optional result
                panelResultClass = panelResultClassOptional.get();
                //Cast the internal Burp Suite Class to a JPanel, which it extends, and add the users custom Component to it.
                ((JPanel) panelResultClass).add(settingsTree);
                //Force update the Components UI
                ((JPanel) panelResultClass).updateUI();
            }
        }
        //Create the new Node containing the custom content
        DefaultMutableTreeNode newNode = new DefaultMutableTreeNode(customSettings);

        //Inject our new setting Component into the global list
        injectCustomPanelIntoGlobalList(treeFrame, (JPanel) panelResultClass);

        //Add our new entry to the top level custom settings Menu Node
        baseCustomSettingsNode.insert(newNode,0);
        //Notify the model the new nodes were added
        treeModel.nodesWereInserted(rootNode, new int[]{rootNode.getIndex(baseCustomSettingsNode)});
        //reload the model
        treeModel.reload();
    }

    /**
     * Called by {@link BurpGuiControl#addMenuToSettingsTree} to inject the custom extension setting Component into the global list. Internally Burp Suite uses
     * a single JPanel which holds all possible settings Components and switches which one is visible based on which Menu Node the user clicks. I do not know
     * why they did it that way. Regardless, You have to add any custom settings components to the Jtree Model AND this JPanel or switching to the custom Component
     * won't work. To do that, we search for the JPanel Field on the internal Burp Suite Class that represents the Settings Frame, set the Field as accessible,
     * cast it to a JPanel, and add the users Component to it.
     * @param treeFrame The top level Burp Suite Settings JFrame
     * @param settingsTree The user supplied {@link Component} to inject into the global JPanel list
     * @throws IllegalAccessException Thrown if access to the JPanel field failed
     */
    private static void injectCustomPanelIntoGlobalList(Frame treeFrame, Component settingsTree) throws IllegalAccessException {
        for(Field field : treeFrame.getClass().getDeclaredFields()) {
            if(JPanel.class.isAssignableFrom(field.getType())) {
                Field[] typesFields = field.getType().getDeclaredFields();
                if(typesFields.length == 2) {
                    for(Field typeField : typesFields) {
                        if(typeField.getType().isAssignableFrom(Component.class)) {
                            field.setAccessible(true);
                            Object fieldObject = field.get(treeFrame);
                            ((JPanel) fieldObject).add(settingsTree);
                            return;
                        }
                    }
                }
            }
        }
    }

}