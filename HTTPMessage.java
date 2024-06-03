import java.util.HashMap;
import java.util.Map;

public abstract class HTTPMessage<T> {
    private String httpVersion;
    private Map<String, String> headers = new HashMap<>();
    private T body;

    public String getHttpVersion() {
        return httpVersion;
    }

    public void setHttpVersion(String httpVersion) {
        this.httpVersion = httpVersion;
    }

    public Map<String, String> Headers() {
        return new HashMap<String, String>(headers);
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
}


