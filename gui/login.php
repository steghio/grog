<?php
require_once("db.php");
require_once("check.php");
check_login($_POST["username"], $_POST["pwd"]);
?>
