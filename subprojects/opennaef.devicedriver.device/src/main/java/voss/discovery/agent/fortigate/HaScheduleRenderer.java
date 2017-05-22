package voss.discovery.agent.fortigate;

import voss.discovery.agent.common.SimpleExtInfoRenderer;
import voss.discovery.agent.fortigate.FortigateMib.FortigateHaSchedule;
import voss.model.VlanModel;

public class HaScheduleRenderer extends SimpleExtInfoRenderer<Integer> {

    public HaScheduleRenderer(VlanModel model) {
        super(FortigateExtInfoNames.HA_SCHEDULE, model);
    }

    public FortigateHaSchedule getSchedule() {
        Integer value = get();
        if (value == null) {
            return null;
        }
        FortigateHaSchedule schedule = FortigateHaSchedule.getByMode(value.intValue());
        if (schedule != null) {
            return schedule;
        }
        return FortigateHaSchedule.none;
    }
}