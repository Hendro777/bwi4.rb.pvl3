import java.util.HashMap;
import java.util.Map;

public class HTTPRequest<T> extends HTTPMessage<T> {
    public enum HTTPMethod {
        GET,
        HEAD,
        POST;

        public static HTTPMethod parseMethodString(String methodString) {
            switch (methodString.toUpperCase()) {
                case ("GET"):
                    return GET;
                case ("HEAD"):
                    return HEAD;
                case ("POST"):
                    return POST;
                default:
                    throw new UnsupportedOperationException();
            }
        }
    }

    private HTTPMethod method;
    private String path;

    public HTTPMethod getMethod() {
        return method;
    }

    public void setMethod(String method) {
        this.method = HTTPMethod.parseMethodString(method);
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(method).append(" ").append(path).append(" ").append(getHttpVersion()).append("\n");
        for (Map.Entry<String, String> entry : this.Headers().entrySet()) {
            sb.append(entry.getKey()).append(": ").append(entry.getValue()).append("\n");
        }
        sb.append("\n");
        sb.append("Body:");
        sb.append("\n");
        sb.append(this.Body());
        return sb.toString();
    }
}
