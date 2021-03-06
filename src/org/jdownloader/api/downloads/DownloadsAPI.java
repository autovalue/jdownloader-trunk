package org.jdownloader.api.downloads;

import java.util.List;

import org.appwork.remoteapi.APIQuery;
import org.appwork.remoteapi.RemoteAPIInterface;
import org.appwork.remoteapi.annotations.APIParameterNames;
import org.appwork.remoteapi.annotations.ApiNamespace;

@ApiNamespace("downloads")
@Deprecated
public interface DownloadsAPI extends RemoteAPIInterface {
    /*
     * Controlls
     */
    boolean start();

    boolean stop();

    @APIParameterNames({ "value" })
    boolean pause(Boolean value);

    String getJDState();

    /*
     * Queries
     */
    /**
     * Query Packages currently in downloads
     *
     * Example: http://localhost:3128/downloads/queryPackages?{"saveTo":true,"childCount":true,"hosts":true,"startAt":0,"maxResults":10}
     * Optionally you can query only certian packages:
     * http://localhost:3128/downloads/queryPackages?{"packageUUIDs":[1358496436106,1358496436107]}
     *
     * Default fields returned: name, uuid
     *
     * @param queryParams
     *            Hashmap with the following allowed values:
     *
     *            Optional selectors: startAt, Integer, index of first element to be returned maxResults, Integer, total number of elements
     *            to be returned
     *
     *            Optional fields (Boolean): saveTo size childCount hosts done comment enabled
     *
     * @return
     */
    @APIParameterNames({ "queryParams" })
    List<FilePackageAPIStorable> queryPackages(APIQuery queryParams);

    /**
     * Query Packages links in downloads
     *
     * Example:
     * http://localhost:3128/downloads/queryLinks?{"packageUUIDs":[1358496436106,1358496436107],"enabled":true,"size":true,"host":true
     * ,"startAt":0,"maxResults":10}
     *
     * Default fields returned: name, uuid, packageUUID
     *
     * @param queryParams
     *            Hashmap with the following allowed values:
     *
     *            Optional selectors: packageUUIDs, List<Long>, links contained in the packages with given uuids are returned, if empty all
     *            links are returned startAt, Integer, index of first element to be returned maxResults, Integer, total number of elements
     *            to be returned
     *
     *            Optional fields (Boolean): host size done enabled
     *
     * @return
     */
    @APIParameterNames({ "queryParams" })
    List<DownloadLinkAPIStorable> queryLinks(APIQuery queryParams);

    /*
     * Functions
     */
    @APIParameterNames({ "linkIds" })
    boolean removeLinks(final List<Long> linkIds);

    @APIParameterNames({ "linkIds", "packageIds" })
    boolean removeLinks(final List<Long> linkIds, final List<Long> packageIds);

    @APIParameterNames({ "linkIds", "packageIds" })
    boolean forceDownload(List<Long> linkIds, List<Long> packageIds);

    @APIParameterNames({ "linkIds" })
    boolean forceDownload(final List<Long> linkIds);

    @APIParameterNames({ "linkIds" })
    boolean enableLinks(List<Long> linkIds);

    @APIParameterNames({ "linkIds", "packageIds" })
    boolean enableLinks(List<Long> linkIds, List<Long> packageIds);

    @APIParameterNames({ "linkIds" })
    boolean disableLinks(List<Long> linkIds);

    @APIParameterNames({ "linkIds", "packageIds" })
    boolean disableLinks(List<Long> linkIds, List<Long> packageIds);

    @APIParameterNames({ "linkIds" })
    boolean resetLinks(List<Long> linkIds);

    @APIParameterNames({ "linkIds", "packageIds" })
    boolean resetLinks(List<Long> linkIds, List<Long> packageIds);

    @APIParameterNames({ "packageId", "newName" })
    boolean renamePackage(Long packageId, String newName);

    /*
     * Sorting
     */
    @APIParameterNames({ "query" })
    boolean movePackages(APIQuery query);

    @APIParameterNames({ "query" })
    boolean moveLinks(APIQuery query);

    /*
     * Info
     */
    int speed();

    int packageCount();

    /*
     * Changed?
     */
    @APIParameterNames({ "structureWatermark" })
    Long getChildrenChanged(Long structureWatermark);
}
