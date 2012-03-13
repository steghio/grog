<?php
require_once("check.php");
check_session();
require_once("utilities.php");

//String url, String appid, String type, String manifest

exec("java -jar ./tmp/it.eng.paas.telnet.jar 192.168.23.27 {$_POST["appid"]} start ./tmp/{$_POST["appid"]}.MF 0");



header('Location: profile.php');

?>
