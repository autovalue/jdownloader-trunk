//    jDownloader - Downloadmanager
//    Copyright (C) 2008  JD-Team jdownloader@freenet.de
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

package jd.plugins.host;

import java.io.File;
import java.util.regex.Pattern;
import jd.http.HTTPConnection;
import jd.parser.Regex;
import jd.plugins.DownloadLink;
import jd.plugins.PluginForHost;
import jd.plugins.download.RAFDownload;

public class zShare extends PluginForHost {
    private static final String HOST = "zshare.net";

    static private final Pattern patternSupported = Pattern.compile("http://[\\w\\.]*?zshare\\.net/(download|video|image|audio|flash)/.*", Pattern.CASE_INSENSITIVE);

    public zShare() {
        super();
    }

    @Override
    public boolean doBotCheck(File file) {
        return false;
    }

    @Override
    public String getAGBLink() {
        return "http://www.zshare.net/TOS.html";
    }

    @Override
    public String getCoder() {
        return "JD-Team";
    }

    @Override
    public boolean getFileInformation(DownloadLink downloadLink) {
        try {
            br.setCookiesExclusive(true);br.clearCookies(HOST);
            br.getPage(downloadLink.getDownloadURL().replaceFirst("zshare.net/(download|video|audio|flash)", "zshare.net/image"));
            String[] fileInfo = br.getRegex("File Name: .*?<font color=\".666666\">(.*?)</font>.*?Image Size: <font color=\".666666\">([0-9\\.\\,]*)(.*?)</font></td>").getMatches()[0];
            downloadLink.setName(fileInfo[0]);
            try {
                double length = Double.parseDouble(fileInfo[1].replaceAll("\\,", "").trim());
                int bytes;
                String type = fileInfo[2].toLowerCase();
                if (type.equalsIgnoreCase("kb")) {
                    bytes = (int) (length * 1024);
                } else if (type.equalsIgnoreCase("mb")) {
                    bytes = (int) (length * 1024 * 1024);
                } else {
                    bytes = (int) length;
                }
                downloadLink.setDownloadSize(bytes);
            } catch (Exception e) {
            }
            // Datei ist noch verfuegbar
            return true;
        } catch (Exception e) {
            // TODO: handle exception
        }
        return false;
    }

    @Override
    public String getHost() {
        return HOST;
    }

    public String getPluginName() {
        return HOST;
    }

    @Override
    public Pattern getSupportedLinks() {
        return patternSupported;
    }

    @Override
    public String getVersion() {
        String ret = new Regex("$Revision$", "\\$Revision: ([\\d]*?) \\$").getMatch(0);
        return ret == null ? "0.0" : ret;
    }

    @Override
    public void handleFree(DownloadLink downloadLink) throws Exception {
        br.setCookiesExclusive(true);br.clearCookies(HOST);

        br.getPage(downloadLink.getDownloadURL().replaceFirst("zshare.net/(download|video|audio|flash)", "zshare.net/image"));

        Regex reg = br.getRegex("<img src=\"(http://[^\"]*?/download/[a-f0-9]*?/[\\d]*?/[\\d]*?/.*?)\"");

        String url = reg.getMatches()[0][0];

        HTTPConnection urlConnection = br.openGetConnection(url);
        dl = new RAFDownload(this, downloadLink, urlConnection);

        dl.startDownload();
    }

    public int getMaxSimultanFreeDownloadNum() {
        return 20;
    }
    
    @Override
    public void reset() {
        // TODO Automatisch erstellter Methoden-Stub
    }

    @Override
    public void resetPluginGlobals() {
        // TODO Automatisch erstellter Methoden-Stub
    }
}
