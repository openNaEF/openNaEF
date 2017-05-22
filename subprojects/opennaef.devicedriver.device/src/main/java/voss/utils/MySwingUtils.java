package voss.utils;

import voss.service.ServiceConstants;

import javax.swing.*;
import java.awt.*;

;

public final class MySwingUtils {
    public static final Cursor HOUR_CURSOR = new Cursor(Cursor.WAIT_CURSOR);
    public static final Cursor DEFAULT_CURSOR = new Cursor(
            Cursor.DEFAULT_CURSOR);

    private MySwingUtils() {
    }

    public static void showMessageDialog(final JComponent parent,
                                         final String message) {
        JOptionPane.showMessageDialog(parent, message);
    }

    public static void showErrorDialog(final Component parent,
                                       final String message) {
        JOptionPane.showMessageDialog(parent, message, "Error", JOptionPane.ERROR_MESSAGE);
    }

    public static void showExceptionDialog(final Component parent,
                                           final Throwable th) {
        showException(th);
        JOptionPane.showMessageDialog(parent, th.getMessage(), th.getClass()
                .getName(), JOptionPane.ERROR_MESSAGE);
    }

    public static void showExceptionDialog(final Throwable th) {
        showException(th);
        showExceptionDialog(null, th);
    }

    private static void showException(final Throwable th) {
        if (null != System.getProperty(ServiceConstants.SHOW_STACKTRACE)) {
            th.printStackTrace(System.err);
        }
    }
}