package voss.model;


public interface AtmPort extends Feature {
    AtmVp[] getVps();

    void addVp(AtmVp vp);

    AtmVp getVp(int vpi);

}