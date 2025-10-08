/*
 Navicat Premium Data Transfer

 Source Server         : localDB
 Source Server Type    : MySQL
 Source Server Version : 50743
 Source Host           : localhost:3306
 Source Schema         : data_collector_db

 Target Server Type    : MySQL
 Target Server Version : 50743
 File Encoding         : 65001

 Date: 08/10/2025 11:23:37
*/

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- ----------------------------
-- Table structure for anti_pattern_info
-- ----------------------------
DROP TABLE IF EXISTS `anti_pattern_info`;
CREATE TABLE `anti_pattern_info`  (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `name` varchar(255) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL COMMENT '名称',
  `category_name` varchar(255) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL COMMENT '粗分类名称',
  `category_index` varchar(10) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL COMMENT '粗分类index',
  `type_name` varchar(255) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL COMMENT '细分类名称',
  `detectable` tinyint(1) NULL DEFAULT NULL COMMENT '0:false,1:r=true',
  `detect_method` varchar(255) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL COMMENT '检测方式：\"static\"|\"dynamic\"|\"static&dynamic\"|null',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `name`(`name`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 85 CHARACTER SET = utf8 COLLATE = utf8_general_ci COMMENT = '反模式信息表' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Records of anti_pattern_info
-- ----------------------------
INSERT INTO `anti_pattern_info` VALUES (1, 'Poor Restful Api Design', 'Internal Design', '1', 'API design', 0, NULL);
INSERT INTO `anti_pattern_info` VALUES (2, 'CRUDY Interface', 'Internal Design', '1', 'API design', 0, NULL);
INSERT INTO `anti_pattern_info` VALUES (3, 'Ambiguous Service', 'Internal Design', '1', 'API design', 0, NULL);
INSERT INTO `anti_pattern_info` VALUES (4, 'Bloated Service', 'Internal Design', '1', 'API design', 0, NULL);
INSERT INTO `anti_pattern_info` VALUES (5, 'Whatever Types', 'Internal Design', '1', 'API design', 0, NULL);
INSERT INTO `anti_pattern_info` VALUES (6, 'No API Versioning', 'Internal Design', '1', 'API design', 1, 'static');
INSERT INTO `anti_pattern_info` VALUES (7, 'Unstable API', 'Internal Design', '1', 'API design', 0, NULL);
INSERT INTO `anti_pattern_info` VALUES (8, 'Crossing API', 'Internal Design', '1', 'API design', 0, NULL);
INSERT INTO `anti_pattern_info` VALUES (9, 'Dismiss Documentation', 'Internal Design', '1', 'API design', 0, NULL);
INSERT INTO `anti_pattern_info` VALUES (10, 'Low Cohesive Operations', 'Internal Design', '1', 'Method Design', 0, NULL);
INSERT INTO `anti_pattern_info` VALUES (11, 'Stovepipe Service', 'Internal Design', '1', 'Method Design', 0, NULL);
INSERT INTO `anti_pattern_info` VALUES (12, 'Data Service', 'Internal Design', '1', 'Method Design', 0, NULL);
INSERT INTO `anti_pattern_info` VALUES (13, 'Nano Microservice', 'Internal Design', '1', 'Granularity Design', 0, NULL);
INSERT INTO `anti_pattern_info` VALUES (14, 'Mega Microservice', 'Internal Design', '1', 'Granularity Design', 1, 'static');
INSERT INTO `anti_pattern_info` VALUES (15, 'Insufficient Message Traceability', 'Communication & Interaction', '2', 'Communication', 0, NULL);
INSERT INTO `anti_pattern_info` VALUES (16, 'Use Of Complex API Calls When Messaging Is a Simpler Solution', 'Communication & Interaction', '2', 'Communication', 0, NULL);
INSERT INTO `anti_pattern_info` VALUES (17, 'Use Of Business Logic In Communication Among Services', 'Communication & Interaction', '2', 'Communication', 0, NULL);
INSERT INTO `anti_pattern_info` VALUES (18, 'Hardcoded Endpoints', 'Communication & Interaction', '2', 'Communication', 1, 'static');
INSERT INTO `anti_pattern_info` VALUES (19, 'Lack Of Communication Standards Among Microservices', 'Communication & Interaction', '2', 'Communication', 0, NULL);
INSERT INTO `anti_pattern_info` VALUES (20, 'Empty Messages', 'Communication & Interaction', '2', 'Communication', 0, NULL);
INSERT INTO `anti_pattern_info` VALUES (21, 'Timeout', 'Communication & Interaction', '2', 'Communication', 0, NULL);
INSERT INTO `anti_pattern_info` VALUES (22, 'Wobbly Service Interactions', 'Communication & Interaction', '2', 'Interaction', 0, NULL);
INSERT INTO `anti_pattern_info` VALUES (23, 'Service Chain', 'Communication & Interaction', '2', 'Interaction', 0, NULL);
INSERT INTO `anti_pattern_info` VALUES (24, 'Chatty Service', 'Communication & Interaction', '2', 'Interaction', 0, NULL);
INSERT INTO `anti_pattern_info` VALUES (25, 'Unnecessary Settings', 'Structure & Infrastructure', '3', 'Infrastructure', 0, NULL);
INSERT INTO `anti_pattern_info` VALUES (26, 'ESB Usage', 'Structure & Infrastructure', '3', 'Infrastructure', 1, 'static');
INSERT INTO `anti_pattern_info` VALUES (27, 'No API-Gateway', 'Structure & Infrastructure', '3', 'Infrastructure', 1, 'static');
INSERT INTO `anti_pattern_info` VALUES (28, 'No Service Discovery', 'Structure & Infrastructure', '3', 'Infrastructure', 0, NULL);
INSERT INTO `anti_pattern_info` VALUES (29, 'Bottleneck Service', 'Structure & Infrastructure', '3', 'Structure', 0, NULL);
INSERT INTO `anti_pattern_info` VALUES (30, 'Chaotic Independence', 'Structure & Infrastructure', '3', 'Structure', 0, NULL);
INSERT INTO `anti_pattern_info` VALUES (31, 'Cyclic Dependency', 'Structure & Infrastructure', '3', 'Structure', 1, 'dynamic');
INSERT INTO `anti_pattern_info` VALUES (32, 'Sand Pile ', 'Structure & Infrastructure', '3', 'Structure', 0, NULL);
INSERT INTO `anti_pattern_info` VALUES (33, 'Knot Service', 'Structure & Infrastructure', '3', 'Structure', 0, NULL);
INSERT INTO `anti_pattern_info` VALUES (34, 'Hub-like Dependency', 'Structure & Infrastructure', '3', 'Structure', 1, 'static');
INSERT INTO `anti_pattern_info` VALUES (35, 'Unstable Dependency', 'Structure & Infrastructure', '3', 'Structure', 0, NULL);
INSERT INTO `anti_pattern_info` VALUES (36, 'Implicit Cross-module Dependency', 'Structure & Infrastructure', '3', 'Structure', 0, NULL);
INSERT INTO `anti_pattern_info` VALUES (37, 'Microservice Greedy', 'Decomposition', '4', 'Functionality', 1, 'static');
INSERT INTO `anti_pattern_info` VALUES (38, 'Wrong Cuts', 'Decomposition', '4', 'Functionality', 1, 'static');
INSERT INTO `anti_pattern_info` VALUES (39, 'Duplicated Services', 'Decomposition', '4', 'Functionality', 0, NULL);
INSERT INTO `anti_pattern_info` VALUES (40, 'Scattered Functionality', 'Decomposition', '4', 'Functionality', 1, 'static');
INSERT INTO `anti_pattern_info` VALUES (41, 'Nobody Home', 'Decomposition', '4', 'Functionality', 0, NULL);
INSERT INTO `anti_pattern_info` VALUES (42, 'Sharing Persistence', 'Decomposition', '4', 'Service Data Management', 1, 'static');
INSERT INTO `anti_pattern_info` VALUES (43, 'Inappropriate Service Intimacy', 'Decomposition', '4', 'Service Data Management', 1, 'static&dynamic');
INSERT INTO `anti_pattern_info` VALUES (44, 'Own Crypto Code', 'Security', '5', 'Data and Communication', 0, NULL);
INSERT INTO `anti_pattern_info` VALUES (45, 'Non-encrypted Data Exposure', 'Security', '5', 'Data and Communication', 0, NULL);
INSERT INTO `anti_pattern_info` VALUES (46, 'Hardcoded Secrets', 'Security', '5', 'Data and Communication', 0, NULL);
INSERT INTO `anti_pattern_info` VALUES (47, 'Non-secured Service-to-service Communications', 'Security', '5', 'Data and Communication', 0, NULL);
INSERT INTO `anti_pattern_info` VALUES (48, 'Insufficient Access Control', 'Security', '5', 'Authentication and Authorization', 0, NULL);
INSERT INTO `anti_pattern_info` VALUES (49, 'Publicly Accessible Microservices', 'Security', '5', 'Authentication and Authorization', 0, NULL);
INSERT INTO `anti_pattern_info` VALUES (50, 'Unnecessary Privileges To Microservices', 'Security', '5', 'Authentication and Authorization', 0, NULL);
INSERT INTO `anti_pattern_info` VALUES (51, 'Unauthenticated Traffic', 'Security', '5', 'Authentication and Authorization', 0, NULL);
INSERT INTO `anti_pattern_info` VALUES (52, 'Multiple User Authentication', 'Security', '5', 'Authentication and Authorization', 0, NULL);
INSERT INTO `anti_pattern_info` VALUES (53, 'Centralized Authorization', 'Security', '5', 'Authentication and Authorization', 0, NULL);
INSERT INTO `anti_pattern_info` VALUES (54, 'Nonhomogeneous Adoption', 'Lifecycle Management', '6', 'Migration', 0, NULL);
INSERT INTO `anti_pattern_info` VALUES (55, 'Local Logging', 'Lifecycle Management', '6', 'Deployment', 0, NULL);
INSERT INTO `anti_pattern_info` VALUES (56, 'Multiple Services Per Deployment Unit', 'Lifecycle Management', '6', 'Deployment', 0, NULL);
INSERT INTO `anti_pattern_info` VALUES (57, 'Manual Configuration', 'Lifecycle Management', '6', 'Deployment', 0, NULL);
INSERT INTO `anti_pattern_info` VALUES (58, 'Single DevOps Toolchain', 'Lifecycle Management', '6', 'Deployment', 0, NULL);
INSERT INTO `anti_pattern_info` VALUES (59, 'Insufficient Monitoring', 'Lifecycle Management', '6', 'Operations', 0, NULL);
INSERT INTO `anti_pattern_info` VALUES (60, 'Manual Per Service Handling Of Network Failures', 'Lifecycle Management', '6', 'Operations', 0, NULL);
INSERT INTO `anti_pattern_info` VALUES (61, 'Weak Source Code And Knowledge Management', 'Lifecycle Management', '6', 'Operations', 0, NULL);
INSERT INTO `anti_pattern_info` VALUES (62, 'No Continuous Integration / Continuous Delivery (CI/CD)', 'Lifecycle Management', '6', 'Operations', 0, NULL);
INSERT INTO `anti_pattern_info` VALUES (63, 'No Health Check', 'Lifecycle Management', '6', 'Operations', 0, NULL);
INSERT INTO `anti_pattern_info` VALUES (64, 'Team Coupling', 'Team & Technology', '7', 'Team ', 0, NULL);
INSERT INTO `anti_pattern_info` VALUES (65, 'Common Ownership', 'Team & Technology', '7', 'Team ', 0, NULL);
INSERT INTO `anti_pattern_info` VALUES (66, 'Too Many Standards', 'Team & Technology', '7', 'Technology', 1, 'static');
INSERT INTO `anti_pattern_info` VALUES (67, 'Inadequate Techniques Support', 'Team & Technology', '7', 'Technology', 0, NULL);
INSERT INTO `anti_pattern_info` VALUES (68, 'Shared Libraries', 'Team & Technology', '7', 'Technology', 1, 'static');
INSERT INTO `anti_pattern_info` VALUES (69, 'Focus On Latest Technologies', 'Team & Technology', '7', 'Technology', 0, NULL);
INSERT INTO `anti_pattern_info` VALUES (70, 'Fragile Service', 'Dynamic', '8', 'Dynamic', 1, 'dynamic');
INSERT INTO `anti_pattern_info` VALUES (71, 'Uneven Load Distribution', 'Dynamic', '8', 'Dynamic', 1, 'dynamic');
INSERT INTO `anti_pattern_info` VALUES (72, 'Inconsistent Service Response', 'Dynamic', '8', 'Dynamic', 1, 'dynamic');
INSERT INTO `anti_pattern_info` VALUES (73, 'Resource Waste', 'Dynamic', '8', 'Dynamic', 1, 'dynamic');
INSERT INTO `anti_pattern_info` VALUES (74, 'Call Rate Anomaly', 'Dynamic', '8', 'Dynamic', 1, 'dynamic');
INSERT INTO `anti_pattern_info` VALUES (75, 'Uneven API Usage', 'Dynamic', '8', 'Dynamic', 1, 'dynamic');
INSERT INTO `anti_pattern_info` VALUES (76, 'High Frequency Of Slow Queries', 'Dynamic', '8', 'Dynamic', 1, 'dynamic');
INSERT INTO `anti_pattern_info` VALUES (77, 'N+1 Queries', 'Dynamic', '8', 'Dynamic', 1, 'dynamic');
INSERT INTO `anti_pattern_info` VALUES (78, 'Frequent GC', 'Dynamic', '8', 'Dynamic', 1, 'dynamic');
INSERT INTO `anti_pattern_info` VALUES (79, 'Long Time GC', 'Dynamic', '8', 'Dynamic', 1, 'dynamic');
INSERT INTO `anti_pattern_info` VALUES (80, 'Memory Jitter Of Service', 'Dynamic', '8', 'Dynamic', 1, 'dynamic');
INSERT INTO `anti_pattern_info` VALUES (81, 'Uneven logic processing', 'Dynamic', '8', 'Dynamic', 1, 'dynamic');
INSERT INTO `anti_pattern_info` VALUES (82, 'Falling Dominoes', 'Dynamic', '8', 'Dynamic', 1, 'dynamic');
INSERT INTO `anti_pattern_info` VALUES (83, 'Unnecessary Processing', 'Dynamic', '8', 'Dynamic', 1, 'dynamic');
INSERT INTO `anti_pattern_info` VALUES (84, 'The Ramp', 'Dynamic', '8', 'Dynamic', 1, 'dynamic');

-- ----------------------------
-- Table structure for interval_window_mapping
-- ----------------------------
DROP TABLE IF EXISTS `interval_window_mapping`;
CREATE TABLE `interval_window_mapping`  (
  `id` int(11) UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
  `name` varchar(255) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL COMMENT '名称',
  `value` int(11) NULL DEFAULT NULL COMMENT '值',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 4 CHARACTER SET = utf8 COLLATE = utf8_general_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of interval_window_mapping
-- ----------------------------
INSERT INTO `interval_window_mapping` VALUES (1, 'bsd_dynamic_interval', 60);
INSERT INTO `interval_window_mapping` VALUES (2, 'bsd_dynamic_cur_window', 60);
INSERT INTO `interval_window_mapping` VALUES (3, 'bsd_dynamic_time_window', 600);

-- ----------------------------
-- Table structure for path_mapping_service_item
-- ----------------------------
DROP TABLE IF EXISTS `path_mapping_service_item`;
CREATE TABLE `path_mapping_service_item`  (
  `service_name` varchar(128) CHARACTER SET utf8 COLLATE utf8_general_ci NOT NULL COMMENT '微服务名称',
  `path_in_local_repository` varchar(255) CHARACTER SET utf8 COLLATE utf8_general_ci NOT NULL,
  `repository_belong_to` varchar(128) CHARACTER SET utf8 COLLATE utf8_general_ci NOT NULL COMMENT '所属仓库名',
  PRIMARY KEY (`service_name`, `repository_belong_to`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8 COLLATE = utf8_general_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of path_mapping_service_item
-- ----------------------------
INSERT INTO `path_mapping_service_item` VALUES ('cloud-admin-service', 'D:\\aaaaa\\PropertyManagementCloud\\admin-service', 'PropertyManagementCloud');
INSERT INTO `path_mapping_service_item` VALUES ('cloud-car-park-service', 'D:\\aaaaa\\PropertyManagementCloud\\car-park-service', 'PropertyManagementCloud');
INSERT INTO `path_mapping_service_item` VALUES ('cloud-house-service', 'D:\\aaaaa\\PropertyManagementCloud\\house-service', 'PropertyManagementCloud');
INSERT INTO `path_mapping_service_item` VALUES ('cloud-property-service', 'D:\\aaaaa\\PropertyManagementCloud\\property-service', 'PropertyManagementCloud');
INSERT INTO `path_mapping_service_item` VALUES ('cloud-user-service', 'D:\\aaaaa\\PropertyManagementCloud\\user-service', 'PropertyManagementCloud');
INSERT INTO `path_mapping_service_item` VALUES ('routeservice', 'D:\\aaaaa\\Service-New-Demo2\\RouteService', 'Service-New-Demo2');
INSERT INTO `path_mapping_service_item` VALUES ('travelservice', 'D:\\aaaaa\\Service-New-Demo2\\TravelService', 'Service-New-Demo2');

SET FOREIGN_KEY_CHECKS = 1;
