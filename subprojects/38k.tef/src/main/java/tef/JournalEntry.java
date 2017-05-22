package tef;

import lib38k.io.IoUtils;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

abstract class JournalEntry {

    static boolean isRunningJournalName(String name) {
        return name.endsWith(RUNNING_JOURNAL_FILE_NAME_SUFFIX);
    }

    static boolean isCommittedJournalName(String name) {
        return name.endsWith(COMMITTED_JOURNAL_FILE_NAME_SUFFIX);
    }

    static boolean isRollbackedJournalName(String name) {
        return name.endsWith(ROLLBACKED_JOURNAL_FILE_NAME_SUFFIX);
    }

    static final String RUNNING_JOURNAL_FILE_NAME_SUFFIX = ".running";
    static final String COMMITTED_JOURNAL_FILE_NAME_SUFFIX = ".committed";
    static final String ROLLBACKED_JOURNAL_FILE_NAME_SUFFIX = ".rollbacked";

    static final String PHASE1_COMMIT_MARK = ".";
    static final String PHASE2_COMMIT_MARK = ".";

    static final String EOF = PHASE1_COMMIT_MARK + PHASE2_COMMIT_MARK;

    static int getTransactionId(String journalName) {
        return Integer.parseInt(journalName.substring(0, journalName.indexOf(".")), 16);
    }

    public final int getTransactionId() {
        return getTransactionId(getJournalName());
    }

    protected abstract String getJournalName();

    protected abstract InputStream getInputStream() throws IOException;

    protected abstract long getSize();

    final byte[] getContents() {
        try {
            InputStream in = null;
            try {
                in = getInputStream();
                return IoUtils.getStreamData(in);
            } finally {
                if (in != null) {
                    in.close();
                }
            }
        } catch (IOException ioe) {
            throw new RuntimeException("reading journal failed: " + getJournalName(), ioe);
        }
    }

    static final class FileJournalEntry extends JournalEntry {

        private File journalFile_;

        FileJournalEntry(File file) {
            journalFile_ = file;
        }

        @Override
        protected String getJournalName() {
            return journalFile_.getName();
        }

        @Override
        protected InputStream getInputStream() throws IOException {
            return new FileInputStream(journalFile_);
        }

        @Override
        protected long getSize() {
            return journalFile_.length();
        }
    }

    static final class ZipJournalEntry extends JournalEntry {

        private ZipFile archive_;
        private ZipEntry journal_;

        ZipJournalEntry(ZipFile archive, ZipEntry zipEntry) {
            archive_ = archive;
            journal_ = zipEntry;
        }

        static String getJournalName(String zipEntryName) {
            String[] pathElements = zipEntryName.split("/");
            return pathElements[pathElements.length - 1];
        }

        @Override
        protected String getJournalName() {
            return getJournalName(journal_.getName());
        }

        @Override
        protected InputStream getInputStream() throws IOException {
            return archive_.getInputStream(journal_);
        }

        @Override
        protected long getSize() {
            return journal_.getSize();
        }
    }
}
