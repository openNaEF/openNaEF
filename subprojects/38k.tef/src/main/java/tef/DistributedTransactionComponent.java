package tef;

public interface DistributedTransactionComponent {

    public static interface RunsAtLocalSide extends DistributedTransactionComponent {
    }

    public static interface RunsAtCoordinatorSide extends DistributedTransactionComponent {
    }
}
