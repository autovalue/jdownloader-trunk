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
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Vector;
import java.util.regex.Pattern;

import jd.parser.SimpleMatches;
import jd.plugins.DownloadLink;
import jd.plugins.HTTP;
import jd.plugins.PluginForDecrypt;
import jd.plugins.PluginStep;
import jd.plugins.RequestInfo;

public class Filer extends PluginForDecrypt {
    static private String        host             = "filer.net";
    private String               version          = "0.1";
    static private final Pattern patternSupported = Pattern.compile("http://[\\w\\.]*?filer.net/folder/(.*)", Pattern.CASE_INSENSITIVE);
    static private final Pattern INFO             = Pattern.compile("(?s)<td><a href=\"\\/get\\/(.*?).html\">(.*?)</a></td>", Pattern.CASE_INSENSITIVE);

    public Filer() {
        super();
        steps.add(new PluginStep(PluginStep.STEP_DECRYPT, null));
        currentStep = steps.firstElement();
    }

    @Override
    public String getCoder() {
        return "JD-Team";
    }

    @Override
    public String getHost() {
        return host;
    }

    @Override
    public String getPluginID() {
        return "Filer-0.1";
    }

    @Override
    public String getPluginName() {
        return host;
    }

    @Override
    public Pattern getSupportedLinks() {
        return patternSupported;
    }

    @Override
    public String getVersion() {
        return version;
    }

    @Override
    public PluginStep doStep(PluginStep step, String parameter) {
        if (step.getStep() == PluginStep.STEP_DECRYPT) {
            Vector<DownloadLink> decryptedLinks = new Vector<DownloadLink>();
            try {

                URL url = new URL(parameter);
                RequestInfo reqinfo = HTTP.getRequest(url);
                ArrayList<ArrayList<String>> matches = SimpleMatches.getAllSimpleMatches(reqinfo.getHtmlCode(), INFO);
                progress.setRange(matches.size());
                String link = SimpleMatches.getFirstMatch(parameter, patternSupported, 1);
                DownloadLink dl;
                for (int i = 0; i < matches.size(); i++) {
                    decryptedLinks.add(dl=this.createDownloadlink("http://www.filer.net/get/" + matches.get(i).get(0)+".html"));
                    dl.setName(matches.get(i).get(1));
                    progress.increase(1);
                }

                step.setParameter(decryptedLinks);
            }
            catch (IOException e) {
                e.printStackTrace();
            }
        }
        return null;
    }

    @Override
    public boolean doBotCheck(File file) {
        return false;
    }
}