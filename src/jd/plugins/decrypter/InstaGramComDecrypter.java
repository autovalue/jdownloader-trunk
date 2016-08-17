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

import java.util.ArrayList;
import java.util.LinkedHashMap;

import jd.PluginWrapper;
import jd.config.SubConfiguration;
import jd.controlling.AccountController;
import jd.controlling.ProgressController;
import jd.http.Browser;
import jd.parser.Regex;
import jd.plugins.Account;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
import jd.plugins.PluginForDecrypt;
import jd.plugins.PluginForHost;
import jd.utils.JDUtilities;

import org.appwork.utils.StringUtils;
import org.jdownloader.scripting.JavaScriptEngineFactory;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 3, names = { "instagram.com" }, urls = { "https?://(www\\.)?instagram\\.com/(?!p/)[^/]+" }) 
public class InstaGramComDecrypter extends PluginForDecrypt {

    public InstaGramComDecrypter(PluginWrapper wrapper) {
        super(wrapper);
    }

    @SuppressWarnings({ "unchecked", "rawtypes", "deprecation" })
    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        br = new Browser();
        final ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        // https and www. is required!
        String parameter = param.toString().replaceFirst("^http://", "https://").replaceFirst("://in", "://www.in");
        if (!parameter.endsWith("/")) {
            /* Add slash to the end to prevent 302 redirect to speed up the crawl process a tiny bit. */
            parameter += "/";
        }
        final PluginForHost hostplugin = JDUtilities.getPluginForHost("instagram.com");
        boolean logged_in = false;
        final Account aa = AccountController.getInstance().getValidAccount(hostplugin);
        if (aa != null) {
            /* Login whenever possible */
            try {
                jd.plugins.hoster.InstaGramCom.login(this.br, aa, false);
                logged_in = true;
            } catch (final Throwable e) {
            }
        }
        jd.plugins.hoster.InstaGramCom.prepBR(this.br);
        br.getPage(parameter);
        if (br.getHttpConnection().getResponseCode() == 404 || !this.br.containsHTML("user\\?username=.+")) {
            decryptedLinks.add(this.createOfflinelink(parameter));
            return decryptedLinks;
        }
        final String username_url = new Regex(parameter, "instagram\\.com/([^/]+)").getMatch(0);
        final String json = br.getRegex(">window\\._sharedData = (\\{.*?);</script>").getMatch(0);
        final String id_owner = br.getRegex("\"owner\": ?\\{\"id\": ?\"(\\d+)\"\\}").getMatch(0);
        if (json == null) {
            return null;
        }
        LinkedHashMap<String, Object> entries = (LinkedHashMap<String, Object>) JavaScriptEngineFactory.jsonToJavaObject(json);
        final boolean isPrivate = ((Boolean) JavaScriptEngineFactory.walkJson(entries, "entry_data/ProfilePage/{0}/user/is_private")).booleanValue();

        final FilePackage fp = FilePackage.getInstance();
        fp.setName(username_url);

        final boolean abort_on_rate_limit_reached = SubConfiguration.getConfig(this.getHost()).getBooleanProperty(jd.plugins.hoster.InstaGramCom.QUIT_ON_RATE_LIMIT_REACHED, jd.plugins.hoster.InstaGramCom.defaultQUIT_ON_RATE_LIMIT_REACHED);
        String nextid = (String) JavaScriptEngineFactory.walkJson(entries, "entry_data/ProfilePage/{0}/user/media/page_info/end_cursor");
        final String maxid = (String) JavaScriptEngineFactory.walkJson(entries, "entry_data/ProfilePage/{0}/__get_params/max_id");
        ArrayList<Object> resource_data_list = (ArrayList) JavaScriptEngineFactory.walkJson(entries, "entry_data/ProfilePage/{0}/user/media/nodes");
        final long count = JavaScriptEngineFactory.toLong(JavaScriptEngineFactory.walkJson(entries, "entry_data/ProfilePage/{0}/user/media/count"), -1);
        if (isPrivate && !logged_in && count != -1 && resource_data_list == null) {
            logger.info("Cannot parse url as profile is private");
            decryptedLinks.add(this.createOfflinelink(parameter));
            return decryptedLinks;
        }

        if (id_owner == null) {
            // this isn't a error persay! check https://www.instagram.com/israbox/
            return decryptedLinks;
        }

