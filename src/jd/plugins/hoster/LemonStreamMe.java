//    jDownloader - Downloadmanager
//    Copyright (C) 2013  JD-Team support@jdownloader.org
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

import org.jdownloader.plugins.components.RefreshSessionLink;

import jd.PluginWrapper;
import jd.http.Browser;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;
import jd.utils.JDUtilities;

@HostPlugin(revision = "$Revision: 21813 $", interfaceVersion = 2, names = { "streaming.lemonstream.me" }, urls = { "https?://streaming\\.lemonstream\\.me(?::\\d+)?/[a-f0-9]{32,}/[^\"'\\s<>]+" })
public class LemonStreamMe extends PluginForHost {

    // raztoki embed video player template.

    private String dllink = null;

    public LemonStreamMe(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public String getAGBLink() {
        return "";
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return -1;
    }

    @Override
    public void handleFree(DownloadLink downloadLink) throws Exception {
        requestFileInformation(downloadLink);
        dllink = downloadLink.getDownloadURL();
        br.setCurrentURL(downloadLink.getStringProperty("source_url", null));
        dl = new jd.plugins.BrowserAdapter().openDownload(br, downloadLink, dllink, true, 1);
        if (!dl.getConnection().isOK()) {
            dl.getConnection().disconnect();
            dllink = refreshDirectlink(downloadLink);
            // save this for resume events outside of while loop (GUI abort/disable/stop download)
            downloadLink.setPluginPatternMatcher(dllink);
            br = new Browser();
            br.setCurrentURL(downloadLink.getStringProperty("source_url", null));
            dl = new jd.plugins.BrowserAdapter().openDownload(br, downloadLink, dllink, true, 1);
            if (!dl.getConnection().isOK()) {
                br.followConnection();
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
        }
        if (downloadLink.isNameSet()) {
            // maybe we set a filename but doesn't have extension yet!
            String fileName = downloadLink.getName();
            final String ext = jd.plugins.hoster.DirectHTTP.getExtensionFromMimeType(dl.getConnection().getContentType());
            if (ext != null && !fileName.contains("." + ext)) {
                fileName = fileName + "." + ext;
                downloadLink.setFinalFileName(fileName);
            }
        }
        dl.startDownload();
    }

    @Override
    public AvailableStatus requestFileInformation(DownloadLink downloadLink) throws Exception {
        return AvailableStatus.TRUE;
    }

    /**
     * Refresh directurls from external providers
     *
     * @throws Exception
     */
    private String refreshDirectlink(final DownloadLink downloadLink) throws Exception {
        final String refresh_url_plugin = downloadLink.getStringProperty("refresh_url_plugin", null);
        if (refresh_url_plugin != null) {
            return ((RefreshSessionLink) JDUtilities.getPluginForDecrypt(refresh_url_plugin)).refreshVideoDirectUrl(downloadLink);
        }
        return null;
    }

    @Override
    public void reset() {
    }

    @Override
    public void resetDownloadlink(DownloadLink link) {
    }

    @Override
    public void resetPluginGlobals() {
    }
}