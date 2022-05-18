package org.zstack.header.volume;

public interface VolumeConstant {
    String SERVICE_ID = "volume";

    String ACTION_CATEGORY = "volume";

    String VOLUME_FORMAT_RAW = "raw";
    String VOLUME_FORMAT_QCOW2 = "qcow2";
    String VOLUME_FORMAT_VMTX = "vmtx";

    long DATA_VOLUME_LIMIT = 22;

    enum Capability {
        MigrationInCurrentPrimaryStorage,
        MigrationToOtherPrimaryStorage
    }
}
