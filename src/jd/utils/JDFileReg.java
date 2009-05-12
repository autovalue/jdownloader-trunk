//    jDownloader - Downloadmanager
//    Copyright (C) 2008  JD-Team support@jdownloader.org
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

package jd.utils;

import jd.config.SubConfiguration;
import jd.controlling.JDLogger;
import jd.gui.UserIO;
import jd.nutils.OSDetector;
import jd.nutils.io.JDIO;

public class JDFileReg {

    public static String createSetKey(String key, String valueName, String value) {
        StringBuilder sb = new StringBuilder();

        sb.append("\r\n[HKEY_CLASSES_ROOT\\" + key + "]");

        if (valueName != null && valueName.trim().length() > 0) {
            sb.append("\r\n\"" + valueName + "\"=\"" + value + "\"");
        } else {
            sb.append("\r\n@=\"" + value + "\"");
        }

        return sb.toString();
    }

    public static void unregisterFileExts() {
        JDUtilities.runCommand("cmd", new String[] { "/c", "regedit", "/S", JDUtilities.getResourceFile("tools/windows/uninstall.reg").getAbsolutePath() }, JDUtilities.getResourceFile("tmp").getAbsolutePath(), 600);
    }

    public static void registerFileExts() {
        if (!OSDetector.isWindows()) return;
        if (!SubConfiguration.getConfig("CNL2").getBooleanProperty("INSTALLED", false)) {
            StringBuilder sb = new StringBuilder();
            sb.append(createRegisterWinFileExt("jd"));
            sb.append(createRegisterWinFileExt("dlc"));
            sb.append(createRegisterWinFileExt("ccf"));
            sb.append(createRegisterWinFileExt("rsdf"));
            sb.append(createRegisterWinProtocol("jd"));
            sb.append(createRegisterWinProtocol("jdlist"));
            sb.append(createRegisterWinProtocol("dlc"));
            sb.append(createRegisterWinProtocol("ccf"));
            sb.append(createRegisterWinProtocol("rsdf"));
            JDIO.writeLocalFile(JDUtilities.getResourceFile("tmp/installcnl.reg"), "Windows Registry Editor Version 5.00\r\n\r\n\r\n\r\n" + sb.toString());

            if ((UserIO.getInstance().requestConfirmDialog(UserIO.NO_COUNTDOWN, JDLocale.L("gui.cnl.install.title", "Click'n'Load Installation"), JDLocale.L("gui.cnl.install.text", "Click'n'load is a very comfortable way to add links to JDownloader. \r\nTo install Click'n'Load, JDownloader has to set some registry entries. \r\nYou might have to confirm some Windows messages to continue."), JDTheme.II("gui.clicknload", 48, 48), null, null) & UserIO.RETURN_OK) > 0) {
                JDUtilities.runCommand("cmd", new String[] { "/c", "regedit", JDUtilities.getResourceFile("tmp/installcnl.reg").getAbsolutePath() }, JDUtilities.getResourceFile("tmp").getAbsolutePath(), 600);
                JDUtilities.runCommand("cmd", new String[] { "/c", "regedit", "/e", JDUtilities.getResourceFile("tmp/test.reg").getAbsolutePath(), "HKEY_CLASSES_ROOT\\.dlc" }, JDUtilities.getResourceFile("tmp").getAbsolutePath(), 600);
                if (JDUtilities.getResourceFile("tmp/test.reg").exists()) {

                    JDLogger.getLogger().info("Installed Click'n'Load and associated .*dlc,.*ccf,.*rsdf and .*jd with JDownloader. Uninstall with " + JDUtilities.getResourceFile("tools/windows/uninstall.reg"));
                } else {

                    UserIO.getInstance().requestConfirmDialog(UserIO.NO_CANCEL_OPTION, JDLocale.L("gui.cnl.install.error.title", "Click'n'Load Installation"), JDLocale.LF("gui.cnl.install.error.message", "Installation of CLick'n'Load failed. Try these alternatives:\r\n * Start JDownloader as Admin.\r\n * Try to execute %s manually.\r\n * Open Configuration->General->Click'n'load-> [Install].\r\nFor details, visit http://jdownloader.org/click-n-load.", JDUtilities.getResourceFile("tmp/installcnl.reg").getAbsolutePath()), JDTheme.II("gui.clicknload", 48, 48), null, null);

                    JDLogger.getLogger().severe("Installation of CLick'n'Load failed. Please try to start JDownloader as Admin. For details, visit http://jdownloader.org/click-n-load. Try to execute " + JDUtilities.getResourceFile("tmp/installcnl.reg").getAbsolutePath() + " manually");
                }

            }
            SubConfiguration.getConfig("CNL2").setProperty("INSTALLED", true);
            SubConfiguration.getConfig("CNL2").save();
        }
        JDUtilities.getResourceFile("tmp/test.reg").delete();

    }

    private static String createRegisterWinFileExt(String ext) {

        String command = JDUtilities.getResourceFile("JDownloader.exe").getAbsolutePath().replace("\\", "\\\\") + " \\\"%1\\\"";
        StringBuilder sb = new StringBuilder();
        sb.append("\r\n\r\n;Register fileextension ." + ext);
        sb.append(createSetKey("." + ext, "", "JDownloader " + ext + " file"));
        sb.append(createSetKey("JDownloader " + ext + " file" + "\\shell", "", "open"));
        sb.append(createSetKey("JDownloader " + ext + " file" + "\\DefaultIcon", "", JDUtilities.getResourceFile("JDownloader.exe").getAbsolutePath().replace("\\", "\\\\")));
        sb.append(createSetKey("JDownloader " + ext + " file" + "\\shell\\open\\command", "", command));
        return sb.toString();
    }

    private static String createRegisterWinProtocol(String p) {
        String command = JDUtilities.getResourceFile("JDownloader.exe").getAbsolutePath().replace("\\", "\\\\") + " --add-link \\\"%1\\\"";
        StringBuilder sb = new StringBuilder();
        sb.append("\r\n\r\n;Register Protocol " + p + "://jdownloader.org/sample." + p);
        sb.append(createSetKey(p, "", "JDownloader " + p));
        sb.append(createSetKey(p + "\\DefaultIcon", "", JDUtilities.getResourceFile("JDownloader.exe").getAbsolutePath().replace("\\", "\\\\")));
        sb.append(createSetKey(p + "\\shell", "", "open"));
        sb.append(createSetKey(p, "Url Protocol", ""));
        sb.append(createSetKey(p + "\\shell\\open\\command", "", command));
        return sb.toString();
    }
}
