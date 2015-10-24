package com.twitter.finatra.request;

import com.twitter.finagle.http.MediaType;

public enum ContentType {

    ALL("*/*"),
    CSS("text/css"),
    JSON(MediaType.Json()),
    HTML(MediaType.Html()),
    PLAIN(MediaType.PlainText()),
    XML(MediaType.Xml()),
    RSS(MediaType.Rss()),
    JAVASCRIPT(MediaType.Javascript()),
    GIF(MediaType.Gif()),
    FORM(MediaType.WwwForm()),
    OCTET_STREAM(MediaType.OctetStream()),
    JPEG(MediaType.Jpeg()),
    PNG(MediaType.Png()),
    XLS(MediaType.Xls()),
    CSV(MediaType.Csv()),
    ZIP(MediaType.Zip()),
    ATOM(MediaType.Atom()),
    WWWFORM(MediaType.WwwForm()),
    IFRAME(MediaType.Iframe());

    public final String contentTypeName;
    private static final java.util.Map<String, ContentType> CONTENT_TYPE_LOOKUP_MAP
            = new java.util.HashMap<String, ContentType>();

    static {
        for (ContentType contentType : ContentType.values()) {
            CONTENT_TYPE_LOOKUP_MAP.put(contentType.contentTypeName, contentType);
        }
    }

    ContentType(String contentType) {
        this.contentTypeName = contentType;
    }

    public static ContentType fromString(String contentType) {
        return CONTENT_TYPE_LOOKUP_MAP.get(contentType);
    }

    @Override
    public String toString() {
        return contentTypeName;
    }

}
