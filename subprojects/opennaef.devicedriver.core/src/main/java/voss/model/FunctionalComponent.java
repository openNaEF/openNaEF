package voss.model;

import java.io.Serializable;

public interface FunctionalComponent extends Serializable {
    String getExtInfoKey();

    String getKey();
}