package org.zstack.header.storage.snapshot;

import org.zstack.header.configuration.PythonClassInventory;
import org.zstack.header.query.ExpandedQueries;
import org.zstack.header.query.ExpandedQuery;
import org.zstack.header.query.ExpandedQueryAlias;
import org.zstack.header.query.ExpandedQueryAliases;
import org.zstack.header.search.Inventory;
import org.zstack.header.vo.ForeignKey;
import org.zstack.header.volume.VolumeEO;
import org.zstack.header.volume.VolumeInventory;

import javax.persistence.Column;
import java.sql.Timestamp;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

@Inventory(mappingVOClass = VolumeSnapshotReferenceVO.class)
@PythonClassInventory
@ExpandedQueries({
        @ExpandedQuery(expandedField = "volume", inventoryClass = VolumeInventory.class,
                foreignKey = "volumeUuid", expandedInventoryKey = "uuid"),
        @ExpandedQuery(expandedField = "referenceVolume", inventoryClass = VolumeInventory.class,
                foreignKey = "referenceVolumeUuid", expandedInventoryKey = "uuid"),
        @ExpandedQuery(expandedField = "volumeSnapshot", inventoryClass = VolumeSnapshotInventory.class,
                foreignKey = "volumeSnapshotUuid", expandedInventoryKey = "uuid"),
})
public class VolumeSnapshotReferenceInventory {
    private long id;

    private String volumeUuid;

    private String volumeSnapshotUuid;

    private String volumeSnapshotInstallUrl;

    private String referenceUuid;

    private String referenceType;

    private String referenceInstallUrl;

    private String referenceVolumeUuid;

    private Timestamp createDate;

    private Timestamp lastOpDate;

    public static VolumeSnapshotReferenceInventory valueOf(VolumeSnapshotReferenceVO vo) {
        VolumeSnapshotReferenceInventory inv = new VolumeSnapshotReferenceInventory();
        inv.id = vo.getId();
        inv.volumeUuid = vo.getVolumeUuid();
        inv.volumeSnapshotUuid = vo.getVolumeSnapshotUuid();
        inv.volumeSnapshotInstallUrl = vo.getVolumeSnapshotInstallUrl();
        inv.referenceUuid = vo.getReferenceUuid();
        inv.referenceType = vo.getReferenceType();
        inv.referenceInstallUrl = vo.getReferenceInstallUrl();
        inv.referenceVolumeUuid = vo.getReferenceVolumeUuid();
        inv.createDate = vo.getCreateDate();
        inv.lastOpDate = vo.getLastOpDate();
        return inv;
    }

    public static List<VolumeSnapshotReferenceInventory> valueOf(Collection<VolumeSnapshotReferenceVO> vos) {
        return vos.stream().map(VolumeSnapshotReferenceInventory::valueOf).collect(Collectors.toList());
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getVolumeUuid() {
        return volumeUuid;
    }

    public void setVolumeUuid(String volumeUuid) {
        this.volumeUuid = volumeUuid;
    }

    public String getVolumeSnapshotUuid() {
        return volumeSnapshotUuid;
    }

    public void setVolumeSnapshotUuid(String volumeSnapshotUuid) {
        this.volumeSnapshotUuid = volumeSnapshotUuid;
    }

    public String getVolumeSnapshotInstallUrl() {
        return volumeSnapshotInstallUrl;
    }

    public void setVolumeSnapshotInstallUrl(String volumeSnapshotInstallUrl) {
        this.volumeSnapshotInstallUrl = volumeSnapshotInstallUrl;
    }

    public String getReferenceUuid() {
        return referenceUuid;
    }

    public void setReferenceUuid(String referenceUuid) {
        this.referenceUuid = referenceUuid;
    }

    public String getReferenceType() {
        return referenceType;
    }

    public void setReferenceType(String referenceType) {
        this.referenceType = referenceType;
    }

    public String getReferenceInstallUrl() {
        return referenceInstallUrl;
    }

    public void setReferenceInstallUrl(String referenceInstallUrl) {
        this.referenceInstallUrl = referenceInstallUrl;
    }

    public String getReferenceVolumeUuid() {
        return referenceVolumeUuid;
    }

    public void setReferenceVolumeUuid(String referenceVolumeUuid) {
        this.referenceVolumeUuid = referenceVolumeUuid;
    }

    public Timestamp getCreateDate() {
        return createDate;
    }

    public void setCreateDate(Timestamp createDate) {
        this.createDate = createDate;
    }

    public Timestamp getLastOpDate() {
        return lastOpDate;
    }

    public void setLastOpDate(Timestamp lastOpDate) {
        this.lastOpDate = lastOpDate;
    }
}
