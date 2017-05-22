package tef;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

class FullJournalEntries {

    static abstract class EntriesBlock {

        static class ZipArchive extends EntriesBlock {

            private final ZipFile zipFile_;

            ZipArchive(File zipFilePath) throws IOException {
                zipFile_ = new ZipFile(zipFilePath);

                for (ZipEntry zipEntry : getZipEntries()) {
                    String entryName = zipEntry.getName();
                    if (JournalEntry.isCommittedJournalName(entryName)) {
                        updateTxId(zipentryname2txid(entryName));
                    }
                    if (JournalEntry.isRollbackedJournalName(entryName)) {
                        updateMaxRollbackedTxId(zipentryname2txid(entryName));
                    }
                }
            }

            private static int zipentryname2txid(String zipEntryName) {
                return JournalEntry.getTransactionId
                        (JournalEntry.ZipJournalEntry.getJournalName(zipEntryName));
            }

            @Override
            List<JournalEntry> getEntries() {
                try {
                    List<JournalEntry> result = new ArrayList<JournalEntry>();
                    for (ZipEntry zipEntry : getZipEntries()) {
                        String entryName = zipEntry.getName();
                        if (JournalEntry.isCommittedJournalName(entryName)) {
                            result.add(new JournalEntry.ZipJournalEntry(zipFile_, zipEntry));
                        }
                    }
                    return result;
                } catch (IOException ioe) {
                    throw new RuntimeException(ioe);
                }
            }

            @Override
            String getName() {
                return zipFile_.getName();
            }

            @Override
            void close() throws IOException {
                zipFile_.close();
            }

            private List<ZipEntry> getZipEntries() throws IOException {
                List<ZipEntry> result = new ArrayList<ZipEntry>();
                for (Enumeration<? extends ZipEntry> e = zipFile_.entries();
                     e.hasMoreElements(); ) {
                    result.add(e.nextElement());
                }

                if (result.size() != zipFile_.size()) {
                    throw new TefInitializationFailedException
                            ("zip file read error: invalid entries, "
                                    + result.size() + " " + zipFile_.size());
                }

                return result;
            }
        }

        static class DirectoryFiles extends EntriesBlock {

            private final File directory_;

            DirectoryFiles(File directory) {
                if (!directory.isDirectory()) {
                    throw new IllegalArgumentException(directory.getAbsolutePath());
                }
                directory_ = directory;

                for (File file : directory_.listFiles()) {
                    String filename = file.getName();
                    if (JournalEntry.isCommittedJournalName(filename)) {
                        updateTxId(filename2txid(filename));
                    }
                    if (JournalEntry.isRollbackedJournalName(filename)) {
                        updateMaxRollbackedTxId(filename2txid(filename));
                    }
                }
            }

            private static int filename2txid(String filename) {
                return JournalEntry.getTransactionId(filename);
            }

            @Override
            String getName() {
                return directory_.getName();
            }

            @Override
            List<JournalEntry> getEntries() {
                List<JournalEntry> result = new ArrayList<JournalEntry>();
                for (File journalFile : directory_.listFiles()) {
                    String filename = journalFile.getName();
                    if (JournalEntry.isCommittedJournalName(filename)) {
                        result.add(new JournalEntry.FileJournalEntry(journalFile));
                    }
                }
                return result;
            }

            @Override
            void close() {
            }
        }

        private int minTxId_ = Integer.MAX_VALUE;
        private int maxTxId_ = Integer.MIN_VALUE;
        private int maxRollbackedTxId_ = Integer.MIN_VALUE;
        private int entriesCount_ = 0;

        EntriesBlock() {
        }

        protected void updateTxId(int txid) {
            minTxId_ = Math.min(txid, minTxId_);
            maxTxId_ = Math.max(txid, maxTxId_);
            entriesCount_++;
        }

        protected void updateMaxRollbackedTxId(int txid) {
            maxRollbackedTxId_ = maxRollbackedTxId_ < txid ? txid : maxRollbackedTxId_;
        }

        int getEntriesCount() {
            return entriesCount_;
        }

        int getMinTxId() {
            return minTxId_;
        }

        int getMaxTxId() {
            return maxTxId_;
        }

        int getMaxRollbackedTxId() {
            return maxRollbackedTxId_;
        }

