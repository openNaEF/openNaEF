package tef.logic;

public interface BusinessLogic {

    public static class LogicException extends Exception {

        public LogicException() {
        }

        public LogicException(String message) {
            super(message);
        }

        public LogicException(String message, Throwable cause) {
            super(message, cause);
        }

        public LogicException(Throwable cause) {
            super(cause);
        }
    }

    public void initialize();

    public void dispose();
}
