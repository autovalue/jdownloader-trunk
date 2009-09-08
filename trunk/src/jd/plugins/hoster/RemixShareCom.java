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

package jd.plugins.hoster;

import java.io.IOException;
import java.util.regex.Pattern;

import jd.PluginWrapper;
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.parser.html.Form;
import jd.plugins.DownloadLink;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.Plugin;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.utils.locale.JDL;

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = {"remixshare.com"}, urls = {"http://[\\w\\.]*?remixshare\\.com/.*?\\?file=[a-z0-9]+"}, flags = {0})
public class RemixShareCom extends PluginForHost {

    public RemixShareCom(PluginWrapper wrapper) {
        super(wrapper);
        this.setStartIntervall(500l);
    }

    // @Override
    public String getAGBLink() {
        return "http://remixshare.com/information/";
    }

    // @Override
    public AvailableStatus requestFileInformation(DownloadLink downloadLink) throws IOException, InterruptedException, PluginException {
        this.setBrowserExclusive();
        br.setCookie("http://remixshare.com", "lang_en", "english");
        br.setFollowRedirects(true);
        br.getPage(downloadLink.getDownloadURL());
        br.setFollowRedirects(false);
        if (br.containsHTML("Error Code: 500.")) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        String filename = Encoding.htmlDecode(br.getRegex(Pattern.compile("<span title='(.*?)'>", Pattern.CASE_INSENSITIVE)).getMatch(0));
        String filesize = br.getRegex("</span><span class='light'><br />([0-9\\.]+&nbsp;MB)&").getMatch(0);
        if (filename == null) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        downloadLink.setName(filename.trim());
        if(filesize != null) {
            filesize = filesize.replaceAll("&nbsp;", " ");
            downloadLink.setDownloadSize(Regex.getSize(filesize.replaceAll(",", "\\.")));
        }
        return AvailableStatus.TRUE;
    }

    // @Override
    /*
     * public String getVersion() { return getVersion("$Revision$"); }
     */

    // @Override
    public void handleFree(DownloadLink downloadLink) throws Exception {
        requestFileInformation(downloadLink);
        br.setFollowRedirects(false);

        if (br.containsHTML("Download password")) {
            Form pw = br.getFormbyProperty("name", "pass");
            String pass = downloadLink.getStringProperty("pass", null);
            if (pass == null) pass = Plugin.getUserInput("Password?", downloadLink);
            pw.put("passwd", pass);
            br.submitForm(pw);
            br.getPage(br.getRedirectLocation());
            if (br.containsHTML("Incorrect password entered")) {
                downloadLink.setProperty("pass", null);
                throw new PluginException(LinkStatus.ERROR_FATAL, JDL.L("plugins.errors.wrongpassword", "Password wrong"));
            } else {
                downloadLink.setProperty("pass", pass);
            }
        }

        Form down = br.getFormbyProperty("name", "downform");
        // this.sleep(12000, downloadLink); // uncomment when they find a better
        // way to force wait time
        br.submitForm(down);
        // br.openGetConnection(downloadLink.getDownloadURL());
        br.setFollowRedirects(true);
        dl = jd.plugins.BrowserAdapter.openDownload(br, downloadLink, br.getRedirectLocation(), false, 1);
        dl.startDownload();

    }

    // @Override
    public int getMaxSimultanFreeDownloadNum() {
        return 10;
    }

    // @Override
    public void reset() {
    }

    // @Override
    public void resetPluginGlobals() {
    }

    // @Override
    public void resetDownloadlink(DownloadLink link) {
        // TODO Auto-generated method stub
    }

}
