package org.aws.gcrrp.datatos3.utils;

import java.util.regex.Pattern;

/**
 * Validation and quoting helpers for every value that ends up in a shell command
 * sent to the transfer instances through SSM (AWS-RunShellScript).
 */
public final class CommandSafety {

    /**
     * rclone paths such as "gcs:bucket/dir" or "s3:bucket/dir".
     * No shell metacharacters, no leading "-" (option injection) and no ","
     * or leading ":" (rclone on-the-fly backend / connection-string syntax).
     */
    private static final Pattern SAFE_PATH = Pattern.compile("^(?![-:])[A-Za-z0-9._/:=+@~% -]{1,1024}$");
    private static final Pattern AMI_ID = Pattern.compile("^ami-[0-9a-f]{8,17}$");
    private static final Pattern REGION = Pattern.compile("^[a-z]{2}(-[a-z]+)+-[0-9]{1,2}$");
    private static final Pattern EMAIL = Pattern.compile("^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$");

    private CommandSafety() {
    }

    public static String requireSafePath(String value, String field) {
        if (value == null || !SAFE_PATH.matcher(value).matches()) {
            throw new IllegalArgumentException("Invalid " + field);
        }
        return value;
    }

    public static String requireAmiId(String value) {
        if (value == null || !AMI_ID.matcher(value).matches()) {
            throw new IllegalArgumentException("Invalid amiId");
        }
        return value;
    }

    public static String requireRegion(String value) {
        if (value == null || !REGION.matcher(value).matches()) {
            throw new IllegalArgumentException("Invalid awsRegion");
        }
        return value;
    }

    public static String requireEmail(String value) {
        if (value == null || !EMAIL.matcher(value).matches()) {
            throw new IllegalArgumentException("Invalid email address");
        }
        return value;
    }

    /** Quote a value as a single POSIX shell argument. */
    public static String shQuote(String value) {
        if (value == null) {
            return "''";
        }
        return "'" + value.replace("'", "'\"'\"'") + "'";
    }

    /** Turn a task path into a file-name segment that contains no shell or path-traversal characters. */
    public static String toSafeSegment(String path) {
        String s = path.replaceAll("[^A-Za-z0-9._-]", "_");
        if (s.startsWith(".")) {
            s = "_" + s.substring(1);
        }
        return s.length() > 200 ? s.substring(0, 200) : s;
    }
}
