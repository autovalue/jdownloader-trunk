package jd.gui.swing.jdgui;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;

import javax.swing.Icon;
import javax.swing.Timer;

import jd.gui.swing.jdgui.interfaces.JDMouseAdapter;
import jd.gui.swing.jdgui.interfaces.View;
import jd.gui.swing.jdgui.maintab.TabHeader;

import org.appwork.utils.StringUtils;
import org.appwork.utils.images.IconIO;
import org.jdownloader.gui.mainmenu.DonateAction;
import org.jdownloader.settings.staticreferences.CFG_GUI;

public class DonateTabHeader extends TabHeader implements PromotionTabHeader {

    private static final String NOTIFY_ID = "xmas.22.12.2016.1";

    public DonateTabHeader(final View view) {
        super(view);
        if (doFlash()) {
            final Timer blinker = new Timer(1500, new ActionListener() {
                private int        i               = 0;
                private final Icon iconNormal      = view.getIcon();
                private final Icon iconTransparent = IconIO.getTransparentIcon(IconIO.toBufferedImage(iconNormal), 0.5f);

                @Override
                public void actionPerformed(final ActionEvent e) {
                    if (!doFlash()) {
                        labelIcon.setIcon(iconNormal);
                        ((Timer) e.getSource()).stop();
                    } else {
                        if (i++ % 2 == 0) {
                            labelIcon.setIcon(iconNormal);
                        } else {
                            labelIcon.setIcon(iconTransparent);
                        }
                    }
                }
            });
            blinker.setRepeats(true);
            blinker.start();
        }
    }

    private boolean doFlash() {
        return !StringUtils.equalsIgnoreCase(CFG_GUI.CFG.getDonationNotifyID(), NOTIFY_ID);
    }

    @Override
    protected void initMouseForwarder() {
        addMouseListener(new JDMouseAdapter() {
            @Override
            public void mouseDragged(MouseEvent e) {
                JDMouseAdapter.forwardEvent(e, MainTabbedPane.getInstance());
            }

            @Override
            public void mouseMoved(MouseEvent e) {
                JDMouseAdapter.forwardEvent(e, MainTabbedPane.getInstance());
            }

            @Override
            public void mouseClicked(MouseEvent e) {
                new DonateAction().actionPerformed(null);
            }

            @Override
            public void mouseEntered(MouseEvent e) {
                JDMouseAdapter.forwardEvent(e, MainTabbedPane.getInstance());
            }

            @Override
            public void mouseExited(MouseEvent e) {
                JDMouseAdapter.forwardEvent(e, MainTabbedPane.getInstance());
            }

            @Override
            public void mousePressed(MouseEvent e) {
                setShown();
                CFG_GUI.CFG.setDonationNotifyID(NOTIFY_ID);
                new DonateAction().actionPerformed(null);
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                setHidden();
            }

        });
    }

    public void onClick() {

    }

}
