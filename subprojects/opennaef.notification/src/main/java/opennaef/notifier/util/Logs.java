package opennaef.notifier.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Logger
 */
public class Logs {
    public static final Logger common = LoggerFactory.getLogger("common");
    public static final Logger naef = LoggerFactory.getLogger("naef");

    public static final Logger commit = LoggerFactory.getLogger("commit");
    public static final Logger scheduled = LoggerFactory.getLogger("scheduled");
    public static final Logger filter = LoggerFactory.getLogger("filter");
    public static final Logger hook = LoggerFactory.getLogger("webhook");
}
