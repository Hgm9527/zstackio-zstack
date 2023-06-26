package org.zstack.header.tag

import org.zstack.header.errorcode.ErrorCode
import org.zstack.header.tag.TagPatternInventory

doc {

	title "创建标签的结果"

	field {
		name "success"
		desc ""
		type "boolean"
		since "0.6"
	}
	ref {
		name "error"
		path "org.zstack.tag2.APICreateTagEvent.error"
		desc "错误码，若不为null，则表示操作失败, 操作成功时该字段为null",false
		type "ErrorCode"
		since "3.2.0"
		clz ErrorCode.class
	}
	ref {
		name "inventory"
		path "org.zstack.tag2.APICreateTagEvent.inventory"
		desc "null"
		type "TagPatternInventory"
		since "3.2.0"
		clz TagPatternInventory.class
	}
}
