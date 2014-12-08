package org.jdownloader.gui.views.linkgrabber.actions;

import java.awt.Color;
import java.awt.Component;
import java.awt.Dialog.ModalityType;
import java.awt.Point;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.Timer;

import jd.controlling.linkcollector.LinkCollectingJob;
import jd.controlling.linkcollector.LinkCollector;
import jd.controlling.linkcollector.LinkOrigin;
import jd.controlling.linkcrawler.CrawledLink;
import jd.controlling.linkcrawler.LinkCrawler;
import jd.controlling.linkcrawler.LinkCrawlerRule;
import jd.controlling.linkcrawler.LinkCrawlerRule.RULE;
import jd.gui.swing.jdgui.JDGui;
import jd.gui.swing.jdgui.components.IconedProcessIndicator;
import jd.plugins.Plugin;

import org.appwork.swing.MigPanel;
import org.appwork.swing.components.circlebar.CircledProgressBar;
import org.appwork.swing.components.circlebar.ImagePainter;
import org.appwork.utils.Files;
import org.appwork.utils.StringUtils;
import org.appwork.utils.formatter.TimeFormatter;
import org.appwork.utils.swing.EDTRunner;
import org.appwork.utils.swing.SwingUtils;
import org.appwork.utils.swing.dialog.AbstractDialog;
import org.appwork.utils.swing.dialog.Dialog;
import org.appwork.utils.swing.dialog.DialogCanceledException;
import org.appwork.utils.swing.dialog.DialogClosedException;
import org.appwork.utils.swing.dialog.locator.RememberRelativeDialogLocator;
import org.jdownloader.gui.helpdialogs.HelpDialog;
import org.jdownloader.gui.translate._GUI;
import org.jdownloader.images.NewTheme;
import org.jdownloader.settings.staticreferences.CFG_GUI;

public class AddLinksProgress extends AbstractDialog<Object> {

    private CircledProgressBar                 progress;
    private JLabel                             duration;
    private JLabel                             old;

    private JLabel                             header;

    private Timer                              updateTimer;
    private long                               startTime;

    private final LinkCollectingJob            job;
    private final AtomicReference<LinkCrawler> lcReference = new AtomicReference<LinkCrawler>();
    private JLabel                             filtered;

    public AddLinksProgress(LinkCollectingJob crawljob) {
        super(Dialog.STYLE_SHOW_DO_NOT_DISPLAY_AGAIN, _GUI._.AddLinksProgress_AddLinksProgress_(), null, _GUI._.literally_hide(), _GUI._.literally_abort());
        setLocator(new RememberRelativeDialogLocator("AddLinksProgress", JDGui.getInstance().getMainFrame()));
        this.job = crawljob;
    }

    @Override
    protected Object createReturnValue() {
        return null;
    }

    @Override
    public ModalityType getModalityType() {
        return ModalityType.MODELESS;
    }

    @Override
    public Window getOwner() {
        return super.getOwner();
    }

