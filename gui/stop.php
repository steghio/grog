<?php
require_once("check.php");
check_session();
require_once("utilities.php");

/*
java -jar SendStopAppMessage
*/

header('Location: manage.php');

?>
