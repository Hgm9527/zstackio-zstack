package org.zstack.sdk;

import org.zstack.sdk.TwinManagementNodeResourceMapInventory;

public class AddTwinManagementNodeResourceMapResult {
    public TwinManagementNodeResourceMapInventory inventory;
    public void setInventory(TwinManagementNodeResourceMapInventory inventory) {
        this.inventory = inventory;
    }
    public TwinManagementNodeResourceMapInventory getInventory() {
        return this.inventory;
    }

}
