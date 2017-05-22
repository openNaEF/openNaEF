package tef.skelton.dto;

import tef.skelton.Attribute;
import tef.skelton.ConfigurationException;
import tef.skelton.Model;
import tef.skelton.SkeltonTefService;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public abstract class DtoFactory {

    private final SkeltonTefService tefService_;

    private final List<DtoInitializer<?, ?>> initializers_ = new ArrayList<DtoInitializer<?, ?>>();
    private final Set<DtoAttrTranscript<?, ?>> transcripts_ = new HashSet<DtoAttrTranscript<?, ?>>();

    private final Map<Class<?>, Map<String, DtoAttrTranscript<?, ?>>> aggressiveTranscripts_
        = new HashMap<Class<?>, Map<String, DtoAttrTranscript<?, ?>>>();
    private final Map<Class<?>, Map<String, DtoAttrTranscript<?, ?>>> lazyTranscripts_
        = new HashMap<Class<?>, Map<String, DtoAttrTranscript<?, ?>>>();

    protected DtoFactory(SkeltonTefService tefService) {
        tefService_ = tefService;
    }

    public synchronized void addInitializer(DtoInitializer<?, ?> initializer) {
        initializers_.add(initializer);
    }

    synchronized List<DtoInitializer<?, ?>> getInitializers() {
        return initializers_;
    }

    public synchronized void addDtoAttrTranscript(DtoAttrTranscript<?, ?> transcript) {
        Class<? extends Model> klass = transcript.getDtoClass();
        Attribute<?, Model> attr = (Attribute<?, Model>) transcript.getAttribute();

        tefService_.installAttributes(klass, attr);
        transcripts_.add(transcript);

        aggressiveTranscripts_.clear();
        lazyTranscripts_.clear();
    }

    synchronized DtoAttrTranscript<?, ?> getDtoAttrTranscript(Class<?> klass, String attrname) {
        DtoAttrTranscript aggressive = aggressiveTranscripts_.get(klass).get(attrname);
        if (aggressive != null) {
            return aggressive;
        }

        DtoAttrTranscript lazy = lazyTranscripts_.get(klass).get(attrname);
        if (lazy != null) {
            return lazy;
        }

        return null;
    }

    synchronized Set<DtoAttrTranscript<?, ?>> getAggressiveTranscripts(Class<?> klass) {
        configure(klass);

        return new HashSet<DtoAttrTranscript<?, ?>>(aggressiveTranscripts_.get(klass).values());
    }

    synchronized boolean isLazyTranscript(Class<?> klass, String attrname) {
        configure(klass);

        DtoAttrTranscript<?, ?> transcript = lazyTranscripts_.get(klass).get(attrname);
        return transcript != null;
    }

    private synchronized void configure(Class<?> klass) {
        if (aggressiveTranscripts_.get(klass) == null
            || lazyTranscripts_.get(klass) == null)
        {
            Map<String, DtoAttrTranscript<?, ?>> lazys = new HashMap<String, DtoAttrTranscript<?, ?>>();
            Map<String, DtoAttrTranscript<?, ?>> aggressives = new HashMap<String, DtoAttrTranscript<?, ?>>();
            for (DtoAttrTranscript<?, ?> transcript : transcripts_) {
                String attrname = transcript.getAttribute().getName();
                if (! transcript.getDtoClass().isAssignableFrom(klass)) {
                    continue;
                }

                if (transcript.isLazyInit()) {
                    DtoAttrTranscript<?, ?> existing = lazys.get(attrname);
                    lazys.put(attrname, selectMoreSpecificOne(existing, transcript));
                } else {
                    DtoAttrTranscript<?, ?> existing = aggressives.get(attrname);
                    aggressives.put(attrname, selectMoreSpecificOne(existing, transcript));
                }
            }
            lazyTranscripts_.put(klass, lazys);
            aggressiveTranscripts_.put(klass, aggressives);
        }
    }

    private DtoAttrTranscript<?, ?> selectMoreSpecificOne(DtoAttrTranscript<?, ?> o1, DtoAttrTranscript<?, ?> o2) {
        if (o1 == null) {
            return o2;
        }
        if (o1.getDtoClass().isAssignableFrom(o2.getDtoClass())) {
            return o2;
        }
        if (o2.getDtoClass().isAssignableFrom(o1.getDtoClass())) {
            return o1;
        }
        throw new ConfigurationException("undecidable: " + o1.getClass() + ", " + o2.getClass());
    }
}
