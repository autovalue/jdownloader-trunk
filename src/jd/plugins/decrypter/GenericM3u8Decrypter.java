//jDownloader - Downloadmanager
//Copyright (C) 2009  JD-Team support@jdownloader.org
//
//This program is free software: you can redistribute it and/or modify
//it under the terms of the GNU General Public License as published by
//the Free Software Foundation, either version 3 of the License, or
//(at your option) any later version.
//
//This program is distributed in the hope that it will be useful,
//but WITHOUT ANY WARRANTY; without even the implied warranty of
//MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
//GNU General Public License for more details.
//
//You should have received a copy of the GNU General Public License
//along with this program.  If not, see <http://www.gnu.org/licenses/>.

package jd.plugins.decrypter;

import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import jd.PluginWrapper;
import jd.controlling.ProgressController;
import jd.controlling.linkcrawler.CrawledLink;
import jd.http.Browser;
import jd.http.Cookies;
import jd.http.URLConnectionAdapter;
import jd.http.requests.HeadRequest;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.PluginForDecrypt;

import org.appwork.utils.Regex;
import org.appwork.utils.StringUtils;

@DecrypterPlugin(revision = "$Revision: 26321 $", interfaceVersion = 3, names = { "m3u8" }, urls = { "https?://.+\\.m3u8($|\\?[^\\s<>\"']*)" })
public class GenericM3u8Decrypter extends PluginForDecrypt {

    @Override
    public Boolean siteTesterDisabled() {
        return Boolean.TRUE;
    }

    public GenericM3u8Decrypter(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        final ArrayList<DownloadLink> ret = new ArrayList<DownloadLink>();
        CrawledLink source = getCurrentLink();
        String referer = null;
        String cookiesString = null;
        while (source != null) {
            if (source.getDownloadLink() != null && StringUtils.equals(source.getURL(), param.getCryptedUrl())) {
                final DownloadLink downloadLink = source.getDownloadLink();
                cookiesString = downloadLink.getStringProperty("cookies", null);
                if (cookiesString != null) {
                    final String host = Browser.getHost(source.getURL());
                    br.setCookies(host, Cookies.parseCookies(cookiesString, host, null));
                }
            }
            if (!StringUtils.equals(source.getURL(), param.getCryptedUrl())) {
                if (source.getCryptedLink() != null) {
                    referer = source.getURL();
                    br.getPage(source.getURL());
                }
                break;
            } else {
                source = source.getSourceLink();
            }
        }
        br.getPage(param.getCryptedUrl());
        if (br.getHttpConnection() == null || br.getHttpConnection().getResponseCode() == 403 || br.getHttpConnection().getResponseCode() == 404) {
            // invalid link
            return ret;
        }
        if (br.containsHTML("#EXT-X-STREAM-INF")) {
            final ArrayList<String> infos = new ArrayList<String>();
            for (final String line : Regex.getLines(br.toString())) {
                if (StringUtils.startsWithCaseInsensitive(line, "concat") || StringUtils.contains(line, "file:")) {
                    continue;
                } else if (!line.startsWith("#")) {
                    final URL url = br.getURL(line);
                    final DownloadLink link = createDownloadlink(url.toString());
                    if (referer != null) {
                        link.setProperty("Referer", referer);
                    }
                    if (cookiesString != null) {
                        link.setProperty("cookies", cookiesString);
                    }
                    addToResults(ret, br, url, link);
                    infos.clear();
                } else {
                    infos.add(line);
                }
            }
        } else {
            final DownloadLink link = createDownloadlink("m3u8" + param.getCryptedUrl().substring(4));
            if (referer != null) {
                link.setProperty("Referer", referer);
            }
            if (cookiesString != null) {
                link.setProperty("cookies", cookiesString);
            }
            ret.add(link);
        }
        return ret;
    }

    private void addToResults(final List<DownloadLink> results, final Browser br, final URL url, final DownloadLink link) {
        if (StringUtils.endsWithCaseInsensitive(url.getPath(), ".m3u8")) {
            results.add(link);
        } else {
            final Browser brc = br.cloneBrowser();
            URLConnectionAdapter con = null;
            try {
                con = brc.openRequestConnection(new HeadRequest(url));
                if (con.isOK() && StringUtils.equalsIgnoreCase(con.getContentType(), "application/vnd.apple.mpegurl")) {
                    link.setPluginPatternMatcher("m3u8" + url.toString().substring(4));
                    results.add(link);
                }
            } catch (final Throwable e) {
                logger.log(e);
            } finally {
                if (con != null) {
                    con.disconnect();
                }
            }
        }
    }

}