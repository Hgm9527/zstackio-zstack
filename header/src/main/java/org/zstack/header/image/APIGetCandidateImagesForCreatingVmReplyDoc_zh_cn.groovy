package org.zstack.header.image

import org.zstack.header.image.ImageInventory
import org.zstack.header.errorcode.ErrorCode

doc {

	title "获取用于创建云主机的候选镜像返回"

	ref {
		name "inventories"
		path "org.zstack.header.image.APIGetCandidateImagesForCreatingVmReply.inventories"
		desc "候选镜像"
		type "List"
		since "4.1.1"
		clz ImageInventory.class
	}
	field {
		name "success"
		desc ""
		type "boolean"
		since "4.1.1"
	}
	ref {
		name "error"
		path "org.zstack.header.image.APIGetCandidateImagesForCreatingVmReply.error"
		desc "错误码，若不为null，则表示操作失败, 操作成功时该字段为null",false
		type "ErrorCode"
		since "4.1.1"
		clz ErrorCode.class
	}
}
