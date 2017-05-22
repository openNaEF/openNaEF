package voss.model;

import java.io.Serializable;


public class NodeInfoRef implements Serializable {
    private static final long serialVersionUID = 2L;
    private static final String SITE_NODEID_DELIMITER = "$";
    private final String id;
    private String nodeIdentifier;
    private String site;
    private String ipAddress;
    private int hash;
    private boolean hashCached = false;

    public NodeInfoRef() {
        this(null, null, null);
    }

    public NodeInfoRef(String site, String nodeIdentifier, String ipAddress) {
        assert site != null;
        assert nodeIdentifier != null;

        this.id = this.getClass().getSimpleName()
                + ":" + System.currentTimeMillis()
                + ":" + Integer.toString((int) (Math.random() * 10000.0));
        this.site = site;
        this.nodeIdentifier = nodeIdentifier;
        this.ipAddress = ipAddress;
    }

    public String getNodeIdentifier() {
        return nodeIdentifier;
    }

    public void setNodeIdentifier(String nodeIdentifier) {
        assert nodeIdentifier != null;
        this.nodeIdentifier = nodeIdentifier;
    }

    public String getSite() {
        return site;
    }

    public String getIpAddress() {
        return ipAddress;
    }

    public void setSite(String site) {
        assert site != null;
        this.site = site;
    }

    public void setIpAddress(String newIpAddress) {
        this.ipAddress = newIpAddress;
    }

    public String getCaption() {
        String s = this.site + ":" + this.nodeIdentifier;
        if (this.ipAddress != null) {
            s = s + " (" + this.ipAddress + ")";
        }
        return s;
    }

    public void update(NodeInfo nodeInfo) {
        this.site = nodeInfo.getSiteName();
        this.nodeIdentifier = nodeInfo.getNodeIdentifier();
        if (nodeInfo.getFirstIpAddress() != null) {
            this.ipAddress = nodeInfo.getFirstIpAddress().getHostAddress();
        } else {
            this.ipAddress = null;
        }
    }

    public void update(NodeInfoRef nodeInfo) {
        this.site = nodeInfo.getSite();
        this.nodeIdentifier = nodeInfo.getNodeIdentifier();
        if (nodeInfo.getIpAddress() != null) {
            this.ipAddress = nodeInfo.getIpAddress();
        } else {
            this.ipAddress = null;
        }
    }

    @Override
    public int hashCode() {
        if (!hashCached) {
            this.hash = new StringBuilder().append(this.site).append(this.nodeIdentifier).toString().hashCode();
            this.hashCached = true;
        }
        return this.hash;
    }

    @Override
    public boolean equals(Object o) {
        if (o == null) {
            return false;
        } else if (!(o instanceof NodeInfoRef)) {
            return false;
        }
        if (this == o) {
            return true;
        }
        NodeInfoRef other = (NodeInfoRef) o;
        return nullToValue(this.site).equals(nullToValue(other.site))
                && this.nodeIdentifier.equals(other.nodeIdentifier);
    }

    private String nullToValue(Object obj) {
        if (obj == null) {
            return "";
        }
        return obj.toString();
    }

    public boolean matches(NodeInfo nodeinfo) {
        assert nodeinfo != null;
        NodeInfoRef ref_ = NodeInfoRef.get(nodeinfo);
        return this.getExternalForm().equals(ref_.getExternalForm());
    }

    @Override
    public String toString() {
        return (this.site == null ? NodeInfo.DEFAULT : this.site) + ":" + this.nodeIdentifier + ":" + this.id;
    }

    public String getExternalForm() {
        return this.site + SITE_NODEID_DELIMITER + this.nodeIdentifier;
    }

    public static NodeInfoRef get(String externalform) {
        String[] arr = externalform.split("\\" + SITE_NODEID_DELIMITER);
        if (arr.length != 2) {
            throw new IllegalArgumentException("illegal form: " + externalform);
        }
        return new NodeInfoRef(arr[0], arr[1], null);
    }

    public static NodeInfoRef get(NodeInfo nodeinfo) {
        if (nodeinfo == null) {
            return null;
        }
        String ip = (nodeinfo.getFirstIpAddress() == null
                ? "" : nodeinfo.getFirstIpAddress().getHostAddress());
        NodeInfoRef ref = new NodeInfoRef(nodeinfo.getSiteName(),
                nodeinfo.getNodeIdentifier(),
                ip);
        return ref;
    }
}