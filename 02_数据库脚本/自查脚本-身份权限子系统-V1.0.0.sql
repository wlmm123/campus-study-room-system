-- ============================================================
-- 身份权限子系统 建库自查脚本 V1.0.0
-- 用法（在本目录执行，口令交互输入，避免长命令被输入法吞字）：
--   "C:\Program Files\MySQL\MySQL Server 8.0\bin\mysql.exe" -uroot -p -t < 自查脚本-身份权限子系统-V1.0.0.sql
-- ============================================================
USE auth_db;

SELECT '1. 三表行数（期望 2 / 2 / 2）' AS check_item;
SELECT
    (SELECT COUNT(*) FROM sys_user)      AS sys_user_cnt,
    (SELECT COUNT(*) FROM sys_role)      AS sys_role_cnt,
    (SELECT COUNT(*) FROM sys_user_role) AS sys_user_role_cnt;

SELECT '2. 角色字典（期望 STUDENT / ADMIN）' AS check_item;
SELECT id, role_code, role_name FROM sys_role ORDER BY id;

SELECT '3. 账号与角色绑定（期望 1->STUDENT, 2->ADMIN）' AS check_item;
SELECT u.id, u.username, u.real_name, r.role_code
FROM sys_user u
LEFT JOIN sys_user_role ur ON ur.user_id = u.id
LEFT JOIN sys_role r       ON r.id = ur.role_id
ORDER BY u.id;

SELECT '4. 账号状态（期望 status 全为 1=启用）' AS check_item;
SELECT id, username, status, is_deleted FROM sys_user ORDER BY id;

SELECT '5. 表引擎与字符集（期望 InnoDB / utf8mb4）' AS check_item;
SELECT table_name, engine, table_collation
FROM information_schema.tables
WHERE table_schema = 'auth_db' AND table_name IN ('sys_user','sys_role','sys_user_role');
