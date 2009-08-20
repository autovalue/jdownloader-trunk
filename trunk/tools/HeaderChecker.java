import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;

import javax.swing.JFileChooser;

import jd.nutils.io.JDIO;
import jd.parser.Regex;
import jd.utils.JDUtilities;

public class HeaderChecker {

    public static void main(String[] args) {
        HeaderChecker hc = new HeaderChecker(HeaderChecker.START_DIRECTORY);
        hc.check();
//        hc.quickCheck();
    }

    private static final boolean DEBUG = !false;
    private static final boolean FIXIT = !true;

    private static final String[] IGNORE_NAMES = new String[] { "junique", "jaxe", "template", "ShortCuts", "DiffMatchPatch", "Tester", "Base64", "XTrustProvider", "HTMLEntities" };

    private static String LICENSE_PREFIX;
    private static String LICENSE;

    private static final File START_DIRECTORY = new File("C:\\Dokumente und Einstellungen\\Towelie\\Eigene Dateien\\Java\\jd\\src\\");

    private File dir;

    private ArrayList<File> files;

    public HeaderChecker(File dir) {
        LICENSE = JDIO.getLocalFile(JDUtilities.getResourceFile("gpl.header"));
        LICENSE_PREFIX = LICENSE.substring(0, Math.min(LICENSE.indexOf('\r'), LICENSE.indexOf('\n')));

        if (dir == null || !dir.exists()) {
            JFileChooser fc = new JFileChooser();
            fc.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
            fc.setCurrentDirectory(new File("."));
            if (fc.showOpenDialog(null) == JFileChooser.APPROVE_OPTION) {
                dir = fc.getSelectedFile();
            } else {
                System.out.println("Keine Datei ausgewählt!");
                System.exit(0);
            }
        }

        this.dir = dir;
        this.files = HeaderChecker.getSourceFiles(dir);
    }

    public void quickCheck() {
        String content;
        int count;
        for (File file : getSourceFiles(dir)) {
            content = JDIO.getLocalFile(file);
            count = new Regex(content, "jDownloader - Downloadmanager").count();
            if (count > 1) {
                System.out.println(prepareFilename(file));
            }
        }
    }

    public void check() {
        ArrayList<File> filesIgnore = new ArrayList<File>();
        ArrayList<File> filesMissing = new ArrayList<File>();
        ArrayList<File> filesOk = new ArrayList<File>();
        for (File file : files) {
            debug("Checking " + prepareFilename(file) + ": ");
            if (ignoreFile(file)) {
                debug("IGNORE");
                filesIgnore.add(file);
            } else if (!getFirstLine(file).startsWith(HeaderChecker.LICENSE_PREFIX)) {
                debug("MISSING");
                filesMissing.add(file);
            } else {
                debug("OK");
                filesOk.add(file);
            }
        }
        debug("======================================================");
        debug("Checking complete!");
        debug("OK:       " + filesOk.size());
        debug("Ignored:  " + filesIgnore.size());
        debug("Missing:  " + filesMissing.size());
        debug("======================================================");
        for (File file : filesMissing) {
            System.out.println(prepareFilename(file));
            if (FIXIT) System.out.println(fixFile(file) ? "FIXED" : "NOT FIXED");
        }
    }

    private boolean fixFile(File file) {
        String content = JDIO.getLocalFile(file);
        if (content.contains(LICENSE_PREFIX)) return false;

        StringBuilder sb = new StringBuilder();
        sb.append(LICENSE);
        sb.append(content);

        return JDIO.writeLocalFile(file, sb.toString());
    }

    private void debug(String string) {
        if (DEBUG) System.out.println(string);
    }

    private boolean ignoreFile(File file) {
        String filename = file.getAbsolutePath();
        for (String ignoreName : HeaderChecker.IGNORE_NAMES) {
            if (filename.contains(ignoreName)) return true;
        }
        return false;
    }

    private String prepareFilename(File file) {
        String filename = file.getAbsolutePath();
        filename = filename.substring(dir.getAbsolutePath().length() + 1, filename.length() - 5);
        filename = filename.replaceAll("(\\\\)", ".");
        return filename;
    }

    private static String getFirstLine(File file) {
        if (file == null || !file.exists()) return null;
        try {
            BufferedReader f = new BufferedReader(new InputStreamReader(new FileInputStream(file), "UTF8"));
            String firstLine = f.readLine();
            f.close();
            return firstLine;
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    private static ArrayList<File> getSourceFiles(File dir) {
        if (dir == null) return null;

        ArrayList<File> files = new ArrayList<File>();

        for (File file : dir.listFiles()) {
            if (file.isDirectory()) {
                files.addAll(getSourceFiles(file));
            } else if (file.getName().toLowerCase().endsWith(".java")) {
                files.add(file);
            }
        }

        return files;
    }

}
