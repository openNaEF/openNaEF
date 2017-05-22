package tef;

enum TimeBoundaryType implements Comparable<TimeBoundaryType> {

    /*
     * 0: past (criterion is transaction target-time.)
     */
    UNTIL,

    /*
     * 1: past+present (criterion is transaction target-time.)
     */
    THROUGH,

    /*
     * 2: present (criterion is transaction target-time.)
     */
    PRECISE_AT,

    /*
     * 3: present+future (criterion is transaction target-time.)
     */
    HEREAFTER,

    /*
     * 4: future (criterion is transaction target-time.)
     */
    AFTER
}
