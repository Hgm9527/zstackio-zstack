package org.zstack.header.vm

import org.zstack.header.storage.snapshot.group.VolumeSnapshotGroupInventory
import org.zstack.header.errorcode.ErrorCode

doc {

	title "获取资源引用快照组列表返回"

	ref {
		name "inventories"
		path "org.zstack.header.vm.APIGetMemorySnapshotGroupReferenceReply.inventories"
		desc "被引用的内存快照组列表"
		type "List"
		since "4.4.24"
		clz VolumeSnapshotGroupInventory.class
	}
	field {
		name "resourceUuid"
		desc "资源UUID"
		type "String"
		since "4.4.24"
	}
	field {
		name "success"
		desc ""
		type "boolean"
		since "4.4.24"
	}
	ref {
		name "error"
		path "org.zstack.header.vm.APIGetMemorySnapshotGroupReferenceReply.error"
		desc "错误码，若不为null，则表示操作失败, 操作成功时该字段为null",false
		type "ErrorCode"
		since "4.4.24"
		clz ErrorCode.class
	}
}