    @Override
    public JComponent layoutDialogContent() {
        MigPanel p = new MigPanel("ins 0,wrap 9", "[][][][][][][][][]", "[grow,fill]");
        progress = new CircledProgressBar();
        progress.setIndeterminate(true);
        progress.setValueClipPainter(new ImagePainter(NewTheme.I().getIcon("linkgrabber", 26), 1.0f));
        ((ImagePainter) progress.getValueClipPainter()).setBackground(Color.WHITE);
        ((ImagePainter) progress.getValueClipPainter()).setForeground(Color.GREEN);

        progress.setNonvalueClipPainter(new ImagePainter(NewTheme.I().getIcon("linkgrabber", 26), 0.5f));
        ((ImagePainter) progress.getNonvalueClipPainter()).setBackground(Color.WHITE);
        ((ImagePainter) progress.getNonvalueClipPainter()).setForeground(Color.GREEN);
        p.add(progress, "height 40!,width 40!,spany 2");
        p.add(header = new JLabel(), "spanx");
        SwingUtils.toBold(header);
        String stz = getSearchInText();
        header.setText(_GUI._.AddLinksProgress_layoutDialogContent_header_(stz.substring(0, Math.min(45, stz.length()))));
        p.add(label(_GUI._.AddLinksProgress_layoutDialogContent_duration()));

        p.add(duration = new JLabel(), "width 50:n:n,growx");
        p.add(new JLabel(NewTheme.I().getIcon("go-next", 18)));
        p.add(label(_GUI._.AddLinksProgress_found()));
        p.add(old = new JLabel());
        JLabel lbl;
        p.add(lbl = new JLabel(NewTheme.I().getIcon("filter", 18)));
        lbl.setToolTipText(_GUI._.AddLinksProgress_filter());
        p.add(filtered = new JLabel(), "alignx right,sg 1");

        startTime = System.currentTimeMillis();
        updateTimer = new Timer(500, new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                duration.setText(TimeFormatter.formatMilliSeconds(System.currentTimeMillis() - startTime, 0));
                final LinkCrawler lc = lcReference.get();
                old.setText("" + (lc == null ? 0 : lc.getCrawledLinksFoundCounter()));
                filtered.setText("" + (lc == null ? 0 : lc.getFilteredLinks().size()));

            }
        });
        old.setText("0");
        filtered.setText("0");
        getAddLinksDialogThread(job, lcReference).start();
        updateTimer.setRepeats(true);
        updateTimer.start();
        return p;
    }

    public Thread getAddLinksDialogThread(final LinkCollectingJob job, final AtomicReference<LinkCrawler> lcReference) {
        return new Thread("AddLinksDialog:" + job.getOrigin().getOrigin()) {
            public void run() {
                final Thread thread = Thread.currentThread();
                LinkCrawler lc = LinkCollector.getInstance().addCrawlerJob(job);
                if (lcReference != null) {
                    lcReference.set(lc);
                }
                if (lc != null) {
                    lc.waitForCrawling();
                    if (!job.isDeepAnalyse() && lc.getProcessedLinksCounter() == 0 && lc.getUnhandledLinksFoundCounter() > 0) {
                        final List<CrawledLink> unhandledLinks = new ArrayList<CrawledLink>(lc.getUnhandledLinks());
                        if (unhandledLinks.size() == 1 && LinkOrigin.ADD_LINKS_DIALOG.equals(job.getOrigin().getOrigin())) {
                            for (CrawledLink unhandledLink : unhandledLinks) {
                                unhandledLink.setCrawlDeep(true);
                            }
                        } else {
                            try {
                                Dialog.getInstance().showConfirmDialog(0, _GUI._.AddLinksAction_actionPerformed_deep_title(), _GUI._.AddLinksAction_actionPerformed_deep_msg(), null, _GUI._.literally_yes(), _GUI._.literall_no());
                                for (CrawledLink unhandledLink : unhandledLinks) {
                                    unhandledLink.setCrawlDeep(true);
                                }
                            } catch (DialogClosedException e) {
                                e.printStackTrace();
                            } catch (DialogCanceledException e) {
                                e.printStackTrace();
                            }
                        }
                        lc = LinkCollector.getInstance().addCrawlerJob(unhandledLinks, job);
                        if (lcReference != null) {
                            lcReference.set(lc);
                        }
                        lc.waitForCrawling();
                        final List<CrawledLink> results = lc.getCrawledLinks();
                        if (unhandledLinks.size() == 1 && results.size() == 1) {
                            final String unhandledURL = unhandledLinks.get(0).getURL();
                            final String fileName = Plugin.extractFileNameFromURL(unhandledURL);
                            final String fileExtension = Files.getExtension(fileName);
                            if (StringUtils.isNotEmpty(fileExtension)) {
                                boolean add = true;
                                for (LinkCrawlerRule rule : LinkCrawler.listLinkCrawlerRules()) {
                                    if (RULE.DIRECTHTTP.equals(rule.getRule()) && rule.matches(unhandledURL)) {
                                        add = false;
                                        break;
                                    }
                                }
                                if (add) {
                                    final LinkCrawlerRule rule = new LinkCrawlerRule();
                                    rule.setName("Learned file extension:" + fileExtension);
                                    rule.setPattern("(?i).*\\." + fileExtension + "($|\\?.*$)");
                                    rule.setRule(RULE.DIRECTHTTP);
                                    LinkCrawler.addLinkCrawlerRule(rule);
                                }
                            }
                        }
                    }
                }
                new EDTRunner() {
                    @Override
                    protected void runInEDT() {
                        dispose(thread);
                    }
                };

            }
        };
    }

    protected String getSearchInText() {
        return job.getText();
    }

    @Override
    protected void setReturnmask(boolean b) {
        if (b) {
            // Hide

            new Thread("SimpleTextBallon") {
                public void run() {
                    IconedProcessIndicator iconComp = JDGui.getInstance().getStatusBar().getLinkGrabberIndicator();
                    Point loc = iconComp.getLocationOnScreen();
                    if (CFG_GUI.HELP_DIALOGS_ENABLED.isEnabled()) {
                        HelpDialog.show(false, false, new Point(loc.x + iconComp.getWidth() / 2, loc.y + iconComp.getHeight() / 2), "linkcrawlerprogressdialog", Dialog.STYLE_SHOW_DO_NOT_DISPLAY_AGAIN, _GUI._.AddLinksProgress_setReturnmask_title_(), _GUI._.AddLinksProgress_setReturnmask_msg_(), NewTheme.I().getIcon("linkgrabber", 32));
                    }

                }
            }.start();

        } else {
            // cancel
            final LinkCrawler lc = lcReference.get();
            if (lc != null) {
                LinkCollector.getInstance().abort();
                // lc.stopCrawling();
            }
        }
        super.setReturnmask(b);
    }

    public void dispose(Thread thread) {
        if (thread != null) {
            thread.interrupt();
        }
        dispose();
    }

    @Override
    public void dispose() {
        this.setVisible(false);
        super.dispose();
        updateTimer.stop();
    }

    private Component label(String lbl) {
        JLabel ret = new JLabel(lbl);
        ret.setEnabled(false);
        return ret;
    }

}
