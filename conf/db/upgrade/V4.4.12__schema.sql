DROP PROCEDURE IF EXISTS `Alter_SCSI_Table`;
DELIMITER $$
CREATE PROCEDURE Alter_SCSI_Table()
BEGIN
    IF NOT EXISTS( SELECT NULL
                   FROM INFORMATION_SCHEMA.COLUMNS
                   WHERE table_name = 'ScsiLunHostRefVO'
                     AND table_schema = 'zstack'
                     AND column_name = 'path')  THEN

        ALTER TABLE `zstack`.`ScsiLunHostRefVO`
            ADD COLUMN `hctl` VARCHAR(64) DEFAULT NULL,
            ADD COLUMN `path` VARCHAR(128) DEFAULT NULL;

        UPDATE `zstack`.`ScsiLunHostRefVO` ref
            INNER JOIN `zstack`.`ScsiLunVO` lun ON ref.scsiLunUuid = lun.uuid
        SET ref.path = lun.path, ref.hctl = lun.hctl;

    END IF;
END $$
DELIMITER ;

CALL Alter_SCSI_Table();
DROP PROCEDURE Alter_SCSI_Table;

CREATE TABLE `zstack`.`BareMetal2BondingVO` (
   `uuid` varchar(32) NOT NULL,
   `name` varchar(255) NOT NULL,
   `slaves` varchar(255) NOT NULL,
   `opts` varchar(255) DEFAULT NULL,
   `chassisUuid` varchar(32) NOT NULL,
   `mode` varchar(255) NOT NULL,
   `lastOpDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00' ON UPDATE CURRENT_TIMESTAMP,
   `createDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00',
   PRIMARY KEY (`uuid`),
   KEY `fkchassisUuid` (`chassisUuid`),
   CONSTRAINT `fkchassisUuid` FOREIGN KEY (`chassisUuid`) REFERENCES `BareMetal2ChassisVO` (`uuid`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE `zstack`.`BareMetal2BondingNicRefVO` (
     `id` bigint(20) NOT NULL AUTO_INCREMENT,
     `nicUuid` varchar(32) NOT NULL,
     `instanceUuid` varchar(32) NOT NULL,
     `bondingUuid` varchar(32) NOT NULL,
     `lastOpDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00' ON UPDATE CURRENT_TIMESTAMP,
     `createDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00',
     PRIMARY KEY (`id`),
     KEY `fkinstance` (`instanceUuid`),
     KEY `fknic` (`nicUuid`),
     KEY `fkbonding` (`bondingUuid`),
     CONSTRAINT `fkinstance` FOREIGN KEY (`instanceUuid`) REFERENCES `BareMetal2InstanceVO` (`uuid`) ON DELETE CASCADE ,
     CONSTRAINT `fknic` FOREIGN KEY (`nicUuid`) REFERENCES `VmNicVO` (`uuid`) ON DELETE CASCADE,
     CONSTRAINT `fkbonding` FOREIGN KEY (`bondingUuid`) REFERENCES `BareMetal2BondingVO` (`uuid`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

ALTER TABLE `zstack`.`BareMetal2InstanceVO` add column agentVersion varchar(32) DEFAULT NULL;
ALTER TABLE `zstack`.`BareMetal2InstanceVO` add column isLatestAgent tinyint(1) unsigned DEFAULT 0;
ALTER TABLE `zstack`.`BareMetal2ChassisNicVO` add column nicName varchar(255) DEFAULT NULL;
