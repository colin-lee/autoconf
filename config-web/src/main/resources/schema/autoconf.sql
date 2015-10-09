/*!40101 SET @OLD_CHARACTER_SET_CLIENT = @@CHARACTER_SET_CLIENT */;
/*!40101 SET @OLD_CHARACTER_SET_RESULTS = @@CHARACTER_SET_RESULTS */;
/*!40101 SET @OLD_COLLATION_CONNECTION = @@COLLATION_CONNECTION */;
/*!40101 SET NAMES utf8 */;
/*!40014 SET @OLD_FOREIGN_KEY_CHECKS = @@FOREIGN_KEY_CHECKS, FOREIGN_KEY_CHECKS = 0 */;
/*!40101 SET @OLD_SQL_MODE = @@SQL_MODE, SQL_MODE = 'NO_AUTO_VALUE_ON_ZERO' */;
/*!40111 SET @OLD_SQL_NOTES = @@SQL_NOTES, SQL_NOTES = 0 */;

CREATE DATABASE IF NOT EXISTS autoconf;

USE autoconf;

# Dump of table config
# ------------------------------------------------------------

CREATE TABLE IF NOT EXISTS `config` (
  `id`          INT(11) UNSIGNED NOT NULL AUTO_INCREMENT,
  `version`     INT(11) UNSIGNED NOT NULL DEFAULT '0'
  COMMENT '版本号',
  `modify_time` TIMESTAMP        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '最后修改时间',
  `encoding`    VARCHAR(32)               DEFAULT NULL
  COMMENT '保存到zookeeper的编码方式',
  `editor`      VARCHAR(64)               DEFAULT ''
  COMMENT '作者',
  `name`        VARCHAR(64)      NOT NULL
  COMMENT '配置文件名',
  `profile`     VARCHAR(64)      NOT NULL DEFAULT ''
  COMMENT '配置信息',
  `path`        VARCHAR(128)              DEFAULT ''
  COMMENT '配置文件路径',
  `content`     LONGTEXT COMMENT '配置内容',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uniqConfig` (`name`, `profile`)
)
  ENGINE = InnoDB
  DEFAULT CHARSET = utf8;

# Dump of table config_history
# ------------------------------------------------------------

CREATE TABLE IF NOT EXISTS `config_history` (
  `id`          INT(11) UNSIGNED NOT NULL AUTO_INCREMENT,
  `config_id`   INT(11) UNSIGNED NOT NULL,
  `version`     INT(11) UNSIGNED NOT NULL DEFAULT '0',
  `modify_time` TIMESTAMP        NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `editor`      VARCHAR(64)               DEFAULT '',
  `name`        VARCHAR(64)      NOT NULL,
  `profile`     VARCHAR(64)      NOT NULL DEFAULT '',
  `path`        VARCHAR(128)              DEFAULT '',
  `content`     LONGTEXT,
  PRIMARY KEY (`id`),
  KEY `editor` (`editor`),
  KEY `configId` (`config_id`)
)
  ENGINE = InnoDB
  DEFAULT CHARSET = utf8;


# Dump of table user
# ------------------------------------------------------------

CREATE TABLE IF NOT EXISTS `user` (
  `id`          INT(11) UNSIGNED NOT NULL AUTO_INCREMENT,
  `username`    VARCHAR(128)     NOT NULL DEFAULT '',
  `password`    VARCHAR(128)     NOT NULL DEFAULT '',
  `salt`        VARCHAR(32)      NOT NULL DEFAULT '',
  `roles`       VARCHAR(255)     NOT NULL DEFAULT '',
  `permissions` TEXT       NOT NULL DEFAULT '',
  `locked`      TINYINT(2) NOT NULL DEFAULT 0,
  `last_login`  TIMESTAMP        NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `create_time` DATETIME   NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `name` (`username`)
)
  ENGINE = InnoDB
  DEFAULT CHARSET = utf8;

LOCK TABLES `user` WRITE;
/*!40000 ALTER TABLE `user` DISABLE KEYS */;

INSERT INTO `user` (`id`, `username`, `password`, `salt`, `roles`, `permissions`, `locked`, `create_time`)
VALUES
  (1, 'root', '8ca4175aa749cbb9b80d072b7f9775bc', '117f16dd8f9da43849d75f438f04eb41', 'admin', '', 0, NOW());

/*!40000 ALTER TABLE `user` ENABLE KEYS */;
UNLOCK TABLES;

/*!40111 SET SQL_NOTES = @OLD_SQL_NOTES */;
/*!40101 SET SQL_MODE = @OLD_SQL_MODE */;
/*!40014 SET FOREIGN_KEY_CHECKS = @OLD_FOREIGN_KEY_CHECKS */;
/*!40101 SET CHARACTER_SET_CLIENT = @OLD_CHARACTER_SET_CLIENT */;
/*!40101 SET CHARACTER_SET_RESULTS = @OLD_CHARACTER_SET_RESULTS */;
/*!40101 SET COLLATION_CONNECTION = @OLD_COLLATION_CONNECTION */;
