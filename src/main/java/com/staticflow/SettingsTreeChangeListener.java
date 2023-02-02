package main.java.com.staticflow;

import javax.swing.event.TreeModelEvent;
import javax.swing.event.TreeModelListener;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import java.awt.*;

/**
 * This class is a custom TreeModelListener attached to the JTree Model used by the Burp Suite Settings menu.
 * This listener is needed because of how Burp Suite handles changes to the JTree when a user uses the search bar
 * or clicks on the User/Project filter buttons. When any of those happens, Burp Suite recreates the entire JTree from
 * a constant list of predefined options which will remove any custom inserted settings. To combat this, this listener
 * fires on JTree changes and re-inserts the custom setting node.
 */
public class SettingsTreeChangeListener implements TreeModelListener {
    //Reference to the internal Burp Suite JTree model
    private final DefaultTreeModel model;
    //Reference to the user supplied name of the custom Settings Node
    private final String name;
    //Reference to the user supplied Component of the custom Settings Node
    private final Component settingsTreeComponent;

    public SettingsTreeChangeListener(DefaultTreeModel model, String name, Component settingsTreeComponent) {
        this.model = model;
        this.name = name;
        this.settingsTreeComponent = settingsTreeComponent;
    }

    @Override
    public void treeNodesChanged(TreeModelEvent e) {
        //Not needed
    }

    @Override
    public void treeNodesInserted(TreeModelEvent e) {
        //Not needed
    }

    @Override
    public void treeNodesRemoved(TreeModelEvent e) {
        //Not needed
    }

    @Override
    public void treeStructureChanged(TreeModelEvent e) {
        //Get access to the last root node
        DefaultMutableTreeNode lastChild = (DefaultMutableTreeNode) this.model.getChild(this.model.getRoot(), this.model.getChildCount(this.model.getRoot())-1);
        //Check if the last node is NOT the custom Node
        if(!lastChild.toString().equals("Custom Extension Settings")) {
            try {
                //re-insert the custom extension settings node
                BurpGuiControl.addMenuToSettingsTree(this.name,this.settingsTreeComponent);
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }
    }
}
