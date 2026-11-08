package yep.web;

/** Returned by a handler when the response is not JSON (for example a CSV download or a printable certificate). */
public record RawResponse(String contentType, String body, String downloadName) {
}
