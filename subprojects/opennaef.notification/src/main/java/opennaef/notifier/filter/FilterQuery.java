package opennaef.notifier.filter;

import javax.persistence.Column;
import javax.persistence.ElementCollection;
import javax.persistence.Embeddable;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * Filter 条件
 * <p>
 * for_all(∀): DtoChangesに指定したtypeだけ含まれる
 * exists(∃): 指定したtypeがDtoChangesに1つでも含まれる
 */
@Embeddable
public class FilterQuery {
    public static final transient FilterQuery NULL_FILTER;
    public static final String FOR_ALL = "set_for_all";
    public static final String EXISTS = "set_exists";

    static {
        FilterQuery nullFilter = new FilterQuery();
        nullFilter.setForAll(Collections.emptySet());
        nullFilter.setExists(Collections.emptySet());
        NULL_FILTER = nullFilter;
    }

    @Column(name = FOR_ALL, nullable = false)
    @ElementCollection
    private Set<String> _forAll = new HashSet<>();

    @Column(name = EXISTS, nullable = false)
    @ElementCollection
    private Set<String> _exists = new HashSet<>();

    public FilterQuery() {
    }

    public Set<String> getForAll() {
        return _forAll;
    }

    public void setForAll(Set<String> all) {
        if (all == null) {
            this._forAll.clear();
        } else {
            this._forAll = all;
        }
    }

    public Set<String> getExists() {
        return _exists;
    }

    public void setExists(Set<String> exists) {
        if (exists == null) {
            this._exists.clear();
        } else {
            this._exists = exists;
        }
    }
}
