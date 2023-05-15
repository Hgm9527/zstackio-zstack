package org.zstack.header.host

import org.zstack.header.host.HostIpmiInventory
import org.zstack.header.errorcode.ErrorCode

doc {

	title "更新物理机IPMI信息消息回复"

	ref {
		name "hostIpmiInventory"
		path "org.zstack.header.host.APIUpdateHostIpmiEvent.hostIpmiInventory"
		desc "null"
		type "HostIpmiInventory"
		since "0.6"
		clz HostIpmiInventory.class
	}
	field {
		name "success"
		desc ""
		type "boolean"
		since "0.6"
	}
	ref {
		name "error"
		path "org.zstack.header.host.APIUpdateHostIpmiEvent.error"
		desc "错误码，若不为null，则表示操作失败, 操作成功时该字段为null",false
		type "ErrorCode"
		since "0.6"
		clz ErrorCode.class
	}
}
