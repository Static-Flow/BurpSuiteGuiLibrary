package main.java.com.staticflow;

import javax.swing.*;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.MutableTreeNode;
import java.awt.*;
import java.awt.event.ActionListener;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.*;
import java.util.List;

/**
This Library provides a set of tools for easily manipulating the Swing Components that comprise Burp Suite's UI.
 */
public final class BurpGuiControl {

    static final Frame SETTINGS_JFRAME = getSettingsFrame();

    public static void replaceComponentAtIndex(int index, Component newComponent) {
        getMainTabbedPane().setComponentAt(index,newComponent);
    }

    public static void replaceMainBurpComponent(String oldComponentTabName, Component newComponent) {
        JTabbedPane mainTabbedPane = getMainTabbedPane();
        mainTabbedPane.setComponentAt(mainTabbedPane.indexOfTab(oldComponentTabName),newComponent);
    }

    public static void addMainBurpComponent(String tabName, Component newComponent) {
        getMainTabbedPane().addTab(tabName,newComponent);
    }

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
     * This method returns the main Swing component of Burp Suite, the JTabbedPane containing the major tabs.
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
     * This method searches all children of a source Component for any Components matching the provided Component class
     * @param parentContainer Starting container to search children
     * @param clazz The java.awt.Component class which a child Component must match
     * @return all children of the parentContainer that match the clazz java.awt.Component
     */
    public static List<Component> findAllChildComponentsOfType(Container parentContainer, Class<? extends java.awt.Component> clazz) {
        ArrayList<Component> matchingComponents = new ArrayList<>();
        for(Component comp : parentContainer.getComponents()) {
            if(clazz.isAssignableFrom(comp.getClass())){
                matchingComponents.add(comp);
            }
        }
        return matchingComponents;
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
            int tabIndex = mainTabs.indexOfTab(tabName);
            if(tabIndex != -1) {
                return mainTabs.getComponentAt(mainTabs.indexOfTab(tabName));
            }
        }
        return null;
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
     * Called by Extensions, typically when being removed or on Burp Suite close, to remove any custom settings menus. If this is not called, the custom menu
     * will persist after the extension is gone.
     * @param name The name of the menu to remove.
     */
    public static void removeCustomSettingsTree(String name) {
        System.out.println("Removing: "+name);
        //Get the JTree reference from it
        JTree tree = (JTree) findFirstComponentOfType(SETTINGS_JFRAME,JTree.class);
        //Get the model from the JTree
        assert tree != null;
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
            JToggleButton allToggle = (JToggleButton) findFirstComponentOfType(SETTINGS_JFRAME,JToggleButton.class);
            assert allToggle != null;
            for(ActionListener listener : allToggle.getActionListeners()) {
                if(AllToggleActionListener.class.getName().equals(listener.getClass().getName())) {
                    allToggle.removeActionListener(listener);
                }
            }
        }
        treeModel.reload();
    }

    /**
     * Here there be dragons! Burp Suite does not provide a means of accessing the Settings Tree to add custom options. This library achieves this by using
     * reflection to access the classes controlling the Settings menu and injecting the user supplied menu component. At a high level the process goes like this:
     *      1. Get a reference to the top level Settings JFrame using {@link BurpGuiControl#getSettingsFrame()}
     *      2. Call {@link BurpGuiControl#initializeCustomSettingsMenu} to set up the custom menu for the first time
     *      3. Get a reference to the "All" filter button in the settings menu
     *      4. add a custom ActionListener {@link AllToggleActionListener} to the filter button that recreates the custom extension settings menus when clicked
     * @param name The name for the custom setting node
     * @param settingsTree The {@link Component} to be shown when clicking on the custom setting node
     */
    public static void addMenuToSettingsTree(String name, Component settingsTree) {
        initializeCustomSettingsMenu(name,settingsTree);
        JToggleButton allToggle = (JToggleButton) findFirstComponentOfType(SETTINGS_JFRAME,JToggleButton.class);
        assert allToggle != null;
        allToggle.addActionListener(new AllToggleActionListener(name,settingsTree));
    }


    /**
     *      1. Get a reference to the JTree and its DefaultTreeModel
     *      2. Check if the last child in the tree is a "Custom Extension Settings" node
     *      3. Get a reference to the root node and its last child
     *      4. If the last child is not "Custom Extension Settings" add the top level node and call {@link BurpGuiControl#createNewExtensionSettingsNode}
     *      5. If the last child is "Custom Extension Settings" call {@link BurpGuiControl#createNewExtensionSettingsNode}
     *      6. Insert the new node from step 4/5 into the tree and update the model
     * @param name The name for the custom setting node
     * @param settingsTree The {@link Component} to be shown when clicking on the custom setting node
     */
    static void initializeCustomSettingsMenu(String name, Component settingsTree) {
        System.out.println(name);
        JTree tree = (JTree) findFirstComponentOfType(SETTINGS_JFRAME,JTree.class);
        assert tree != null;
        //Retrieve the Model from the JTree
        DefaultTreeModel treeModel = (DefaultTreeModel) tree.getModel();
        DefaultMutableTreeNode root = (DefaultMutableTreeNode) treeModel.getRoot();
        DefaultMutableTreeNode lastChild = (DefaultMutableTreeNode) treeModel.getChild(root, treeModel.getChildCount(root)-1);
        try {
            if (!lastChild.toString().equals("Custom Extension Settings")) {
                System.out.println("Top level custom extension settings node does not exist, creating it");
                DefaultMutableTreeNode baseCustomSettingsNode = new DefaultMutableTreeNode("Custom Extension Settings");
                root.insert(baseCustomSettingsNode, root.getChildCount());
                DefaultMutableTreeNode newNode =  createNewExtensionSettingsNode((DefaultMutableTreeNode) treeModel.getChild(root, 0),name,settingsTree);
                //Add our new entry to the top level custom settings Menu Node
                baseCustomSettingsNode.insert(newNode, 0);
                //Notify the model the new nodes were added
                treeModel.nodesWereInserted(root, new int[]{root.getIndex(baseCustomSettingsNode)});
                //reload the model
                treeModel.reload();
            } else {
                System.out.println("Top level custom extension settings node exists, adding to it");
                DefaultMutableTreeNode newNode =  createNewExtensionSettingsNode((DefaultMutableTreeNode) treeModel.getChild(root, 0),name,settingsTree);
                //Add our new entry to the top level custom settings Menu Node
                lastChild.insert(newNode, 0);
                //Notify the model the new nodes were added
                treeModel.nodesWereInserted(root, new int[]{root.getIndex(lastChild)});
                //reload the model
                treeModel.reload();
            }
        } catch (Exception ex) {
            System.out.println("Could not add custom extension settings panel: "+ex);
        }
    }

    /**
     * This class is called by {@link BurpGuiControl#initializeCustomSettingsMenu} to create a new settings node for an Extension's setting panel. an overview
     * of how it works is below:<Br>
     *      1. Create a new instance of the Burp Suite internal class representing a Menu Tree Node
     *      2. Call the constructor of the Class from step 1 to generate the default Component shown when clicking on the Menu Node
     *      3. Cast the internal Burp Suite class returned from the step 2 method call to JPanel and add the users custom Component to it
     *      4. Call {@link BurpGuiControl#injectCustomPanelIntoGlobalList} to add the JPanel from step 3 to the global list of settings panels
     *      5. Create a new DefaultMutableTreeNode, and set the Object from step 2 as it's UserObject and return
     * @param childToClone reference to the internal BurpSuite class representing a top level settings node
     * @param newNodeName the user supplied name for the custom settings component
     * @param newNodeComponent the user supplied panel containing settings components
     * @return New {@link DefaultMutableTreeNode} containing the custom extension settings panel
     * @throws ClassNotFoundException Thrown if the BurpSuite class representing a Settings node cannot be found
     * @throws NoSuchMethodException Thrown if a method on the BurpSuite class representing a Settings node cannot be found
     * @throws InvocationTargetException Thrown if a new instance of the BurpSuite class representing a Settings node cannot be created
     * @throws InstantiationException Thrown if a new instance of the BurpSuite class representing a Settings node cannot be created
     * @throws IllegalAccessException Thrown if a new instance of the BurpSuite class representing a Settings node cannot be created
     */
    private static DefaultMutableTreeNode createNewExtensionSettingsNode(DefaultMutableTreeNode childToClone, String newNodeName, Component newNodeComponent ) throws ClassNotFoundException, NoSuchMethodException, InvocationTargetException, InstantiationException, IllegalAccessException {
        //Retrieve the Class name of the internal Burp Suite Class that represents Settings Menu Nodes
        String toolsMenuNodeUserObjectClassName = childToClone.getUserObject().getClass().getName();
        //Load the Class using Burp Suite's ClassLoader
        Class<?> clazz = childToClone.getUserObject().getClass().getClassLoader().loadClass(toolsMenuNodeUserObjectClassName);
        //Retrieve the parameterized constructor which takes the name of the Menu Node as a parameter
        Constructor<?> clazzConstructor = clazz.getDeclaredConstructor(String.class);
        //Make the constructor publicly accessible
        clazzConstructor.setAccessible(true);
        //Instantiate a new Menu Node Object using the user supplied name
        Object customSettings = clazzConstructor.newInstance(newNodeName);

        //Reference to the internal Burp Suite object which represents the Component rendered when a Menu Node is clicked
        Object panelResultClass = null;
        //Loop over all methods within the internal Burp Suite Class that represents Settings Menu Nodes
        for (Method method : customSettings.getClass().getDeclaredMethods()) {
            /*
                If the method takes one parameter and that parameter is an internal Burp Suite class, it is likely to be the method we need.
                This is leaky but really the only way to do it. You can't call it by name because the obfuscation on the Burp Suite class files will
                continue to change. This is the only method on this class which has both 1 parameter and that parameter is a Burp Suite internal class so it works.
             */
            if (method.getParameterCount() == 1 && method.getParameterTypes()[0].getName().startsWith("burp")) {
                //make it publicly accessible
                method.setAccessible(true);
                //invoke the method and receive the Object representing the default blank Component which is shown when clicking on a Menu Node
                Optional panelResultClassOptional = (Optional) method.invoke(customSettings, new Object[]{null});
                //Retrieve the Object from the Optional result
                panelResultClass = panelResultClassOptional.get();
                //Cast the internal Burp Suite Class to a JPanel, which it extends, and add the users custom Component to it.
                ((JPanel) panelResultClass).add(newNodeComponent);
                //Force update the Components UI
                ((JPanel) panelResultClass).updateUI();
            }
        }

        //Inject our new setting Component into the global list
        injectCustomPanelIntoGlobalList(SETTINGS_JFRAME, (JPanel) panelResultClass);

        //Create the new Node containing the custom content
        return new DefaultMutableTreeNode(customSettings);
    }

    /**
     * Called by {@link BurpGuiControl#initializeCustomSettingsMenu} to inject the custom extension setting Component into the global list. Internally Burp Suite uses
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