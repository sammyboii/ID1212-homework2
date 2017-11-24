package server;

import shared.Parser.ContentType;

public class Response {
    private ContentType type;
    private String content;

    public Response(ContentType type, String content) {
        this.type = type;
        this.content = content;
    }

    public ContentType getType() {
        return type;
    }

    public String getContent() {
        return content;
    }
}
