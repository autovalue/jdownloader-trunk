package jd.gui.skins.jdgui.views;

import java.awt.Color;

import javax.swing.BorderFactory;
import javax.swing.Icon;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JToolBar;
import javax.swing.ScrollPaneConstants;
import javax.swing.UIManager;
import javax.swing.plaf.metal.MetalLookAndFeel;

import jd.gui.skins.jdgui.interfaces.DroppedPanel;
import jd.gui.skins.jdgui.interfaces.SwitchPanel;
import jd.gui.skins.simple.tasks.TaskPanel;
import jd.utils.JDTheme;
import net.miginfocom.swing.MigLayout;

import com.jtattoo.plaf.AbstractLookAndFeel;
import com.jtattoo.plaf.ColorHelper;

/**
 * A view is an abstract class for a contentpanel in JDGui
 * 
 * @author Coalado
 * 
 */
public abstract class View extends JPanel {
    public static final int ICON_SIZE = 16;
    private JPanel rightPane;
    private JScrollPane sidebar;
    private TaskPanel sidebarContent;
    private SwitchPanel content;
    private JPanel topContent;
    private JToolBar toolBar;
    private JPanel bottomContent;
    private DroppedPanel infoPanel;
    private DroppedPanel defaultInfoPanel;

    public View() {
        this.setLayout(new MigLayout("ins 0", "[]0[grow,fill]", "[grow,fill]"));

        add(sidebar = new JScrollPane(), "width 200!,hidemode 1");
        Color line;
        if (UIManager.getLookAndFeel() instanceof AbstractLookAndFeel) {
            Color frameColor = AbstractLookAndFeel.getTheme().getBackgroundColor();
            line = ColorHelper.darker(frameColor, 20);
        } else {
            // MetalLookAndFeel.getControlDarkShadow();
            // MetalLookAndFeel.getControlHighlight() ;
            line = MetalLookAndFeel.getControl();
        }

        sidebar.setBorder(BorderFactory.createMatteBorder(0, 0, 0, 1, line));
        sidebar.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        sidebar.setVisible(false);
        rightPane = new JPanel(new MigLayout("ins 0", "[grow,fill]", "[grow,fill]"));
        add(rightPane);
        add(topContent = new JPanel(new MigLayout("ins 0", "[grow,fill]", "[]")), "dock NORTH,hidemode 3");
        topContent.setVisible(false);

        add(bottomContent = new JPanel(new MigLayout("ins 0", "[grow,fill]", "[]")), "dock SOUTH");
        bottomContent.setVisible(false);
    }

    /**
     * Sets the default infopanel
     * 
     * @param panel
     */
    protected void setDefaultInfoPanel(DroppedPanel panel) {
        this.defaultInfoPanel = panel;
        if (this.getInfoPanel() == null) setInfoPanel(panel);
    }

    /**
     * SOUTH CONTENT sets the south infopanel. if set to null, the default info
     * panel is shown. of this is null, too the info area is hidden
     * 
     * @param infoPanel
     */
    public void setInfoPanel(DroppedPanel info) {
        if (info == null) info = defaultInfoPanel;
        if (infoPanel == info) return;

        if (infoPanel != null) infoPanel.onHide();
        if (info == null) {

            bottomContent.setVisible(false);

        } else {
            bottomContent.setVisible(true);
            bottomContent.removeAll();
            bottomContent.add(info);
            info.onShow();
        }
        revalidate();
        this.infoPanel = info;
    }

    public DroppedPanel getInfoPanel() {
        return infoPanel;
    }

    /**
     * TOPCONTENT Sets the views toolbar. null removes the toolbar
     * 
     * @param toolbar
     */
    protected void setToolBar(JToolBar toolbar) {
        if (toolbar == null) {
            topContent.setVisible(false);

        } else {
            topContent.setVisible(true);
            topContent.removeAll();
            topContent.add(toolbar);
        }

        this.toolBar = toolbar;

    }

    /**
     * CENTER-MAIN-CONTENT Sets the left side main content bar
     * 
     * @param right
     */
    protected void setContent(SwitchPanel right) {
        rightPane.removeAll();
        rightPane.add(right);
        this.content = right;
        this.revalidate();
    }

    /**
     * SIDEBAR WEST CONTENT sets the left sidebar
     * 
     * @param left
     */
    protected void setSideBar(TaskPanel left) {
        if (left == null) {
            sidebar.setVisible(false);

        } else {
            sidebar.setVisible(true);
            sidebar.setViewportView(left);

        }
        this.sidebarContent = left;
    }

    /**
     * returns the Tab tooltip
     * 
     * @return
     */
    abstract public String getTooltip();

    /**
     * Returns the tab title
     * 
     * @return
     */
    abstract public String getTitle();

    /**
     * returns the tab icon
     * 
     * @return
     */
    abstract public Icon getIcon();

    /**
     * Returns the defaulticon
     * 
     * @return
     */
    public static Icon getDefaultIcon() {
        // TODO Auto-generated method stub
        return JDTheme.II("gui.images.add_package", 16, 16);
    }
}
