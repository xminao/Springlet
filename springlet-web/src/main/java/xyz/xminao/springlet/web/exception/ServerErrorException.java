package xyz.xminao.springlet.web.exception;

// 500 错误
public class ServerErrorException extends ErrorResponseException {

    public ServerErrorException() {
        super(500);
    }

    public ServerErrorException(String message) {
        super(500, message);
    }

    public ServerErrorException(Throwable cause) {
        super(500, cause);
    }

    public ServerErrorException(String message, Throwable cause) {
        super(500, message, cause);
    }
}
