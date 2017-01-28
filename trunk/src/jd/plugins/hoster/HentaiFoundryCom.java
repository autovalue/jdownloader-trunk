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

package jd.plugins.hoster;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import jd.PluginWrapper;
import jd.config.Property;
import jd.http.Browser;
import jd.http.Browser.BrowserException;
import jd.http.Cookie;
import jd.http.Cookies;
import jd.http.Request;
import jd.http.URLConnectionAdapter;
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.plugins.Account;
import jd.plugins.Account.AccountType;
import jd.plugins.AccountInfo;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "hentai-foundry.com" }, urls = { "http://www\\.hentai\\-foundrydecrypted\\.com/pictures/user/[A-Za-z0-9\\-_]+/\\d+|http://www\\.hentai\\-foundry\\.com/stories/user/[A-Za-z0-9\\-_]+/\\d+/[A-Za-z0-9\\-_]+\\.pdf" })
public class HentaiFoundryCom extends PluginForHost {

    public HentaiFoundryCom(PluginWrapper wrapper) {
        super(wrapper);
        this.enablePremium("http://www.hentai-foundry.com/users/create");
    }

    /* DEV NOTES */
    // Tags:
    // protocol: no https
    // other: connections & downloads limites cause we re downloading small files

    private static final String type_direct_pdf = "http://www\\.hentai\\-foundry\\.com/stories/user/[A-Za-z0-9\\-_]+/\\d+/[A-Za-z0-9\\-_]+\\.pdf";
    private static final String type_picture    = "http://www\\.hentai\\-foundry\\.com/pictures/user/[A-Za-z0-9\\-_]+/\\d+";

    private String              dllink          = null;

    @Override
    public String getAGBLink() {
        return "http://www.hentai-foundry.com/";
    }

    public void correctDownloadLink(final DownloadLink link) {
        link.setUrlDownload(link.getDownloadURL().replace("hentai-foundrydecrypted.com/", "hentai-foundry.com/"));
    }

    public static String getFID(final String url) {
        return new Regex(url, "/user/[A-Za-z0-9\\-_]+/(\\d+)").getMatch(0);
    }

