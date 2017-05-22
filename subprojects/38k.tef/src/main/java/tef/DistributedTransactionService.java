package tef;

import lib38k.logger.FileLogger;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.rmi.AlreadyBoundException;
import java.rmi.Remote;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.text.NumberFormat;
import java.util.*;
import java.util.List;

public interface DistributedTransactionService
        extends Remote, DistributedTransactionComponent {
    public void enlist(TefServiceProxy tefService) throws RemoteException;

    public TransactionManager.Distributed createDistributedTransaction
            (StackTraceCatalog.CatalogId stacktraceCatalogId)
            throws RemoteException;

    public GlobalTransactionId getGlobalTxId
            (String tefServiceId, TransactionId.W localTxId, boolean isStrict)
            throws RemoteException;

    public TransactionId.W getLocalTxId(String tefServiceId, GlobalTransactionId globalTxId)
            throws RemoteException;

    public static class Impl
            extends UnicastRemoteObject
            implements DistributedTransactionService, RunsAtCoordinatorSide {
        static class CommittedParticipants {

            GlobalTransactionId txId;
            int[] txIds;

            CommittedParticipants(GlobalTransactionId txId, int[] txIds) {
                this.txId = txId;
                this.txIds = txIds;
            }

            @Override
            public String toString() {
                StringBuilder result = new StringBuilder();
                for (int txId : txIds) {
                    result.append(result.length() == 0 ? "" : ":");
                    result.append(Integer.toString(txId, 16));
                }
                return result.toString();
            }
        }

        public static void main(String[] args)
                throws RemoteException, AlreadyBoundException {
            if (args.length != 3) {
                System.out.println
                        ("args: [port] [service name]"
                                + " [acceptable tef service ids (comma separated)]");
                return;
            }

            String portStr = args[0];
            String serviceName = args[1];
            String[] acceptableTefServiceIds = args[2].split(",");

            int port;
            try {
                port = Integer.parseInt(portStr);
            } catch (NumberFormatException nfe) {
                throw new IllegalArgumentException("invalid number format: port");
            }

            new Impl(port, serviceName, acceptableTefServiceIds);
        }

        private static Impl instance__;
        private final FileLogger transactionCoordinationLogger_;

        private final Map<String, TefServiceProxy> tefServices_;

        private final Map<String, Integer> tefServiceIndexes_;

        private List<CommittedParticipants> history_;

        Impl(int port, String serviceName, String[] acceptableTefServiceIds)
                throws RemoteException, AlreadyBoundException {
            synchronized (Impl.class) {
                if (instance__ != null) {
                    throw new IllegalStateException();
                } else {
                    instance__ = this;
                }
            }

            Logs logs = new Logs(System.currentTimeMillis(), new File("."), "logs");
            transactionCoordinationLogger_ = logs.createLogger("distributed-transactions");

            tefServices_ = new HashMap<String, TefServiceProxy>();
            for (String acceptableTefServiceId : acceptableTefServiceIds) {
                tefServices_.put(acceptableTefServiceId, null);
            }

            tefServiceIndexes_ = new HashMap<String, Integer>();
            for (int i = 0; i < acceptableTefServiceIds.length; i++) {
                tefServiceIndexes_.put(acceptableTefServiceIds[i], new Integer(i));
            }

            try {
                history_
                        = restoreTxHistory(transactionCoordinationLogger_.getFile().getParentFile());
            } catch (IOException ioe) {
                throw new RuntimeException(ioe);
            }

            System.out.println("restored: " + history_.size());
            System.gc();
            long memoryUsage
                    = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory();
            System.out.println("memory: " + NumberFormat.getInstance().format(memoryUsage));

            Registry registry = LocateRegistry.createRegistry(port);
            registry.bind(serviceName, this);
        }

        @Override
        public void enlist(TefServiceProxy tefService) {
            String tefServiceId;
            try {
                tefServiceId = tefService.getTefServiceId();
            } catch (RemoteException re) {
                throw new RuntimeException(re);
            }
            if (!tefServices_.keySet().contains(tefServiceId)) {
                throw new IllegalArgumentException();
            }

            TefServiceProxy current = tefServices_.get(tefServiceId);
            if (current != null) {
                try {
                    if (current.isAlive()) {
                        new Throwable().printStackTrace();
                        System.exit(0);
                    }
                } catch (RemoteException e) {
                    tefServices_.remove(tefServiceId);
                }
            }

            tefServices_.put(tefServiceId, tefService);
        }

        @Override
        public TransactionManager.Distributed createDistributedTransaction
                (StackTraceCatalog.CatalogId stacktraceCatalogId)
                throws RemoteException {
            TefServiceProxy[] tefServices
                    = tefServices_.values().toArray(new TefServiceProxy[0]);
            LocalTransactionProxy[] participants
                    = new LocalTransactionProxy[tefServices.length];
            for (int i = 0; i < tefServices.length; i++) {
                participants[i] = tefServices[i].createLocalTransactionProxy();
            }

            return new TransactionManager.Distributed
                    .Impl(this, transactionCoordinationLogger_, participants, stacktraceCatalogId);
        }

        @Override
        public GlobalTransactionId getGlobalTxId
                (String tefServiceId, TransactionId.W localTxId, boolean isStrict) {
            final Integer searchIndex = tefServiceIndexes_.get(tefServiceId);
            if (searchIndex == null) {
                throw new IllegalArgumentException(tefServiceId == null ? "null" : tefServiceId);
            }

            synchronized (history_) {
                int[] txids = new int[tefServiceIndexes_.size()];
                txids[searchIndex] = localTxId.serial;
                CommittedParticipants searchKey = new CommittedParticipants(null, txids);
                int searchResult = Collections.binarySearch
                        (history_,
                                searchKey,
                                new Comparator<CommittedParticipants>() {

                                    @Override
                                    public int compare
                                            (CommittedParticipants o1, CommittedParticipants o2) {
                                        return o1.txIds[searchIndex] - o2.txIds[searchIndex];
                                    }
                                });
                if (searchResult < 0) {
                    if (isStrict) {
                        return null;
                    }

                    searchResult = -searchResult - 1;
                }
                return history_.get(searchResult).txId;
            }
        }

        @Override
        public TransactionId.W getLocalTxId
                (String tefServiceId, GlobalTransactionId globalTxId) {
            synchronized (this) {
                CommittedParticipants searchKey = new CommittedParticipants(globalTxId, null);
                int searchResult = Collections.binarySearch
                        (history_,
                                searchKey,
                                new Comparator<CommittedParticipants>() {

                                    @Override
                                    public int compare
                                            (CommittedParticipants o1, CommittedParticipants o2) {
                                        long diff = o1.txId.time() - o2.txId.time();
                                        return diff == 0
                                                ? o1.txId.id() - o2.txId.id()
                                                : (int) (diff / Math.abs(diff));
                                    }
                                });
                int index = tefServiceIndexes_.get(tefServiceId);
                return searchResult < 0
                        ? null
                        : new TransactionId.W(history_.get(searchResult).txIds[index]);
            }
        }

        private List<CommittedParticipants> restoreTxHistory(File logsDir) throws IOException {
            File[] logFiles = logsDir.listFiles();
            Arrays.sort(logFiles, new Comparator<File>() {

                @Override
                public int compare(File o1, File o2) {
                    return o1.getName().compareTo(o2.getName());
                }
            });

            List<CommittedParticipants> result = new ArrayList<CommittedParticipants>();
            for (File logFile : logFiles) {
                BufferedReader reader = null;
                try {
                    reader = new BufferedReader(new FileReader(logFile));
                    String line;
                    String processingTxId = null;
                    Map<String, TransactionId.W> txIdsMap = null;
                    while ((line = reader.readLine()) != null) {
                        String[] tokens = line.split("\t");
                        String token0 = tokens[0];
                        String token3 = tokens.length <= 3 ? null : tokens[3];
                        String token4 = tokens.length <= 4 ? null : tokens[4];

                        if (!token0.matches("[0-9]+-[0-9]+")) {
                            continue;
                        }

                        if (token3.equals("*")) {
                            if (processingTxId != null) {
                                throw new IllegalStateException(line);
                            }

                            processingTxId = token0;
                            txIdsMap = new HashMap<String, TransactionId.W>();
                        } else if (token3.equals("c")) {
                            if (txIdsMap == null) {
                                throw new IllegalStateException(line);
                            }

                            if (txIdsMap.size() != tefServices_.size()) {
                                System.out.println("* error: " + line);
                            } else {
                                GlobalTransactionId txId = GlobalTransactionId.parse(token0);
                                CommittedParticipants newEntry
                                        = newHistoryEntry(txId, txIdsMap);

                                removeRewindedEntries(result, newEntry);

                                CommittedParticipants last
                                        = result.size() == 0 ? null : result.get(result.size() - 1);
                                if (last != null && newEntry.txId.time() < last.txId.time()) {
                                    System.out.println("* warning: " + line);
                                }

                                result.add(newEntry);
                            }

                            processingTxId = null;
                            txIdsMap = null;
                        } else if (token3.equals("r")) {
                            processingTxId = null;
                            txIdsMap = null;
                        } else {
                            if (tefServices_.containsKey(token3)) {
                                String tefServiceId = token3;
                                String txIdStr = token4;
                                TransactionId txId = TransactionId.getInstance(txIdStr);
                                if (txId.getClass() != TransactionId.W.class) {
                                    throw new IllegalStateException(line);
                                }
                                txIdsMap.put(tefServiceId, (TransactionId.W) txId);
                            }
                        }
                    }
                } finally {
                    if (reader != null) {
                        reader.close();
                    }
                }
            }

            return result;
        }

        void addHistoryEntry(CommittedParticipants historyEntry) {
            synchronized (history_) {
                history_.add(historyEntry);
            }
        }

        CommittedParticipants newHistoryEntry
                (GlobalTransactionId txId, Map<String, TransactionId.W> txIdsMap) {
            int[] txIds = new int[txIdsMap.size()];
            for (String tefServiceId : txIdsMap.keySet()) {
                Integer index = tefServiceIndexes_.get(tefServiceId);
                if (index == null) {
                    throw new IllegalStateException();
                }

                txIds[index.intValue()] = txIdsMap.get(tefServiceId).serial;
            }
            return new CommittedParticipants(txId, txIds);
        }

        private void removeRewindedEntries
                (List<CommittedParticipants> list, CommittedParticipants newEntry) {
            while (true) {
                if (list.size() == 0) {
                    return;
                }
                CommittedParticipants last = list.get(list.size() - 1);

                CommittedParticipants newer = selectNewOne(newEntry, last);
                if (newer == newEntry) {
                    return;
                }
                if (newer == last) {
                    list.remove(list.size() - 1);
                    System.out.println
                            ("* removed: " + last.toString() + ", " + newEntry.toString());
                }
                if (newer == null) {
                    list.remove(list.size() - 1);
                    System.out.println
                            ("* error: " + last.toString() + ", " + newEntry.toString());
                }
            }
        }

        private CommittedParticipants selectNewOne
                (CommittedParticipants o1, CommittedParticipants o2) {
            if (o1.txIds.length != o2.txIds.length) {
                return null;
            }

            CommittedParticipants result = null;
            for (int i = 0; i < o1.txIds.length; i++) {
                CommittedParticipants newer = null;
                if (o1.txIds[i] < o2.txIds[i]) {
                    newer = o2;
                } else if (o2.txIds[i] < o1.txIds[i]) {
                    newer = o1;
                }

                if (result == null) {
                    result = newer;
                } else if (result != newer) {
                    return null;
                }
            }
            return result;
        }
    }
}
