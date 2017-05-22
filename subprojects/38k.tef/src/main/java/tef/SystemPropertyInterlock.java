package tef;

public abstract class SystemPropertyInterlock<T> {

    public abstract static class ValueParser<U> {

        public static final ValueParser<Boolean> booleanParser = new ValueParser<Boolean>() {

            @Override
            public Boolean parse(String value) {
                return TefUtils.parseAsBoolean(value);
            }

            @Override
            public String toString(Boolean value) {
                return value == null ? null : value.toString();
            }
        };

        public static final ValueParser<Integer> integerParser = new ValueParser<Integer>() {

            @Override
            public Integer parse(String value) {
                return TefUtils.parseAsInteger(value);
            }

            @Override
            public String toString(Integer value) {
                return value == null ? null : value.toString();
            }
        };

        public abstract U parse(String value);

        public abstract String toString(U value);
    }

    protected SystemPropertyInterlock
            (final ValueParser<T> parser,
             final SystemProperties systemProperties,
             final String propertyName,
             final T initialValue) {
        systemProperties.addChangeListener
                (propertyName,
                        new SystemProperties.ChangeListener.Sync() {

                            @Override
                            public void checkValue(SystemProperties.PropertyChangedEvent e)
                                    throws SystemProperties.PropertyChangeVetoException {
                                SystemPropertyInterlock.this.checkValue(parser.parse(e.value));
                            }

                            public void notifyPropertyChanged(SystemProperties.PropertyChangedEvent e) {
                                SystemPropertyInterlock.this.interlock(parser.parse(e.value));
                            }
                        });

        systemProperties.set(propertyName, parser.toString(initialValue));
    }

    protected void checkValue(T value) {
    }

    abstract protected void interlock(T value);

    static void configure(TefService tefService) {
        final SystemProperties properties = tefService.getSystemProperties();

        final JournalReceiver journalReceiver = tefService.getJournalReceiver();
        if (journalReceiver != null) {
            new SystemPropertyInterlock<Boolean>
                    (ValueParser.booleanParser, properties,
                            "tef.journal-receiver.mirroring-enabled", Boolean.TRUE) {
                @Override
                protected void interlock(Boolean value) {
                    if (value != null) {
                        journalReceiver.setMirroringEnabled(value);
                    }
                }
            };

            new SystemPropertyInterlock<Integer>
                    (ValueParser.integerParser, properties,
                            "tef.journal-receiver.reconnect-interval",
                            JournalReceiver.DEFAULT_RECONNECTINTERVAL) {
                @Override
                protected void interlock(Integer value) {
                    if (value != null) {
                        journalReceiver.setReconnectInterval(value);
                    }
                }
            };
        }
    }
}
