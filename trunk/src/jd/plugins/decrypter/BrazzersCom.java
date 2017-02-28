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

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;

import jd.PluginWrapper;
import jd.config.SubConfiguration;
import jd.controlling.AccountController;
import jd.controlling.ProgressController;
import jd.http.Browser;
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.plugins.Account;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
import jd.plugins.PluginForDecrypt;
import jd.plugins.PluginForHost;
import jd.plugins.components.SiteType.SiteTemplate;
import jd.utils.JDUtilities;

import org.appwork.utils.formatter.SizeFormatter;
import org.jdownloader.scripting.JavaScriptEngineFactory;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 3, names = { "brazzers.com" }, urls = { "https?://ma\\.brazzers\\.com/(?:scene/view/\\d+/[a-z0-9\\-]+/?|scene/hqpics/\\d+/?)|https?://(?:www\\.)?brazzers\\.com/(?:scenes/view/id/\\d+/[a-z0-9\\-]+/?|embed/\\d+)" })
public class BrazzersCom extends PluginForDecrypt {

    public BrazzersCom(PluginWrapper wrapper) {
        super(wrapper);
    }

    public static final String  type_video_free    = "https?://(?:www\\.)?brazzers\\.com/scenes/view/id/\\d+/?(?:[a-z0-9\\-]+/?)?";
    private static final String type_video_premium = "https?://ma\\.brazzers\\.com/scene/view/\\d+/[a-z0-9\\-]+/?";
    private static final String type_video_embed   = "https?://(?:www\\.)?brazzers\\.com/embed/\\d+";
    private static final String type_pics          = "https?://ma\\.brazzers\\.com/scene/hqpics/\\d+/?";

    private static final String brazzers_decrypted = "http://brazzersdecrypted.com/scenes/view/id/%s/";

    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        final String parameter = param.toString();
        final PluginForHost plg = JDUtilities.getPluginForHost(this.getHost());
        final Account aa = AccountController.getInstance().getValidAccount(plg);
        final String fid = new Regex(parameter, "^.+/(\\d+)/?").getMatch(0);
        final SubConfiguration cfg = SubConfiguration.getConfig(this.getHost());
        if (aa != null) {
            ((jd.plugins.hoster.BrazzersCom) plg).login(this.br, aa, false);
        }
        if (parameter.matches(type_pics)) {
            if (aa == null) {
                logger.info("Account needed to use this crawler for this linktype (pictures)");
                return decryptedLinks;
            }
            this.br.getPage(parameter);
        } else {
            final List<Account> moch_accounts = AccountController.getInstance().getMultiHostAccounts(this.getHost());
            if (aa == null && (moch_accounts == null || moch_accounts.size() == 0)) {
                logger.info("Account needed to use this crawler for this linktype (videos)");
                return decryptedLinks;
            }
            if (aa == null && moch_accounts != null && moch_accounts.size() > 0) {
                /* Only MOCH download possible --> Add link for hostplugin */
                final DownloadLink dl = this.createDownloadlink(String.format(brazzers_decrypted, fid));
                decryptedLinks.add(dl);
                return decryptedLinks;
            } else {
                /* Normal host account is available */
                this.br.getPage(getVideoUrlPremium(fid));
            }
        }
        if (isOffline(this.br)) {
            decryptedLinks.add(this.createOfflinelink(parameter));
            return decryptedLinks;
        }
        String title = this.br.getRegex(">([^<>\"]+)<span class=\"icon\\-new").getMatch(0);
        if (title == null) {
            title = this.br.getRegex("<title>([^<>\"]+) \\- BRAZZERS</title>").getMatch(0);
        }
        if (title == null) {
            /* Fallback */
            title = fid;
        }
        title = Encoding.htmlDecode(title).trim();
        if (parameter.matches(type_video_premium) || parameter.matches(type_video_free) || parameter.matches(type_video_embed)) {
            final String base_url = new Regex(this.br.getURL(), "(https?://[^/]+)/").getMatch(0);
            final String htmldownload = this.br.getRegex("<ul id=\"video\\-download\\-format\">(.*?)</ul>").getMatch(0);
            final String[] dlinfo = htmldownload.split("</li>");
            for (final String video : dlinfo) {
                final String dlurl = new Regex(video, "(/download/[^<>\"]+/)\"").getMatch(0);
                final String quality = new Regex(video, "<span>([^<>\"]+)</span>").getMatch(0);
                final String filesize = new Regex(video, "<var>([^<>\"]+)</var>").getMatch(0);
                final String quality_url = dlurl != null ? new Regex(dlurl, "/([^/]+)/?$").getMatch(0) : null;
                if (dlurl == null || quality == null || filesize == null || quality_url == null) {
                    continue;
                } else if (!cfg.getBooleanProperty("GRAB_" + quality_url, true)) {
                    /* Skip unwanted qualities - only add what the user wants to have. */
                    continue;
                }
                final DownloadLink dl = this.createDownloadlink(base_url + dlurl);
                dl.setDownloadSize(SizeFormatter.getSize(filesize));
                dl.setName(title + "_" + quality + ".mp4");
                dl.setAvailable(true);
                dl.setProperty("fid", fid);
                dl.setProperty("quality", quality);
                decryptedLinks.add(dl);
            }
        } else {
            final String json_pictures = getPictureJson(this.br);
            if (json_pictures == null) {
                return null;
            }
            final String pic_format_string = getPicFormatString(this.br);
            final LinkedHashMap<String, Object> entries = (LinkedHashMap<String, Object>) JavaScriptEngineFactory.jsonToJavaMap(json_pictures);
            final int count_pics = (int) JavaScriptEngineFactory.toLong(entries.get("length"), 0);
            if (pic_format_string == null || count_pics == 0) {
                return null;
            }
            final DecimalFormat df = new DecimalFormat("0000");
            for (int i = 1; i <= count_pics; i++) {
                final String number_formatted = df.format(i);
                String finallink = String.format(pic_format_string, number_formatted);
                if (!finallink.startsWith("http://photos.bb.contentdef.com/")) {
                    /* WTF */
                    continue;
                }
                finallink = finallink.replace("http://photos.bb.contentdef.com/", "http://brazzersdecrypted.photos.bb.contentdef.com/");
                final DownloadLink dl = this.createDownloadlink(finallink);
                dl.setFinalFileName(title + "_" + number_formatted + ".jpg");
                dl.setAvailable(true);
                dl.setProperty("fid", fid);
                dl.setProperty("picnumber_formatted", number_formatted);
                decryptedLinks.add(dl);
            }
        }

