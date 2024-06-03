import java.net.http.HttpResponse;
import java.util.Map;

public class HTTPResponse<T> extends HTTPMessage<T> {
    public enum HTTPStatusCode {
        OK(200, "OK"),
        BAD_REQUEST(400, "Bad Request"),
        NOT_FOUND(404, "Not Found"),
        INTERNAL_SERVER_ERROR(500, "Internal Server Error"),
        NOT_ACCEPTABLE(406, "Not Acceptable");

        private final int code;
        private final String message;

        HTTPStatusCode(int code, String message) {
            this.code = code;
            this.message = message;
        }

        public int getCode() {
            return code;
        }

        public String getMessage() {
            return message;
        }
    }
    private HTTPStatusCode statusCode;

    private HTTPResponse() {
    }

    public int getStatusCode() {
        return statusCode.code;
    }

    public void setStatusCode(HTTPStatusCode statusCode) {
        this.statusCode = statusCode;
    }

    public String getStatusMessage() {
        return statusCode.message;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(getHttpVersion()).append(" ").append(getStatusCode()).append(" ").append(getStatusMessage()).append("\n");
        for (Map.Entry<String, String> entry : Headers().entrySet()) {
            sb.append(entry.getKey()).append(": ").append(entry.getValue()).append("\n");
        }
        sb.append("\n");
        sb.append("Body:");
        sb.append("\n");
        sb.append(Body());
        return sb.toString();
    }
}
