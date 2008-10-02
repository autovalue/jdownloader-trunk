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

package jd.plugins.decrypt;

import java.io.File;
import java.util.ArrayList;
import java.util.regex.Pattern;

import jd.PluginWrapper;
import jd.http.Browser;
import jd.parser.Form;
import jd.parser.Regex;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterException;
import jd.plugins.DownloadLink;
import jd.plugins.Plugin;
import jd.plugins.PluginForDecrypt;

public class BestMovies extends PluginForDecrypt {

    static private final Pattern patternCaptcha_Needed = Pattern.compile("<img src=\"clockcaptcha.php\"");
    static private final Pattern patternCaptcha_Wrong = Pattern.compile("<b>Falsch</b>");
    static private final Pattern patternIframe = Pattern.compile("<iframe src=\"(.+?)\"", Pattern.DOTALL);

    public BestMovies(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public ArrayList<DownloadLink> decryptIt(CryptedLink param) throws Exception {
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        String parameter = param.toString();

        br.getPage(parameter);
        for (int retrycounter = 1; retrycounter <= 5; retrycounter++) {
            if (br.getRegex(patternCaptcha_Wrong).matches()) {
                /* Falscher Captcha, Seite neu laden */
                br.getPage(parameter);
            }

            if (br.getRegex(patternCaptcha_Needed).matches()) {
                /* Captcha vorhanden */
                File captchaFile = this.getLocalCaptchaFile(this);
                Browser.download(captchaFile, br.cloneBrowser().openGetConnection("http://crypt.best-movies.us/clockcaptcha.php"));
                String captchaCode = Plugin.getCaptchaCode(captchaFile, this, param);
                Form form = br.getForm(0);
                String time[] = new Regex(captchaCode, "(\\d+):(\\d+)").getRow(0);
                if (time.length != 2) throw new DecrypterException(DecrypterException.CAPTCHA);
                int hour = new Integer(time[0]);
                if (hour > 12) hour = hour - 12;
                form.put("clockhour", hour + "");
                form.put("clockmin", time[1]);
                br.submitForm(form);
            } else {
                /* Kein Captcha */
                String link = br.getRegex(patternIframe).getMatch(0);
                if (link != null) decryptedLinks.add(createDownloadlink(link));
            }
        }

        return decryptedLinks;
    }

    @Override
    public String getVersion() {
        String ret = new Regex("$Revision$", "\\$Revision: ([\\d]*?) \\$").getMatch(0);
        return ret == null ? "0.0" : ret;
    }
}
