package yep.util;

/**
 * Thrown when a business rule from the pseudo-code is broken
 * (for example "Email already registered"). The message is shown to the user.
 */
public class YepException extends RuntimeException {
    private final int status;

    public YepException(String message) {
        this(400, message);
    }

    public YepException(int status, String message) {
        super(message);
        this.status = status;
    }

    public int getStatus() {
        return status;
    }

    public static YepException denied() {
        return new YepException(403, "Access denied");
    }

    public static YepException notFound(String what) {
        return new YepException(404, what + " not found");
    }
}
