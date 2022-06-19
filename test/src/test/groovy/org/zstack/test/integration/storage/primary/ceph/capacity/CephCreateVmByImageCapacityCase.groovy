package org.zstack.test.integration.storage.primary.ceph.capacity

import org.springframework.http.HttpEntity
import org.zstack.sdk.ClusterInventory
import org.zstack.sdk.DiskOfferingInventory
import org.zstack.sdk.GetPrimaryStorageCapacityResult
import org.zstack.sdk.ImageInventory
import org.zstack.sdk.InstanceOfferingInventory
import org.zstack.sdk.L3NetworkInventory
import org.zstack.sdk.PrimaryStorageInventory
import org.zstack.test.integration.storage.Env
import org.zstack.test.integration.storage.StorageTest
import org.zstack.testlib.EnvSpec
import org.zstack.testlib.CephPrimaryStorageSpec
import org.zstack.testlib.PrimaryStorageSpec
import org.zstack.testlib.SubCase
import org.zstack.utils.gson.JSONObjectUtil
import org.zstack.test.integration.storage.CephEnv
import org.zstack.storage.ceph.primary.CephPrimaryStorageBase
import org.zstack.header.image.ImageConstant
import org.zstack.testlib.BackupStorageSpec
import org.zstack.utils.data.SizeUnit
import org.zstack.storage.ceph.backup.CephBackupStorageBase
import org.zstack.core.Platform

/**
 * Created by SyZhao on 2017/4/17.
 */
class CephCreateVmByImageCapacityCase extends SubCase {
    EnvSpec env

    @Override
    void clean() {
        env.delete()
    }

    @Override
    void setup() {
        useSpring(StorageTest.springSpec)
    }

    @Override
    void environment() {
        env = CephEnv.CephStorageOneVmEnv()
    }

    @Override
    void test() {
        env.create {
            testCreateVmByImageCheckCapacity()
        }
    }

    void testCreateVmByImageCheckCapacity() {
        PrimaryStorageInventory ps = env.inventoryByName("ceph-pri")
        ClusterInventory cluster = env.inventoryByName("test-cluster")
        ImageInventory image = env.inventoryByName("image")
        DiskOfferingInventory diskOffering = env.inventoryByName("diskOffering")
        InstanceOfferingInventory instanceOffering = env.inventoryByName("instanceOffering")
        L3NetworkInventory l3 = env.inventoryByName("l3")
        def bs = env.inventoryByName("ceph-bk")

        def download_image_path_invoked = false
        def image_virtual_size = SizeUnit.GIGABYTE.toByte(10)//10G
        def image_physical_size = SizeUnit.GIGABYTE.toByte(1)//1G

        env.simulator(CephBackupStorageBase.DOWNLOAD_IMAGE_PATH) {
            def rsp = new CephBackupStorageBase.DownloadRsp()
            rsp.size = image_virtual_size
            rsp.actualSize = image_physical_size
            download_image_path_invoked = true
            return rsp
        }

        ImageInventory sizedImage = addImage {
            name = "sized-image"
            url = "http://my-site/foo.qcow2"
            backupStorageUuids = [bs.uuid]
            format = ImageConstant.QCOW2_FORMAT_STRING
        }

        assert download_image_path_invoked

        GetPrimaryStorageCapacityResult beforeCapacityResult = getPrimaryStorageCapacity {
            primaryStorageUuids = [ps.uuid]
        }

        createVmInstance {
            name = "crt-vm"
            instanceOfferingUuid = instanceOffering.uuid
            imageUuid = sizedImage.uuid
            l3NetworkUuids = [l3.uuid]
        }

        GetPrimaryStorageCapacityResult capacityResult = getPrimaryStorageCapacity {
            primaryStorageUuids = [ps.uuid]
        }
        assert beforeCapacityResult.availableCapacity == capacityResult.availableCapacity + image_virtual_size + image_physical_size

        env.simulator(CephPrimaryStorageBase.INIT_PATH) { HttpEntity<String> e, EnvSpec spec ->
            def cmd = JSONObjectUtil.toObject(e.body, CephPrimaryStorageBase.InitCmd.class)
            CephPrimaryStorageSpec cspec = spec.specByUuid(cmd.uuid)
            assert cspec != null: "cannot find ceph primary storage[uuid:${cmd.uuid}], check your environment()"

            def rsp = new CephPrimaryStorageBase.InitRsp()
            rsp.fsid = cspec.fsid
            rsp.userKey = Platform.uuid
            rsp.totalCapacity = cspec.totalCapacity
            rsp.availableCapacity = cspec.availableCapacity - image_physical_size
            return rsp
        }
        reconnectPrimaryStorage {
            uuid = ps.uuid
        }
        GetPrimaryStorageCapacityResult afterCapacityResult = getPrimaryStorageCapacity {
            primaryStorageUuids = [ps.uuid]
        }
        assert beforeCapacityResult.availablePhysicalCapacity == afterCapacityResult.availablePhysicalCapacity + image_physical_size
    }
}
