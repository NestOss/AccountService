# --------------------------------------------------------
# Host:                         127.0.0.1
# Database:                     accountservice
# Server version:               5.6.25-log
# Server OS:                    Win32
# HeidiSQL version:             5.0.0.3272
# Date/time:                    2015-06-28 14:03:48
# --------------------------------------------------------

/*!40101 SET @OLD_CHARACTER_SET_CLIENT=@@CHARACTER_SET_CLIENT */;
/*!40101 SET NAMES utf8 */;
/*!40014 SET @OLD_FOREIGN_KEY_CHECKS=@@FOREIGN_KEY_CHECKS, FOREIGN_KEY_CHECKS=0 */;
/*!40101 SET @OLD_SQL_MODE=@@SQL_MODE, SQL_MODE='NO_AUTO_VALUE_ON_ZERO' */;
# Dumping database structure for accountservice
CREATE DATABASE IF NOT EXISTS `AccountService` /*!40100 DEFAULT CHARACTER SET utf8 COLLATE utf8_bin */;
USE `AccountService`;


# Dumping structure for table accountservice.account
CREATE TABLE IF NOT EXISTS `Account` (
  `id` int(10) unsigned NOT NULL,
  `amount` bigint(20) NOT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COLLATE=utf8_bin;

# Data exporting was unselected.


# Dumping structure for table accountservice.kafkapartition
CREATE TABLE IF NOT EXISTS `kafkapartition` (
  `id` int(10) unsigned NOT NULL,
  `offset` bigint(20) unsigned DEFAULT '0',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COLLATE=utf8_bin;

# Data exporting was unselected.
/*!40101 SET SQL_MODE=@OLD_SQL_MODE */;
/*!40014 SET FOREIGN_KEY_CHECKS=@OLD_FOREIGN_KEY_CHECKS */;
/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