        int page = 0;
        do {
            if (this.isAbort()) {
                logger.info("User aborted decryption");
                return decryptedLinks;
            }
            if (page > 0) {
                Browser br = null;
                // prepBRAjax(br, username_url, maxid);
                int retrycounter = 1;
                int errorcounter_403_wtf = 0;
                int errorcounter_429_ratelimit_reached = 0;
                boolean failed = true;
                int responsecode;

                /* Access next page - 403 error may happen once for logged in users - reason unknown - will work fine on 2nd request! */
                do {
                    if (this.isAbort()) {
                        logger.info("User aborted decryption");
                        return decryptedLinks;
                    }

                    br = this.br.cloneBrowser();
                    if (retrycounter > 1) {
                        if (abort_on_rate_limit_reached) {
                            logger.info("abort_on_rate_limit_reached setting active --> Rate limit has been reached --> Aborting");
                            return decryptedLinks;
                        }
                        /*
                         * Try to bypass rate-limit - usually kicks in after about 4000 items and it is bound to IP, not User-Agent or
                         * cookies! Also we need to continue with the cookies we got at the beginning otherwise we'll get a 403! Aftzer
                         * about 60 seconds wait we should be able to continue but it might happen than we only get one batch of items and
                         * are blocked again then.
                         */
                        this.sleep(30000, param);
                    }
                    prepBRAjax(br, username_url, maxid);
                    final String p = "q=ig_user(" + id_owner + ")+%7B+media.after(" + nextid + "%2C+12)+%7B%0A++count%2C%0A++nodes+%7B%0A++++caption%2C%0A++++code%2C%0A++++comments+%7B%0A++++++count%0A++++%7D%2C%0A++++date%2C%0A++++dimensions+%7B%0A++++++height%2C%0A++++++width%0A++++%7D%2C%0A++++display_src%2C%0A++++id%2C%0A++++is_video%2C%0A++++likes+%7B%0A++++++count%0A++++%7D%2C%0A++++owner+%7B%0A++++++id%0A++++%7D%2C%0A++++thumbnail_src%0A++%7D%2C%0A++page_info%0A%7D%0A+%7D&ref=users%3A%3Ashow";
                    br.postPage("https://www." + this.getHost() + "/query/", p);
                    responsecode = br.getHttpConnection().getResponseCode();
                    if (responsecode == 403 || responsecode == 429) {
                        failed = true;
                        if (responsecode == 403) {
                            errorcounter_403_wtf++;
                        } else {
                            errorcounter_429_ratelimit_reached++;
                        }
                        logger.info("403 errors so far: " + errorcounter_403_wtf);
                        logger.info("429 errors so far: " + errorcounter_429_ratelimit_reached);
                    } else {
                        failed = false;
                    }
                    retrycounter++;
                    /* Stop on too many 403s as 403 is not a rate limit issue! */
                } while (failed && retrycounter <= 300 && errorcounter_403_wtf < 20);

                if (failed) {
                    logger.warning("Failed to bypass rate-limit!");
                    return decryptedLinks;
                } else if (responsecode == 439) {
                    logger.info("Seems like user is using an unverified account - cannot grab more items");
                    break;
                }
                entries = (LinkedHashMap<String, Object>) JavaScriptEngineFactory.jsonToJavaObject(br.toString());
                resource_data_list = (ArrayList) JavaScriptEngineFactory.walkJson(entries, "media/nodes");
                nextid = (String) JavaScriptEngineFactory.walkJson(entries, "media/page_info/end_cursor");
            }
            if (resource_data_list.size() == 0) {
                logger.info("Found no new links on page " + page + " --> Stopping decryption");
                break;
            }
            for (final Object o : resource_data_list) {
                entries = (LinkedHashMap<String, Object>) o;
                final String linkid = (String) entries.get("code");
                final boolean isVideo = ((Boolean) entries.get("is_video")).booleanValue();
                final String filename;
                final String ext;
                if (isVideo) {
                    ext = ".mp4";
                } else {
                    ext = ".jpg";
                }
                if (StringUtils.isNotEmpty(username_url)) {
                    filename = username_url + " - " + linkid + ext;
                } else {
                    filename = linkid + ext;
                }
                final String content_url = "https://www.instagram.com/p/" + linkid;
                final DownloadLink dl = this.createDownloadlink(content_url);
                dl.setContentUrl(content_url);
                dl.setLinkID(getHost() + "://" + linkid);
                dl._setFilePackage(fp);
                dl.setAvailable(true);
                dl.setName(filename);
                if (isPrivate) {
                    /*
                     * Without account, private urls look exactly the same as offline urls --> Save private status for better host plugin
                     * errorhandling.
                     */
                    dl.setProperty("private_url", true);
                }
                decryptedLinks.add(dl);
                distribute(dl);
            }
            page++;
        } while (nextid != null);

        return decryptedLinks;
    }

    private void prepBRAjax(final Browser br, final String username_url, final String maxid) {
        final String csrftoken = br.getCookie("instagram.com", "csrftoken");
        br.getHeaders().put("X-Requested-With", "XMLHttpRequest");
        br.getHeaders().put("X-Instagram-AJAX", "1");
        br.getHeaders().put("Accept", "application/json, text/javascript, */*; q=0.01");
        br.getHeaders().put("Content-Type", "application/x-www-form-urlencoded; charset=UTF-8");
        if (csrftoken != null) {
            br.getHeaders().put("X-CSRFToken", csrftoken);
        }
        if (maxid != null) {
            br.getHeaders().put("Referer", "https://www.instagram.com/" + username_url + "/?max_id=" + maxid);
        }
        br.setCookie(this.getHost(), "ig_vw", "1680");
    }
}
