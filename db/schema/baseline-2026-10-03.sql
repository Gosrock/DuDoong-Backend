-- prod 스키마 기준선 (구조만, 데이터 없음). 2026-10-03 mysqldump --no-data, AUTO_INCREMENT 값 제거
-- 관리 규칙: db/migration/README.md

/*!40101 SET @OLD_CHARACTER_SET_CLIENT=@@CHARACTER_SET_CLIENT */;
/*!40101 SET @OLD_CHARACTER_SET_RESULTS=@@CHARACTER_SET_RESULTS */;
/*!40101 SET @OLD_COLLATION_CONNECTION=@@COLLATION_CONNECTION */;
/*!50503 SET NAMES utf8mb4 */;
/*!40103 SET @OLD_TIME_ZONE=@@TIME_ZONE */;
/*!40103 SET TIME_ZONE='+00:00' */;
/*!40014 SET @OLD_UNIQUE_CHECKS=@@UNIQUE_CHECKS, UNIQUE_CHECKS=0 */;
/*!40014 SET @OLD_FOREIGN_KEY_CHECKS=@@FOREIGN_KEY_CHECKS, FOREIGN_KEY_CHECKS=0 */;
/*!40101 SET @OLD_SQL_MODE=@@SQL_MODE, SQL_MODE='NO_AUTO_VALUE_ON_ZERO' */;
/*!40111 SET @OLD_SQL_NOTES=@@SQL_NOTES, SQL_NOTES=0 */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `BATCH_JOB_EXECUTION` (
  `JOB_EXECUTION_ID` bigint NOT NULL,
  `VERSION` bigint DEFAULT NULL,
  `JOB_INSTANCE_ID` bigint NOT NULL,
  `CREATE_TIME` datetime(6) NOT NULL,
  `START_TIME` datetime(6) DEFAULT NULL,
  `END_TIME` datetime(6) DEFAULT NULL,
  `STATUS` varchar(10) DEFAULT NULL,
  `EXIT_CODE` varchar(2500) DEFAULT NULL,
  `EXIT_MESSAGE` varchar(2500) DEFAULT NULL,
  `LAST_UPDATED` datetime(6) DEFAULT NULL,
  `JOB_CONFIGURATION_LOCATION` varchar(2500) DEFAULT NULL,
  PRIMARY KEY (`JOB_EXECUTION_ID`),
  KEY `JOB_INST_EXEC_FK` (`JOB_INSTANCE_ID`),
  CONSTRAINT `JOB_INST_EXEC_FK` FOREIGN KEY (`JOB_INSTANCE_ID`) REFERENCES `BATCH_JOB_INSTANCE` (`JOB_INSTANCE_ID`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `BATCH_JOB_EXECUTION_CONTEXT` (
  `JOB_EXECUTION_ID` bigint NOT NULL,
  `SHORT_CONTEXT` varchar(2500) NOT NULL,
  `SERIALIZED_CONTEXT` text,
  PRIMARY KEY (`JOB_EXECUTION_ID`),
  CONSTRAINT `JOB_EXEC_CTX_FK` FOREIGN KEY (`JOB_EXECUTION_ID`) REFERENCES `BATCH_JOB_EXECUTION` (`JOB_EXECUTION_ID`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `BATCH_JOB_EXECUTION_PARAMS` (
  `JOB_EXECUTION_ID` bigint NOT NULL,
  `TYPE_CD` varchar(6) NOT NULL,
  `KEY_NAME` varchar(100) NOT NULL,
  `STRING_VAL` varchar(250) DEFAULT NULL,
  `DATE_VAL` datetime(6) DEFAULT NULL,
  `LONG_VAL` bigint DEFAULT NULL,
  `DOUBLE_VAL` double DEFAULT NULL,
  `IDENTIFYING` char(1) NOT NULL,
  KEY `JOB_EXEC_PARAMS_FK` (`JOB_EXECUTION_ID`),
  CONSTRAINT `JOB_EXEC_PARAMS_FK` FOREIGN KEY (`JOB_EXECUTION_ID`) REFERENCES `BATCH_JOB_EXECUTION` (`JOB_EXECUTION_ID`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `BATCH_JOB_EXECUTION_SEQ` (
  `ID` bigint NOT NULL,
  `UNIQUE_KEY` char(1) NOT NULL,
  UNIQUE KEY `UNIQUE_KEY_UN` (`UNIQUE_KEY`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `BATCH_JOB_INSTANCE` (
  `JOB_INSTANCE_ID` bigint NOT NULL,
  `VERSION` bigint DEFAULT NULL,
  `JOB_NAME` varchar(100) NOT NULL,
  `JOB_KEY` varchar(32) NOT NULL,
  PRIMARY KEY (`JOB_INSTANCE_ID`),
  UNIQUE KEY `JOB_INST_UN` (`JOB_NAME`,`JOB_KEY`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `BATCH_JOB_SEQ` (
  `ID` bigint NOT NULL,
  `UNIQUE_KEY` char(1) NOT NULL,
  UNIQUE KEY `UNIQUE_KEY_UN` (`UNIQUE_KEY`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `BATCH_STEP_EXECUTION` (
  `STEP_EXECUTION_ID` bigint NOT NULL,
  `VERSION` bigint NOT NULL,
  `STEP_NAME` varchar(100) NOT NULL,
  `JOB_EXECUTION_ID` bigint NOT NULL,
  `START_TIME` datetime(6) NOT NULL,
  `END_TIME` datetime(6) DEFAULT NULL,
  `STATUS` varchar(10) DEFAULT NULL,
  `COMMIT_COUNT` bigint DEFAULT NULL,
  `READ_COUNT` bigint DEFAULT NULL,
  `FILTER_COUNT` bigint DEFAULT NULL,
  `WRITE_COUNT` bigint DEFAULT NULL,
  `READ_SKIP_COUNT` bigint DEFAULT NULL,
  `WRITE_SKIP_COUNT` bigint DEFAULT NULL,
  `PROCESS_SKIP_COUNT` bigint DEFAULT NULL,
  `ROLLBACK_COUNT` bigint DEFAULT NULL,
  `EXIT_CODE` varchar(2500) DEFAULT NULL,
  `EXIT_MESSAGE` varchar(2500) DEFAULT NULL,
  `LAST_UPDATED` datetime(6) DEFAULT NULL,
  PRIMARY KEY (`STEP_EXECUTION_ID`),
  KEY `JOB_EXEC_STEP_FK` (`JOB_EXECUTION_ID`),
  CONSTRAINT `JOB_EXEC_STEP_FK` FOREIGN KEY (`JOB_EXECUTION_ID`) REFERENCES `BATCH_JOB_EXECUTION` (`JOB_EXECUTION_ID`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `BATCH_STEP_EXECUTION_CONTEXT` (
  `STEP_EXECUTION_ID` bigint NOT NULL,
  `SHORT_CONTEXT` varchar(2500) NOT NULL,
  `SERIALIZED_CONTEXT` text,
  PRIMARY KEY (`STEP_EXECUTION_ID`),
  CONSTRAINT `STEP_EXEC_CTX_FK` FOREIGN KEY (`STEP_EXECUTION_ID`) REFERENCES `BATCH_STEP_EXECUTION` (`STEP_EXECUTION_ID`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `BATCH_STEP_EXECUTION_SEQ` (
  `ID` bigint NOT NULL,
  `UNIQUE_KEY` char(1) NOT NULL,
  UNIQUE KEY `UNIQUE_KEY_UN` (`UNIQUE_KEY`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `tbl_cart` (
  `cart_id` bigint NOT NULL AUTO_INCREMENT,
  `created_at` datetime DEFAULT NULL,
  `updated_at` datetime DEFAULT NULL,
  `cart_name` varchar(255) DEFAULT NULL,
  `user_id` bigint NOT NULL,
  PRIMARY KEY (`cart_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `tbl_cart_line_item` (
  `cart_line_id` bigint NOT NULL AUTO_INCREMENT,
  `created_at` datetime DEFAULT NULL,
  `updated_at` datetime DEFAULT NULL,
  `item_id` bigint NOT NULL,
  `amount` varchar(255) NOT NULL,
  `quantity` bigint NOT NULL,
  `cart_id` bigint DEFAULT NULL,
  PRIMARY KEY (`cart_line_id`),
  KEY `FKh98crc1yb0mej0er3m15k1bqc` (`cart_id`),
  CONSTRAINT `FKh98crc1yb0mej0er3m15k1bqc` FOREIGN KEY (`cart_id`) REFERENCES `tbl_cart` (`cart_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `tbl_cart_option_answer` (
  `cart_option_answer_id` bigint NOT NULL AUTO_INCREMENT,
  `created_at` datetime DEFAULT NULL,
  `updated_at` datetime DEFAULT NULL,
  `amount` varchar(255) NOT NULL,
  `answer` varchar(255) DEFAULT NULL,
  `option_id` bigint NOT NULL,
  `cart_line_id` bigint DEFAULT NULL,
  PRIMARY KEY (`cart_option_answer_id`),
  KEY `FKaviryokdav30g3m9hthlrr4jv` (`cart_line_id`),
  CONSTRAINT `FKaviryokdav30g3m9hthlrr4jv` FOREIGN KEY (`cart_line_id`) REFERENCES `tbl_cart_line_item` (`cart_line_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `tbl_comment` (
  `comment_id` bigint NOT NULL AUTO_INCREMENT,
  `created_at` datetime DEFAULT NULL,
  `updated_at` datetime DEFAULT NULL,
  `comment_status` varchar(255) DEFAULT NULL,
  `content` varchar(200) DEFAULT NULL,
  `event_id` bigint DEFAULT NULL,
  `nick_name` varchar(15) DEFAULT NULL,
  `user_id` bigint DEFAULT NULL,
  PRIMARY KEY (`comment_id`),
  KEY `FKgjlkbaxyvqqrmxeoxh6pb4oj7` (`user_id`),
  CONSTRAINT `FKgjlkbaxyvqqrmxeoxh6pb4oj7` FOREIGN KEY (`user_id`) REFERENCES `tbl_user` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `tbl_coupon_campaign` (
  `coupon_campaign_id` bigint NOT NULL AUTO_INCREMENT,
  `created_at` datetime DEFAULT NULL,
  `updated_at` datetime DEFAULT NULL,
  `apply_target` varchar(255) DEFAULT 'ALL',
  `coupon_code` varchar(255) DEFAULT NULL,
  `issued_amount` bigint DEFAULT NULL,
  `remaining_amount` bigint DEFAULT NULL,
  `end_at` datetime DEFAULT NULL,
  `start_at` datetime DEFAULT NULL,
  `discount_amount` bigint DEFAULT NULL,
  `discount_type` varchar(255) DEFAULT NULL,
  `minimum_cost` bigint DEFAULT NULL,
  `user_id` bigint DEFAULT NULL,
  `valid_term` bigint DEFAULT NULL,
  PRIMARY KEY (`coupon_campaign_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `tbl_event` (
  `event_id` bigint NOT NULL AUTO_INCREMENT,
  `created_at` datetime DEFAULT NULL,
  `updated_at` datetime DEFAULT NULL,
  `name` varchar(30) DEFAULT NULL,
  `run_time` bigint DEFAULT NULL,
  `start_at` datetime DEFAULT NULL,
  `content` longtext,
  `image_key` varchar(255) DEFAULT NULL,
  `latitude` double DEFAULT NULL,
  `longitude` double DEFAULT NULL,
  `place_address` varchar(255) DEFAULT NULL,
  `place_name` varchar(255) DEFAULT NULL,
  `host_id` bigint DEFAULT NULL,
  `status` varchar(255) DEFAULT NULL,
  PRIMARY KEY (`event_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `tbl_event_detail_image` (
  `event_detail_image_id` bigint NOT NULL AUTO_INCREMENT,
  `created_at` datetime DEFAULT NULL,
  `updated_at` datetime DEFAULT NULL,
  `event_id` bigint DEFAULT NULL,
  `image_url` varchar(255) DEFAULT NULL,
  PRIMARY KEY (`event_detail_image_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `tbl_event_settlement` (
  `event_settlement_id` bigint NOT NULL AUTO_INCREMENT,
  `created_at` datetime DEFAULT NULL,
  `updated_at` datetime DEFAULT NULL,
  `coupon_amount` varchar(255) DEFAULT NULL,
  `dudoong_amount` varchar(255) DEFAULT NULL,
  `dudoong_fee` varchar(255) DEFAULT NULL,
  `event_id` bigint DEFAULT NULL,
  `event_order_excel_key` varchar(255) DEFAULT NULL,
  `event_settlement_status` varchar(255) DEFAULT NULL,
  `payment_amount` varchar(255) DEFAULT NULL,
  `pg_fee` varchar(255) DEFAULT NULL,
  `total_amount` varchar(255) DEFAULT NULL,
  `total_sales_amount` varchar(255) DEFAULT NULL,
  `pg_fee_vat` varchar(255) DEFAULT NULL,
  PRIMARY KEY (`event_settlement_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `tbl_example` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `created_at` datetime DEFAULT NULL,
  `updated_at` datetime DEFAULT NULL,
  `content` varchar(255) DEFAULT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `tbl_host` (
  `host_id` bigint NOT NULL AUTO_INCREMENT,
  `created_at` datetime DEFAULT NULL,
  `updated_at` datetime DEFAULT NULL,
  `master_user_id` bigint DEFAULT NULL,
  `partner` bit(1) DEFAULT NULL,
  `contact_email` varchar(255) DEFAULT NULL,
  `contact_number` varchar(15) DEFAULT NULL,
  `introduce` varchar(255) DEFAULT NULL,
  `name` varchar(15) DEFAULT NULL,
  `image_key` varchar(255) DEFAULT NULL,
  `slack_url` varchar(255) DEFAULT NULL,
  PRIMARY KEY (`host_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `tbl_host_host_users` (
  `tbl_host_host_id` bigint NOT NULL,
  `host_users_host_user_id` bigint NOT NULL,
  PRIMARY KEY (`tbl_host_host_id`,`host_users_host_user_id`),
  UNIQUE KEY `UK_4kph07lljafutpdt6t7byy9qi` (`host_users_host_user_id`),
  CONSTRAINT `FK9nm5au4c6w2ua8s48n2bjbcow` FOREIGN KEY (`host_users_host_user_id`) REFERENCES `tbl_host_user` (`host_user_id`),
  CONSTRAINT `FKra13docrpahvs2tsg0116ou4x` FOREIGN KEY (`tbl_host_host_id`) REFERENCES `tbl_host` (`host_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `tbl_host_user` (
  `host_user_id` bigint NOT NULL AUTO_INCREMENT,
  `created_at` datetime DEFAULT NULL,
  `updated_at` datetime DEFAULT NULL,
  `active` bit(1) DEFAULT NULL,
  `role` varchar(255) DEFAULT NULL,
  `user_id` bigint DEFAULT NULL,
  `host_id` bigint DEFAULT NULL,
  PRIMARY KEY (`host_user_id`),
  UNIQUE KEY `UKgeyfenlnlmcftp47ygnujj4ng` (`host_id`,`user_id`),
  CONSTRAINT `FKkudje151hkc212awp8nx98iov` FOREIGN KEY (`host_id`) REFERENCES `tbl_host` (`host_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `tbl_issued_coupon` (
  `issued_coupon_id` bigint NOT NULL AUTO_INCREMENT,
  `created_at` datetime DEFAULT NULL,
  `updated_at` datetime DEFAULT NULL,
  `usage_status` bit(1) DEFAULT NULL,
  `user_id` bigint DEFAULT NULL,
  `coupon_campaign_id` bigint NOT NULL,
  PRIMARY KEY (`issued_coupon_id`),
  KEY `FK3awptenf9ahm7uo82tfadby2d` (`coupon_campaign_id`),
  CONSTRAINT `FK3awptenf9ahm7uo82tfadby2d` FOREIGN KEY (`coupon_campaign_id`) REFERENCES `tbl_coupon_campaign` (`coupon_campaign_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `tbl_issued_ticket` (
  `issued_ticket_id` bigint NOT NULL AUTO_INCREMENT,
  `created_at` datetime DEFAULT NULL,
  `updated_at` datetime DEFAULT NULL,
  `entered_at` datetime DEFAULT NULL,
  `event_id` bigint DEFAULT NULL,
  `issued_ticket_no` varchar(255) DEFAULT NULL,
  `issued_ticket_status` varchar(255) DEFAULT NULL,
  `pay_type` varchar(255) DEFAULT NULL,
  `amount` varchar(255) NOT NULL,
  `ticket_item_id` bigint DEFAULT NULL,
  `ticket_name` varchar(255) DEFAULT NULL,
  `ticket_type` varchar(255) DEFAULT NULL,
  `order_line_id` bigint DEFAULT NULL,
  `order_uuid` varchar(255) DEFAULT NULL,
  `email` varchar(255) DEFAULT NULL,
  `phone_number` varchar(255) DEFAULT NULL,
  `user_id` bigint DEFAULT NULL,
  `user_name` varchar(255) DEFAULT NULL,
  `uuid` varchar(255) NOT NULL,
  PRIMARY KEY (`issued_ticket_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `tbl_issued_ticket_option_answer` (
  `issued_ticket_option_answer_id` bigint NOT NULL AUTO_INCREMENT,
  `created_at` datetime DEFAULT NULL,
  `updated_at` datetime DEFAULT NULL,
  `amount` varchar(255) NOT NULL,
  `answer` varchar(255) DEFAULT NULL,
  `option_id` bigint DEFAULT NULL,
  `issued_ticket_id` bigint DEFAULT NULL,
  PRIMARY KEY (`issued_ticket_option_answer_id`),
  KEY `FKijerncvo1u6xibs2xpfjiarm4` (`issued_ticket_id`),
  CONSTRAINT `FKijerncvo1u6xibs2xpfjiarm4` FOREIGN KEY (`issued_ticket_id`) REFERENCES `tbl_issued_ticket` (`issued_ticket_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `tbl_item_option_group` (
  `item_option_group_id` bigint NOT NULL AUTO_INCREMENT,
  `item_id` bigint DEFAULT NULL,
  `option_group_id` bigint DEFAULT NULL,
  PRIMARY KEY (`item_option_group_id`),
  UNIQUE KEY `UKs30ntm5sggc7jx5mbkr3lttmf` (`item_id`,`option_group_id`),
  KEY `FK4436ctf8vtwit53qcb4aia1a7` (`option_group_id`),
  CONSTRAINT `FK4436ctf8vtwit53qcb4aia1a7` FOREIGN KEY (`option_group_id`) REFERENCES `tbl_option_group` (`option_group_id`),
  CONSTRAINT `FKfusivoekl6gx83yvm4f96n8is` FOREIGN KEY (`item_id`) REFERENCES `tbl_ticket_item` (`ticket_item_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `tbl_option` (
  `option_id` bigint NOT NULL AUTO_INCREMENT,
  `amount` varchar(255) NOT NULL,
  `answer` varchar(255) DEFAULT NULL,
  `option_group_id` bigint DEFAULT NULL,
  PRIMARY KEY (`option_id`),
  KEY `FKj4n4l5tea1tmvex6be7piwimk` (`option_group_id`),
  CONSTRAINT `FKj4n4l5tea1tmvex6be7piwimk` FOREIGN KEY (`option_group_id`) REFERENCES `tbl_option_group` (`option_group_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `tbl_option_group` (
  `option_group_id` bigint NOT NULL AUTO_INCREMENT,
  `description` varchar(255) DEFAULT NULL,
  `event_id` bigint DEFAULT NULL,
  `is_essential` bit(1) DEFAULT NULL,
  `name` varchar(255) DEFAULT NULL,
  `option_group_status` varchar(255) DEFAULT 'VALID',
  `type` varchar(255) DEFAULT NULL,
  PRIMARY KEY (`option_group_id`),
  KEY `FK3bx8ity7ivur2fcy9to0hx1kb` (`event_id`),
  CONSTRAINT `FK3bx8ity7ivur2fcy9to0hx1kb` FOREIGN KEY (`event_id`) REFERENCES `tbl_event` (`event_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `tbl_order` (
  `order_id` bigint NOT NULL AUTO_INCREMENT,
  `created_at` datetime DEFAULT NULL,
  `updated_at` datetime DEFAULT NULL,
  `approved_at` datetime DEFAULT NULL,
  `event_id` bigint NOT NULL,
  `coupon_id` bigint DEFAULT NULL,
  `amount` varchar(255) NOT NULL,
  `coupon_name` varchar(255) DEFAULT NULL,
  `order_method` varchar(255) NOT NULL,
  `order_name` varchar(255) NOT NULL,
  `order_no` varchar(255) DEFAULT NULL,
  `order_status` varchar(255) NOT NULL,
  `payment_key` varchar(255) DEFAULT NULL,
  `payment_method` varchar(255) DEFAULT NULL,
  `payment_provider` varchar(255) DEFAULT NULL,
  `receipt_url` varchar(255) DEFAULT NULL,
  `vat_amount` varchar(255) DEFAULT NULL,
  `discount_amount` varchar(255) DEFAULT NULL,
  `payment_amount` varchar(255) DEFAULT NULL,
  `supply_amount` varchar(255) DEFAULT NULL,
  `user_id` bigint NOT NULL,
  `uuid` varchar(255) NOT NULL,
  `with_draw_at` datetime DEFAULT NULL,
  `fail_reason` varchar(500) DEFAULT NULL,
  `cancel_reason` varchar(500) DEFAULT NULL,
  `refund_status` varchar(30) NOT NULL DEFAULT 'NONE',
  `refund_status_changed_at` datetime DEFAULT NULL,
  PRIMARY KEY (`order_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `tbl_order_line` (
  `order_line_item_id` bigint NOT NULL AUTO_INCREMENT,
  `created_at` datetime DEFAULT NULL,
  `updated_at` datetime DEFAULT NULL,
  `item_group_id` bigint DEFAULT NULL,
  `item_id` bigint DEFAULT NULL,
  `name` varchar(255) DEFAULT NULL,
  `amount` varchar(255) NOT NULL,
  `quantity` bigint DEFAULT NULL,
  `order_id` bigint DEFAULT NULL,
  PRIMARY KEY (`order_line_item_id`),
  KEY `FKbcjdj3gqvx5uyjf5da653r7ou` (`order_id`),
  CONSTRAINT `FKbcjdj3gqvx5uyjf5da653r7ou` FOREIGN KEY (`order_id`) REFERENCES `tbl_order` (`order_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `tbl_order_option_answer` (
  `order_option_answer_id` bigint NOT NULL AUTO_INCREMENT,
  `created_at` datetime DEFAULT NULL,
  `updated_at` datetime DEFAULT NULL,
  `amount` varchar(255) NOT NULL,
  `answer` varchar(255) DEFAULT NULL,
  `option_id` bigint DEFAULT NULL,
  `order_line_item_id` bigint DEFAULT NULL,
  PRIMARY KEY (`order_option_answer_id`),
  KEY `FK79vbcunqomqjpg2pkg3mb7tla` (`order_line_item_id`),
  CONSTRAINT `FK79vbcunqomqjpg2pkg3mb7tla` FOREIGN KEY (`order_line_item_id`) REFERENCES `tbl_order_line` (`order_line_item_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `tbl_ticket_item` (
  `ticket_item_id` bigint NOT NULL AUTO_INCREMENT,
  `created_at` datetime DEFAULT NULL,
  `updated_at` datetime DEFAULT NULL,
  `account_holder` varchar(255) DEFAULT NULL,
  `account_number` varchar(255) DEFAULT NULL,
  `bank_name` varchar(255) DEFAULT NULL,
  `description` varchar(255) DEFAULT NULL,
  `event_id` bigint DEFAULT NULL,
  `is_quantity_public` bit(1) DEFAULT NULL,
  `is_sellable` bit(1) DEFAULT NULL,
  `name` varchar(255) DEFAULT NULL,
  `pay_type` varchar(255) DEFAULT NULL,
  `amount` varchar(255) NOT NULL,
  `purchase_limit` bigint DEFAULT NULL,
  `quantity` bigint DEFAULT NULL,
  `sale_end_at` datetime DEFAULT NULL,
  `sale_start_at` datetime DEFAULT NULL,
  `supply_count` bigint DEFAULT NULL,
  `ticket_item_status` varchar(255) DEFAULT 'VALID',
  `type` varchar(255) DEFAULT NULL,
  PRIMARY KEY (`ticket_item_id`),
  KEY `FKhs3phof31syugbf5l3pdcxvta` (`event_id`),
  CONSTRAINT `FKhs3phof31syugbf5l3pdcxvta` FOREIGN KEY (`event_id`) REFERENCES `tbl_event` (`event_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `tbl_ticket_item_item_option_groups` (
  `tbl_ticket_item_ticket_item_id` bigint NOT NULL,
  `item_option_groups_item_option_group_id` bigint NOT NULL,
  UNIQUE KEY `UK_fisu1b08n5p9u4midnyyv5bpu` (`item_option_groups_item_option_group_id`),
  KEY `FK6wnupd9wnp28c71bbilx7gsj3` (`tbl_ticket_item_ticket_item_id`),
  CONSTRAINT `FK6wnupd9wnp28c71bbilx7gsj3` FOREIGN KEY (`tbl_ticket_item_ticket_item_id`) REFERENCES `tbl_ticket_item` (`ticket_item_id`),
  CONSTRAINT `FKphcfcsgjemgn4nh0uyypbb9nd` FOREIGN KEY (`item_option_groups_item_option_group_id`) REFERENCES `tbl_item_option_group` (`item_option_group_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `tbl_transaction_settlement` (
  `transaction_settlement_id` bigint NOT NULL AUTO_INCREMENT,
  `created_at` datetime DEFAULT NULL,
  `updated_at` datetime DEFAULT NULL,
  `approved_at` datetime DEFAULT NULL,
  `event_id` bigint DEFAULT NULL,
  `fee_supply_amount` varchar(255) DEFAULT NULL,
  `fee_vat` varchar(255) DEFAULT NULL,
  `interest_fee` varchar(255) DEFAULT NULL,
  `order_uuid` varchar(255) DEFAULT NULL,
  `paid_out_date` date DEFAULT NULL,
  `payment_amount` varchar(255) DEFAULT NULL,
  `payment_key` varchar(255) DEFAULT NULL,
  `payment_method` varchar(255) DEFAULT NULL,
  `settlement_amount` varchar(255) DEFAULT NULL,
  `sold_date` date DEFAULT NULL,
  `transaction_key` varchar(255) DEFAULT NULL,
  PRIMARY KEY (`transaction_settlement_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `tbl_transaction_settlement_fee_detail` (
  `tbl_transaction_settlement_transaction_settlement_id` bigint NOT NULL,
  `fee` bigint DEFAULT NULL,
  `type` varchar(255) DEFAULT NULL,
  KEY `FKr2dbxpxcgvqn509tnf6obnr8i` (`tbl_transaction_settlement_transaction_settlement_id`),
  CONSTRAINT `FKr2dbxpxcgvqn509tnf6obnr8i` FOREIGN KEY (`tbl_transaction_settlement_transaction_settlement_id`) REFERENCES `tbl_transaction_settlement` (`transaction_settlement_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `tbl_user` (
  `user_id` bigint NOT NULL AUTO_INCREMENT,
  `created_at` datetime DEFAULT NULL,
  `updated_at` datetime DEFAULT NULL,
  `account_role` varchar(255) DEFAULT NULL,
  `account_state` varchar(255) DEFAULT NULL,
  `last_login_at` datetime DEFAULT NULL,
  `marketing_agree` bit(1) DEFAULT NULL,
  `oid` varchar(255) DEFAULT NULL,
  `provider` varchar(255) DEFAULT NULL,
  `email` varchar(255) DEFAULT NULL,
  `name` varchar(255) DEFAULT NULL,
  `phone_number` varchar(255) DEFAULT NULL,
  `image_key` varchar(255) DEFAULT NULL,
  `receive_mail` bit(1) DEFAULT NULL,
  PRIMARY KEY (`user_id`),
  UNIQUE KEY `UKbcuce1hewvoysp0ot4ewcyr6u` (`oid`,`provider`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40103 SET TIME_ZONE=@OLD_TIME_ZONE */;

/*!40101 SET SQL_MODE=@OLD_SQL_MODE */;
/*!40014 SET FOREIGN_KEY_CHECKS=@OLD_FOREIGN_KEY_CHECKS */;
/*!40014 SET UNIQUE_CHECKS=@OLD_UNIQUE_CHECKS */;
/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
/*!40101 SET CHARACTER_SET_RESULTS=@OLD_CHARACTER_SET_RESULTS */;
/*!40101 SET COLLATION_CONNECTION=@OLD_COLLATION_CONNECTION */;
/*!40111 SET SQL_NOTES=@OLD_SQL_NOTES */;
