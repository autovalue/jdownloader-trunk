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

import java.awt.Cursor;

import javax.swing.ImageIcon;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;

import jd.gui.skins.simple.components.DownloadView.JDProgressBar;
import net.miginfocom.swing.MigLayout;

public class TinyProgressBar extends JPanel {

    private static final long serialVersionUID = 8385631080915257786L;
    private JLabel lbl;
    private JDProgressBar prg;

    public TinyProgressBar() {
        this.setLayout(new MigLayout("ins 0", "[grow,fill]1[grow,fill]", "[grow,fill]"));

        this.add(lbl = new JLabel());
        this.add(prg = new JDProgressBar(), "width 10!");
        lbl.setOpaque(false);
        prg.setOpaque(false);
        prg.setOrientation(SwingConstants.VERTICAL);
        // this.setBorder(prg.getBorder());
        prg.setBorder(null);
        this.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

    }

    public void setEnabled(boolean b) {
        super.setEnabled(b);
        lbl.setEnabled(b);
        prg.setEnabled(b);
    }



    public void setIcon(ImageIcon hosterIcon) {
        lbl.setIcon(hosterIcon);
    }

    public void setMaximum(long max) {
        prg.setMaximum(max);
    }

    public void setValue(long left) {
        prg.setValue(left);
    }

}