        Iterator<JournalEntry> getEntryIterator() {
            List<JournalEntry> entries = getEntries();
            Collections.sort
                    (entries,
                            new Comparator<JournalEntry>() {

                                public int compare(JournalEntry o1, JournalEntry o2) {
                                    int id1 = o1.getTransactionId();
                                    int id2 = o2.getTransactionId();

                                    if ((o1 != o2)
                                            && (id1 == id2)) {
                                        throw new Error
                                                ("duplicated transaction id found: "
                                                        + Integer.toString(id1, 16));
                                    }

                                    return id1 - id2;
                                }
                            });
            return entries.iterator();
        }

        abstract List<JournalEntry> getEntries();

        abstract String getName();

        abstract void close() throws IOException;
    }

    private final List<EntriesBlock> blocks_;
    private int maxRollbackedTxId_ = Integer.MIN_VALUE;

    FullJournalEntries(TefService tefService) throws IOException {
        File journalsDir = tefService.getLogs().getJournalsDirectory();

        blocks_ = new ArrayList<EntriesBlock>();
        for (File file : journalsDir.listFiles()) {
            if (file.isFile() && JournalEntry.isRunningJournalName(file.getName())) {
                updateMaxRollbackedTxId(JournalEntry.getTransactionId(file.getName()));
            } else {
                EntriesBlock block = null;
                if (file.isFile() && file.getName().endsWith(".zip")) {
                    block = new EntriesBlock.ZipArchive(file);
                } else if (file.isDirectory()) {
                    block = new EntriesBlock.DirectoryFiles(file);
                }

                if (block != null && 0 < block.getEntriesCount()) {
                    blocks_.add(block);
                }

                if (block != null) {
                    updateMaxRollbackedTxId(block.getMaxRollbackedTxId());
                }
            }
        }

        Collections.sort
                (blocks_,
                        new Comparator<EntriesBlock>() {

                            @Override
                            public int compare(EntriesBlock block1, EntriesBlock block2) {
                                if (block1 == block2) {
                                    return 0;
                                }
                                if (block1.getMaxTxId() < block2.getMinTxId()) {
                                    return -1;
                                }
                                if (block2.getMaxTxId() < block1.getMinTxId()) {
                                    return 1;
                                }
                                throw new IllegalStateException
                                        ("invalid transaction id range: "
                                                + block1.getName() + " " + block2.getName());
                            }
                        });

        List<String> blocksBoundsStr = new ArrayList<String>();
        for (EntriesBlock block : blocks_) {
            blocksBoundsStr.add
                    (block.getName()
                            + ":" + Integer.toString(block.getMinTxId(), 16)
                            + "-" + Integer.toString(block.getMaxTxId(), 16));
        }
        tefService.logMessage("journal blocks:", blocksBoundsStr);
    }

    public Iterable<JournalEntry> getJournalEntryIterable() {
        return new Iterable<JournalEntry>() {

            @Override
            public Iterator<JournalEntry> iterator() {
                return new Iterator<JournalEntry>() {

                    private final Iterator<EntriesBlock> blocksItr_ = blocks_.iterator();
                    private Iterator<JournalEntry> current_
                            = blocksItr_.hasNext()
                            ? blocksItr_.next().getEntryIterator()
                            : null;

                    private synchronized void updateCurrent() {
                        while (current_ != null && !current_.hasNext()) {
                            if (!blocksItr_.hasNext()) {
                                current_ = null;
                                return;
                            }

                            current_ = blocksItr_.next().getEntryIterator();
                            if (current_.hasNext()) {
                                return;
                            }
                        }
                    }

                    @Override
                    public synchronized boolean hasNext() {
                        updateCurrent();
                        return current_ != null && current_.hasNext();
                    }

                    @Override
                    public synchronized JournalEntry next() {
                        updateCurrent();
                        if (current_ != null) {
                            return current_.next();
                        } else {
                            throw new NoSuchElementException();
                        }
                    }

                    @Override
                    public void remove() {
                        throw new UnsupportedOperationException();
                    }
                };
            }
        };
    }

    private void updateMaxRollbackedTxId(int transactionId) {
        maxRollbackedTxId_ = Math.max(maxRollbackedTxId_, transactionId);
    }

    Integer getMaxRollbackedTransactionId() {
        return maxRollbackedTxId_ == Integer.MIN_VALUE
                ? null
                : maxRollbackedTxId_;
    }

    void close() throws IOException {
        for (EntriesBlock block : blocks_) {
            block.close();
        }
    }
}