    @SuppressWarnings("deprecation")
    @Override
    public AvailableStatus requestFileInformation(final DownloadLink downloadLink) throws IOException, PluginException {
        dllink = null;
        String filename = null;
        String ext = null;
        final String fid = getFID(downloadLink.getDownloadURL());
        this.setBrowserExclusive();
        br.setFollowRedirects(true);
        if (downloadLink.getDownloadURL().matches(type_direct_pdf)) {
            dllink = downloadLink.getDownloadURL() + "?enterAgree=1&size=0";
            ext = ".pdf";
            filename = fid + "_" + new Regex(downloadLink.getDownloadURL(), "([A-Za-z0-9\\-_]+\\.pdf)$").getMatch(0);
        } else {
            br.getPage(downloadLink.getDownloadURL() + "?enterAgree=1&size=0");
            if (br.getHttpConnection().getResponseCode() == 404) {
                throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            }
            filename = br.getRegex("<title>([^<>]*?) - Hentai Foundry</title>").getMatch(0);
            dllink = br.getRegex("(//pictures\\.hentai-foundry\\.com/{1,}[^<>\"]*?)\"").getMatch(0);
            if (filename == null || dllink == null) {
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
            dllink = Request.getLocation(Encoding.htmlDecode(dllink), br.getRequest());
            filename = fid + "_" + Encoding.htmlDecode(filename);
            filename = filename.trim();
            filename = encodeUnicode(filename);
        }
        if (ext == null) {
            ext = getFileNameExtensionFromString(dllink, ".png");
        }
        if (!filename.endsWith(ext)) {
            filename += ext;
        }
        downloadLink.setFinalFileName(filename);
        final Browser br2 = br.cloneBrowser();
        // In case the link redirects to the finallink
        br2.setFollowRedirects(true);
        URLConnectionAdapter con = null;

        try {
            con = br.openHeadConnection(dllink);
        } catch (final BrowserException e) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        } finally {
            try {
                con.disconnect();
            } catch (final Throwable e) {
            }
        }
        if (!con.getContentType().contains("html")) {
            downloadLink.setDownloadSize(con.getLongContentLength());
        } else {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        downloadLink.setProperty("directlink", dllink);
        return AvailableStatus.TRUE;
    }

    @Override
    public void handleFree(final DownloadLink downloadLink) throws Exception, PluginException {
        requestFileInformation(downloadLink);
        doFree(downloadLink, true, 1, "free_directlink");
    }

    private void doFree(final DownloadLink downloadLink, final boolean resumable, final int maxchunks, final String directlinkproperty) throws Exception, PluginException {
        dl = jd.plugins.BrowserAdapter.openDownload(br, downloadLink, dllink, resumable, maxchunks);
        if (dl.getConnection().getContentType().contains("html")) {
            if (dl.getConnection().getResponseCode() == 403) {
                throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Server error 403", 60 * 60 * 1000l);
            } else if (dl.getConnection().getResponseCode() == 404) {
                throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Server error 404", 60 * 60 * 1000l);
            }
            br.followConnection();
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        dl.startDownload();
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return 5;
    }

    private static final String MAINPAGE = "http://hentai-foundry.com";
    private static Object       LOCK     = new Object();

    @SuppressWarnings("unchecked")
    public static void login(final Browser br, final Account account, final boolean force) throws Exception {
        synchronized (LOCK) {
            try {
                // Load cookies
                br.setCookiesExclusive(true);
                final Object ret = account.getProperty("cookies", null);
                boolean acmatch = Encoding.urlEncode(account.getUser()).equals(account.getStringProperty("name", Encoding.urlEncode(account.getUser())));
                if (acmatch) {
                    acmatch = Encoding.urlEncode(account.getPass()).equals(account.getStringProperty("pass", Encoding.urlEncode(account.getPass())));
                }
                if (acmatch && ret != null && ret instanceof HashMap<?, ?> && !force) {
                    final HashMap<String, String> cookies = (HashMap<String, String>) ret;
                    if (account.isValid()) {
                        for (final Map.Entry<String, String> cookieEntry : cookies.entrySet()) {
                            final String key = cookieEntry.getKey();
                            final String value = cookieEntry.getValue();
                            br.setCookie(MAINPAGE, key, value);
                        }
                        return;
                    }
                }
                br.setFollowRedirects(true);
                br.getPage("http://www.hentai-foundry.com/?enterAgree=1&size=0");
                final String csrftoken = br.getRegex("value=\"([a-z0-9]+)\" name=\"YII_CSRF_TOKEN\"").getMatch(0);
                if (csrftoken == null) {
                    if ("de".equalsIgnoreCase(System.getProperty("user.language"))) {
                        throw new PluginException(LinkStatus.ERROR_PREMIUM, "\r\nPlugin defekt, bitte den JDownloader Support kontaktieren!", PluginException.VALUE_ID_PREMIUM_DISABLE);
                    } else if ("pl".equalsIgnoreCase(System.getProperty("user.language"))) {
                        throw new PluginException(LinkStatus.ERROR_PREMIUM, "\r\nBłąd wtyczki, skontaktuj się z Supportem JDownloadera!", PluginException.VALUE_ID_PREMIUM_DISABLE);
                    } else {
                        throw new PluginException(LinkStatus.ERROR_PREMIUM, "\r\nPlugin broken, please contact the JDownloader Support!", PluginException.VALUE_ID_PREMIUM_DISABLE);
                    }
                }
                br.postPage("/site/login", "LoginForm%5BrememberMe%5D=1&YII_CSRF_TOKEN=" + csrftoken + "&LoginForm%5Busername%5D=" + Encoding.urlEncode(account.getUser()) + "&LoginForm%5Bpassword%5D=" + Encoding.urlEncode(account.getPass()));
                if (!br.containsHTML("site/logout\\'>Logout</a>")) {
                    if ("de".equalsIgnoreCase(System.getProperty("user.language"))) {
                        throw new PluginException(LinkStatus.ERROR_PREMIUM, "\r\nUngültiger Benutzername oder ungültiges Passwort!\r\nSchnellhilfe: \r\nDu bist dir sicher, dass dein eingegebener Benutzername und Passwort stimmen?\r\nFalls dein Passwort Sonderzeichen enthält, ändere es und versuche es erneut!", PluginException.VALUE_ID_PREMIUM_DISABLE);
                    } else {
                        throw new PluginException(LinkStatus.ERROR_PREMIUM, "\r\nInvalid username/password!\r\nQuick help:\r\nYou're sure that the username and password you entered are correct?\r\nIf your password contains special characters, change it (remove them) and try again!", PluginException.VALUE_ID_PREMIUM_DISABLE);
                    }
                }
                // Save cookies
                final HashMap<String, String> cookies = new HashMap<String, String>();
                final Cookies add = br.getCookies(MAINPAGE);
                for (final Cookie c : add.getCookies()) {
                    cookies.put(c.getKey(), c.getValue());
                }
                account.setProperty("name", Encoding.urlEncode(account.getUser()));
                account.setProperty("pass", Encoding.urlEncode(account.getPass()));
                account.setProperty("cookies", cookies);
            } catch (final PluginException e) {
                account.setProperty("cookies", Property.NULL);
                throw e;
            }
        }
    }

    @SuppressWarnings("deprecation")
    @Override
    public AccountInfo fetchAccountInfo(final Account account) throws Exception {
        final AccountInfo ai = new AccountInfo();
        try {
            login(this.br, account, true);
        } catch (PluginException e) {
            account.setValid(false);
            throw e;
        }
        ai.setUnlimitedTraffic();
        account.setType(AccountType.FREE);
        /* free accounts can still have captcha */
        account.setMaxSimultanDownloads(5);
        account.setConcurrentUsePossible(false);
        ai.setStatus("Free Account");
        account.setValid(true);
        return ai;
    }

    @Override
    public void handlePremium(final DownloadLink link, final Account account) throws Exception {
        requestFileInformation(link);
        login(this.br, account, false);
        br.setFollowRedirects(false);
        br.getPage(link.getDownloadURL());
        doFree(link, true, 1, "account_free_directlink");
    }

    @Override
    public void reset() {
    }

    @Override
    public void resetPluginGlobals() {
    }

    @Override
    public void resetDownloadlink(DownloadLink link) {
    }
}
