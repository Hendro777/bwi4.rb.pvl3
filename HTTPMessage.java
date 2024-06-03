import java.util.HashMap;
import java.util.Map;

public abstract class HTTPMessage<T> {
    private String httpVersion;
    private Map<String, String> headers = new HashMap<String, String>();
    private T body;

    public HTTPMessage(String httpVersion, Map<String, String> headers, T body) {
        this.httpVersion = httpVersion;
        this.headers = headers;
        this.body = body;
    }

    public String getHttpVersion() {
        return httpVersion;
    }

    public void setHttpVersion(String httpVersion) {
        this.httpVersion = httpVersion;
    }

    public Map<String, String> Headers() {
        return new HashMap<String, String>(headers);
    }

    protected void setHeaders(Map<String, String> headers) {
        this.headers = headers;
    }

    public String getHeader(String key) {
        return headers.get(key);
    }

    public void setHeader(String key, String value) {
        headers.put(key, value);
    }

    public void setBody(T body) {
        this.body = body;
    }

    public T Body() {
        return body;
    }

    /*
     * This method should parse a single line of an HTTP header and return a Map.Entry object
     * containing the key and value of the header. The key should be the first word in the line
     * and the value should be the rest of the line.
     *
     * @param headerLine a single line of an HTTP header
     * @return a Map.Entry object containing the key and value of the header
     */
    public Map.Entry<String, String> parseHeader(String headerLine) {
        String[] headerParts = headerLine.split(": ");
        return new Map.Entry<String, String>() {
            @Override
            public String getKey() {
                return headerParts[0];
            }

            @Override
            public String getValue() {
                return headerParts[1];
            }

            @Override
            public String setValue(String value) {
                return null;
            }
        };
    }
}


