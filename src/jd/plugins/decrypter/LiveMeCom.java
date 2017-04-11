package jd.plugins.decrypter;

import java.util.ArrayList;
import java.util.Map;

import jd.PluginWrapper;
import jd.controlling.ProgressController;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.PluginForDecrypt;

import org.appwork.storage.JSonStorage;
import org.appwork.storage.TypeRef;
import org.appwork.utils.Regex;
import org.appwork.utils.StringUtils;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "liveme.com" }, urls = { "http://(www\\.)?liveme\\.com/media/play/\\?videoid=\\d+" })
public class LiveMeCom extends PluginForDecrypt {

    public LiveMeCom(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public ArrayList<DownloadLink> decryptIt(CryptedLink parameter, ProgressController progress) throws Exception {
        final ArrayList<DownloadLink> ret = new ArrayList<DownloadLink>();
        final String videoid = new Regex(parameter.getCryptedUrl(), "(\\d+)$").getMatch(0);
        br.setFollowRedirects(true);
        br.getPage("https://live.ksmobile.net/live/queryinfo?videoid=" + videoid);
        final Map<String, Object> response = JSonStorage.restoreFromString(br.toString(), TypeRef.HASHMAP);
        final Map<String, Object> data = (Map<String, Object>) response.get("data");
        final Map<String, Object> video_info = (Map<String, Object>) data.get("video_info");
        final String title = (String) video_info.get("title");
        final String url = (String) video_info.get("videosource");
        final DownloadLink link;
        if (StringUtils.endsWithCaseInsensitive(url, "m3u8")) {
            link = createDownloadlink("m3u8" + url.substring(4));
        } else {
            link = createDownloadlink(url);
        }
        if (title != null) {
            link.setFinalFileName(title + ".mp4");
        }
        link.setContentUrl(parameter.getCryptedUrl());
        ret.add(link);
        return ret;
    }

}
