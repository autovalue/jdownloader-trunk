package jd.controlling.captcha;

import java.awt.Image;
import java.io.File;
import java.net.MalformedURLException;

import jd.controlling.IOPermission;
import jd.controlling.IOPermission.CAPTCHA;
import jd.controlling.JDPluginLogger;
import jd.gui.swing.dialog.CaptchaDialog;
import jd.gui.swing.dialog.CaptchaDialogInterface;
import jd.gui.swing.dialog.CaptchaDialogInterface.DialogType;
import jd.plugins.PluginForDecrypt;
import jd.plugins.PluginForHost;

import org.appwork.storage.StorageException;
import org.appwork.utils.event.queue.QueueAction;
import org.appwork.utils.images.IconIO;
import org.appwork.utils.logging.Log;
import org.appwork.utils.swing.EDTRunner;
import org.appwork.utils.swing.dialog.ComboBoxDialog;
import org.appwork.utils.swing.dialog.ComboBoxDialogInterface;
import org.appwork.utils.swing.dialog.Dialog;
import org.appwork.utils.swing.dialog.DialogCanceledException;
import org.appwork.utils.swing.dialog.DialogClosedException;
import org.appwork.utils.swing.dialog.DialogNoAnswerException;
import org.jdownloader.DomainInfo;
import org.jdownloader.controlling.UniqueSessionID;
import org.jdownloader.gui.uiserio.NewUIO;
import org.jdownloader.translate._JDT;

public class CaptchaDialogQueueEntry extends QueueAction<String, RuntimeException> {

    private final UniqueSessionID id = new UniqueSessionID();

    /**
     * @return the iD
     */
    public UniqueSessionID getID() {
        return id;
    }

    private CaptchaController captchaController;

    /**
     * @return the captchaController
     */
    public CaptchaController getCaptchaController() {
        return captchaController;
    }

    private int           flag;
    private String        def;
    private String        resp         = null;
    private boolean       externalSet  = false;
    private CaptchaDialog dialog;
    private IOPermission  ioPermission = null;

    public CaptchaDialogQueueEntry(CaptchaController captchaController, int flag, String def) {
        this.captchaController = captchaController;
        this.ioPermission = captchaController.getIOPermission();
        this.flag = flag;
        this.def = def;
    }

    public DomainInfo getHost() {
        return captchaController.getHost();
    }

    public File getFile() {
        return captchaController.getCaptchafile();
    }

    public void setResponse(String resp) {
        externalSet = true;
        this.resp = resp;
        new EDTRunner() {
            @Override
            protected void runInEDT() {
                try {
                    if (dialog != null && dialog.isInitialized()) dialog.dispose();
                } catch (final Throwable e) {
                    Log.exception(e);
                }
            }
        };
    }

    protected String run() {
        /* external response already set, no need to ask user */
        if (externalSet) return resp;
        String ret = viaGUI();
        /* external response set, return this instead */
        if (externalSet) return resp;
        return ret;
    }

    private String viaGUI() {
        if (ioPermission != null && !ioPermission.isCaptchaAllowed(getHost().getTld())) { return null; }

        try {
            DialogType dialogType = null;
            JDPluginLogger logger = null;
            if (captchaController.getPlugin() instanceof PluginForHost) {
                logger = ((PluginForHost) captchaController.getPlugin()).getLogger();
                dialogType = DialogType.HOSTER;
            } else if (captchaController.getPlugin() instanceof PluginForDecrypt) {
                logger = ((PluginForDecrypt) captchaController.getPlugin()).getLogger();
                dialogType = DialogType.CRAWLER;
            }
            int f = flag;
            if (CaptchaSettings.CFG.isCountdownEnabled() && CaptchaSettings.CFG.getCountdown() > 0) {
                f = f | Dialog.LOGIC_COUNTDOWN;
            }
            Image[] images = null;
            if (captchaController.getCaptchafile().getName().toLowerCase().endsWith("gif")) {
                images = CaptchaDialog.getGifImages(captchaController.getCaptchafile().toURI().toURL());
            } else {
                images = new Image[] { IconIO.getImage(captchaController.getCaptchafile().toURI().toURL()) };
            }
            if (images == null || images.length == 0 || images[0] == null) {
                logger.severe("Could not load CaptchaImage! " + captchaController.getCaptchafile().getAbsolutePath());
                return null;
            }
            this.dialog = new CaptchaDialog(f, dialogType, getHost(), images, def, captchaController.getExplain());
            dialog.setPlugin(captchaController.getPlugin());
            dialog.setMethodName(captchaController.getMethodname());
            dialog.setCountdownTime(CaptchaSettings.CFG.getCountdown());
            CaptchaDialogInterface answer = NewUIO.I().show(CaptchaDialogInterface.class, dialog);
            return answer.getCaptchaCode();
        } catch (DialogNoAnswerException e) {
            if (resp == null) {
                /* no external response available */
                if (!e.isCausedByTimeout()) {
                    String[] options = new String[] { _JDT._.captchacontroller_cancel_dialog_allorhost_next(), _JDT._.captchacontroller_cancel_dialog_allorhost_cancelhost(captchaController.getHost().getTld()), _JDT._.captchacontroller_cancel_dialog_allorhost_all() };
                    try {
                        int defSelection = CaptchaSettings.CFG.getLastCancelOption();
                        ComboBoxDialog combo = new ComboBoxDialog(Dialog.LOGIC_COUNTDOWN | Dialog.STYLE_SHOW_DO_NOT_DISPLAY_AGAIN, _JDT._.captchacontroller_cancel_dialog_allorhost(), _JDT._.captchacontroller_cancel_dialog_allorhost_msg(), options, defSelection, null, null, null, null);

                        switch (NewUIO.I().show(ComboBoxDialogInterface.class, combo).getSelectedIndex()) {
                        case 0:
                            // nothing
                            CaptchaSettings.CFG.setLastCancelOption(0);
                            break;
                        case 1:
                            if (ioPermission != null) {
                                ioPermission.setCaptchaAllowed(getHost().getTld(), CAPTCHA.BLOCKHOSTER);
                            }
                            CaptchaSettings.CFG.setLastCancelOption(1);
                            break;
                        case 2:
                            if (ioPermission != null) {
                                ioPermission.setCaptchaAllowed(null, CAPTCHA.BLOCKALL);
                            }
                            CaptchaSettings.CFG.setLastCancelOption(2);
                            break;
                        }
                    } catch (DialogClosedException e1) {
                        e1.printStackTrace();
                    } catch (DialogCanceledException e1) {
                        e1.printStackTrace();
                    } catch (StorageException e1) {
                        e1.printStackTrace();
                    }
                }
            }
        } catch (MalformedURLException e) {
            e.printStackTrace();
        }

        return null;
    }

    public IOPermission getIOPermission() {
        return ioPermission;
    }

}