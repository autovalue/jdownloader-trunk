//    jDownloader - Downloadmanager
//    Copyright (C) 2009  JD-Team support@jdownloader.org
//
//    This program is free software: you can redistribute it and/or modify
//    it under the terms of the GNU General Public License as published by
//    the Free Software Foundation, either version 3 of the License, or
//    (at your option) any later version.
//
//    This program is distributed in the hope that it will be useful,
//    but WITHOUT ANY WARRANTY; without even the implied warranty of
//    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
//    GNU General Public License for more details.
//
//    You should have received a copy of the GNU General Public License
//    along with this program.  If not, see <http://www.gnu.org/licenses/>.

package jd.gui.skins.simple;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;

import javax.swing.ImageIcon;
import javax.swing.JButton;

import jd.gui.skins.SwingGui;
import jd.utils.JDTheme;
import jd.utils.locale.JDL;

public class JDSeparator extends JButton implements ActionListener,MouseListener {

    private static final long serialVersionUID = 3007033193590223026L;

    private boolean closed = false;

    private String leftToolTip;
    private String rightToolTip;

    private ImageIcon left;
    private ImageIcon right;

    public JDSeparator() {
        leftToolTip = JDL.L("gui.tooltips.jdseparator", "Close sidebar");
        rightToolTip = JDL.L("gui.tooltips.jdseparator.open", "Open sidebar");

        left = JDTheme.II("gui.images.minimize.left", 5, 10);
        right = JDTheme.II("gui.images.minimize.right", 5, 10);
this.addMouseListener(this);
        setFocusable(false);
        setMinimized(false);
this.setOpaque(false);
this.setContentAreaFilled(false);
this.setBorderPainted(false);
        addActionListener(this);
    }

    public void setMinimized(boolean b) {
        closed = b;
        setIcon(b ? right : left);
        setToolTipText(b ? rightToolTip : leftToolTip);
    }

    public void actionPerformed(ActionEvent e) {
        SimpleGUI.CURRENTGUI.hideSideBar(!closed);
    }

    public void mouseClicked(MouseEvent e) {
        // TODO Auto-generated method stub
        
    }

    public void mouseEntered(MouseEvent e) {
        this.setContentAreaFilled(true);
        
    }

    public void mouseExited(MouseEvent e) {
        this.setContentAreaFilled(false);
        
    }

    public void mousePressed(MouseEvent e) {
        // TODO Auto-generated method stub
        
    }

    public void mouseReleased(MouseEvent e) {
        // TODO Auto-generated method stub
        
    }

}