        final FilePackage fp = FilePackage.getInstance();
        fp.setName(Encoding.htmlDecode(title.trim()));
        fp.addLinks(decryptedLinks);

        return decryptedLinks;
    }

    public static String getVideoUrlFree(final String fid) {
        return "http://www.brazzers.com/scenes/view/id/" + fid + "/";
    }

    public static String getVideoUrlPremium(final String fid) {
        return "http://ma.brazzers.com/scene/view/" + fid + "/";
    }

    public static String getPicUrl(final String fid) {
        return "http://ma.brazzers.com/scene/hqpics/" + fid + "/";
    }

    public static boolean isOffline(final Browser br) {
        return br.getHttpConnection().getResponseCode() == 404;
    }

    public static String getPictureJson(final Browser br) {
        return br.getRegex("var\\s*?gallerySource\\s*?=\\s*?(\\{.*?\\});").getMatch(0);
    }

    public static String getVideoJson(final Browser br) {
        return br.getRegex("\\.addVideoInfo\\((.*?)\\)").getMatch(0);
    }

    public static LinkedHashMap<String, Object> getVideoMapHttpStreams(final String json) {
        LinkedHashMap<String, Object> entries = null;
        try {
            entries = (LinkedHashMap<String, Object>) JavaScriptEngineFactory.jsonToJavaMap(json);
        } catch (final Throwable e) {
        }
        return getVideoMapHttpStreams(entries);
    }

    public static LinkedHashMap<String, Object> getVideoMapHttpStreams(LinkedHashMap<String, Object> entries) {
        try {
            entries = (LinkedHashMap<String, Object>) JavaScriptEngineFactory.walkJson(entries, "stream_info/http/paths");
        } catch (final Throwable e) {
        }
        return entries;
    }

    public static LinkedHashMap<String, Object> getVideoMapHttpDownloads(final String json) {
        LinkedHashMap<String, Object> entries = null;
        try {
            entries = (LinkedHashMap<String, Object>) JavaScriptEngineFactory.jsonToJavaMap(json);
        } catch (final Throwable e) {
        }
        return getVideoMapHttpDownloads(entries);
    }

    public static LinkedHashMap<String, Object> getVideoMapHttpDownloads(LinkedHashMap<String, Object> entries) {
        try {
            entries = (LinkedHashMap<String, Object>) JavaScriptEngineFactory.walkJson(entries, "downloadHttp/paths");
        } catch (final Throwable e) {
        }
        return entries;
    }

    public static String getPicFormatString(final Browser br) {
        String pic_format_string = null;
        try {
            final String json_pictures = getPictureJson(br);
            LinkedHashMap<String, Object> entries = (LinkedHashMap<String, Object>) JavaScriptEngineFactory.jsonToJavaMap(json_pictures);
            entries = (LinkedHashMap<String, Object>) entries.get("fullsize");
            final String path = (String) entries.get("path");
            final String hash = (String) entries.get("hash");
            if (path != null && !path.equals("") && hash != null && !hash.equals("")) {
                pic_format_string = path + "%s" + ".jpg" + hash;
            }
        } catch (final Throwable e) {
        }
        return pic_format_string;
    }

    @Override
    public SiteTemplate siteTemplateType() {
        return SiteTemplate.PornPortal;
    }

}
