package voss.multilayernms.inventory.diff.web.flow.state;

public enum StateId {

    unknownRequest,

    diffPropertyReload, diffPropertyView,

    createDiff, createDiffInterrupt, applyDiff, ignoreDiff,
    viewDiff,

    lock, unlock, unlockForce, diffStatus,
}