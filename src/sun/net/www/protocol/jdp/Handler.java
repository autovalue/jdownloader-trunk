package sun.net.www.protocol.jdp;

import java.io.IOException;
import java.net.Proxy;
import java.net.URL;
import java.net.URLConnection;

import jd.http.Browser;
import jd.http.HTTPConnection;

public class Handler extends sun.net.www.protocol.http.Handler {
    public Handler() {
        proxy = null;
        proxyPort = -1;
    }

    public Handler(String proxy, int port) {
        this.proxy = proxy;
        this.proxyPort = port;
    }

    @Override
    protected URLConnection openConnection(URL u) throws IOException {

        return openConnection(u, (Proxy) null);

    }

    protected URLConnection openConnection(URL u, Proxy p) throws IOException {
        String urlCorrect = u.toString();
        if (urlCorrect.startsWith("jdp")) {
            urlCorrect = "http" + urlCorrect.substring(3);
        }
        if (u.getUserInfo() != null) {
            String[] logins = u.getUserInfo().split(":");
            Browser.getAssignedBrowserInstance(u).setAuth(u.getHost(), logins[0], logins.length > 1 ? logins[1] : "");
        }
        return new HTTPConnection(Browser.reAssignUrlToBrowserInstance(u, new URL(urlCorrect)), p, this);

    }

}
