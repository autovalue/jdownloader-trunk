package org.jdownloader.api.linkcollector.v2;

import org.appwork.storage.Storable;
import org.jdownloader.myjdownloader.client.bindings.linkgrabber.CrawledLinkQuery;

public class CrawledLinkQueryStorable extends CrawledLinkQuery implements Storable {
    public static final CrawledLinkQueryStorable FULL = new CrawledLinkQueryStorable();
    static {
        FULL.setAvailability(true);
        FULL.setBytesTotal(true);
        FULL.setComment(true);
        FULL.setEnabled(true);
        FULL.setHost(true);
        FULL.setPriority(true);
        FULL.setStatus(true);
        FULL.setUrl(true);
        FULL.setVariantIcon(true);
        FULL.setVariantID(true);
        FULL.setVariantName(true);

        ;

    }

    public CrawledLinkQueryStorable() {
        super(/* Storable */);
    }

}