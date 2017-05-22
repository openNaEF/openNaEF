package naef.dto;

import naef.NaefTefService;
import naef.mvo.AbstractNodeElement;
import naef.mvo.NodeElement;
import tef.MVO;
import tef.MVO.MvoId;
import tef.skelton.ResolveException;
import tef.skelton.SkeltonTefService;

import java.io.Serializable;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class NodeElementHierarchy implements Serializable {

    public static class Entry implements Serializable {

        private final MvoId mvoid_;
        private final Set<NodeElementDescriptor<?>> descriptors_ = new HashSet<NodeElementDescriptor<?>>();
        private Entry ownerEntry_;
        private final Set<Entry> subentries_ = new HashSet<Entry>();

        public Entry(MvoId mvoid) {
            mvoid_ = mvoid;
        }

        public MvoId getMvoId() {
            return mvoid_;
        }

        void setOwnerEntry(Entry entry) {
            ownerEntry_ = entry;
        }

        public Entry getOwnerEntry() {
            return ownerEntry_;
        }

        void addSubEntry(Entry entry) {
            subentries_.add(entry);
        }

        public Set<Entry> getSubEntries() {
            return Collections.<Entry>unmodifiableSet(subentries_);
        }

        void addDescriptor(NodeElementDescriptor<?> descriptor) {
            descriptors_.add(descriptor);
        }

        public Set<NodeElementDescriptor<?>> getDescriptors() {
            return Collections.<NodeElementDescriptor<?>>unmodifiableSet(descriptors_);
        }
    }

    private final Map<NodeElementDescriptor<?>, MvoId> descriptorToMvoidMapping_;
    private final Map<MvoId, Entry> entries_;
    private final Map<NodeElementDescriptor<?>, String> resolveErrors_;

    public NodeElementHierarchy(Set<NodeElementDescriptor<?>> descriptors) {
        descriptorToMvoidMapping_ = new HashMap<NodeElementDescriptor<?>, MvoId>();
        entries_ = new HashMap<MvoId, Entry>();
        resolveErrors_ = new HashMap<NodeElementDescriptor<?>, String>();
        for (NodeElementDescriptor<?> descriptor : descriptors) {
            if (descriptor == null) {
                throw new NullPointerException("引数にnullが含まれます.");
            }

            try {
                MvoId mvoid = ((MVO) descriptor.resolve()).getMvoId();
                descriptorToMvoidMapping_.put(descriptor, mvoid);

                Entry entry = entries_.get(mvoid);
                if (entry == null) {
                    entry = new Entry(mvoid);
                    entries_.put(mvoid, entry);
                }
                entry.addDescriptor(descriptor);
            } catch (ResolveException re) {
                resolveErrors_.put(descriptor, re.getMessage());
            }
        }

        for (MvoId mvoid : entries_.keySet()) {
            Entry entry = entries_.get(mvoid);

            MVO mvo = NaefTefService.instance().getMvoRegistry().get(mvoid);
            if (mvo == null) {
                throw new IllegalArgumentException(
                    "指定された mvo-id を持つオブジェクトは存在しません: " + mvoid.getLocalStringExpression());
            }
            if (! (mvo instanceof NodeElement)) {
                throw new IllegalArgumentException(
                    "指定されたオブジェクトは node-element ではありません: "
                    + SkeltonTefService.instance().uiTypeNames().getName(mvo.getClass()));
            }

            NodeElement elem = (NodeElement) mvo;
            for (NodeElement owner = elem.getOwner(); owner != null; owner = owner.getOwner()) {
                Entry ownerEntry = entries_.get(((AbstractNodeElement) owner).getMvoId());
                if (ownerEntry != null) {
                    entry.setOwnerEntry(ownerEntry);
                    ownerEntry.addSubEntry(entry);
                    break;
                }
            }
        }
    }

    public Entry getEntry(NodeElementDescriptor<?> descriptor) {
        return getEntry(descriptorToMvoidMapping_.get(descriptor));
    }

    public Entry getEntry(MvoId mvoid) {
        return entries_.get(mvoid);
    }

    public Set<Entry> getRootEntries() {
        Set<Entry> result = new HashSet<Entry>();
        for (Entry entry : entries_.values()) {
            if (entry.getOwnerEntry() == null) {
                result.add(entry);
            }
        }
        return result;
    }

    public Set<NodeElementDescriptor<?>> getResolveErrors() {
        return resolveErrors_.keySet();
    }

    public String getErrorMessage(NodeElementDescriptor<?> descriptor) {
        return resolveErrors_.get(descriptor);
    }
}
