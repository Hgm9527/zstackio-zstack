package org.zstack.sdk;

import org.zstack.sdk.L2NetworkInventory;

public class DetachL2NetworkFromHostResult {
    public L2NetworkInventory inventory;
    public void setInventory(L2NetworkInventory inventory) {
        this.inventory = inventory;
    }
    public L2NetworkInventory getInventory() {
        return this.inventory;
    }

}
